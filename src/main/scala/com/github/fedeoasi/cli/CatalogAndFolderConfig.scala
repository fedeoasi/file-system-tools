package com.github.fedeoasi.cli

import java.nio.file.Path

case class CatalogAndFolderConfig(catalog: Option[Path] = None, folder: Option[Path] = None)
