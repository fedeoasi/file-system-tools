package com.github.fedeoasi

import java.nio.file.{Path, Paths}

import com.github.fedeoasi.Model.FileEntries
import com.github.fedeoasi.cli.{CliAware, CliCommand}
import scopt.OptionParser

case class FindDuplicateFilesWithinSecondaryFolderConfig(catalog: Option[Path] = None,
                                                         reference: Option[String] = None,
                                                         search: Option[String] = None)

object FindDuplicateFilesWithinSecondaryFolder extends Logging with CliAware {
  override val command = CliCommand("lookup-duplicates", "Look for duplicate files in specific folders.")
  private val parser = new OptionParser[FindDuplicateFilesWithinSecondaryFolderConfig](command.name) {
    head(command.description + "\n")

    opt[String]('c', "catalog")
      .required()
      .action { case (catalog, config) => config.copy(catalog = Some(Paths.get(catalog))) }
      .text("The catalog file (csv)")

    opt[String]('p', "reference")
      .required()
      .action { case (ref, config) => config.copy(reference = Some(ref)) }
      .text("The reference folder containing the files to search for")

    opt[String]('s', "search")
      .required()
      .action { case (srch, config) => config.copy(search = Some(srch)) }
      .text("The folder to search for duplicates")

    help("help").text("prints this usage text")
  }

  /** Given a source folder lists the files that are duplicated and prints
    * the locations of the duplicates under the given input folder.
    *
    * @param args [CATALOG] [PRIMARY_FOLDER] [SECONDARY_FOLDER]
    */
  def main(args: Array[String]): Unit = {
    parser.parse(args, FindDuplicateFilesWithinSecondaryFolderConfig()) match {
      case Some(FindDuplicateFilesWithinSecondaryFolderConfig(Some(catalog), Some(primaryFolder), Some(secondaryFolder))) =>
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
      case _ =>
    }
  }
}