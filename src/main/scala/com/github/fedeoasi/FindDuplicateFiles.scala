package com.github.fedeoasi

import java.nio.file.Paths

import com.github.fedeoasi.Model.{FileEntries, FileEntry, FileSystemEntry}
import com.github.fedeoasi.cli.CatalogConfig
import scopt.OptionParser

object FindDuplicateFiles {
  //TODO Make this return a nice report case class instead of Unit
  def findDuplicates(entries: Seq[FileSystemEntry]): Unit = {
    val files = entries.collect { case f: FileEntry if f.md5.nonEmpty => f }
    println(files.size)
    val duplicatesByMd5 = files.groupBy(_.md5).filter {
      case (_, entriesForMd5) =>
        entriesForMd5.size > 1//&& entriesForMd5.size < 5
    }

    val duplicatesByMd5LessThanK = duplicatesByMd5.filter(_._2.size < 5)
    val topK = duplicatesByMd5LessThanK.toSeq
      .sortBy(_._2.size)
      .reverse
      .take(100)
    println(topK.map(e => e._2.mkString("\n")).mkString("\n\n"))

    println()


    val duplicateCountByFolder = duplicatesByMd5.values.toSeq.flatten.groupBy(_.parent).transform {
      case (_, filesForFolder) =>
        filesForFolder.size
    }
    val foldersAndDuplicateCounts = duplicateCountByFolder.toSeq.sortBy(_._2).reverse.take(10)
    println(foldersAndDuplicateCounts.mkString("\n"))
  }

  private val parser = new OptionParser[CatalogConfig](getClass.getSimpleName) {
    head(getClass.getSimpleName)

    opt[String]('c', "catalog")
      .action { case (catalog, config) => config.copy(catalog = Some(Paths.get(catalog))) }
      .text("The catalog file (csv)")

    help("help").text("prints this usage text")
  }

  def main(args: Array[String]): Unit = {
    parser.parse(args, CatalogConfig()) match {
      case Some(CatalogConfig(optionalCatalog)) =>
        val catalog = optionalCatalog.getOrElse(Constants.DefaultCatalogPath)
        val files = FileEntries(EntryPersistence.read(catalog))
        findDuplicates(files)
      case _ =>
    }
  }
}
