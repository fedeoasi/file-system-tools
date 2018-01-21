package com.github.fedeoasi

import com.github.fedeoasi.Model.{FileEntries, FileEntry, FileSystemEntry}

object ExtensionsByFileCount {
  def groupByExtension(entries: Seq[FileSystemEntry]): Map[String, Seq[FileEntry]] = {
    val files = FileEntries(entries)
    val filesAndExtensions = files.collect { case f if f.extension.isDefined => (f, f.extension.get.toLowerCase) }
    filesAndExtensions.groupBy(_._2).mapValues(_.map(_._1))
  }

  /** Ranks extensions by number of files. */
  def main(args: Array[String]): Unit = {
    val metadataFile = if (args.nonEmpty) args(0) else Constants.DefaultMetadataFile
    val entries = EntryPersistence.read(metadataFile)
    val filesByExtension = groupByExtension(entries)
    val countsByExtension = filesByExtension.transform { (_, files) =>
      (files.size, files.map(_.md5).toSet.size)
    }.toSeq.sortBy(_._2._1).reverse
    println(countsByExtension.take(50).mkString("\n"))
  }
}


