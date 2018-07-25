package com.github.fedeoasi

import java.io.File
import java.nio.file.{Path, Paths}

import com.github.fedeoasi.Model.FileSystemEntry
import scopt.OptionParser

object DeletionChecker extends Logging {
  case class DeletionCheckerConfig(catalog: Option[Path] = None, folder: Option[String] = None)

  //TODO make sure that the the parent folder exists, otherwise we will wipe out a catalog if you forgot to
  //connect your hard drive
  def findDeletions(entries: Seq[FileSystemEntry]): (Seq[FileSystemEntry], Seq[FileSystemEntry]) = {
    entries.partition(e => new File(e.path).exists())
  }

  private val parser = new OptionParser[DeletionCheckerConfig](getClass.getSimpleName) {
    head(getClass.getSimpleName)

    opt[String]('c', "catalog")
      .required()
      .action { case (catalog, config) => config.copy(catalog = Some(Paths.get(catalog))) }
      .text("The catalog file (csv)")

    opt[String]('f', "folder")
      .action { case (folder, config) => config.copy(folder = Some(folder)) }
      .text("The root from which to check for deletions")

    help("help").text("prints this usage text")
  }

  /** Performs deletions on the catalog
    */
  def main(args: Array[String]): Unit = {
    parser.parse(args, DeletionCheckerConfig()) match {
      case Some(DeletionCheckerConfig(Some(catalog), optionalFolder)) =>
        val entries = EntryPersistence.read(catalog)
        val (toCheck, toKeepWithoutCheck) = optionalFolder match {
          case Some(folder) =>
            entries.partition(_.path.startsWith(folder))
          case None =>
            (entries, Seq.empty)
        }
        val (toKeep, toDelete) = findDeletions(toCheck)
        info(s"Found ${toDelete.size} entries to delete")
        EntryPersistence.write(toKeepWithoutCheck ++ toKeep, catalog)
      case _ =>
    }
  }
}
