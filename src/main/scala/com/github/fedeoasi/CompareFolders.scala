package com.github.fedeoasi

import com.github.fedeoasi.FolderComparison.FolderDiff
import com.github.fedeoasi.Model.{DirectoryEntry, FileEntries, FileEntry}

trait FolderComparison {
  def diffFolders(source: String, target: String, files: Seq[FileEntry]): FolderDiff = {
    val sourceFiles = allFilesForFolder(files, source)
    val targetFiles = allFilesForFolder(files, target)
    diffFolders(source, target, sourceFiles, targetFiles)
  }

  def diffFolders(source: String, target: String, sourceFiles: Seq[FileEntry], targetFiles: Seq[FileEntry]): FolderDiff = {
    val sourceFileById = sourceFiles.map { file =>
      val id = toRelativePath(file, source)
      (id, file)
    }.toMap
    val targetFileById = targetFiles.map { file =>
      val id = toRelativePath(file, target)
      (id, file)
    }.toMap
    val folderDiff = FolderDiff(source, target, Seq.empty, Seq.empty, Seq.empty, Seq.empty)
    (sourceFileById.keySet ++ targetFileById.keySet).foldLeft(folderDiff) { case (acc, key) =>
      (sourceFileById.get(key), targetFileById.get(key)) match {
        case (Some(inSource), Some(inTarget)) if sameMd5(inSource, inTarget) =>
          acc.copy(equalEntries = inSource +: acc.equalEntries)
        case (Some(inSource), Some(inTarget)) if !sameMd5(inSource, inTarget) =>
          acc.copy(differentContent = (inSource, inTarget) +: acc.differentContent)
        case (Some(inSource), None) =>
          acc.copy(missingInTarget = inSource +: acc.missingInTarget)
        case (None, Some(inTarget)) =>
          acc.copy(missingInSource = inTarget +: acc.missingInSource)
        case _ =>
          acc
      }
    }
  }

  def sameMd5(source: FileEntry, target: FileEntry): Boolean = {
    (source.md5, target.md5) match {
      case (Some(sourceMd5), Some(targetMd5)) if sourceMd5 == targetMd5 => true
      case _ => false
    }
  }

  def subFolders(folder: String, directories: Seq[DirectoryEntry]): Seq[DirectoryEntry] = {
    directories.filter(_.parent == folder)
  }

  def allFilesForFolder(files: Seq[FileEntry], folder: String): Seq[FileEntry] = {
    files.filter(_.path.startsWith(folder))
  }

  def toRelativePath(file: FileEntry, inFolder: String): String = {
    require(file.path.startsWith(inFolder))
    file.path.substring(inFolder.length)
  }
}

object FolderComparison {
  case class FileIdentifier(relativePath: String, md5: String)

  case class FolderDiff(
    source: String,
    target: String,
    equalEntries: Seq[FileEntry],
    missingInTarget: Seq[FileEntry],
    missingInSource: Seq[FileEntry],
    differentContent: Seq[(FileEntry, FileEntry)]) {

    def differentEntriesCount: Int = missingInSource.size + missingInTarget.size + differentContent.size
  }

  //TODO remove this
  case class FolderDiffOld(
    source: String,
    target: String,
    equalEntries: Set[FileIdentifier],
    differentEntries: Set[FileIdentifier])
}

object CompareFolders extends FolderComparison {
  /** Compare folders based on the contents of the contained folders
    * and files.
    *
    * @param args [CATALOG] [FOLDER_1] [FOLDER_2]
    */
  def main(args: Array[String]): Unit = {
    val catalog = args(0)
    val folder1 = args(1)
    val folder2 = args(2)
    val entries = EntryPersistence.read(catalog)

    val files = FileEntries(entries)

    val folderDiff = diffFolders(folder1, folder2, files)

    println(s"${folderDiff.equalEntries.size} are equal in the two folders\n")
    println(s"${folderDiff.missingInTarget.size} are in source but not in target")
    println(folderDiff.missingInTarget.map(toRelativePath(_, folder1)).mkString("\n"))
    println(s"\n${folderDiff.missingInSource.size} are in target but not in source")
    println(folderDiff.missingInSource.map(toRelativePath(_, folder2)).mkString("\n"))
    println(s"\n${folderDiff.differentContent.size} differ in content")
    println(folderDiff.differentContent.map { case (f1, f2) =>
      s"${toRelativePath(f1, folder1)} ${f1.modifiedTime} ${f2.modifiedTime}"
    }.mkString("\n"))
  }
}


