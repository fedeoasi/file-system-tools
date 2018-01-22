package com.github.fedeoasi

import java.nio.file.{Path, Paths}

object GenerateCatalog {
  case class GenerateCatalogReport(added: Long, total: Long)

  def generateMetadata(inputFolder: Path, toFilename: String): GenerateCatalogReport = {
    val entriesFile = Paths.get(toFilename)
    val entriesFileExists = entriesFile.toFile.exists()
    val existingEntries = if (entriesFileExists) EntryPersistence.read(entriesFile) else Seq.empty
    val entries = new FileSystemWalk(inputFolder, existingEntries).run()
    EntryPersistence.write(entries, entriesFile, entriesFileExists)
    val readEntries = EntryPersistence.read(entriesFile)
    GenerateCatalogReport(entries.size, readEntries.size)
  }

  /** Generate catalog for a folder and dump it to a file (defaults to external_hard_drive.csv).
    *
    * @param args [FOLDER] [METADATA_FILENAME]
    */
  def main(args: Array[String]): Unit = {
    val folderName = args(0)
    val catalog = if (args.length > 1) args(1) else Constants.DefaultCatalogFilename
    val folder = Paths.get(folderName)
    val report = generateMetadata(folder, catalog)
    println(s"Added: ${report.added} Total: ${report.total}")
  }
}
