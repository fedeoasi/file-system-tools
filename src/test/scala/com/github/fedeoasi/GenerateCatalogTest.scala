package com.github.fedeoasi

import java.io.File
import java.nio.file.Paths
import java.time.Instant

import com.github.fedeoasi.catalog.GenerateCatalog.GenerateCatalogReport
import com.github.fedeoasi.Model.{DirectoryEntry, FileEntry}
import com.github.fedeoasi.catalog.{EntryPersistence, GenerateCatalog}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class GenerateCatalogTest extends AnyFunSpec with Matchers with TemporaryFiles {
  private val folder = Paths.get("src/test/resources/root-folder")
  private val entries = Seq(
    directory("src/test/resources", "root-folder"),
    file("src/test/resources/root-folder", "file.txt", Some("5276effc61dd44a9fe1d5354bf2ad9c4"), 14),
    directory("src/test/resources/root-folder", "folder"),
    file("src/test/resources/root-folder/folder", "file2.txt", Some("cb5da54c7ac2f4da9dcdf0d9d9955179"), 20)
  )

  it("generates the catalog for a nested folder structure") {
    withTempDir("catalog") { tempDir =>
      val tmpFile = tempDir.resolve(generateCatalogFilename())
      val report = GenerateCatalog.generateMetadata(folder, tmpFile, populateMd5 = true)
      report shouldBe GenerateCatalogReport(4L, 4L)
      EntryPersistence.read(tmpFile) should contain theSameElementsAs entries
    }
  }

  it("generates the catalog for a nested folder structure without md5") {
    val entries = Seq(
      directory("src/test/resources", "root-folder"),
      file("src/test/resources/root-folder", "file.txt", None, 14),
      directory("src/test/resources/root-folder", "folder"),
      file("src/test/resources/root-folder/folder", "file2.txt", None, 20)
    )
    withTmpFile("catalog", "csv") { tmpFile =>
      EntryPersistence.write(Seq.empty, tmpFile)
      val report = GenerateCatalog.generateMetadata(folder, tmpFile, populateMd5 = false)
      report shouldBe GenerateCatalogReport(4L, 4L)
      EntryPersistence.read(tmpFile) should contain theSameElementsAs entries
    }
  }

  it("reads the existing structure and does not add any entries") {
    withTmpFile("catalog", "csv") { tmpFile =>
      EntryPersistence.write(Seq.empty, tmpFile)
      GenerateCatalog.generateMetadata(folder, tmpFile, populateMd5 = true)
      val report = GenerateCatalog.generateMetadata(folder, tmpFile, populateMd5 = true)
      report shouldBe GenerateCatalogReport(0L, 4L)
      EntryPersistence.read(tmpFile) should contain theSameElementsAs entries
    }
  }

  private def file(parent: String, name: String, md5: Option[String], size: Int): FileEntry = {
    FileEntry(parent, name, md5, size, modifiedDate(parent + File.separator + name))
  }

  private def directory(parent: String, name: String): DirectoryEntry = {
    DirectoryEntry(parent, name, modifiedDate(parent + File.separator + name))
  }

  private def modifiedDate(path: String): Instant = Instant.ofEpochMilli(new File(path).lastModified())
}
