package com.github.fedeoasi

import java.nio.file.{Path, Paths}

import scopt.OptionParser

case class MergeCatalogConfig(
  catalog: Option[Path] = None,
  secondaryCatalog: Option[Path] = None,
  outputCatalog: Option[Path] = None)

object MergeCatalogs {
  private val parser = new OptionParser[MergeCatalogConfig](getClass.getSimpleName) {
    head(getClass.getSimpleName)

    opt[String]('c', "catalog")
      .action { case (catalog, config) => config.copy(catalog = Some(Paths.get(catalog))) }
      .text("The first input catalog file (csv)")
      .required()

    opt[String]('s', "secondary-catalog")
      .action { case (catalog, config) => config.copy(secondaryCatalog = Some(Paths.get(catalog))) }
      .text("The second input catalog file (csv)")
      .required()

    opt[String]('o', "output-catalog")
      .action { case (catalog, config) => config.copy(outputCatalog = Some(Paths.get(catalog))) }
      .text("The output catalog file (csv)")
      .required()

    help("help").text("prints this usage text")
  }

  /** Merge two given catalogs into an output catalog
    *
    * @param args [CATALOG_1] [CATALOG_2] [OUTPUT_CATALOG]
    */
  def main(args: Array[String]): Unit = {
    parser.parse(args, MergeCatalogConfig()) match {
      case Some(MergeCatalogConfig(Some(catalog), Some(secondaryCatalog), Some(outputCatalog))) =>
        val allEntries = EntryPersistence.read(catalog) ++ EntryPersistence.read(secondaryCatalog)
        EntryPersistence.write(allEntries, outputCatalog)
      case _ =>
    }
  }
}
