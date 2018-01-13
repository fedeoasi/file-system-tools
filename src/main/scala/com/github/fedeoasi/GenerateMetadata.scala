package com.github.fedeoasi

import java.nio.file.Paths

object GenerateMetadata {
  /** Generate metadata file for a folder and dump it to a file (external_hard_drive.csv).
    *
    * @param args [FOLDER]
    */
  def main(args: Array[String]): Unit = {
    val folder = args(0)
    val path = Paths.get(folder)
    val entriesFile = Paths.get(Constants.DefaultMetadataFile)
    val entriesFileExists = entriesFile.toFile.exists()
    val existingEntries = if (entriesFileExists) EntryPersistence.read(entriesFile) else Seq.empty
    val entries = new FileSystemWalk(path, existingEntries).run()
    EntryPersistence.write(entries, entriesFile, entriesFileExists)
    val readEntries = EntryPersistence.read(entriesFile)
    println(s"Added: ${entries.size} Total: ${readEntries.size}")
  }
}
