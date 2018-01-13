package com.github.fedeoasi

import com.github.fedeoasi.Model.{FileSystemEntry, FileEntries, FileEntry}

object FindDuplicateFiles {
  def findDuplicates(entries: Seq[FileSystemEntry]): Unit = {
    val files = entries.collect { case f: FileEntry if f.md5.nonEmpty => f }
    println(files.size)
    val duplicatesByMd5 = files.groupBy(_.md5).filter {
      case (_, entriesForMd5) =>
        entriesForMd5.size > 1//&& entriesForMd5.size < 5
    }

    val duplicatesByMd5LessThanK = duplicatesByMd5.filter(_._2.size < 5)
    val topK = duplicatesByMd5LessThanK.toSeq
      .sortBy(_._2.size)
      .reverse
      .take(100)
    println(topK.map(e => e._2.mkString("\n")).mkString("\n\n"))

    println()


    val duplicateCountByFolder = duplicatesByMd5.values.toSeq.flatten.groupBy(_.parent).transform {
      case (_, filesForFolder) =>
        filesForFolder.size
    }
    val foldersAndDuplicateCounts = duplicateCountByFolder.toSeq.sortBy(_._2).reverse.take(10)
    println(foldersAndDuplicateCounts.mkString("\n"))
  }

  def main(args: Array[String]): Unit = {
    val entries = EntryPersistence.read(Constants.DefaultMetadataFile)
    findDuplicates(FileEntries(entries))
  }
}
