package com.github.fedeoasi

import com.github.fedeoasi.FindIdenticalFolders.findIdenticalFolders
import com.github.fedeoasi.FolderComparison.{FileIdentifier, FolderDiff, FolderDiffOld}
import com.github.fedeoasi.Model._
import org.apache.spark.{SparkConf, SparkContext}

object FindIdenticalFolders {
  def findIdenticalFolders(entries: Seq[FileSystemEntry]): Seq[FolderDiffOld] = {
    val conf = new SparkConf().setAppName("Find Identical Folders").setMaster("local[*]")
    val sc = new SparkContext(conf)

    val entryRdd = sc.parallelize(entries)

    val filesRdd = entryRdd.collect { case f: FileEntry => f }
    val directoriesRdd = entryRdd.collect { case d: DirectoryEntry => (d.path, d) }

    val ancestorAndFileRdd = filesRdd.flatMap { file =>
      ancestors(file).map((_, file))
    }
    val filesByFolderRdd = ancestorAndFileRdd.groupByKey()

    val foldersAndFilesRdd = directoriesRdd.join(filesByFolderRdd).values

    val duplicatesByNameRdd = foldersAndFilesRdd.groupBy(_._1.name).filter(_._2.size > 1)

    val folderDiffRdd = duplicatesByNameRdd.flatMap { case (_, duplicateFolders) =>
      val Seq((d1, d1Files), (d2, d2Files), _*) = duplicateFolders
      if (d1.path.contains(d2.path) || d2.path.contains(d1.path)) {
        None
      } else {
        val sourceFiles = d1Files.map(f => FileIdentifier(f.path.substring(d1.path.length), f.md5)).toSet
        val targetFiles = d2Files.map(f => FileIdentifier(f.path.substring(d2.path.length), f.md5)).toSet
        val diff12 = sourceFiles.diff(targetFiles)
        val diff21 = targetFiles.diff(sourceFiles)
        Some(FolderDiffOld(d1.path, d2.path, sourceFiles ++ targetFiles, diff12 ++ diff21))
      }
    }
    val result = folderDiffRdd.collect().toSeq
    sc.stop()
    result
  }

  private def ancestors(file: FileEntry) = {
    val parts = file.parent.split("/")
    val sb = new StringBuilder(parts(0))
    (1 until parts.length).map { i =>
      val ancestor = sb.toString()
      sb.append("/").append(parts(i))
      ancestor
    }
  }

  /** Find identical folders present in the metadata file. */
  def main(args: Array[String]): Unit = {
    val entries = EntryPersistence.read(Constants.DefaultMetadataFile)
    val folderDiffs = findIdenticalFolders(entries)
    folderDiffs
      .filter(d => d.differentEntries.isEmpty && d.equalEntries.nonEmpty)
      .sortBy(_.equalEntries.size)
      .reverse
      .foreach { d =>
        println(s"${d.source} is identical to ${d.target} ${d.equalEntries.size}")
      }
  }
}

object FindSimilarFolders {
  def main(args: Array[String]): Unit = {
    val entries = EntryPersistence.read(Constants.DefaultMetadataFile)
    val folderDiffs = findIdenticalFolders(entries)
    folderDiffs
      .filter(d => d.equalEntries.nonEmpty && d.differentEntries.nonEmpty)
      .sortBy(d => d.equalEntries.size - d.differentEntries.size)
      .reverse
      .take(50)
      .foreach { d =>
        println(s"${d.source} is similar to ${d.target} ${d.equalEntries.size} ${d.differentEntries.size}")
      }
  }
}