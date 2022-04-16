package com.github.fedeoasi

import java.io.FileOutputStream
import java.nio.file.Path

import com.github.fedeoasi.catalog.{DeletionChecker, EntryPersistence, GenerateCatalog}
import com.github.fedeoasi.catalog.DeletionChecker.DeletionCheckerResult
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

import resource._

class DeletionCheckerTest extends AnyFunSpec with Matchers with TemporaryFiles {
  it("does not find any deletion when nothing was deleted") {
    withTempDir() { contentDir =>
      withTempDir() { catalogDir =>
        writeToFile(contentDir.resolve("file.txt"), "Some content")
        writeToFile(contentDir.resolve("file2.txt"), "Some other content")

        val catalogFile = catalogDir.resolve("catalog.csv")
        GenerateCatalog.generateMetadata(contentDir, catalogFile, populateMd5 = false)
        val entries = EntryPersistence.read(catalogFile)

        val result = DeletionChecker.check(catalogFile, None)
        result shouldBe DeletionCheckerResult(Seq.empty, entries, Seq.empty)
      }
    }
  }

  it("finds a file that was deleted") {
    withTempDir() { contentDir =>
      withTempDir() { catalogDir =>
        writeToFile(contentDir.resolve("file.txt"), "Some content")
        val secondFile = contentDir.resolve("file2.txt")
        writeToFile(secondFile, "Some other content")

        val catalogFile = catalogDir.resolve("catalog.csv")
        GenerateCatalog.generateMetadata(contentDir, catalogFile, populateMd5 = false)
        val entries = EntryPersistence.read(catalogFile)

        secondFile.toFile.delete()

        val result = DeletionChecker.check(catalogFile, None)
        result shouldBe DeletionCheckerResult(Seq.empty, entries.filterNot(_.name == "file2.txt"), entries.find(_.name == "file2.txt").toSeq)
      }
    }
  }

  it("does not find any deletion in a nested folder") {
    withTempDir() { contentDir =>
      withTempDir() { catalogDir =>
        val nestedDir = contentDir.resolve("nested")
        nestedDir.toFile.mkdir()
        writeToFile(contentDir.resolve("file.txt"), "Some content")
        writeToFile(contentDir.resolve("file2.txt"), "Some other content")
        writeToFile(nestedDir.resolve("file.txt"), "Some content")
        writeToFile(nestedDir.resolve("file2.txt"), "Some other content")

        val catalogFile = catalogDir.resolve("catalog.csv")
        GenerateCatalog.generateMetadata(contentDir, catalogFile, populateMd5 = false)
        val entries = EntryPersistence.read(catalogFile)

        val result = DeletionChecker.check(catalogFile, Some(nestedDir))
        result shouldBe DeletionCheckerResult(
          entries.filterNot(_.path.contains("nested")),
          entries.filter(_.path.contains("nested")),
          Seq.empty)
      }
    }
  }

  it("finds a deletion in the given nested folder") {
    withTempDir() { contentDir =>
      withTempDir() { catalogDir =>
        val nestedDir = contentDir.resolve("nested")
        nestedDir.toFile.mkdir()
        writeToFile(contentDir.resolve("file.txt"), "Some content")
        writeToFile(contentDir.resolve("file2.txt"), "Some other content")
        writeToFile(nestedDir.resolve("file.txt"), "Some content")
        writeToFile(nestedDir.resolve("file2.txt"), "Some other content")

        val catalogFile = catalogDir.resolve("catalog.csv")
        GenerateCatalog.generateMetadata(contentDir, catalogFile, populateMd5 = false)
        val entries = EntryPersistence.read(catalogFile)

        nestedDir.resolve("file2.txt").toFile.delete()

        val result = DeletionChecker.check(catalogFile, Some(nestedDir))
        result shouldBe DeletionCheckerResult(
          entries.filterNot(_.path.contains("nested")),
          entries.filter(e => e.path.contains("nested") && e.name != "file2.txt"),
          entries.find(_.path == nestedDir.resolve("file2.txt").toString).toSeq)
      }
    }
  }

  describe("Command line arguments") {
    it("Removes a file that was deleted") {
      withTempDir() { contentDir =>
        withTempDir() { catalogDir =>
          writeToFile(contentDir.resolve("file.txt"), "Some content")
          val secondFile = contentDir.resolve("file2.txt")
          writeToFile(secondFile, "Some other content")

          val catalogFile = catalogDir.resolve("catalog.csv")
          GenerateCatalog.generateMetadata(contentDir, catalogFile, populateMd5 = false)
          val entries = EntryPersistence.read(catalogFile)

          secondFile.toFile.delete()

          DeletionChecker.main(Array("--catalog", catalogFile.toString))

          val updatedEntries = EntryPersistence.read(catalogFile)
          updatedEntries should contain theSameElementsAs entries.filterNot(_.name == "file2.txt")
        }
      }
    }

    it("does not find any deletion in a nested folder") {
      withTempDir() { contentDir =>
        withTempDir() { catalogDir =>
          val nestedDir = contentDir.resolve("nested")
          nestedDir.toFile.mkdir()
          writeToFile(contentDir.resolve("file.txt"), "Some content")
          writeToFile(contentDir.resolve("file2.txt"), "Some other content")
          writeToFile(nestedDir.resolve("file.txt"), "Some content")
          writeToFile(nestedDir.resolve("file2.txt"), "Some other content")

          val catalogFile = catalogDir.resolve("catalog.csv")
          GenerateCatalog.generateMetadata(contentDir, catalogFile, populateMd5 = false)
          val entries = EntryPersistence.read(catalogFile)

          DeletionChecker.main(Array(
            "--catalog", catalogFile.toString,
            "--folder", nestedDir.toString
          ))

          val updatedEntries = EntryPersistence.read(catalogFile)
          updatedEntries should contain theSameElementsAs entries
        }
      }
    }
  }

  def writeToFile(file: Path, text: String): Unit = {
    managed(new FileOutputStream(file.toFile)).acquireAndGet { fos => fos.write(text.getBytes) }
  }
}
