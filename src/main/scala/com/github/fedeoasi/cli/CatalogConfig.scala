package com.github.fedeoasi.cli

import java.nio.file.{Path, Paths}

import scopt.OptionParser

case class CatalogConfig(catalog: Option[Path] = None)

trait CatalogConfigParsing {
  protected val parser = new OptionParser[CatalogConfig](getClass.getSimpleName) {
    head(getClass.getSimpleName)

    opt[String]('c', "catalog")
      .action { case (catalog, config) => config.copy(catalog = Some(Paths.get(catalog))) }
      .text("The catalog file (csv)")

    help("help").text("prints this usage text")
  }

}