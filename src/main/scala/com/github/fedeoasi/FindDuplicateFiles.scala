package com.github.fedeoasi

import java.nio.file.{Path, Paths}

import com.github.fedeoasi.Model.{FileEntries, FileEntry, FileSystemEntry}
import scopt.OptionParser

object FindDuplicateFiles extends Logging {
  case class FundDuplicateFilesConfig(
    catalog: Option[Path] = None,
    extension: Option[String] = None)

  //TODO Make this return a nice report case class instead of Unit
  def findDuplicates(entries: Seq[FileSystemEntry]): Unit = {
    val files = entries.collect { case f: FileEntry if f.md5.nonEmpty => f }

    val duplicatesByMd5 = files.groupBy(_.md5).filter {
      case (_, entriesForMd5) =>
        entriesForMd5.size > 1
    }

    val allDuplicates = duplicatesByMd5.mapValues(_.head).values.toSeq
    val k = 25
    val topK = new TopKFinder(allDuplicates).top(k)(Ordering.by(_.size))
    info(topK.mkString("\n"))
    info()

    val duplicateCountByFolder = duplicatesByMd5.values.toSeq.flatten.groupBy(_.parent).transform {
      case (_, filesForFolder) =>
        filesForFolder.size
    }

    val folders = new TopKFinder(duplicateCountByFolder.toSeq).top(k)
    val topKFolders = folders.take(k)
    info(topKFolders.mkString("\n"))
  }

  private val parser = new OptionParser[FundDuplicateFilesConfig](getClass.getSimpleName) {
    head(getClass.getSimpleName)

    opt[String]('c', "catalog")
      .required()
      .action { case (catalog, config) => config.copy(catalog = Some(Paths.get(catalog))) }
      .text("The catalog file (csv)")

    opt[String]('e', "extension")
      .action { case (extension, config) => config.copy(extension = Some(extension)) }
      .text("The extension of the files to be searched")

    help("help").text("prints this usage text")
  }

  def main(args: Array[String]): Unit = {
    parser.parse(args, FundDuplicateFilesConfig()) match {
      case Some(FundDuplicateFilesConfig(Some(catalog), optionalExtension)) =>
        val files = FileEntries(EntryPersistence.read(catalog))
        val filteredFiles = optionalExtension match {
          case Some(extension) => files.filter(_.extension.exists(_.equalsIgnoreCase(extension)))
          case None => files
        }
        findDuplicates(filteredFiles)
      case _ =>
    }
  }
}
