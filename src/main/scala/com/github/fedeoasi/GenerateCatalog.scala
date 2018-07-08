package com.github.fedeoasi

import java.nio.file.{Path, Paths}

import scopt.OptionParser

object GenerateCatalog {
  case class GenerateCatalogReport(added: Long, total: Long)

  case class GenerateCatalogConfig(inputFolder: Option[Path] = None, catalog: Option[Path] = None, populateMd5: Boolean = true)

  private val parser = new OptionParser[GenerateCatalogConfig](getClass.getSimpleName) {
    head(getClass.getSimpleName)

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
      .text("The csv file where the catalog will be stored")

    help("help").text("prints this usage text")
  }

  def generateMetadata(inputFolder: Path, catalogFile: Path, populateMd5: Boolean): GenerateCatalogReport = {
    val entriesFileExists = catalogFile.toFile.exists()
    val existingEntries = if (entriesFileExists) EntryPersistence.read(catalogFile) else Seq.empty
    val entries = new FileSystemWalk(inputFolder, existingEntries, populateMd5).run()
    EntryPersistence.write(entries, catalogFile, entriesFileExists)
    val readEntries = EntryPersistence.read(catalogFile)
    GenerateCatalogReport(entries.size, readEntries.size)
  }

  /** Generate catalog for a folder and dump it to a file (defaults to external_hard_drive.csv). */
  def main(args: Array[String]): Unit = {
    parser.parse(args, GenerateCatalogConfig()) match {
      case Some(GenerateCatalogConfig(Some(inputFolder), Some(catalog), populateMd5)) =>
        val report = generateMetadata(inputFolder, catalog, populateMd5)
        println(s"Added: ${report.added} Total: ${report.total}")
      case _ =>
    }
  }
}
