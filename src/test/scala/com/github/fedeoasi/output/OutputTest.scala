package com.github.fedeoasi.output

import com.github.fedeoasi.TemporaryFiles
import org.mockito.Mockito._
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import wvlet.log.Logger

import scala.io.Source

class OutputTest extends AnyFunSpec with Matchers with TemporaryFiles with MockitoSugar {
  it("outputs to a file") {
    val logger = mock[Logger]
    withTempDir("output") { dir =>
      val outputFile = dir.resolve("output.csv")
      Output(Some(outputFile), logger).write(Seq("hello"))
      Source.fromFile(outputFile.toFile).getLines().mkString shouldBe "hello"
    }
  }

  it("outputs to stdout") {
    val logger = mock[Logger]
    Output(None, logger).write(Seq("hello"))
    verify(logger, times(1)).info("hello")
  }
}
