package com.github.fedeoasi

import java.nio.file.{Path, Paths}

import com.github.fedeoasi.DiffFolders.folderAndNestedFileRdd
import com.github.fedeoasi.FolderSimilarity.{folderSimilarities, withSparkContext}
import com.github.fedeoasi.cli.{CliAware, CliCommand}
import scopt.OptionParser

object FolderPairSimilarity extends Logging with FolderComparison with CliAware {
  override val command = CliCommand("folder-pair-similarity", "Computes the similarity between a pair of folders")

  case class Config(
    catalog: Option[Path] = None,
    source: Option[String] = None,
    target: Option[String] = None)

  private val parser = new OptionParser[Config](getClass.getSimpleName) {
    head(getClass.getSimpleName)

    opt[String]('c', "catalog")
      .action { case (catalog, config) => config.copy(catalog = Some(Paths.get(catalog))) }
      .text("The csv file where the catalog will be stored")
      .required()
    opt[String]('s', "source")
      .action { case (folder, config) => config.copy(source = Some(folder)) }
      .text("The source folder to compare")
      .required()
    opt[String]('t', "target")
      .action { case (folder, config) => config.copy(target = Some(folder)) }
      .text("The target folder to compare")
      .required()

    help("help").text("prints this usage text")
  }

  def main(args: Array[String]): Unit = {
    parser.parse(args, Config()) match {
      case Some(Config(Some(catalog), Some(source), Some(target))) =>
        withSparkContext { sc =>
          val entries = EntryPersistence.read(catalog)
          val filteredEntries = entries.filter { e => e.path.startsWith(source) || e.path.startsWith(target) }
          val foldersAndFiles = folderAndNestedFileRdd(sc.parallelize(filteredEntries))
          val similarities = folderSimilarities(foldersAndFiles)
          val tuples = similarities.filter { case ((f1, f2), _) =>
            f1.entry.path == source && f2.entry.path == target ||
              f2.entry.path == source && f1.entry.path == target
          }.collect()

          val Some(sourceFiles) = foldersAndFiles.filter(_._1.path == source).collect().headOption.map(_._2)
          val Some(targetFiles) = foldersAndFiles.filter(_._1.path == target).collect().headOption.map(_._2)

          val folderDiff = diffFolders(source, target, sourceFiles.toSeq, targetFiles.toSeq)

          customizeLogger()
          tuples.headOption match {
            case Some(((f1, f2), score)) =>
              logger.info(s"${f1.entry.path} ${f2.entry.path} ${score.cosineSimilarity}")
              logger.info(folderDiff.report)
            case None =>
              logger.info("Could not find similarity for input folders")
          }
        }
      case _ =>
    }
  }
}
