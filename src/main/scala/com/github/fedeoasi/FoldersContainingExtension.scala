package com.github.fedeoasi

import java.nio.file.{Path, Paths}

import com.github.fedeoasi.ExtensionsByFileCount.groupByExtension
import com.github.fedeoasi.Model.{FileEntry, FileSystemEntry}
import scopt.OptionParser

object FoldersContainingExtension extends Logging {

  def foldersContainingExtension(entries: Seq[FileSystemEntry], extension: String): Map[String, Seq[FileEntry]] = {
    groupByExtension(entries).getOrElse(extension, Seq.empty).groupBy(_.parent)
  }

  case class FoldersContainingExtensionConfig(extension: Option[String] = None, catalog: Option[Path] = None)

  private val parser = new OptionParser[FoldersContainingExtensionConfig](getClass.getSimpleName) {
    head(getClass.getSimpleName)

    opt[String]('e', "extension")
      .action { case (extension, config) => config.copy(extension = Some(extension)) }
      .text("The extension to search for")
      .required()
    opt[String]('c', "catalog")
      .action { case (catalog, config) => config.copy(catalog = Some(Paths.get(catalog))) }
      .text("The catalog file (csv)")
      .required()

    help("help").text("prints this usage text")
  }

  /** Prints folders that contain a given extension. */
  def main(args: Array[String]): Unit = {
    parser.parse(args, FoldersContainingExtensionConfig()) match {
      case Some(FoldersContainingExtensionConfig(Some(extension), Some(catalog))) =>
        val entries = EntryPersistence.read(catalog)
        val folders = foldersContainingExtension(entries, extension)
        val countsByFolder = folders.mapValues(_.size)
        info(countsByFolder.toSeq.sortBy(_._2).reverse.take(20).mkString("\n"))
      case _ =>
    }
  }
}
