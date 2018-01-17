package com.github.fedeoasi

import com.github.fedeoasi.FolderComparison.FolderDiff
import com.github.fedeoasi.Model.{DirectoryEntries, DirectoryEntry, FileEntries, FileEntry}

trait FolderComparison {
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

object FolderComparison {
  case class FolderDiff(
    source: String,
    target: String,
    equalEntries: Set[(String, String)],
    differentEntries: Set[(String, String)])
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


