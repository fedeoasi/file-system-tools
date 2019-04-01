package com.github.fedeoasi.streams

import org.scalatest.{FunSpec, Matchers}
import StreamUtils._
import akka.stream.scaladsl.{Flow, Sink}

import scala.concurrent.Await
import scala.concurrent.duration._

import scala.collection.immutable

class ReporterTest extends FunSpec with Matchers {
  it("reports using a non trivial flow and a simple sink") {

    val result = withMaterializer("StreamUtilsTest") { implicit materializer =>
      new LoggingReporter().processAndReport(1 to 100, Flow[Int].statefulMapConcat[Int] { () =>
        var state = 0
        n: Int =>
          state += n * 2
          immutable.Iterable(state)
      }, Sink.last[Int])
    }
    Await.result(result, 1.second) shouldBe 10100
  }

  it("reports using a simple flow and a non trivial sink") {
    val result = withMaterializer("StreamUtilsTest") { implicit materializer =>
      new LoggingReporter().processAndReport(1 to 100, Flow[Int].map(n => n * 2), Sink.fold[Int, Int](0)(_ + _))
    }
    Await.result(result, 1.second) shouldBe 10100
  }
}
