package com.github.fedeoasi

import com.github.fedeoasi.Model.FileEntries

object FindDuplicateFilesWithinSecondaryFolder extends Logging {
  /** Given a source folder lists the files that are duplicated and prints
    * the locations of the duplicates under the given input folder.
    *
    * @param args [CATALOG] [PRIMARY_FOLDER] [SECONDARY_FOLDER]
    */
  def main(args: Array[String]): Unit = {
    val catalog = args(0)
    val primaryFolder = args(1)
    val secondaryFolder = args(2)
    require(primaryFolder != secondaryFolder, "Primary and secondary folder must be different")
    val entries = EntryPersistence.read(catalog)
    val files = FileEntries(entries)

    val primaryFiles = files.filter(_.parent == primaryFolder)
    val secondaryFiles = files.filter(_.parent == secondaryFolder)

    val primaryFilesByMd5 = primaryFiles.groupBy(_.md5)

    secondaryFiles.foreach { secondaryFile =>
      if (primaryFilesByMd5.contains(secondaryFile.md5)) {
        info(secondaryFile.path)
      }
    }
  }
}