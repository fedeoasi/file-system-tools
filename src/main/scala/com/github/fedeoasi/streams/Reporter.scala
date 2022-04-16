package com.github.fedeoasi.streams

import akka.NotUsed
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Keep, RunnableGraph, Sink, Source}
import akka.stream.{ClosedShape, Materializer}
import com.github.fedeoasi.output.Logging
import com.github.fedeoasi.streams.Reporter.ProgressReport

import java.time.Instant
import scala.concurrent.duration._

class LoggingReporter(batchSize: Int = 1000000, reportingInterval: FiniteDuration = 3.seconds)
  extends Reporter(LoggingReporter.log, batchSize, reportingInterval)

object LoggingReporter extends Logging {
  def log(report: ProgressReport): Unit = {
    info(s"Processed (${report.processedCount}/${report.totalCount}) elements. elapsed=${report.elapsedTime} " +
      s"estimatedCompletionTime=${report.estimatedCompletionTime}")
  }
}

class Reporter(
  consumeReport: ProgressReport => Unit,
  batchSize: Int = 1000000,
  reportingInterval: FiniteDuration = 3.seconds) extends Logging {

  def transformWithProgressReport[A, B, R](
    seq: Seq[A], transformFlow: Flow[A, B, NotUsed], processingSink: Sink[B, R])(implicit mat: Materializer): R = {

    val startTime = Instant.now()

    val inputSize = seq.size
    info(s"Starting to process $inputSize elements in actor system ${mat.system.name}")

    val progressSink = Flow[B]
      .scan(0) { case (acc, _) => acc + 1 } // Like fold but it does not wait for completion
      .groupedWithin(batchSize, reportingInterval)
      .map(_.max)
      .toMat(Sink.foreach { count =>
        val now = Instant.now()
        val elapsed = java.time.Duration.between(startTime, now)
        consumeReport(ProgressReport(now, count, inputSize, elapsed))
      })(Keep.left)

    val graph = RunnableGraph.fromGraph(GraphDSL.createGraph(processingSink, progressSink)((_, _)) { implicit builder =>
      (s1, s2) =>
        import GraphDSL.Implicits._

        val broadcast = builder.add(Broadcast[B](2))
        val source = Source.fromIterator[A](() => seq.iterator).via(transformFlow)

        source ~> broadcast.in
        broadcast.out(0) ~> s1
        broadcast.out(1) ~> s2
        ClosedShape
    })

    val (result, _) = graph.run()
    result
  }
}

object Reporter {
  case class ProgressReport(reportedTime: Instant, processedCount: Long, totalCount: Long, elapsedTime: java.time.Duration) {
    def estimatedCompletionTime: Instant = {
      val millisToCompletion = (elapsedTime.toMillis / processedCount) * (totalCount - processedCount)
      reportedTime.plusMillis(millisToCompletion)
    }
  }
}