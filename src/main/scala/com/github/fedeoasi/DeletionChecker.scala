package com.github.fedeoasi

import java.io.File
import java.nio.file.Paths

import com.github.fedeoasi.Model.FileSystemEntry

object DeletionChecker {
  def findDeletions(entries: Seq[FileSystemEntry]): (Seq[FileSystemEntry], Seq[FileSystemEntry]) = {
    entries.partition(e => new File(e.path).exists())
  }

  /** Performs deletions on the catalog
    */
  def main(args: Array[String]): Unit = {
    val catalog = Paths.get(Constants.DefaultCatalogFilename)
    val entries = EntryPersistence.read(catalog)
    val (toKeep, toDelete) = findDeletions(entries)
    println(s"Found ${toDelete.size} entries to delete")
    EntryPersistence.write(toKeep, catalog)
  }
}
