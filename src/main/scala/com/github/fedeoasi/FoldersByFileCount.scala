package com.github.fedeoasi

import java.nio.file.Paths

import com.github.fedeoasi.Model._

object FoldersByFileCount {
  def fileCounts(root: String, entriesByParent: Map[String, Seq[FileSystemEntry]]): Map[String, Int] = {
    //Gather directories so that they are sorted by level in reverse order, so that
    //we can rely on the file count for a child to be present when we compute the
    //file count for a folder
    def gather(folder: String, allFolders: Seq[String]): Seq[String] = {
      val entries = entriesByParent.getOrElse(folder, Seq.empty)
      val folders = DirectoryEntries(entries)
      folders.flatMap(d => gather(d.path, allFolders)) :+ folder
    }

    val orderedFolders = gather(root, Seq.empty)
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
    val catalog = args(0)
    val folder = args(1)
    val entries = EntryPersistence.read(catalog)

    val entriesByParent = entries.groupBy(_.parent)

    val countByFolder = fileCounts(folder, entriesByParent)

    println(countByFolder.toSeq.sortBy(_._2).reverse.take(100).mkString("\n"))
  }
}
