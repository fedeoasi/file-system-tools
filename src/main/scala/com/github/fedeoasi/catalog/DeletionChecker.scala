package com.github.fedeoasi.catalog

import java.io.File
import java.nio.file.{Path, Paths}

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.github.fedeoasi.Model.FileSystemEntry
import com.github.fedeoasi.cli.{CliAware, CliCommand}
import com.github.fedeoasi.output.Logging
import com.github.fedeoasi.streams.StreamUtils
import scopt.OptionParser

import scala.concurrent.Await
import scala.concurrent.duration._

import scala.concurrent.ExecutionContext.Implicits.global

object DeletionChecker extends Logging with CliAware {
  override val command = CliCommand("deletion-checker", "Remove entries of deleted files from a catalog file.")

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

    implicit val system: ActorSystem = ActorSystem("Downloader")
    try {
      implicit val materializer: ActorMaterializer = ActorMaterializer()
      val entryAndExistsFuture = StreamUtils.doAndReport(toCheck,
        (e: FileSystemEntry) => Some((e, new File(e.path).exists())))
      val checkerResult = entryAndExistsFuture.map { entryAndExists =>
        val (toKeep, toDelete) = entryAndExists.partition(_._2)
        DeletionCheckerResult(toKeepWithoutCheck, toKeep.map(_._1), toDelete.map(_._1))
      }
      Await.result(checkerResult, 1.hour)
    } finally {
      println(s"shutting down actor system ${system.name}")
      system.terminate()
    }
  }

  private val parser = new OptionParser[DeletionCheckerConfig](command.name) {
    head(command.description + "\n")

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
