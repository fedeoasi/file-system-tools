package com.github.fedeoasi

import java.nio.file.{Path, Paths}
import java.util.function.Consumer

import com.github.fedeoasi.Model.FileSystemEntry
import org.scalatest.{FunSpec, Matchers}

class FindDuplicateFilesForFolderTest extends FunSpec with Matchers with TemporaryFiles {
  class EntryAccumulator extends Consumer[FileSystemEntry] {
    private var _entries = Seq.empty[FileSystemEntry]

    override def accept(entry: FileSystemEntry): Unit = {
      _entries = entry +: _entries
    }

    def entries = _entries.reverse
  }

  private val findDuplicatesDir: Path = Paths.get("src/test/resources/FindDuplicates")
  withTempDir() { tmpDir =>
    val tmpFile = tmpDir.resolve(generateCatalogFilename())
    val accumulator = new EntryAccumulator
    GenerateCatalog.generateMetadata(findDuplicatesDir, tmpFile, true, accumulator)
    val entries = EntryPersistence.read(tmpFile)
    val noDuplicatesDir = findDuplicatesDir.resolve("NoDuplicates")

    it("does not find duplicates within the folder") {
      val finder = new FindDuplicateFilesWithinFolder(entries, noDuplicatesDir)
      finder.filesAndDuplicates.map { case (filePath, duplicates) => filePath -> duplicates.map(_.path.toString) } shouldBe Seq.empty
    }

    it("finds a duplicate outside of the given folder") {
      val finder = new FindDuplicateFilesForFolder(entries, noDuplicatesDir)
      val file = noDuplicatesDir.resolve("d.txt")
      finder.filesAndDuplicates.map { case (filePath, duplicates) => filePath -> duplicates.map(_.path.toString) } shouldBe Seq(
        file.toString -> Seq(findDuplicatesDir.resolve("OtherFolder").resolve("e.txt").toString)
      )
    }
  }
}
