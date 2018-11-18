package com.github.fedeoasi

import java.time.Instant

import com.github.fedeoasi.Model.{DirectoryEntry, FileEntry}
import org.scalatest.{FunSpec, Matchers}

class FolderSimilarityTest extends FunSpec with Matchers with SparkTest {
  import FolderSimilarity._

  private val instant = Instant.now
  private val root = DirectoryEntry("/catalog", "root", instant)
  private val folder1 = DirectoryEntry(root.path, "folder1", instant)
  private val folder2 = DirectoryEntry(root.path, "folder2", instant)
  private val folder3 = DirectoryEntry(root.path, "folder3", instant)

  private val uniqueFile1 = FileEntry(folder1.path, "file.txt", Some("file_md5"), 1000, instant)
  private val uniqueFile2 = FileEntry(folder2.path, "file2.txt", Some("file2_md5"), 1000, instant)
  private val commonFile1 = FileEntry(folder1.path, "dup.txt", Some("dup_md5"), 1000, instant)
  private val commonFile2 = FileEntry(folder2.path, "dup.txt", Some("dup_md5"), 1000, instant)
  private val commonFile3 = FileEntry(folder3.path, "dup.txt", Some("dup_md5"), 1000, instant)

  private val allEntries = Seq(
    root, folder1, folder2, folder3,
    uniqueFile1, uniqueFile2, commonFile1, commonFile2, commonFile3)

  private lazy val similarities = folderSimilarities(sparkContext, allEntries).collect

  it("only compares the same folder pair once") {
    similarities.foreach { case ((f1, f2), _) =>
      f1.entry.path should be < f2.entry.path
    }
  }

  it("sorts by descending similarity") {
    similarities.sliding(2).foreach {
      case Array(((_, _), score1), ((_, _), score2)) => score1.cosineSimilarity should be >= score2.cosineSimilarity
    }
  }

  it("considers folder1 and folder2 the most dissimilar") {
    similarities.last should matchPattern {
      case ((f1: Folder, f2: Folder), _) if f1.entry.name == "folder1" && f2.entry.name == "folder2" =>
    }
  }
}
