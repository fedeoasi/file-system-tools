package com.github.fedeoasi

import java.io.File
import java.nio.file.{Path, Paths}

import com.github.fedeoasi.Model.FileSystemEntry
import scopt.OptionParser

object DeletionChecker extends Logging {
  case class DeletionCheckerConfig(catalog: Option[Path] = None, folder: Option[Path] = None)
  case class DeletionCheckerResult(toKeepWithoutCheck: Seq[FileSystemEntry], toKeep: Seq[FileSystemEntry], toDelete: Seq[FileSystemEntry])

  def check(catalog: Path, folder: Option[Path]): DeletionCheckerResult = {
    val entries = EntryPersistence.read(catalog)
    val (toCheck, toKeepWithoutCheck) = folder match {
      case Some(dir) =>
        entries.partition(_.path.startsWith(dir.toString))
      case None =>
        (entries, Seq.empty)
    }
    val (toKeep, toDelete) = toCheck.partition(e => new File(e.path).exists())
    DeletionCheckerResult(toKeepWithoutCheck, toKeep, toDelete)
  }

  private val parser = new OptionParser[DeletionCheckerConfig](getClass.getSimpleName) {
    head(getClass.getSimpleName)

    opt[String]('c', "catalog")
      .required()
      .action { case (catalog, config) => config.copy(catalog = Some(Paths.get(catalog))) }
      .text("The catalog file (csv)")

    opt[String]('f', "folder")
      .action { case (folder, config) => config.copy(folder = Some(Paths.get(folder))) }
      .text("The root from which to check for deletions")

    help("help").text("prints this usage text")
  }

  /** Performs deletions on the catalog
    */
  def main(args: Array[String]): Unit = {
    parser.parse(args, DeletionCheckerConfig()) match {
      case Some(DeletionCheckerConfig(Some(catalog), optionalFolder)) =>
        val result = check(catalog, optionalFolder)
        info(s"Found ${result.toDelete.size} entries to delete")
        EntryPersistence.write(result.toKeepWithoutCheck ++ result.toKeep, catalog)
      case _ =>
    }
  }
}
