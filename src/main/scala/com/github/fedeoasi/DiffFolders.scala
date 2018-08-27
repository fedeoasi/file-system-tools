package com.github.fedeoasi

import com.github.fedeoasi.DiffFolders.diff
import com.github.fedeoasi.FolderComparison.FolderDiff
import com.github.fedeoasi.Model._
import com.github.fedeoasi.cli.{CatalogConfig, CatalogConfigParsing}
import org.apache.spark.{SparkConf, SparkContext}

/** Finds identical folders. Two folders are considered identical if they have the same name and contain exact copies
  * of the same files.
  *
  * The analysis is performed in parallel using Spark.
  */
object DiffFolders extends FolderComparison with CatalogConfigParsing with Logging {
  def diff(entries: Seq[FileSystemEntry]): Seq[FolderDiff] = {
    val conf = new SparkConf().setAppName("Find Identical Folders").setMaster("local[*]")
    val sc = new SparkContext(conf)

    val allEntries = sc.parallelize(entries)

    val files = allEntries.collect { case f: FileEntry => f }
    val directories = allEntries.collect { case d: DirectoryEntry => (d.path, d) }

    val ancestorsAndFiles = files.flatMap { file =>
      file.ancestors.map((_, file))
    }
    val filesByAncestor = ancestorsAndFiles.groupByKey()

    val foldersAndFiles = directories.join(filesByAncestor).values

    val duplicateFoldersByName = foldersAndFiles.groupBy(_._1.name).filter(_._2.size > 1)

    val folderDiffRdd = duplicateFoldersByName.flatMap { case (_, duplicateFolders) =>
      val Seq((d1, d1Files), (d2, d2Files), _*) = duplicateFolders.toSeq.sortBy(_._1.path)
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

  //This assumes a UNIX file system where the root and separator are '/'
  private[fedeoasi] def ancestors(file: FileEntry): Seq[String] = {
    val parts = file.parent.split("/").filterNot(_.isEmpty)
    val sb = new StringBuilder()
    parts.foldLeft(Seq.empty[String]) {
      case (result, part) =>
        sb.append("/").append(part)
        sb.toString() +: result
    }.reverse
  }

  /** Find identical folders present in the catalog. */
  def main(args: Array[String]): Unit = {
    parser.parse(args, CatalogConfig()) match {
      case Some(CatalogConfig(Some(catalog))) =>
        val entries = EntryPersistence.read(catalog)
        val folderDiffs = diff(entries)
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
        val folderDiffs = diff(entries)
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