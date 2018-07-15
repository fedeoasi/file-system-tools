package com.github.fedeoasi

import org.scalatest.{FunSpec, Matchers}

class EntryIndexTest extends FunSpec with Matchers {
  private val entryIndex = new EntryIndex(Map(
    "/root" -> Seq("file.txt"),
    "/root/nested" -> Seq("file2.txt", "file3.txt")
  ))

  it("does not find a non existent folder") {
    entryIndex.contains("/folder") shouldBe false
  }

  it("does not find a file in a non existent folder") {
    entryIndex.contains("/folder/file.txt") shouldBe false
  }

  it("finds an existing folder") {
    entryIndex.contains("/root") shouldBe true
  }

  it("finds an existing file") {
    entryIndex.contains("/root/nested/file3.txt") shouldBe true
  }
}
