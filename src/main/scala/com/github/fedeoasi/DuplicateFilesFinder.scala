package com.github.fedeoasi

import java.nio.file.{Path, Paths}

import com.github.fedeoasi.Model.{FileEntries, FileEntry, FileSystemEntry}
import com.github.fedeoasi.cli.{CliAware, CliCommand}
import com.github.fedeoasi.collection.TopKFinder
import scopt.OptionParser

class DuplicateFilesFinder(entries: Seq[FileSystemEntry], folder: Option[Path] = None) extends Logging {
  private val files = entries.collect { case f: FileEntry if f.md5.nonEmpty => f }

  private val duplicatesByMd5 = files.groupBy(_.md5).filter {
    case (_, entriesForMd5) =>
      entriesForMd5.size > 1
  }

  def largestDuplicates(k: Option[Int]): Seq[(FileEntry, Seq[FileEntry])] = {
    k match {
      case Some(kval) =>
        logger.info(s"There are ${filesAndDuplicates.size} duplicates. Showing the first $kval")
        new TopKFinder(filesAndDuplicates).top(kval)(Ordering.by(_._1.size))
      case None =>
        filesAndDuplicates
    }
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

object DuplicateFilesFinder extends Logging with CliAware {
  override val command = CliCommand("find-duplicate-files", "Find duplicate files in a catalog file.")

  case class FindDuplicateFilesConfig(
    catalog: Option[Path] = None,
    folder: Option[Path] = None,
    extension: Option[String] = None,
    showDuplicates: Boolean = false,
    showAllDuplicates: Boolean = false)

  def findDuplicates(entries: Seq[FileSystemEntry], folder: Option[Path], topK: Option[Int]): Seq[(FileEntry, Seq[FileEntry])] = {
    val finder = new DuplicateFilesFinder(entries, folder)
    finder.largestDuplicates(topK)
  }

  def printDuplicates(entries: Seq[FileSystemEntry], folder: Option[Path], printDups: Boolean, printAllDups: Boolean): Unit = {
    val topK = if(printAllDups) { None } else { Some(25) }
    val filesAndDuplicates = findDuplicates(entries, folder, topK)
    info(filesAndDuplicates.map { case (canonical, duplicates) =>
      lazy val dups = duplicates.map(f => s"    - ${f.path}").mkString("\n")
      canonical.path + (if (printDups) s"\n$dups" else "")
    }.mkString("\n"))
  }

  private val parser = new OptionParser[FindDuplicateFilesConfig](command.name) {
    head(command.description + "\n")

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

    opt[Unit]('u', "show-duplicates")
      .action { case (_, config) => config.copy(showDuplicates = true) }
      .text("Print paths of the duplicate files along with the canonical file")

    opt[Unit]('a', "show-all")
      .action { case (_, config) => config.copy(showAllDuplicates = true)}
      .text("When showing duplicates, show all the duplicates instead of just the largest.")

    help("help").text("prints this usage text")
  }

  def main(args: Array[String]): Unit = {
    parser.parse(args, FindDuplicateFilesConfig()) match {
      case Some(FindDuplicateFilesConfig(Some(catalog), optionalFolder, optionalExtension, showDuplicates, showAllDuplicates)) =>
        val files = FileEntries(EntryPersistence.read(catalog))
        val filteredFiles = optionalExtension match {
          case Some(extension) => files.filter(_.extension.exists(_.equalsIgnoreCase(extension)))
          case None => files
        }
      printDuplicates(filteredFiles, optionalFolder, showDuplicates, showAllDuplicates)
      case _ =>
    }
  }
}
