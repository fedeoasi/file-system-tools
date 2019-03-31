package com.github.fedeoasi.streams

import org.scalatest.{FunSpec, Matchers}
import StreamUtils._
import akka.stream.scaladsl.Sink

import scala.concurrent.Await
import scala.concurrent.duration._


class StreamUtilsTest extends FunSpec with Matchers {
  it("reports") {
    val result = withMaterializer("StreamUtilsTest") { implicit materializer =>
      doAndReport(1 to 100, (n: Int) => n * 2, Sink.fold[Int, Int](0)(_ + _))
    }
    Await.result(result, 1.second) shouldBe 10100
  }
}
