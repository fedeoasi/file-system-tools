package com.github.fedeoasi

import java.nio.file.Path
import java.time.Instant

import com.github.fedeoasi.Model.{DirectoryEntry, FileEntry, FileSystemEntry}
import com.github.fedeoasi.catalog.{EntryReader, EntryWriter}
import org.scalatest.{FunSpec, Matchers}
import resource.managed

class EntryWriterTest extends FunSpec with Matchers with TemporaryFiles {
  private val now = Instant.now()
  private val entries = Seq(
    DirectoryEntry("/", "root", now),
    FileEntry("/root", "file.txt", Some("5dd39cab1c53c2c77cd352983f9641e1"), 20L, now))

  it("persists a sequence of entries") {
    withTempDir("entries") { tmpDir =>
      val tmpFile = tmpDir.resolve(generateCatalogFilename())
      writeEntries(tmpFile, entries)
      readEntries(tmpFile) shouldBe entries
    }
  }

  it("incrementally adds a new entry") {
    withTempDir("entries") { tmpDir =>
      val tmpFile = tmpDir.resolve(generateCatalogFilename())
      writeEntries(tmpFile, entries)
      val newEntry = FileEntry("/root", "file2.txt", Some("5dd39cab1c53c2c77cd352983f9641e1"), 20L, now)
      writeEntries(tmpFile, Seq(newEntry))
      readEntries(tmpFile) shouldBe entries ++ Seq(newEntry)
    }
  }

  private def writeEntries(tmpFile: Path, entries: Seq[FileSystemEntry]): Unit = {
    managed(new EntryWriter(tmpFile)).acquireAndGet { writer =>
      entries.foreach(writer.write)
    }
  }

  private def readEntries(tmpFile: Path): Seq[FileSystemEntry] = {
    new EntryReader(tmpFile).readEntries()
  }
}
