package com.github.fedeoasi

import com.github.fedeoasi.Model.FileEntries

object FindDuplicateFilesForFolder {
  /** Given a source folder lists the files that are duplicated and prints
    * the locations of the duplicates.
    *
    * @param args [FOLDER]
    */
  def main(args: Array[String]): Unit = {
    val folder = args(0)
    val entries = EntryPersistence.read(Constants.DefaultCatalogFilename)
    val files = FileEntries(entries)
//    val filesByParent = files.groupBy(_.parent)
    val filesByMd5 = files.groupBy(_.md5)

    //val filesInFolder = filesByParent.getOrElse(folder, Seq.empty)

    val filedInFolderRecursive = files.filter(_.parent.startsWith(folder))

    val filesAndDuplicates = filedInFolderRecursive.filterNot(_.path.contains(".svn")).map { f =>
      val sameMd5Files = filesByMd5.getOrElse(f.md5, Seq.empty).filterNot(_.path.contains(f.path))
      (f.path, sameMd5Files)
    }
    println(filesAndDuplicates.map { case (file, duplicates) =>
      val dups = duplicates.take(3).map(f => s"    - ${f.path}").mkString("\n")
      s"$file\n$dups"
    }.mkString("\n"))
  }
}
