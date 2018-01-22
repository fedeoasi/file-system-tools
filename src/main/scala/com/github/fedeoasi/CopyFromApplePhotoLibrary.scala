package com.github.fedeoasi

import java.nio.file.{Files, Paths, StandardCopyOption}
import java.time.{LocalDateTime, ZoneOffset}

object CopyFromApplePhotoLibrary {
  def main(args: Array[String]): Unit = {
    val extension = args(0)
    val catalog = args(1)
    val outputFolder = args(2)
    val entries = EntryPersistence.read(catalog).filter(_.path.contains("/Masters/"))
    val filesByExtension = ExtensionsByFileCount.groupByExtension(entries)
    val filesForExtension = filesByExtension.getOrElse(extension, Seq.empty)
    val uniqueFiles = filesForExtension.map { f => (f.md5, f) }.toMap.values
    val totalSize = uniqueFiles.map(_.size).sum

    println(s"There are ${filesForExtension.size} $extension files for total size ${filesForExtension.map(_.size).sum}")
    println(s"There are ${uniqueFiles.size} unique files of total size $totalSize for extension $extension in catalog $catalog")

    val uniqueFilesByYear = uniqueFiles.groupBy(f => LocalDateTime.ofInstant(f.modifiedTime, ZoneOffset.UTC).getYear)
    println(uniqueFilesByYear.mapValues(_.size).toSeq.sortBy(_._1))

    val toFolder = Paths.get(outputFolder)

    uniqueFilesByYear.foreach { case (year, files) =>
      val folderForYear = toFolder.resolve(year.toString)
      folderForYear.toFile.mkdir()
      println(s"Copying ${files.size} files for $year")
      files.foreach { file =>
        val newFileName = s"${file.parent.split("/").last}_${file.name}"
        val targetFile = folderForYear.resolve(newFileName)
        Files.copy(Paths.get(file.path), targetFile, StandardCopyOption.COPY_ATTRIBUTES)
      }
    }
  }
}
