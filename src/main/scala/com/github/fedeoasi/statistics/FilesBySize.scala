package com.github.fedeoasi.statistics

import java.text.NumberFormat

import com.github.fedeoasi.Model.{FileEntries, FileEntry}
import com.github.fedeoasi.catalog.EntryPersistence
import com.github.fedeoasi.cli.{CatalogConfig, CatalogConfigParsing, CliCommand}
import com.github.fedeoasi.collection.TopKFinder
import com.github.fedeoasi.output.Logging
import wvlet.log._

object FilesBySize extends CatalogConfigParsing with Logging {
  override val command = CliCommand("files-by-size", "Rank files by size.")
  /** Ranks files by size. */
  def main(args: Array[String]): Unit = {
    parser.parse(args, CatalogConfig()) match {
      case Some(CatalogConfig(Some(catalog))) =>
        val entries = EntryPersistence.read(catalog)
        val files = FileEntries(entries)
        val largestFiles = new TopKFinder(files).top(20)(Ordering.by[FileEntry, Long](_.size))
        info(largestFiles.mkString("\n"))
      case _ =>
    }
  }
}

object TotalSize extends CatalogConfigParsing with LogSupport {
  override val command = CliCommand("total-size", "Compute total catalog size.")
  def main(args: Array[String]): Unit = {
    parser.parse(args, CatalogConfig()) match {
      case Some(CatalogConfig(Some(catalog))) =>
        val entries = EntryPersistence.read(catalog)
        val totalSize = FileEntries(entries).map(_.size).sum
        info(s"The total size in bytes is ${NumberFormat.getIntegerInstance.format(totalSize)}")
      case _ =>
    }
  }
}