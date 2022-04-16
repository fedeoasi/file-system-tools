package com.github.fedeoasi

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class StringUtilsTest extends AnyFunSpec with Matchers {
  import com.github.fedeoasi.utils.StringUtils._

  describe("longest prefix") {
    it("finds a prefix") {
      longestPrefix("abcd", "abce") shouldBe "abc"
    }

    it("does not find a prefix") {
      longestPrefix("eabcd", "dabce") shouldBe ""
    }
  }
}
