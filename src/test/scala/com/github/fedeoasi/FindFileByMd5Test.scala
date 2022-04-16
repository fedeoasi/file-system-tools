package com.github.fedeoasi

import java.time.Instant

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import com.github.fedeoasi.search.FindFileByMd5._
import com.github.fedeoasi.Model.FileEntry

class FindFileByMd5Test extends AnyFunSpec with Matchers {
  private val file1 = FileEntry("/parentFolder", "file", Some("5dd39cab1c53c2c77cd352983f9641e1"), 100, Instant.now)
  private val file2 = FileEntry("/parentFolder", "file2", Some("eb90f5d5b37050cfb86c6b36b41cda85"), 100, Instant.now)
  private val fileEntries = Seq(file1, file2)

  it("finds a file by md5") {
    find(fileEntries, "5dd39cab1c53c2c77cd352983f9641e1") shouldBe Seq(file1)
  }

  it("finds another file by md5") {
    find(fileEntries, "eb90f5d5b37050cfb86c6b36b41cda85") shouldBe Seq(file2)
  }

  it("does not find a non existent md5") {
    find(fileEntries, "298asdfasdfasdfasdfasdfaasdfasdf") shouldBe Seq.empty
  }
}
