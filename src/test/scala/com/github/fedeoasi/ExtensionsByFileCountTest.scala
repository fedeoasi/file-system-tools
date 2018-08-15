package com.github.fedeoasi

import java.nio.file.Paths

import com.github.fedeoasi.ExtensionsByFileCount.TopExtensionsResult
import org.scalatest.{FunSpec, Matchers}

class ExtensionsByFileCountTest extends FunSpec with Matchers with TemporaryFiles {
  private val baseDir = Paths.get("src/test/resources/ExtensionsByFileCount")

  it("groups a catalog by extension") {
    withTempDir() { tmpDir =>
      val catalogFile = tmpDir.resolve(generateCatalogFilename())
      GenerateCatalog.generateMetadata(baseDir, catalogFile, populateMd5 = true)
      ExtensionsByFileCount.topExtensions(catalogFile) shouldBe Seq(
        TopExtensionsResult("jpg", 2, 2),
        TopExtensionsResult("txt", 1, 1)
      )
    }
  }
}
