package com.github.fedeoasi

import org.scalatest.{FunSpec, Matchers}

class StringUtilsTest extends FunSpec with Matchers {
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
