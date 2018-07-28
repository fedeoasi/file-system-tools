package com.github.fedeoasi

import java.io.File

import com.github.fedeoasi.Model.FileEntries

object FindDuplicateFilesForFolder extends Logging {
  /** Given a source folder lists the files that are duplicated and prints
    * the locations of the duplicates that are not under the given input folder.
    *
    * @param args [CATALOG] [FOLDER]
    */
  def main(args: Array[String]): Unit = {
    val catalog = args(0)
    val folder = args(1)
    val entries = EntryPersistence.read(catalog)
    val files = FileEntries(entries)

    val (filesInFolderRecursive, filesInOtherFolders) = files.partition(_.parent.startsWith(folder))

    val filesInOtherFoldersByMd5 = filesInOtherFolders.groupBy(_.md5)

    val filesAndDuplicates = filesInFolderRecursive.filterNot(_.path.contains(".svn")).map { f =>
      val sameMd5Files = filesInOtherFoldersByMd5.getOrElse(f.md5, Seq.empty).filterNot(_.path.contains(f.path))
      (f.path, sameMd5Files)
    }

    val (uniqueFiles, duplicatedFiles) = filesAndDuplicates.partition(_._2.isEmpty)
    info(s"There are ${uniqueFiles.size} unique files and ${duplicatedFiles.size} duplicated files\n")

    info(filesAndDuplicates.filter(_._2.nonEmpty).map { case (file, duplicates) =>
      val dups = duplicates.take(3).map(f => s"    - ${f.path}").mkString("\n")
      s"$file\n$dups"
    }.mkString("\n"))
  }
}

object FindDuplicateFilesWithinFolder extends Logging {
  /** Given a source folder lists the files that are duplicated under the given folder and prints
    * the locations of the duplicates.
    *
    * @param args [CATALOG] [FOLDER]
    */
  def main(args: Array[String]): Unit = {
    val catalog = args(0)
    val folder = args(1)
    val entries = EntryPersistence.read(catalog)
    val files = FileEntries(entries)

    val filesInFolder = files.filter(_.parent == folder)

    val duplicatedFilesByMd5 = filesInFolder.groupBy(_.md5).filter(_._2.size > 1)

    val filesToDelete = duplicatedFilesByMd5.flatMap { case (_, filesForMd5) =>
      val canonicalFile = filesForMd5.minBy(_.name)
      filesForMd5.filterNot(_.name == canonicalFile.name)
    }
    filesToDelete.foreach { f =>
      info(f.path)
      new File(f.path).delete()
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