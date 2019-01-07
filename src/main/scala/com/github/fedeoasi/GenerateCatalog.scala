package com.github.fedeoasi

import java.nio.file.{Path, Paths}
import java.util.function.Consumer

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.github.fedeoasi.Model.FileSystemEntry
import com.github.fedeoasi.cli.{CliAware, CliCommand}
import scopt.OptionParser

object GenerateCatalog extends Logging with CliAware {
  override val command = CliCommand("generate-catalog", "Catalog a directory and save it to a CSV file.")

  case class GenerateCatalogReport(added: Long, total: Long)

  case class GenerateCatalogConfig(inputFolder: Option[Path] = None, catalog: Option[Path] = None, populateMd5: Boolean = true)

  private val parser = new OptionParser[GenerateCatalogConfig](command.name) {
    head(command.description + "\n")

    opt[String]('i', "inputFolder")
      .action { case (folder, config) => config.copy(inputFolder = Some(Paths.get(folder))) }
      .text("The root folder from which to generate a catalog")
      .required()
    opt[String]('c', "catalog")
      .action { case (catalog, config) => config.copy(catalog = Some(Paths.get(catalog))) }
      .text("The csv file where the catalog will be stored")
      .required()
    opt[Boolean]('m', "populate-md5")
      .action { case (md5, config) => config.copy(populateMd5 = md5) }
      .text("true to compute the md5 hash of each file, false to leave it empty")

    help("help").text("prints this usage text")
  }

  def generateMetadata(inputFolder: Path, catalogFile: Path, populateMd5: Boolean): GenerateCatalogReport = {
    val entriesFileExists = catalogFile.toFile.exists()
    val existingEntryIndex = if (entriesFileExists) new EntryReader(catalogFile).readIndex() else new EntryIndex(Map.empty)

    val entryWriterConsumer = new EntryWriterConsumer(catalogFile)

    implicit val system: ActorSystem = ActorSystem("GenerateCatalog")
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    try {
      new FileSystemWalk(inputFolder, existingEntryIndex, populateMd5).traverse(entryWriterConsumer)
    } finally {
      system.terminate()
    }
    val readEntries = new EntryReader(catalogFile).count()
    GenerateCatalogReport(entryWriterConsumer.entriesCount, readEntries)
  }

  class EntryWriterConsumer(catalogPath: Path) extends Consumer[FileSystemEntry] {
    private var _entriesCount = 0
    private val writer = new EntryWriter(catalogPath)

    override def accept(t: FileSystemEntry): Unit = {
      _entriesCount += 1
      writer.write(t)
    }

    def entriesCount: Int = _entriesCount
  }

  /** Generate catalog for a folder and dump it to a file (defaults to external_hard_drive.csv). */
  def main(args: Array[String]): Unit = {
    parser.parse(args, GenerateCatalogConfig()) match {
      case Some(GenerateCatalogConfig(Some(inputFolder), Some(catalog), populateMd5)) =>
        val report = generateMetadata(inputFolder, catalog, populateMd5)
        info(s"Added: ${report.added} Total: ${report.total}")
      case _ =>
    }
  }
}
