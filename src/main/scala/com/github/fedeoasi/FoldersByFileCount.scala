package com.github.fedeoasi

import java.nio.file.{Path, Paths}

import com.github.fedeoasi.Model._
import scopt.OptionParser

case class FoldersByFileConfig(catalog: Option[Path] = None, folder: Option[Path] = None)

object FoldersByFileCount {
  private val parser = new OptionParser[FoldersByFileConfig](getClass.getSimpleName) {
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

  def fileCounts(root: Path, entriesByParent: Map[String, Seq[FileSystemEntry]]): Map[String, Int] = {
    //Gather directories so that they are sorted by level in reverse order, so that
    //we can rely on the file count for a child to be present when we compute the
    //file count for a folder
    def gather(folder: String, allFolders: Seq[String]): Seq[String] = {
      val entries = entriesByParent.getOrElse(folder, Seq.empty)
      val folders = DirectoryEntries(entries)
      folders.flatMap(d => gather(d.path, allFolders)) :+ folder
    }

    val orderedFolders = gather(root.toFile.getAbsolutePath, Seq.empty)
    orderedFolders.foldLeft(Map.empty[String, Int]) { case (countsByFolder, folder) =>
      val entries = entriesByParent.getOrElse(folder, Seq.empty)
      val folders = DirectoryEntries(entries)
      val files = FileEntries(entries)
      val count = files.size + folders.map(f => countsByFolder(f.path)).sum
      countsByFolder.updated(folder, count)
    }
  }

  def fileCount(root: String, entriesByParent: Map[String, Seq[FileSystemEntry]]): Int = {
    val entries = entriesByParent.getOrElse(root, Seq.empty)
    val folders = DirectoryEntries(entries)
    val files = FileEntries(entries)
    files.size + folders.foldLeft(0) { case (count, folder) => count + fileCount(folder.path, entriesByParent) }
  }

  /** Ranks folders by file counts using the given folder as root.
    *
    * @param args [CATALOG] [FOLDER]
    */
  def main(args: Array[String]): Unit = {
    parser.parse(args, FoldersByFileConfig()) match {
      case Some(FoldersByFileConfig(Some(catalog), Some(folder))) =>
        val entries = EntryPersistence.read(catalog)
        val entriesByParent = entries.groupBy(_.parent)
        val countByFolder = fileCounts(folder, entriesByParent)
        println(new TopKFinder(countByFolder.toSeq).top(50)(Ordering.by(_._2)).mkString("\n"))
      case _ =>
    }
  }
}
