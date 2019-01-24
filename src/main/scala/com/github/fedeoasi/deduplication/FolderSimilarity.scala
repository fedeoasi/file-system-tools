package com.github.fedeoasi.deduplication

import java.nio.file.{Path, Paths}

import com.github.fedeoasi.Model.{DirectoryEntry, FileEntry, FileSystemEntry}
import com.github.fedeoasi.catalog.EntryPersistence
import com.github.fedeoasi.cli.{CliAware, CliCommand}
import com.github.fedeoasi.deduplication.DiffFolders._
import com.github.fedeoasi.output.{Logging, Output}
import com.github.fedeoasi.spark.SparkSupport
import com.github.fedeoasi.utils.StringUtils
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import resource._
import scopt.OptionParser

/** Ranks folder pairs by similarity. The similarity used is the cosine similarity (https://en.wikipedia.org/wiki/Cosine_similarity)
  * in the vector space formed by the MD5 sums of all the nested files under a folder.
  *
  * This is analogous to the vector space model (https://en.wikipedia.org/wiki/Vector_space_model). In vector space model terms:
  * - A document is a folder
  * - A term is the MD5 sum of a file under the folder (files in nested folders included)
  * - The weights are binary
  */
object FolderSimilarity extends SparkSupport with Logging with CliAware {
  case class FolderSimilarityConfig(
    catalog: Option[Path] = None,
    printPrefix: Boolean = false,
    showAll: Boolean = false,
    output: Option[Path] = None)

  private lazy val parser = new OptionParser[FolderSimilarityConfig](command.name) {
    head(s"${command.description}\n")

    opt[String]('c', "catalog")
      .required()
      .action { case (catalog, config) => config.copy(catalog = Some(Paths.get(catalog))) }
      .text("The catalog file (csv)")

    opt[Unit]('p', "prefix")
      .action { case (_, config) => config.copy(printPrefix = true) }
      .text("Print the longest common prefix between folder pairs")

    opt[Unit]('a', "show-all")
      .action { case (_, config) => config.copy(showAll = true) }
      .text("Show comparison between all folders")

    opt[String]('o', "output")
      .action { case (output, config) => config.copy(output = Some(Paths.get(output))) }
      .text("The output file (csv)")

    help("help").text("prints this usage text")
  }

  override val command = CliCommand("folder-similarity", "Ranks folder pairs by MD5 sum similarity.")
  case class Folder(entry: DirectoryEntry, fileCount: Int)
  case class Score(fileCount1: Int, fileCount2: Int, count: Double) {
    def cosineSimilarity: Double = count / (Math.sqrt(fileCount1) * Math.sqrt(fileCount2))
  }

  def folderSimilarities(foldersAndFiles: RDD[(DirectoryEntry, Iterable[FileEntry])]): RDD[((Folder, Folder), Score)] = {
    val foldersAndMd5s = foldersAndFiles.mapValues(_.flatMap(_.md5).toSet)

    // Approach taken from the research paper "Pairwise Document Similarity in Large Collections with MapReduce"
    // https://www.aclweb.org/anthology/P/P08/P08-2067.pdf
    val foldersByFileMd5 = foldersAndMd5s.flatMap { case (folder, md5s) =>
      md5s.map(md5 => md5 -> Folder(folder, md5s.size))
    }.groupByKey
    val commonFileCountByFolderPair = foldersByFileMd5.flatMap { case (_, folders) =>
      val sortedFolders = folders.toSeq.sortBy(_.entry.path)
      for {
        f1 <- sortedFolders
        f2 <- sortedFolders if f1.entry.path < f2.entry.path && !DiffFolders.areRelated(f1.entry, f2.entry)
      } yield (f1, f2) -> 1.0
    }.reduceByKey(_ + _)
    val rdd = commonFileCountByFolderPair.map { case ((f1, f2), counts) =>
      ((f1, f2), Score(f1.fileCount, f2.fileCount, counts))
    }.sortBy({ case ((f1, f2), score) => (score.cosineSimilarity, f1.fileCount, f2.fileCount) }, ascending = false)
    customizeLogger()
    rdd
  }

  def folderSimilarities(sc: SparkContext, entries: Seq[FileSystemEntry]): RDD[((Folder, Folder), Score)] = {
    val foldersAndFiles = folderAndNestedFileRdd(sc.parallelize(entries))
    folderSimilarities(foldersAndFiles)
  }

  /** Ranks folder pairs by similarity */
  def main(args: Array[String]): Unit = {
    parser.parse(args, FolderSimilarityConfig()) match {
      case Some(FolderSimilarityConfig(Some(catalog), printPrefix, showAll, output)) =>
        withSparkContext { sc =>
          managed(Output(output, logger)).acquireAndGet { out =>
            val entries = EntryPersistence.read(catalog)
            val similarities = folderSimilarities(sc, entries)
            val topSimilarities = similarities

            val results = topSimilarities.filter(_._1._1.fileCount > 10)
            val filteredResults = if (showAll) results.collect else results.take(50)

            val prefixHeader = if (printPrefix) Some("Prefix") else None
            out.write(prefixHeader.toSeq ++ Seq("Source", "Target", "SourceFileCount", "TargetFileCount", "Count", "CosineSimilarity"))
            filteredResults.foreach { case ((f1, f2), score) =>
              val withPrefix = if (printPrefix) {
                val prefix = StringUtils.longestPrefix(f1.entry.path, f2.entry.path)
                Seq(prefix, f1.entry.path.drop(prefix.length), f2.entry.path.drop(prefix.length))
              } else {
                Seq(f1.entry.path, f2.entry.path)
              }
              out.write(withPrefix ++ Seq(f1.fileCount, f2.fileCount, score.count, score.cosineSimilarity))
            }
          }
        }
      case _ =>
    }
  }
}
