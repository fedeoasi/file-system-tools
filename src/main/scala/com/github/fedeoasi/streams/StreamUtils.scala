package com.github.fedeoasi.streams

import java.time.Instant

import akka.event.Logging
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Keep, RunnableGraph, Sink, Source}
import akka.stream.{ActorMaterializer, Attributes, ClosedShape}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.control.NonFatal

object StreamUtils {
  private val BatchSize = 10000

  def doAndReport[A, B](seq: Seq[A], transform: A => Option[B])(implicit mat: ActorMaterializer): Future[Seq[B]] = {
    val startTime = Instant.now()
    println(s"Starting at $startTime")

    val processingFlow = Flow[Option[B]].collect { case Some(value) => value }
      .toMat(Sink.seq)(Keep.right)

    val inputSize = seq.size

    val countingFlow = Flow[Option[B]]
      .scan(0) { case (acc, _) => acc + 1 } // Like fold but it does not wait for completion
      .groupedWithin(BatchSize, 3.seconds)
      .map(_.max)
      .toMat(Sink.foreach { count =>
        val now = Instant.now()
        val elapsed = java.time.Duration.between(startTime, now)
        val nanosToCompletion = (elapsed.getNano.toLong / count) * (inputSize - count)
        val estimatedCompletionTime = now.plusNanos(nanosToCompletion)
        println(s"Processed ($count/$inputSize) elements. elapsed=$elapsed estimatedCompletionTime=$estimatedCompletionTime")
      })(Keep.left)

    val graph = RunnableGraph.fromGraph(GraphDSL.create(processingFlow, countingFlow)((_, _)) { implicit builder =>
      (s1, s2) =>
        import GraphDSL.Implicits._

        val broadcast = builder.add(Broadcast[Option[B]](2))
        val source = Source.fromIterator(() => seq.iterator)
          .log("doAndReport")
          .withAttributes(
            Attributes.logLevels(
              onElement = Logging.DebugLevel,
              onFinish = Logging.InfoLevel,
              onFailure = Logging.WarningLevel
            )
          )
          .map(transform)
          .recover { case NonFatal(ex) => println(ex); None }

        source ~> broadcast.in
        broadcast.out(0) ~> s1
        broadcast.out(1) ~> s2
        ClosedShape
    })

    val (result, _) = graph.run()
    result
  }

}
