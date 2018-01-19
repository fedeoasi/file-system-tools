package com.github.fedeoasi

import java.nio.file.{Path, Paths}

object GenerateMetadata {
  case class GenerateMetadataReport(added: Long, total: Long)

  def generateMetadata(folder: Path, metadataFile: String): GenerateMetadataReport = {
    val entriesFile = Paths.get(metadataFile)
    val entriesFileExists = entriesFile.toFile.exists()
    val existingEntries = if (entriesFileExists) EntryPersistence.read(entriesFile) else Seq.empty
    val entries = new FileSystemWalk(folder, existingEntries).run()
    EntryPersistence.write(entries, entriesFile, entriesFileExists)
    val readEntries = EntryPersistence.read(entriesFile)
    GenerateMetadataReport(entries.size, readEntries.size)
  }

  /** Generate metadata file for a folder and dump it to a file (external_hard_drive.csv).
    *
    * @param args [FOLDER] [METADATA_FILENAME]
    */
  def main(args: Array[String]): Unit = {
    val folderName = args(0)
    val metadataFilename = if (args.length > 1) args(1) else Constants.DefaultMetadataFile
    val folder = Paths.get(folderName)
    val report = generateMetadata(folder, metadataFilename)
    println(s"Added: ${report.added} Total: ${report.total}")
  }
}
