package com.github.fedeoasi

import java.io.File
import java.nio.file.Paths

import com.github.fedeoasi.Model.FileSystemEntry

object DeletionChecker {
  def findDeletions(entries: Seq[FileSystemEntry]): (Seq[FileSystemEntry], Seq[FileSystemEntry]) = {
    entries.partition(e => new File(e.path).exists())
  }

  /** Updated the metadata file performing deletions
    */
  def main(args: Array[String]): Unit = {
    val entriesFile = Paths.get(Constants.DefaultMetadataFile)
    val entries = EntryPersistence.read(entriesFile)
    val (toKeep, toDelete) = findDeletions(entries)
    println(s"Found ${toDelete.size} entries to delete")
    EntryPersistence.write(toKeep, entriesFile)
  }
}
