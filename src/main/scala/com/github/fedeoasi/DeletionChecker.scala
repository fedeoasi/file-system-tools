package com.github.fedeoasi

import java.io.File
import java.nio.file.{Path, Paths}

import com.github.fedeoasi.Model.FileSystemEntry
import scopt.OptionParser

object DeletionChecker {
  case class CatalogConfig(catalog: Option[Path] = None)

  //TODO make sure that the the parent folder exists, otherwise we will wipe out a catalog if you forgot to
  //connect your hard drive
  def findDeletions(entries: Seq[FileSystemEntry]): (Seq[FileSystemEntry], Seq[FileSystemEntry]) = {
    entries.partition(e => new File(e.path).exists())
  }

  private val parser = new OptionParser[CatalogConfig](getClass.getSimpleName) {
    head(getClass.getSimpleName)

    opt[String]('c', "catalog")
      .action { case (catalog, config) => config.copy(catalog = Some(Paths.get(catalog))) }
      .text("The catalog file (csv)")

    help("help").text("prints this usage text")
  }

  /** Performs deletions on the catalog
    */
  def main(args: Array[String]): Unit = {
    parser.parse(args, CatalogConfig()) match {
      case Some(CatalogConfig(optionalCatalog)) =>
        val catalog = optionalCatalog.getOrElse(Constants.DefaultCatalogPath)
        val entries = EntryPersistence.read(catalog)
        val (toKeep, toDelete) = findDeletions(entries)
        println(s"Found ${toDelete.size} entries to delete")
        EntryPersistence.write(toKeep, catalog)
      case _ =>
    }
  }
}
