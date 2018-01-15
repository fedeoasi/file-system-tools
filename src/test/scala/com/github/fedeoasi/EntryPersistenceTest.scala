package com.github.fedeoasi

import com.github.fedeoasi.Model.{DirectoryEntry, FileEntry}
import org.scalatest.{FunSpec, Matchers}

class EntryPersistenceTest extends FunSpec with Matchers with TemporaryFiles {
  val entries = Seq(
    DirectoryEntry("/", "root"),
    FileEntry("/root", "file.txt", "5dd39cab1c53c2c77cd352983f9641e1", 20L))

  it("persists and retrieves a sequence of entries") {
    withTmpFile("entries", "csv") { tmpFile =>
      EntryPersistence.write(entries, tmpFile)
      EntryPersistence.read(tmpFile) shouldBe entries
    }
  }

  it("incrementally adds a new entry") {
    withTmpFile("entries", "csv") { tmpFile =>
      EntryPersistence.write(entries, tmpFile)
      val newEntry = FileEntry("/root", "file2.txt", "5dd39cab1c53c2c77cd352983f9641e1", 20L)
      EntryPersistence.write(Seq(newEntry), tmpFile, append = true)
      EntryPersistence.read(tmpFile) shouldBe entries ++ Seq(newEntry)
    }
  }
}
