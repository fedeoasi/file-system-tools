package com.github.fedeoasi

import java.nio.file.{Path, Paths}

import com.github.fedeoasi.Model.{FileEntries, FileEntry, FileSystemEntry}
import com.github.fedeoasi.collection.TopKFinder
import scopt.OptionParser

class FindDuplicateFiles(entries: Seq[FileSystemEntry], folder: Option[Path] = None) {
  private val files = entries.collect { case f: FileEntry if f.md5.nonEmpty => f }

  private val duplicatesByMd5 = files.groupBy(_.md5).filter {
    case (_, entriesForMd5) =>
      entriesForMd5.size > 1
  }

  def largestDuplicates(k: Int): Seq[(FileEntry, Seq[FileEntry])] = {
    new TopKFinder(filesAndDuplicates).top(k)(Ordering.by(_._1.size))
  }

  val filesAndDuplicates: Seq[(FileEntry, Seq[FileEntry])] = {
    folder match {
      case Some(folderInFocus) =>
        val filesInFolder = files
          .filter(_.parent.startsWith(folderInFocus.toString))
        val filesInFolderByMd5 = filesInFolder.groupBy(_.md5)
        val canonicalFilesInFolder = filesInFolderByMd5.toSeq
          .map { case (_, filesForMd5) => canonicalFile(filesForMd5) }
        canonicalFilesInFolder
          .filter(f => duplicatesByMd5.get(f.md5).nonEmpty).map { file =>
          file -> duplicatesByMd5(file.md5).filterNot(_.path == file.path)
        }
      case None =>
        duplicatesByMd5.map { case (_, filesForMd5) =>
          val canonical = canonicalFile(filesForMd5)
          canonical -> filesForMd5.filterNot(_.path == canonical.path)
        }.toSeq
    }
  }

  private def canonicalFile(files: Seq[FileEntry]): FileEntry = {
    require(files.nonEmpty, "Cannot find canonical file in an empty sequence")
    files.minBy(_.path)
  }

  def foldersWithMostDuplicates(k: Int): Seq[(String, Int)] = {
    val duplicateCountByFolder = duplicatesByMd5.values.toSeq.flatten.groupBy(_.parent).transform {
      case (_, filesForFolder) =>
        filesForFolder.size
    }
    new TopKFinder(duplicateCountByFolder.toSeq).top(k)
  }
}

object FindDuplicateFiles extends Logging {
  case class FundDuplicateFilesConfig(
    catalog: Option[Path] = None,
    folder: Option[Path] = None,
    extension: Option[String] = None,
    showDuplicates: Boolean = false)

  def printDuplicates(entries: Seq[FileSystemEntry], folder: Option[Path], printDups: Boolean): Unit = {
    val finder = new FindDuplicateFiles(entries, folder)
    info(finder.largestDuplicates(k = 25).map { case (canonical, duplicates) =>
      lazy val dups = duplicates.take(3).map(f => s"    - ${f.path}").mkString("\n")
      canonical.path + (if (printDups) s"\n$dups" else "")
    }.mkString("\n"))
  }

  private val parser = new OptionParser[FundDuplicateFilesConfig](getClass.getSimpleName) {
    head(getClass.getSimpleName)

    opt[String]('c', "catalog")
      .required()
      .action { case (catalog, config) => config.copy(catalog = Some(Paths.get(catalog))) }
      .text("The catalog file (csv)")

    opt[String]('f', "folder")
      .action { case (folder, config) => config.copy(folder = Some(Paths.get(folder))) }
      .text("The folder to consider when searching for duplicates")

    opt[String]('e', "extension")
      .action { case (extension, config) => config.copy(extension = Some(extension)) }
      .text("The extension of the files to be searched")

    opt[Boolean]('d', "show-duplicates")
      .action { case (showDuplicates, config) => config.copy(showDuplicates = showDuplicates) }
      .text("Print paths of the duplicate files along with the canonical file")

    help("help").text("prints this usage text")
  }

  def main(args: Array[String]): Unit = {
    parser.parse(args, FundDuplicateFilesConfig()) match {
      case Some(FundDuplicateFilesConfig(Some(catalog), optionalFolder, optionalExtension, showDuplicates)) =>
        val files = FileEntries(EntryPersistence.read(catalog))
        val filteredFiles = optionalExtension match {
          case Some(extension) => files.filter(_.extension.exists(_.equalsIgnoreCase(extension)))
          case None => files
        }
        printDuplicates(filteredFiles, optionalFolder, showDuplicates)
      case _ =>
    }
  }
}
