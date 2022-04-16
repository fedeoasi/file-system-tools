package com.github.fedeoasi

import java.time.Instant

import com.github.fedeoasi.Model.{DirectoryEntry, FileEntry}
import com.github.fedeoasi.catalog.EntryPersistence
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers


class EntryPersistenceTest extends AnyFunSpec with Matchers with TemporaryFiles {
  private val now = Instant.now()
  private val entries = Seq(
    DirectoryEntry("/", "root", now),
    FileEntry("/root", "file.txt", Some("5dd39cab1c53c2c77cd352983f9641e1"), 20L, now))

  it("persists and retrieves a sequence of entries") {
    withTmpFile("entries", "csv") { tmpFile =>
      EntryPersistence.write(entries, tmpFile)
      EntryPersistence.read(tmpFile) shouldBe entries
    }
  }

  it("incrementally adds a new entry") {
    withTmpFile("entries", "csv") { tmpFile =>
      EntryPersistence.write(entries, tmpFile)
      val newEntry = FileEntry("/root", "file2.txt", Some("5dd39cab1c53c2c77cd352983f9641e1"), 20L, now)
      EntryPersistence.write(Seq(newEntry), tmpFile, append = true)
      EntryPersistence.read(tmpFile) shouldBe entries ++ Seq(newEntry)
    }
  }
}
