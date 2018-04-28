package com.github.fedeoasi

import java.nio.file.{Path, Paths}

import com.github.fedeoasi.Model.{DirectoryEntry, FileSystemEntry}
import scopt.OptionParser

object FolderByName {
  case class FolderByNameConfig(folderName: Option[String] = None, catalog: Option[Path] = None)

  def findFoldersByName(folderName: String, entries: Seq[FileSystemEntry]): Seq[DirectoryEntry] = {
    entries.collect { case d: DirectoryEntry if d.name.equalsIgnoreCase(folderName) => d }
  }

  private val parser = new OptionParser[FolderByNameConfig](getClass.getSimpleName) {
    head(getClass.getSimpleName)

    opt[String]('n', "folderName").required()
      .action { case (folderName, config) => config.copy(folderName = Some(folderName)) }
      .text("The root folder from which to generate a catalog")

    opt[String]('c', "catalog")
      .action { case (catalog, config) => config.copy(catalog = Some(Paths.get(catalog))) }
      .text("The catalog file (csv)")

    help("help").text("prints this usage text")
  }

  /** Finds a folder by name (case insensitive) */
  def main(args: Array[String]): Unit = {
    parser.parse(args, FolderByNameConfig()) match {
      case Some(FolderByNameConfig(Some(folderName), Some(catalog))) =>
        val entries = EntryPersistence.read(catalog)
        val matchedFolders = findFoldersByName(folderName, entries)
        println(matchedFolders.mkString("\n"))
      case _ =>
    }
  }
}
