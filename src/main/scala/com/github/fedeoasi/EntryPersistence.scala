package com.github.fedeoasi

import java.nio.file.{Path, Paths}

import com.github.fedeoasi.Model.{DirectoryEntry, FileSystemEntry, FileEntry}
import com.github.tototoshi.csv.{CSVReader, CSVWriter}
import resource._

object EntryPersistence {
  val EntryTypeField = "Type"
  val ParentField = "Parent"
  val NameField = "Name"
  val Md5Field = "MD5"
  val SizeField = "Size"

  def read(filename: String): Seq[FileSystemEntry] = {
    val entriesFile = Paths.get(filename)
    read(entriesFile)
  }

  def read(file: Path): Seq[FileSystemEntry] = {
    managed(CSVReader.open(file.toFile)).acquireAndGet { reader =>
      reader.allWithHeaders().map { row =>
        row(EntryTypeField) match {
          case "F" => FileEntry(row(ParentField), row(NameField), row(Md5Field), row(SizeField).toLong)
          case "D" => DirectoryEntry(row(ParentField), row(NameField))
          case other => throw new RuntimeException(s"Unrecognized entry type $other")
        }
      }
    }
  }

  def write(entries: Seq[FileSystemEntry], toFile: Path, append: Boolean = false): Unit = {
    val header = Seq(EntryTypeField, ParentField, NameField, Md5Field, SizeField)
    val csvEntries = toCsvSeq(entries)
    val allEntries = if (!append) Seq(header) ++ csvEntries else csvEntries
    writeCsv(allEntries, toFile, append)
  }

  private def toCsvSeq(entries: Seq[FileSystemEntry]): Seq[Seq[Any]] = {
    entries.map {
      case FileEntry(parent, name, md5, size) => Seq("F", parent, name, md5, size)
      case DirectoryEntry(parent, name) => Seq("D", parent, name, "")
    }
  }


  private def writeCsv(rows: Seq[Seq[Any]], file: Path, append: Boolean = false): Unit = {
    managed(CSVWriter.open(file.toFile, append = append)).acquireAndGet { writer =>
      writer.writeAll(rows)
    }
  }
}
