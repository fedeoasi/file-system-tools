package com.github.fedeoasi

import com.github.fedeoasi.DiffFolders._
import com.github.fedeoasi.Model.{DirectoryEntry, FileSystemEntry}
import com.github.fedeoasi.cli.{CatalogConfig, CatalogConfigParsing}
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

/** Ranks folder pairs by similarity. The similarity used is the cosine similarity (https://en.wikipedia.org/wiki/Cosine_similarity)
  * in the vector space formed by the MD5 sums of all the nested files under a folder.
  *
  * This is analogous to the vector space model (https://en.wikipedia.org/wiki/Vector_space_model). In vector space model terms:
  * - A document is a folder
  * - A term is the MD5 sum of a file under the folder (files in nested folders included)
  * - The weights are binary
  */
object FolderSimilarity extends CatalogConfigParsing with SparkSupport with Logging {
  case class Folder(entry: DirectoryEntry, fileCount: Int)
  case class Score(fileCount1: Int, fileCount2: Int, count: Double) {
    def cosineSimilarity: Double = count / (Math.sqrt(fileCount1) * Math.sqrt(fileCount2))
  }

  def folderSimilarities(sc: SparkContext, entries: Seq[FileSystemEntry]): RDD[((Folder, Folder), Score)] = {
    val foldersAndFiles = folderAndNestedFiles(sc.parallelize(entries))
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
        f2 <- sortedFolders if !DiffFolders.areRelated(f1.entry, f2.entry)
      } yield (f1, f2) -> 1.0
    }.reduceByKey(_ + _)
    val rdd = commonFileCountByFolderPair.map { case ((f1, f2), counts) =>
      ((f1, f2), Score(f1.fileCount, f2.fileCount, counts))
    }.sortBy({ case ((f1, f2), score) => (score.cosineSimilarity, f1.fileCount, f2.fileCount) }, ascending = false)
    customizeLogger()
    rdd
  }

  /** Ranks folder pairs by similarity */
  def main(args: Array[String]): Unit = {
    parser.parse(args, CatalogConfig()) match {
      case Some(CatalogConfig(Some(catalog))) =>
        withSparkContext { sc =>
          val entries = EntryPersistence.read(catalog)
          val similarities = folderSimilarities(sc, entries)
          val topSimilarities = similarities
          topSimilarities.filter(_._1._1.fileCount > 10).take(50).foreach { case ((f1, f2), score) =>
            val prefix = StringUtils.longestPrefix(f1.entry.path, f2.entry.path)
            info(
              s""""$prefix" "${f1.entry.path.drop(prefix.length)}" "${f2.entry.path.drop(prefix.length)}" ${f1.fileCount} """ +
                s"""${f2.fileCount} ${score.count} ${score.cosineSimilarity}""")
          }
        }
      case _ =>
    }
  }
}
