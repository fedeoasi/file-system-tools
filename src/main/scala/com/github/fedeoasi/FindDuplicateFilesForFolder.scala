package com.github.fedeoasi

import com.github.fedeoasi.Model.FileEntries

object FindDuplicateFilesForFolder {
  /** Given a source folder lists the files that are duplicated and prints
    * the locations of the duplicates if they are not under the given input folder.
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
    println(filesAndDuplicates.filter(_._2.nonEmpty).map { case (file, duplicates) =>
      val dups = duplicates.take(3).map(f => s"    - ${f.path}").mkString("\n")
      s"$file\n$dups"
    }.mkString("\n"))
  }
}

object FindDuplicateFilesWithinFolder {
  /** Given a source folder lists the files that are duplicated and prints
    * the locations of the duplicates under the given input folder.
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
      println(canonicalFile.path)
      filesForMd5.filterNot(_.name == canonicalFile.name)
    }
    println("Delete these:")
    filesToDelete.foreach(f => println(f.path))
    //filesToDelete.foreach(f => new File(f.path).delete())
  }
}
