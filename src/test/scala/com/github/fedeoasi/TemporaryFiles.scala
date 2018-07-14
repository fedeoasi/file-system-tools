package com.github.fedeoasi

import java.nio.file.{Files, Path}
import java.util.UUID

trait TemporaryFiles {
  def withTempDir[T](prefix: String)(f: Path => T): T = {
    val tempDir = Files.createTempDirectory(prefix)
    try {
      f(tempDir)
    } finally {
      tempDir.toFile.delete()
    }
  }

  def withTmpFile[T](prefix: String, suffix: String)(f: Path => T): T = {
    val tempFile = Files.createTempFile(prefix, suffix)
    try {
      f(tempFile)
    } finally {
      tempFile.toFile.delete()
    }
  }

  def generateCatalogFilename(): String = s"${UUID.randomUUID()}.csv"
}
