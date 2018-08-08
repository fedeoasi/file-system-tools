package com.github.fedeoasi

import java.nio.file.{Path, Paths}

import com.github.fedeoasi.Model.{FileEntries, FileEntry, FileSystemEntry}
import com.github.fedeoasi.cli.CatalogAndFolderConfig
import scopt.OptionParser

class FindDuplicateFilesForFolder(entries: Seq[FileSystemEntry], folder: Path) {
  val filesAndDuplicates: Seq[(String, Seq[FileEntry])] = {
    val files = FileEntries(entries)

    val (filesInFolderRecursive, filesInOtherFolders) = files.partition(_.parent.startsWith(folder.toString))

    val filesInOtherFoldersByMd5 = filesInOtherFolders.groupBy(_.md5)

    filesInFolderRecursive.filterNot(_.path.contains(".svn")).map { f =>
      val sameMd5Files = filesInOtherFoldersByMd5.getOrElse(f.md5, Seq.empty).filterNot(_.path.contains(f.path))
      (f.path, sameMd5Files)
    }
  }
}

object FindDuplicateFilesForFolder extends Logging {
  private val parser = new OptionParser[CatalogAndFolderConfig](getClass.getSimpleName) {
    head(getClass.getSimpleName)

    opt[String]('c', "catalog")
      .action { case (catalog, config) => config.copy(catalog = Some(Paths.get(catalog))) }
      .text("The catalog file (csv)")
      .required()

    opt[String]('f', "folder")
      .action { case (folder, config) => config.copy(folder = Some(Paths.get(folder))) }
      .text("The folder to analyze")
      .required()

    help("help").text("prints this usage text")
  }

  /** Given a source folder lists the files that are duplicated and prints
    * the locations of the duplicates that are not under the given input folder.
    *
    * @param args [CATALOG] [FOLDER]
    */
  def main(args: Array[String]): Unit = {
    parser.parse(args, CatalogAndFolderConfig()) match {
      case Some(CatalogAndFolderConfig(Some(catalog), Some(folder))) =>
        val entries = EntryPersistence.read(catalog)
        val finder = new FindDuplicateFilesForFolder(entries, folder)
        val (uniqueFiles, duplicatedFiles) = finder.filesAndDuplicates.partition(_._2.isEmpty)
        info(s"There are ${uniqueFiles.size} unique files and ${duplicatedFiles.size} duplicated files\n")

        info(finder.filesAndDuplicates.filter(_._2.nonEmpty).map { case (file, duplicates) =>
          val dups = duplicates.take(3).map(f => s"    - ${f.path}").mkString("\n")
          s"$file\n$dups"
        }.mkString("\n"))
      case _ =>
    }
  }
}

class FindDuplicateFilesWithinFolder(entries: Seq[FileSystemEntry], folder: Path) {
  val filesAndDuplicates: Seq[(String, Seq[FileEntry])] = {
    val files = FileEntries(entries)
    val filesInFolder = files.filter(_.parent == folder.toString)

    val duplicatedFilesByMd5 = filesInFolder.groupBy(_.md5).filter(_._2.size > 1)

    duplicatedFilesByMd5.map { case (_, filesForMd5) =>
      val canonicalFile = filesForMd5.minBy(_.path)
      canonicalFile.path -> filesForMd5.filterNot(_.path == canonicalFile.path)
    }.toSeq
  }
}

object FindDuplicateFilesWithinFolder extends Logging {
  private val parser = new OptionParser[CatalogAndFolderConfig](getClass.getSimpleName) {
    head(getClass.getSimpleName)

    opt[String]('c', "catalog")
      .action { case (catalog, config) => config.copy(catalog = Some(Paths.get(catalog))) }
      .text("The catalog file (csv)")
      .required()

    opt[String]('f', "folder")
      .action { case (folder, config) => config.copy(folder = Some(Paths.get(folder))) }
      .text("The folder to analyze")
      .required()

    help("help").text("prints this usage text")
  }

  /** Given a source folder lists the files that are duplicated under the given folder and prints
    * the locations of the duplicates.
    *
    * @param args [CATALOG] [FOLDER]
    */
  def main(args: Array[String]): Unit = {
    parser.parse(args, CatalogAndFolderConfig()) match {
      case Some(CatalogAndFolderConfig(Some(catalog), Some(folder))) =>
        val entries = EntryPersistence.read(catalog)
        new FindDuplicateFilesWithinFolder(entries, folder).filesAndDuplicates
      case _ =>
    }
  }
}

object FindDuplicateFilesWithinSecondaryFolder extends Logging {
  /** Given a source folder lists the files that are duplicated and prints
    * the locations of the duplicates under the given input folder.
    *
    * @param args [CATALOG] [PRIMARY_FOLDER] [SECONDARY_FOLDER]
    */
  def main(args: Array[String]): Unit = {
    val catalog = args(0)
    val primaryFolder = args(1)
    val secondaryFolder = args(2)
    require(primaryFolder != secondaryFolder, "Primary and secondary folder must be different")
    val entries = EntryPersistence.read(catalog)
    val files = FileEntries(entries)

    val primaryFiles = files.filter(_.parent == primaryFolder)
    val secondaryFiles = files.filter(_.parent == secondaryFolder)

    val primaryFilesByMd5 = primaryFiles.groupBy(_.md5)

    secondaryFiles.foreach { secondaryFile =>
      if (primaryFilesByMd5.contains(secondaryFile.md5)) {
        info(secondaryFile.path)
      }
    }
  }
}