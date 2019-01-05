package com.github.fedeoasi

import com.github.fedeoasi.FolderComparison.FolderDiff
import com.github.fedeoasi.Model._
import com.github.fedeoasi.cli.{CatalogConfig, CatalogConfigParsing, CliCommand}
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

object DiffFolders extends FolderComparison with Logging {
  def diff(sc: SparkContext, entries: Seq[FileSystemEntry]): Seq[FolderDiff] = {
    val foldersAndFiles = folderAndNestedFiles(sc.parallelize(entries))

    val duplicateFoldersByName = foldersAndFiles.groupBy(_._1.name).filter(_._2.size > 1)

    val folderDiffRdd = duplicateFoldersByName.flatMap { case (_, duplicateFolders) =>
      val Seq((d1, d1Files), (d2, d2Files), _*) = duplicateFolders.toSeq.sortBy(_._1.path)
      if (!areRelated(d1, d2)) {
        Some(diffFolders(d1.path, d2.path, d1Files.toSeq, d2Files.toSeq))
      } else {
        None
      }
    }
    val result = folderDiffRdd.collect().toSeq
    // Make sure that our logging preferences get applied after they've gotten
    // overridden by Spark
    customizeLogger()
    result
  }

  def areRelated(d1: DirectoryEntry, d2: DirectoryEntry): Boolean = {
    d1.path.contains(d2.path) || d2.path.contains(d1.path)
  }

  def folderAndNestedFiles(entries: RDD[FileSystemEntry]): RDD[(DirectoryEntry, Iterable[FileEntry])] = {
    val files = entries.collect { case f: FileEntry => f }
    val directories = entries.collect { case d: DirectoryEntry => (d.path, d) }
    val ancestorsAndFiles = files.flatMap { file => file.ancestors.map((_, file)) }
    val filesByAncestor = ancestorsAndFiles.groupByKey()
    directories.join(filesByAncestor).values
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
}

/** Finds identical folders. Two folders are considered identical if they have the same name and contain exact copies
  * of the same files.
  *
  * The analysis is performed in parallel using Spark.
  */
object FindIdenticalFolders extends CatalogConfigParsing with SparkSupport with Logging {
  override val command = CliCommand("find-identical-folders", "Find identical folders, having the same name and files.")

  /** Find identical folders present in the catalog. */
  def main(args: Array[String]): Unit = {
    parser.parse(args, CatalogConfig()) match {
      case Some(CatalogConfig(Some(catalog))) =>
        withSparkContext { sc =>
          val entries = EntryPersistence.read(catalog)
          val folderDiffs = DiffFolders.diff(sc, entries)
          folderDiffs
            .filter(d => d.differentEntriesCount == 0 && d.equalEntries.nonEmpty)
            .sortBy(_.equalEntries.size)
            .reverse
            .foreach { d =>
              info(s"${d.source} is identical to ${d.target} ${d.equalEntries.size}")
            }
        }
      case _ =>
    }
  }
}

/** Finds similar but not identical folders that have the same name.
  *
  * The analysis is performed in parallel using Spark.
  */
object FindSimilarFolders extends CatalogConfigParsing with Logging with SparkSupport {
  override val command = CliCommand("find-similar-folders", "Finds similar but not identical folders that have the same name.")

  def main(args: Array[String]): Unit = {
    parser.parse(args, CatalogConfig()) match {
      case Some(CatalogConfig(Some(catalog))) =>
        withSparkContext { sc =>
          val entries = EntryPersistence.read(catalog)
          val folderDiffs = DiffFolders.diff(sc, entries)
          folderDiffs
            .filter(d => d.equalEntries.nonEmpty && d.differentEntriesCount > 0)
            .sortBy(d => d.equalEntries.size - d.differentEntriesCount)
            .reverse
            .take(50)
            .foreach { d =>
              info(s""""${d.source}" "${d.target}" ${d.equalEntries.size} ${d.differentEntriesCount}""")
            }
          }
      case _ =>
    }
  }
}