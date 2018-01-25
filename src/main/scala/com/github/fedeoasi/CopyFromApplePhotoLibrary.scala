package com.github.fedeoasi

import java.nio.file.{Files, Paths, StandardCopyOption}
import java.time.{LocalDateTime, ZoneOffset}

import com.github.fedeoasi.Model.{FileEntries, FileEntry}

object RecoverApplePhotosLibrary {
  def photos(files: Seq[FileEntry]): Seq[FileEntry] = {
    val imageFiles = files.filter(_.extension.exists(_.equalsIgnoreCase("png")))
    val data = imageFiles.filter(_.path.contains("/Data/"))
    val masters = imageFiles.filter(_.path.contains("/Masters/"))
    val thumbnails = imageFiles.filter(_.path.contains("/Thumbnails/"))
    val previews = imageFiles.filter(_.path.contains("/Previews/"))
    println(s"data: ${data.size} masters: ${masters.size} thumbnails: ${thumbnails.size} previews: ${previews.size}")
    masters
  }

  def main(args: Array[String]): Unit = {
    val catalog = args(0)
    val entries = EntryPersistence.read(catalog)
    photos(FileEntries(entries))
  }
}

object CopyFromApplePhotoLibrary {
  def main(args: Array[String]): Unit = {
    val extension = args(0)
    val catalog = args(1)
    val outputFolder = args(2)
    val existingCatalog = args(3)
    val uniqueFiles = readUniqueEntriesForCatalog(extension, catalog)
    val uniqueFilesForOtherCatalog = readUniqueEntriesForCatalog(extension, existingCatalog)

    val uniqueFileByHash = uniqueFiles.map { f => (f.md5, f) }.toMap
    val uniqueFilesForOtherCatalogByHash = uniqueFilesForOtherCatalog.map { f => (f.md5, f) }.toMap

    val newUniqueFiles = uniqueFileByHash.keySet.diff(uniqueFilesForOtherCatalogByHash.keySet).map(uniqueFileByHash)

    println(s"${newUniqueFiles.size} files are not already in the old catalog ($existingCatalog)")

    val uniqueFilesByYear = newUniqueFiles.groupBy(f => LocalDateTime.ofInstant(f.modifiedTime, ZoneOffset.UTC).getYear)
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

  private def readUniqueEntriesForCatalog(extension: String, catalog: String): Seq[FileEntry] = {
    val entries = EntryPersistence.read(catalog).filter(_.path.contains("/Masters/"))
    val filesByExtension = ExtensionsByFileCount.groupByExtension(entries)
    val filesForExtension = filesByExtension.getOrElse(extension, Seq.empty)
    val uniqueFiles = filesForExtension.map { f => (f.md5, f) }.toMap.values
    val totalSize = uniqueFiles.map(_.size).sum

    println(s"There are ${filesForExtension.size} $extension files for total size ${filesForExtension.map(_.size).sum}")
    println(s"There are ${uniqueFiles.size} unique files of total size $totalSize for extension $extension in catalog $catalog")
    uniqueFiles.toSeq
  }
}
