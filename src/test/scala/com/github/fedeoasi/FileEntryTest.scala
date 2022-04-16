package com.github.fedeoasi

import java.time.Instant

import com.github.fedeoasi.Model.{DirectoryEntry, FileEntry}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class FileEntryTest extends AnyFunSpec with Matchers {
  private val instant = Instant.now

  private val root = DirectoryEntry("/catalog", "root", instant)
  private val nested1 = DirectoryEntry(root.path, "nested1", instant)
  private val folder = DirectoryEntry(nested1.path, "folder", instant)

  private val uniqueFile = FileEntry(folder.path, "file.txt", Some("file_md5"), 1000, instant)

  it("finds the ancestors for a file") {
    uniqueFile.ancestors should contain theSameElementsAs Seq(root.parent, root.path, nested1.path, folder.path)
  }
}
