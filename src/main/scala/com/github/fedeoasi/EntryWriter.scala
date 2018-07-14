package com.github.fedeoasi

import java.io.Closeable
import java.nio.file.Path

import com.github.fedeoasi.Model.{DirectoryEntry, FileEntry, FileSystemEntry}
import com.github.tototoshi.csv.CSVWriter

class EntryWriter(catalog: Path) extends Closeable {
  import Entries._

  private val writer = {
    val fileExisted = catalog.toFile.exists()
    val writer = CSVWriter.open(catalog.toFile, fileExisted)
    if (!fileExisted) {
      writer.writeRow(header)
    }
    writer
  }

  def write(entry: FileSystemEntry): Unit = {
    writer.writeRow(toCsvSeq(entry))
  }

  override def close(): Unit = writer.close()

  private def toCsvSeq(entry: FileSystemEntry): Seq[Any] = {
    entry match {
      case FileEntry(parent, name, md5, size, modifiedTime) =>
        Seq("F", parent, name, md5.getOrElse(""), size, modifiedTime.toEpochMilli)
      case DirectoryEntry(parent, name, modifiedTime) =>
        Seq("D", parent, name, "", "", modifiedTime.toEpochMilli)
    }
  }
}
