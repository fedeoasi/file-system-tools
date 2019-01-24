package com.github.fedeoasi

import java.nio.file.Paths

import com.github.fedeoasi.Model.FileEntry
import com.github.fedeoasi.catalog.{EntryPersistence, GenerateCatalog}
import com.github.fedeoasi.deduplication.DuplicateFilesFinder
import org.scalatest.{FunSpec, Matchers}

class FindDuplicateFilesTest extends FunSpec with Matchers with TemporaryFiles {
  private val baseDir = Paths.get("src/test/resources/FindDuplicates")

  withTempDir() { tmpDir =>
    val tmpFile = tmpDir.resolve(generateCatalogFilename())
    GenerateCatalog.generateMetadata(baseDir, tmpFile, populateMd5 = true)
    val entries = EntryPersistence.read(tmpFile)
    val noDuplicatesDir = baseDir.resolve("NoDuplicates")
    val dirWithDuplicates = baseDir.resolve("DuplicatesWithin")
    val otherFolder = baseDir.resolve("OtherFolder")

    it("finds all duplicates") {
      val finder = new DuplicateFilesFinder(entries)
      paths(finder.filesAndDuplicates) should contain theSameElementsAs Seq(
        dirWithDuplicates.resolve("b.txt").toString -> Seq(dirWithDuplicates.resolve("c.txt").toString),
        noDuplicatesDir.resolve("d.txt").toString -> Seq(otherFolder.resolve("e.txt").toString)
      )
    }

    it("finds all duplicates for a folder") {
      val finder = new DuplicateFilesFinder(entries, Some(noDuplicatesDir))
      paths(finder.filesAndDuplicates) should contain theSameElementsAs Seq(
        noDuplicatesDir.resolve("d.txt").toString -> Seq(otherFolder.resolve("e.txt").toString)
      )
    }

    it("finds all duplicates for a folder that itself contains duplicates") {
      val finder = new DuplicateFilesFinder(entries, Some(dirWithDuplicates))
      paths(finder.filesAndDuplicates) should contain theSameElementsAs Seq(
        dirWithDuplicates.resolve("b.txt").toString -> Seq(dirWithDuplicates.resolve("c.txt").toString)
      )
    }

    it("finds the largest duplicate") {
      val finder = new DuplicateFilesFinder(entries)
      paths(finder.largestDuplicates(Some(1))) should contain theSameElementsAs Seq(
        noDuplicatesDir.resolve("d.txt").toString -> Seq(otherFolder.resolve("e.txt").toString)
      )
    }

    it("only finds one largest duplicate for a folder with just one duplicate") {
      val finder = new DuplicateFilesFinder(entries, Some(dirWithDuplicates))
      paths(finder.largestDuplicates(Some(3))) should contain theSameElementsAs Seq(
        dirWithDuplicates.resolve("b.txt").toString -> Seq(dirWithDuplicates.resolve("c.txt").toString)
      )
    }

    it("finds the largest duplicates") {
      val finder = new DuplicateFilesFinder(entries)
      paths(finder.largestDuplicates(Some(10))) should contain theSameElementsAs Seq(
        noDuplicatesDir.resolve("d.txt").toString -> Seq(otherFolder.resolve("e.txt").toString),
        dirWithDuplicates.resolve("b.txt").toString -> Seq(dirWithDuplicates.resolve("c.txt").toString)
      )
    }
  }

  private def paths(input: Seq[(FileEntry, Seq[FileEntry])]): Seq[(String, Seq[String])] = {
    input.map { case (file, duplicates) => file.path -> duplicates.map(_.path.toString) }
  }
}
