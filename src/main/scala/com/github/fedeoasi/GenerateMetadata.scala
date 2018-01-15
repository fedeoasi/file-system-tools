package com.github.fedeoasi

import java.nio.file.{Path, Paths}

object GenerateMetadata {
  case class GenerateMetadataReport(added: Long, total: Long)

  def generateMetadata(folder: Path, metadataFile: String = Constants.DefaultMetadataFile): GenerateMetadataReport = {
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
    * @param args [FOLDER]
    */
  def main(args: Array[String]): Unit = {
    val folderName = args(0)
    val folder = Paths.get(folderName)
    val report = generateMetadata(folder)
    println(s"Added: ${report.added} Total: ${report.total}")
  }
}
