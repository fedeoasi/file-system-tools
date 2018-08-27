package com.github.fedeoasi

import java.time.Instant

object Model {
  trait FileSystemEntry {
    def parent: String
    def name: String
    def modifiedTime: Instant
    def path: String = {
      val separator = if (!parent.endsWith(java.io.File.separator)) java.io.File.separator else ""
      parent + separator + name
    }
  }

  case class DirectoryEntry(parent: String, name: String, modifiedTime: Instant) extends FileSystemEntry

  case class FileEntry(
    parent: String,
    name: String,
    md5: Option[String],
    size: Long,
    modifiedTime: Instant) extends FileSystemEntry {

    def extension: Option[String] = name.split("\\.").toSeq.lastOption

    //This assumes a UNIX file system where the root and separator are '/'
    def ancestors: Seq[String] = {
      val parts = parent.split("/").filterNot(_.isEmpty)
      val sb = new StringBuilder()
      parts.foldLeft(Seq.empty[String]) {
        case (result, part) =>
          sb.append("/").append(part).toString() +: result
      }.reverse
    }
  }

  object FileEntries {
    def apply(entries: Seq[FileSystemEntry]): Seq[FileEntry] = entries.collect { case f: FileEntry => f }
  }

  object DirectoryEntries {
    def apply(entries: Seq[FileSystemEntry]): Seq[DirectoryEntry] = entries.collect { case d: DirectoryEntry => d }
  }
}
