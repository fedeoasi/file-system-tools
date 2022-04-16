package com.github.fedeoasi

import java.nio.file.Path
import java.time.Instant

import com.github.fedeoasi.Model.{DirectoryEntry, FileEntry, FileSystemEntry}
import com.github.fedeoasi.catalog.{EntryReader, EntryWriter}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import resource.managed

class EntryReaderTest extends AnyFunSpec with Matchers with TemporaryFiles {
  private val now = Instant.now()
  private val entries = Seq(
    DirectoryEntry("/", "root", now),
    FileEntry("/root", "file.txt", Some("5dd39cab1c53c2c77cd352983f9641e1"), 20L, now),
    DirectoryEntry("/root", "nested", now),
    FileEntry("/root/nested", "file2.txt", Some("eb90f5d5b37050cfb86c6b36b41cda85"), 30L, now))

  it("reads the entry count") {
    withTempDir("entries") { tmpDir =>
      val tmpFile = tmpDir.resolve(generateCatalogFilename())
      writeEntries(tmpFile, entries)
      new EntryReader(tmpFile).count() shouldBe 4
    }
  }

  it("reads all entries") {
    withTempDir("entries") { tmpDir =>
      val tmpFile = tmpDir.resolve(generateCatalogFilename())
      writeEntries(tmpFile, entries)
      new EntryReader(tmpFile).readEntries() shouldBe entries
    }
  }

  it("reads all paths") {
    withTempDir("entries") { tmpDir =>
      val tmpFile = tmpDir.resolve(generateCatalogFilename())
      writeEntries(tmpFile, entries)
      new EntryReader(tmpFile).readPaths() should contain theSameElementsAs Seq(
        "/root", "/root/file.txt", "/root/nested", "/root/nested/file2.txt")
    }
  }

  it("reads the folder paths") {
    withTempDir("entries") { tmpDir =>
      val tmpFile = tmpDir.resolve(generateCatalogFilename())
      writeEntries(tmpFile, entries)
      new EntryReader(tmpFile).readFolderPaths() should contain theSameElementsAs Seq(
        "/root", "/root/nested")
    }
  }

  it("reads an entry index") {
    withTempDir("entries") { tmpDir =>
      val tmpFile = tmpDir.resolve(generateCatalogFilename())
      writeEntries(tmpFile, entries)
      new EntryReader(tmpFile).readIndex().filesByDir shouldBe Map(
        "/root" -> Seq("file.txt"),
        "/root/nested" -> Seq("file2.txt")
      )
    }
  }

  private def writeEntries(tmpFile: Path, entries: Seq[FileSystemEntry]): Unit = {
    managed(new EntryWriter(tmpFile)).acquireAndGet { writer =>
      entries.foreach(writer.write)
    }
  }
}
