package com.github.fedeoasi

import com.github.fedeoasi.FindIdenticalFolders.findIdenticalFolders
import com.github.fedeoasi.FolderComparison.FolderDiff
import com.github.fedeoasi.Model._
import com.github.fedeoasi.cli.{CatalogConfig, CatalogConfigParsing}
import org.apache.spark.{SparkConf, SparkContext}

object FindIdenticalFolders extends FolderComparison with CatalogConfigParsing with Logging {
  def findIdenticalFolders(entries: Seq[FileSystemEntry]): Seq[FolderDiff] = {
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
        Some(diffFolders(d1.path, d2.path, d1Files.toSeq, d2Files.toSeq))
      }
    }
    val result = folderDiffRdd.collect().toSeq
    sc.stop()
    // Make sure that our logging preferences get applied after they've gotten
    // overridden by Spark
    customizeLogger()
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

  /** Find identical folders present in the catalog. */
  def main(args: Array[String]): Unit = {
    parser.parse(args, CatalogConfig()) match {
      case Some(CatalogConfig(Some(catalog))) =>
        val entries = EntryPersistence.read(catalog)
        val folderDiffs = findIdenticalFolders(entries)
        folderDiffs
          .filter(d => d.differentEntriesCount == 0 && d.equalEntries.nonEmpty)
          .sortBy(_.equalEntries.size)
          .reverse
          .foreach { d =>
            info(s"${d.source} is identical to ${d.target} ${d.equalEntries.size}")
          }
      case _ =>
    }
  }
}

object FindSimilarFolders extends CatalogConfigParsing with Logging {
  def main(args: Array[String]): Unit = {
    parser.parse(args, CatalogConfig()) match {
      case Some(CatalogConfig(Some(catalog))) =>
        val entries = EntryPersistence.read(catalog)
        val folderDiffs = findIdenticalFolders(entries)
        folderDiffs
          .filter(d => d.equalEntries.nonEmpty && d.differentEntriesCount > 0)
          .sortBy(d => d.equalEntries.size - d.differentEntriesCount)
          .reverse
          .take(50)
          .foreach { d =>
            info(s""""${d.source}" "${d.target}" ${d.equalEntries.size} ${d.differentEntriesCount}""")
          }
      case _ =>
    }
  }
}