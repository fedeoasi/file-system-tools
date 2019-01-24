package com.github.fedeoasi.search

import java.nio.file.{Path, Paths}

import com.github.fedeoasi.Model.{FileEntries, FileEntry}
import com.github.fedeoasi.catalog.EntryPersistence
import com.github.fedeoasi.cli.{CliAware, CliCommand}
import com.github.fedeoasi.output.Logging
import scopt.OptionParser

object FindFileByMd5 extends Logging with CliAware {
  override val command = CliCommand("find-file-by-md5", "Find a single file by MD5 hash.")
  def find(files: Seq[FileEntry], md5: String): Seq[FileEntry] = {
    files.filter(_.md5.exists(_ == md5))
  }

  case class FindFileByMd5Config(
    catalog: Option[Path] = None,
    md5: Option[String] = None)

  private val parser = new OptionParser[FindFileByMd5Config](command.name) {
    head(command.description + "\n")

    opt[String]('c', "catalog")
      .action { case (catalog, config) => config.copy(catalog = Some(Paths.get(catalog))) }
      .text("The catalog file (csv)")

    opt[String]('m', "md5")
      .action { case (md5, config) => config.copy(md5 = Some(md5)) }
      .text("The extension of the files to be searched")

    help("help").text("prints this usage text")
  }

  def main(args: Array[String]): Unit = {
    parser.parse(args, FindFileByMd5Config()) match {
      case Some(FindFileByMd5Config(Some(catalog), Some(md5))) =>
        val files = FileEntries(EntryPersistence.read(catalog))
        val filteredFiles = find(files, md5)
        info(filteredFiles.mkString("\n"))
      case _ =>
    }
  }
}
