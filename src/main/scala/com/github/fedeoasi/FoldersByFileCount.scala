package com.github.fedeoasi

import java.nio.file.{Path, Paths}
import java.text.NumberFormat

import com.github.fedeoasi.Model._
import com.github.fedeoasi.cli.CatalogAndFolderConfig
import com.github.fedeoasi.collection.TopKFinder
import scopt.OptionParser

trait FoldersByStat extends Logging {
  def stats(
    root: Path,
    entriesByParent: Map[String, Seq[FileSystemEntry]],
    compute: Seq[FileEntry] => Long): Map[String, Long] = {

    // Gather directories so that they are sorted by level in reverse order, so that
    // we can rely on the statistics for a child to be present when we compute statistics
    // for a folder
    def gather(folder: String, allFolders: Seq[String]): Seq[String] = {
      val entries = entriesByParent.getOrElse(folder, Seq.empty)
      val folders = DirectoryEntries(entries)
      folders.flatMap(d => gather(d.path, allFolders)) :+ folder
    }

    val orderedFolders = gather(root.toFile.getAbsolutePath, Seq.empty)
    orderedFolders.foldLeft(Map.empty[String, Long]) { case (countsByFolder, folder) =>
      val entries = entriesByParent.getOrElse(folder, Seq.empty)
      val folders = DirectoryEntries(entries)
      val files = FileEntries(entries)
      val count = compute(files) + folders.map(f => countsByFolder(f.path)).sum
      countsByFolder.updated(folder, count)
    }
  }
}

object FoldersByFileCount extends FoldersByStat {
  private val parser = new OptionParser[CatalogAndFolderConfig](getClass.getSimpleName) {
    head(getClass.getSimpleName)

    opt[String]('c', "catalog")
      .action { case (catalog, config) => config.copy(catalog = Some(Paths.get(catalog))) }
      .text("The catalog file (csv)")
      .required()

    opt[String]('f', "folder")
      .action { case (folder, config) => config.copy(folder = Some(Paths.get(folder))) }
      .text("The root folder to analyze")
      .required()

    help("help").text("prints this usage text")
  }

  def fileCounts(root: Path, entriesByParent: Map[String, Seq[FileSystemEntry]]): Map[String, Long] = {
    stats(root, entriesByParent, _.size)
  }

  /** Ranks folders by file counts using the given folder as root.
    *
    * @param args [CATALOG] [FOLDER]
    */
  def main(args: Array[String]): Unit = {
    parser.parse(args, CatalogAndFolderConfig()) match {
      case Some(CatalogAndFolderConfig(Some(catalog), Some(folder))) =>
        val entries = EntryPersistence.read(catalog)
        val entriesByParent = entries.groupBy(_.parent)
        val countByFolder = fileCounts(folder, entriesByParent)
        info(new TopKFinder(countByFolder.toSeq).top(50)(Ordering.by(_._2)).mkString("\n"))
      case _ =>
    }
  }
}

object FoldersByFileSize extends FoldersByStat {
  private val parser = new OptionParser[CatalogAndFolderConfig](getClass.getSimpleName) {
    head(getClass.getSimpleName)

    opt[String]('c', "catalog")
      .action { case (catalog, config) => config.copy(catalog = Some(Paths.get(catalog))) }
      .text("The catalog file (csv)")
      .required()

    opt[String]('f', "folder")
      .action { case (folder, config) => config.copy(folder = Some(Paths.get(folder))) }
      .text("The root folder to analyze")
      .required()

    help("help").text("prints this usage text")
  }

  def fileSizes(root: Path, entriesByParent: Map[String, Seq[FileSystemEntry]]): Map[String, Long] = {
    stats(root, entriesByParent, _.map(_.size).sum)
  }

  /** Ranks folders by file counts using the given folder as root.
    *
    * @param args [CATALOG] [FOLDER]
    */
  def main(args: Array[String]): Unit = {
    parser.parse(args, CatalogAndFolderConfig()) match {
      case Some(CatalogAndFolderConfig(Some(catalog), Some(folder))) =>
        val entries = EntryPersistence.read(catalog)
        val entriesByParent = entries.groupBy(_.parent)
        val sizeByFolder = fileSizes(folder, entriesByParent)
        info(new TopKFinder(sizeByFolder.toSeq).top(20)(Ordering.by(_._2)).map { case (folderPath, totalSize) =>
          folderPath -> NumberFormat.getIntegerInstance.format(totalSize) }
          .mkString("\n"))
      case _ =>
    }
  }
}