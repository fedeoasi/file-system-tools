package com.github.fedeoasi.catalog

import java.nio.file.Path
import java.time.Instant

import com.github.fedeoasi.Model.{DirectoryEntry, FileEntry, FileSystemEntry}
import com.github.fedeoasi.catalog.EntryPersistence._
import com.github.tototoshi.csv.CSVReader
import resource.managed

class EntryReader(catalog: Path) {
  def count(): Int = fold(0) { case (acc, _) => acc + 1 }

  def readEntries(): Seq[FileSystemEntry] = {
    read(toFileSystemEntry)
  }

  def readPaths(): Seq[String] = {
    read(row => toFileSystemEntry(row).path)
  }

  def readFolderPaths(): Seq[String] = {
    readOpt {
      case row if row(EntryTypeField) == "D" => Some(toFileSystemEntry(row).path)
      case _ => None
    }
  }

  def readIndex(): EntryIndex = {
    val dirPaths = readFolderPaths()
    val emptyFilesByDir = dirPaths.map { dir => dir -> Seq.empty[String] }.toMap
    val filesByDir = fold(emptyFilesByDir) { case (acc, row) =>
      row match {
        case FileEntry(parentPath, name, _, _, _) => acc.updated(parentPath, name +: acc(parentPath))
        case _ => acc
      }
    }
    new EntryIndex(filesByDir)
  }

  def read[T](transform: Map[String, String] => T): Seq[T] = {
    managed(csvReader).acquireAndGet(_.iteratorWithHeaders.map(transform).toList)
  }

  def readOpt[T](transform: Map[String, String] => Option[T]): Seq[T] = {
    managed(csvReader).acquireAndGet(_.iteratorWithHeaders.flatMap(map => transform(map)).toList)
  }

  def fold[T](init: T)(f: (T, FileSystemEntry) => T): T = {
    managed(csvReader).acquireAndGet(_.iteratorWithHeaders.foldLeft(init) { case (acc, row) => f(acc, toFileSystemEntry(row)) })
  }

  private def toFileSystemEntry(row: Map[String, String]): FileSystemEntry = {
    val modifiedTime = Instant.ofEpochMilli(row(ModifiedTimeField).toLong)
    row(EntryTypeField) match {
      case "F" =>
        val md5Value = row(Md5Field)
        val md5 = if (md5Value.nonEmpty) Some(md5Value) else None
        FileEntry(row(ParentField), row(NameField), md5, row(SizeField).toLong, modifiedTime)
      case "D" => DirectoryEntry(row(ParentField), row(NameField), modifiedTime)
      case other => throw new RuntimeException(s"Unrecognized entry type $other")
    }
  }

  private def csvReader: CSVReader = CSVReader.open(catalog.toFile)
}



object Entries {
  val header = Seq(EntryTypeField, ParentField, NameField, Md5Field, SizeField, ModifiedTimeField)
}
