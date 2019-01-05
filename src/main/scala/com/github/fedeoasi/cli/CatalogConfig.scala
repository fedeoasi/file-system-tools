package com.github.fedeoasi.cli

import java.nio.file.{Path, Paths}

import scopt.OptionParser

case class CatalogConfig(catalog: Option[Path] = None)

trait CatalogConfigParsing extends CliAware {
  protected lazy val parser = new OptionParser[CatalogConfig](command.name) {
    head(command.description + "\n")

    opt[String]('c', "catalog")
      .required()
      .action { case (catalog, config) => config.copy(catalog = Some(Paths.get(catalog))) }
      .text("The catalog file (csv)")

    help("help").text("prints this usage text")
  }
}