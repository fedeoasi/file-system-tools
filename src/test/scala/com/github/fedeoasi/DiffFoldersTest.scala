package com.github.fedeoasi

import java.time.Instant

import com.github.fedeoasi.deduplication.FolderComparison.FolderDiff
import com.github.fedeoasi.Model.{DirectoryEntry, FileEntry}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import com.github.fedeoasi.deduplication.DiffFolders._

class DiffFoldersTest extends AnyFunSpec with Matchers with SparkTest {
  private val instant = Instant.now
  private val root = DirectoryEntry("/catalog", "root", instant)
  private val nested1 = DirectoryEntry(root.path, "nested1", instant)
  private val nested2 = DirectoryEntry(root.path, "nested2", instant)
  private val folder = DirectoryEntry(nested1.path, "folder", instant)
  private val otherFolder = DirectoryEntry(nested2.path, "folder", instant)

  private val uniqueFile = FileEntry(folder.path, "file.txt", Some("file_md5"), 1000, instant)
  private val duplicatedFile1 = FileEntry(folder.path, "dup.txt", Some("dup_md5"), 1000, instant)
  private val duplicatedFile2 = FileEntry(otherFolder.path, "dup.txt", Some("dup_md5"), 1000, instant)

  private val allFolders = Seq(root, nested1, nested2, folder, otherFolder)

  it("does not compare an empty set of entries") {
    diff(sparkContext, Seq.empty) shouldBe Seq.empty
  }

  it("does not compare a single entry") {
    diff(sparkContext, Seq(root)) shouldBe Seq.empty
  }

  it("diffs two identical folders") {
    diff(sparkContext, allFolders ++ Seq(duplicatedFile1, duplicatedFile2)) should contain theSameElementsAs Seq(
      FolderDiff(folder.path, otherFolder.path, Seq(duplicatedFile1), Seq.empty, Seq.empty, Seq.empty)
    )
  }

  it("diffs two different folders") {
    diff(sparkContext, allFolders ++ Seq(uniqueFile, duplicatedFile2)) should contain theSameElementsAs Seq(
      FolderDiff(folder.path, otherFolder.path, Seq.empty, Seq(uniqueFile), Seq(duplicatedFile2), Seq.empty)
    )
  }
}
