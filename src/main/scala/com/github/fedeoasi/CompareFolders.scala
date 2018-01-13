package com.github.fedeoasi

import com.github.fedeoasi.CompareFolders.diffFolders
import com.github.fedeoasi.Model.{DirectoryEntries, DirectoryEntry, FileEntries, FileEntry}

trait FolderComparison {
  case class FolderDiff(
    source: String,
    target: String,
    equalEntries: Set[(String, String)],
    differentEntries: Set[(String, String)])

  def diffFolders(source: String, target: String, files: Seq[FileEntry]): FolderDiff = {
    //println(s"Diffing folder $folder1 with $folder2")
    val sourceFiles = allFilesForFolder(files, source)
    val targetFiles = allFilesForFolder(files, target)

    val diff12 = sourceFiles.diff(targetFiles)
    val diff21 = targetFiles.diff(sourceFiles)

    //use relative paths for equal entries so that we can dedup
    FolderDiff(source, target, sourceFiles ++ targetFiles, diff12 ++ diff21)
  }

  def subFolders(folder: String, directories: Seq[DirectoryEntry]): Seq[DirectoryEntry] = {
    directories.filter(_.parent == folder)
  }

  def allFilesForFolder(files: Seq[FileEntry], folder: String): Set[(String, String)] = {
    val recursiveFiles = files.filter(_.path.startsWith(folder))
    recursiveFiles.map(f => (f.name, f.md5)).toSet
  }
}

object CompareFolders extends FolderComparison {
  /** Compare folders based on the contents of the contained folders
    * and files.
    *
    * @param args [FOLDER_1] [FOLDER_2]
    */
  def main(args: Array[String]): Unit = {
    val folder1 = args(0)
    val folder2 = args(1)
    val entries = EntryPersistence.read(Constants.DefaultMetadataFile)

    val files = FileEntries(entries)
    val directories = DirectoryEntries(entries)

    val directFolders1 = subFolders(folder1, directories)
    val directFolders2 = subFolders(folder2, directories)

    println(directFolders1.map(_.name))
    println(directFolders2.map(_.name))

    val folders2ByName = directFolders2.groupBy(_.name)

    directFolders1.foreach { dir1 =>
      folders2ByName.get(dir1.name) match {
        case Some(dir2) =>
          val diff = diffFolders(dir1.path, dir2.head.path, files)
          if (diff.differentEntries.isEmpty) {
            println(s"${dir1.name} is identical")
          }
        case None =>
      }
    }
  }
}

object FindIdenticalFolders {
  /** Find identical folders present in the metadata file. */
  def main(args: Array[String]): Unit = {
    val entries = EntryPersistence.read(Constants.DefaultMetadataFile)
    val entriesByParent = entries.groupBy(_.parent)
    val files = FileEntries(entries)
    val nonEmptyDirectories = DirectoryEntries(entries).filter(d => entriesByParent.contains(d.path))
    val duplicatesByName = nonEmptyDirectories.groupBy(_.name).filter(_._2.size > 1)
    println(s"There are ${nonEmptyDirectories.size} non empty directories and ${duplicatesByName.size} names that are duplicated")
    val folderDiffs = duplicatesByName.flatMap { case (_, duplicateFolders) =>
      val Seq(d1, d2, _*) = duplicateFolders
      if (d1.path.contains(d2.path) || d2.path.contains(d1.path)) {
        None
      } else {
        val diff = diffFolders(d1.path, d2.path, files)
        if (diff.differentEntries.isEmpty) {
          Some(diff)
        } else {
          None
        }
      }
    }
    folderDiffs.toSeq.filter(_.equalEntries.nonEmpty).sortBy(_.equalEntries.size).reverse.foreach { d =>
      println(s"${d.source} is identical to ${d.target} ${d.equalEntries.size}")
    }
  }
}
