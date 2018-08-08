package com.github.fedeoasi

import java.nio.file.Paths

import com.github.fedeoasi.Model.FileEntry
import org.scalatest.{FunSpec, Matchers}

class FindDuplicatesTest extends FunSpec with Matchers with TemporaryFiles {
  private val baseDir = Paths.get("src/test/resources/FindDuplicates")

  withTempDir() { tmpDir =>
    val tmpFile = tmpDir.resolve(generateCatalogFilename())
    GenerateCatalog.generateMetadata(baseDir, tmpFile, populateMd5 = true)
    val entries = EntryPersistence.read(tmpFile)
    val noDuplicatesDir = baseDir.resolve("NoDuplicates")
    val dirWithDuplicates = baseDir.resolve("DuplicatesWithin")

    it("does not find duplicates within a folder that contains duplicates") {
      val finder = new FindDuplicateFilesWithinFolder(entries, dirWithDuplicates)
      paths(finder.filesAndDuplicates) shouldBe Seq(
        dirWithDuplicates.resolve("b.txt").toString -> Seq(dirWithDuplicates.resolve("c.txt").toString)
      )
    }

    it("does not find duplicates within a folder that does not contain duplicates") {
      val finder = new FindDuplicateFilesWithinFolder(entries, noDuplicatesDir)
      paths(finder.filesAndDuplicates) shouldBe Seq.empty
    }

    it("finds a duplicate outside of the given folder") {
      val finder = new FindDuplicateFilesForFolder(entries, noDuplicatesDir)
      val file = noDuplicatesDir.resolve("d.txt")
      paths(finder.filesAndDuplicates) shouldBe Seq(
        file.toString -> Seq(baseDir.resolve("OtherFolder").resolve("e.txt").toString)
      )
    }
  }

  private def paths(input: Seq[(String, Seq[FileEntry])]): Seq[(String, Seq[String])] = {
    input.map { case (filePath, duplicates) => filePath -> duplicates.map(_.path.toString) }
  }
}
