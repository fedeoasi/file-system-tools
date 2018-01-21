package com.github.fedeoasi

import com.github.fedeoasi.Model.FileEntries

object FilesBySize {
  /** Ranks files by size. */
  def main(args: Array[String]): Unit = {
    val metadataFile = if (args.nonEmpty) args(0) else Constants.DefaultMetadataFile
    val entries = EntryPersistence.read(metadataFile)
    val files = FileEntries(entries)
    val largestFiles = files.sortBy(_.size).reverse.take(50)
    println(largestFiles.mkString("\n"))
  }
}

object TotalSize {
  def main(args: Array[String]): Unit = {
    val metadataFile = if (args.nonEmpty) args(0) else Constants.DefaultMetadataFile
    val entries = EntryPersistence.read(metadataFile)
    val totalSize = FileEntries(entries).map(_.size).sum
    println(s"The total size in bytes is $totalSize")
  }
}