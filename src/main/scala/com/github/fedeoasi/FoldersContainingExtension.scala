package com.github.fedeoasi

import com.github.fedeoasi.ExtensionsByFileCount.groupByExtension

object FoldersContainingExtension {
  /** Prints folders that contain a given extension.
    *
    * @param args [EXTENSION]
    */
  def main(args: Array[String]): Unit = {
    val extension = args(0)
    val entries = EntryPersistence.read(Constants.DefaultCatalogFilename)
    val filesByExtension = groupByExtension(entries)
    val filesForExtension = filesByExtension.getOrElse(extension, Seq.empty)
    val folders = filesForExtension.groupBy(_.parent)
    val countsByFolder = folders.mapValues(_.size)
    println(countsByFolder.toSeq.sortBy(_._2).reverse.take(20).mkString("\n"))
  }
}
