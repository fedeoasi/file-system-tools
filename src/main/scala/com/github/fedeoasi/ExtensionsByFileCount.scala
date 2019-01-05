package com.github.fedeoasi

import java.nio.file.Path

import com.github.fedeoasi.Model.{FileEntries, FileEntry, FileSystemEntry}
import com.github.fedeoasi.cli.{CatalogConfig, CatalogConfigParsing, CliCommand}
import com.github.fedeoasi.collection.TopKFinder

object ExtensionsByFileCount extends CatalogConfigParsing with Logging {
  override val command = CliCommand("extensions-by-filecount", "Rank file extensions by count.")
  case class TopExtensionsResult(extension: String, count: Int, uniqueCount: Int)

  def groupByExtension(entries: Seq[FileSystemEntry]): Map[String, Seq[FileEntry]] = {
    val files = FileEntries(entries)
    val filesAndExtensions = files.collect { case f if f.extension.isDefined => (f, f.extension.get.toLowerCase) }
    filesAndExtensions.groupBy(_._2).mapValues(_.map(_._1))
  }

  def groupByExtension(catalog: Path): Map[String, Seq[FileEntry]] = {
    groupByExtension(EntryPersistence.read(catalog))
  }

  def topExtensions(catalog: Path, k: Int = 20): Seq[TopExtensionsResult] = {
    val filesByExtension = groupByExtension(catalog)
    val countsByExtension = filesByExtension.transform { (_, files) =>
      (files.size, files.map(_.md5).toSet.size)
    }.toSeq
    new TopKFinder(countsByExtension).top(20)(Ordering.by(_._2._1)).map {
      case (ext, (count, uniqueCount)) => TopExtensionsResult(ext, count, uniqueCount)
    }
  }

  /** Ranks extensions by number of files. */
  def main(args: Array[String]): Unit = {
    parser.parse(args, CatalogConfig()) match {
      case Some(CatalogConfig(Some(catalog))) =>
        info(topExtensions(catalog)
          .map { result => s"${result.extension}: ${result.count} files (${result.uniqueCount} unique)" }
          .mkString("\n"))
      case _ =>
    }
  }
}
