package com.github.fedeoasi

import java.nio.file.Paths

import com.github.fedeoasi.catalog.GenerateCatalog
import com.github.fedeoasi.statistics.ExtensionsByFileCount.TopExtensionsResult
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class ExtensionsByFileCountTest extends AnyFunSpec with Matchers with TemporaryFiles {
  private val baseDir = Paths.get("src/test/resources/ExtensionsByFileCount")

  it("groups a catalog by extension") {
    withTempDir() { tmpDir =>
      val catalogFile = tmpDir.resolve(generateCatalogFilename())
      GenerateCatalog.generateMetadata(baseDir, catalogFile, populateMd5 = true)
      statistics.ExtensionsByFileCount.topExtensions(catalogFile) shouldBe Seq(
        TopExtensionsResult("jpg", 2, 2),
        TopExtensionsResult("txt", 1, 1)
      )
    }
  }
}
