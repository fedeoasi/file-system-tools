package com.github.fedeoasi

import java.text.NumberFormat

import com.github.fedeoasi.Model.{FileEntries, FileEntry}
import com.github.fedeoasi.cli.{CatalogConfig, CatalogConfigParsing}

object FilesBySize extends CatalogConfigParsing {
  /** Ranks files by size. */
  def main(args: Array[String]): Unit = {
    parser.parse(args, CatalogConfig()) match {
      case Some(CatalogConfig(Some(catalog))) =>
        val entries = EntryPersistence.read(catalog)
        val files = FileEntries(entries)
        val largestFiles = new TopKFinder(files).top(20)(Ordering.by[FileEntry, Long](_.size))
        println(largestFiles.mkString("\n"))
      case _ =>
    }
  }
}

object TotalSize extends CatalogConfigParsing {
  def main(args: Array[String]): Unit = {
    parser.parse(args, CatalogConfig()) match {
      case Some(CatalogConfig(Some(catalog))) =>
        val entries = EntryPersistence.read(catalog)
        val totalSize = FileEntries(entries).map(_.size).sum
        println(s"The total size in bytes is ${NumberFormat.getIntegerInstance.format(totalSize)}")
      case _ =>
    }
  }
}