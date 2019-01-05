package com.github.fedeoasi

import java.nio.file.Paths

import com.github.fedeoasi.FindFileByMd5.FindFileByMd5Config
import com.github.fedeoasi.Model.FileEntries
import com.github.fedeoasi.cli.{CliAware, CliCommand}
import scopt.OptionParser

object FindDuplicateFilesWithinSecondaryFolder extends Logging with CliAware {
  override val command = CliCommand("lookup-duplicates", "Look for duplicate files in specific folders.")
  private val parser = new OptionParser[FindFileByMd5Config](command.name) {
    head(command.description + "\n")

    opt[String]('c', "catalog")
      .action { case (catalog, config) => config.copy(catalog = Some(Paths.get(catalog))) }
      .text("The catalog file (csv)")

    opt[String]('p', "reference")
      .action { case (md5, config) => config.copy(md5 = Some(md5)) }
      .text("The reference folder containing the files to search for")

    opt[String]('s', "search")
      .action { case (md5, config) => config.copy(md5 = Some(md5)) }
      .text("The folder to search for duplicates")

    help("help").text("prints this usage text")
  }

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