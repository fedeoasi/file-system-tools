package com.github.fedeoasi

import java.time.Instant

import com.github.fedeoasi.DiffFolders.ancestors
import com.github.fedeoasi.Model.{DirectoryEntry, FileEntry}
import org.scalatest.{FunSpec, Matchers}

class FileEntryTest extends FunSpec with Matchers {
  private val instant = Instant.now

  private val root = DirectoryEntry("/catalog", "root", instant)
  private val nested1 = DirectoryEntry(root.path, "nested1", instant)
  private val folder = DirectoryEntry(nested1.path, "folder", instant)

  private val uniqueFile = FileEntry(folder.path, "file.txt", Some("file_md5"), 1000, instant)

  it("finds the ancestors for a file") {
    ancestors(uniqueFile) should contain theSameElementsAs Seq(root.parent, root.path, nested1.path, folder.path)
  }
}
