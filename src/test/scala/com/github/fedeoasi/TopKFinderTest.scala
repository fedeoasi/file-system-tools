package com.github.fedeoasi

import com.github.fedeoasi.collection.TopKFinder
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class TopKFinderTest extends AnyFunSpec with Matchers {
  it("does not find the top in an empty sequence") {
    new TopKFinder(Seq.empty[Int]).top(3) shouldBe Seq.empty
  }

  it("finds the top three elements in a sequence of ten") {
    new TopKFinder(1 to 10).top(3) shouldBe Seq(10, 9, 8)
  }

  it("finds two elements when asked for three and only two are available") {
    new TopKFinder(1 to 2).top(3) shouldBe Seq(2, 1)
  }
}
