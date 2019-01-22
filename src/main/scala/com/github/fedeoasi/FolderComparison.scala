package com.github.fedeoasi

import com.github.fedeoasi.Model.{DirectoryEntry, FileEntry}

trait FolderComparison {
  import FolderComparison._

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
}

object FolderComparison {
  def toRelativePath(file: FileEntry, inFolder: String): String = {
    require(file.path.startsWith(inFolder))
    file.path.substring(inFolder.length)
  }

  case class FileIdentifier(relativePath: String, md5: String)

  case class FolderDiff(
    source: String,
    target: String,
    equalEntries: Seq[FileEntry],
    missingInTarget: Seq[FileEntry],
    missingInSource: Seq[FileEntry],
    differentContent: Seq[(FileEntry, FileEntry)]) {

    def differentEntriesCount: Int = missingInSource.size + missingInTarget.size + differentContent.size

    def report: String = {
      // show the relative paths?

      val different = differentContent.map { case (f1, f2) =>
        s"${toRelativePath(f1, source)} ${f1.modifiedTime} ${f2.modifiedTime}"
      }.mkString("\n")

      s"""
         |${equalEntries.size} are equal in the two folders
         |${missingInTarget.size} are in source but not in target
         |${missingInSource.size} are in target but not in source
         |${differentContent.size} differ in content
         |$different
       """.stripMargin
    }
  }

  //TODO remove this
  case class FolderDiffOld(
    source: String,
    target: String,
    equalEntries: Set[FileIdentifier],
    differentEntries: Set[FileIdentifier])
}
