package com.github.fedeoasi.streams

import java.time.Instant

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Keep, RunnableGraph, Sink, Source}
import akka.stream.{ActorMaterializer, ClosedShape}
import com.github.fedeoasi.output.Logging

import scala.concurrent.duration._

object StreamUtils extends Logging {
  private val BatchSize = 1000000

  case class ProgressReport(reportedTime: Instant, processedCount: Long, totalCount: Long, elapsedTime: java.time.Duration) {
    def estimatedCompletionTime: Instant = {
      val millisToCompletion = (elapsedTime.toMillis / processedCount) * (totalCount - processedCount)
      reportedTime.plusMillis(millisToCompletion)
    }
  }

  def doAndReport[A, B, R](
    seq: Seq[A], transform: A => B, processingSink: Sink[B, R])(implicit mat: ActorMaterializer): R = {

    processAndReport(seq, transform, processingSink, report => {
      info(s"Processed (${report.processedCount}/${report.totalCount}) elements. elapsed=${report.elapsedTime} " +
        s"estimatedCompletionTime=${report.estimatedCompletionTime}")
    })
  }

  def processAndReport[A, B, R](
    seq: Seq[A], transform: A => B, processingSink: Sink[B, R], report: ProgressReport => Unit)(implicit mat: ActorMaterializer): R = {

    val startTime = Instant.now()

    val inputSize = seq.size
    info(s"Starting to process $inputSize elements in actor system ${mat.system.name}")

    val progressSink = Flow[B]
      .scan(0) { case (acc, _) => acc + 1 } // Like fold but it does not wait for completion
      .groupedWithin(BatchSize, 3.seconds)
      .map(_.max)
      .toMat(Sink.foreach { count =>
        val now = Instant.now()
        val elapsed = java.time.Duration.between(startTime, now)
        report(ProgressReport(now, count, inputSize, elapsed))
      })(Keep.left)

    val graph = RunnableGraph.fromGraph(GraphDSL.create(processingSink, progressSink)((_, _)) { implicit builder =>
      (s1, s2) =>
        import GraphDSL.Implicits._

        val broadcast = builder.add(Broadcast[B](2))
        val source = Source.fromIterator[A](() => seq.iterator).map(transform)

        source ~> broadcast.in
        broadcast.out(0) ~> s1
        broadcast.out(1) ~> s2
        ClosedShape
    })

    val (result, _) = graph.run()
    result
  }

  def withMaterializer[T](systemName: String)(f: ActorMaterializer => T): T = {
    implicit val system: ActorSystem = ActorSystem(systemName)
    try {
      implicit val materializer: ActorMaterializer = ActorMaterializer()
      f(materializer)
    } finally {
      info(s"shutting down actor system ${system.name}")
      system.terminate()
    }
  }
}
