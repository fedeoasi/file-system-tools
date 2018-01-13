package com.github.fedeoasi

object Model {
  trait FileSystemEntry {
    def parent: String
    def name: String
    def path: String = parent + java.io.File.separator + name
  }

  case class DirectoryEntry(parent: String, name: String) extends FileSystemEntry

  case class FileEntry(parent: String, name: String, md5: String) extends FileSystemEntry {
    def extension: Option[String] = name.split("\\.").toSeq.lastOption
  }

  object FileEntries {
    def apply(entries: Seq[FileSystemEntry]): Seq[FileEntry] = entries.collect { case f: FileEntry => f }
  }

  object DirectoryEntries {
    def apply(entries: Seq[FileSystemEntry]): Seq[DirectoryEntry] = entries.collect { case d: DirectoryEntry => d }
  }
}
