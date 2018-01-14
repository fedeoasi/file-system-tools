package com.github.fedeoasi

import com.github.fedeoasi.Model.FileEntries

object FilesBySize {
  /** Ranks files by size. */
  def main(args: Array[String]): Unit = {
    val entries = EntryPersistence.read(Constants.DefaultMetadataFile)
    val files = FileEntries(entries)
    val largestFiles = files.sortBy(_.size).reverse.take(50)
    println(largestFiles.mkString("\n"))
  }
}
