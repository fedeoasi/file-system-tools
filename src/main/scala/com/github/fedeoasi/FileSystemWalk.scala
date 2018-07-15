package com.github.fedeoasi

import java.io.{File, FileInputStream}
import java.nio.file.Path
import java.time.Instant
import java.util.function.Consumer

import com.github.fedeoasi.Model.{DirectoryEntry, FileEntry, FileSystemEntry}
import org.apache.commons.codec.digest.DigestUtils
import resource.managed

import scala.collection.mutable
import scala.util.{Failure, Success, Try}

/** Walks the file system tree and exposes all new entries one by one to a `Consumer`.
  *
  * Supports incremental walks by taking the `existingEntries` parameter.
  */
class FileSystemWalk(directory: Path, existingEntryIndex: EntryIndex, populateMd5: Boolean = true) {
  require(directory.toFile.isDirectory)

  def traverse(consumer: Consumer[FileSystemEntry]): Unit = {
    traverse(directory.toFile, consumer)
  }

  def traverse(file: File, consumer: Consumer[FileSystemEntry]): Unit = {
    val stack = new mutable.Stack[File]()
    stack.push(file)
    while (stack.nonEmpty) {
      val curr = stack.pop()
      if (curr.isDirectory) {
        if (!existingEntryIndex.contains(curr.getPath)) {
          consumer.accept(createDirectory(curr))
        }
        curr.listFiles().foreach { n =>
          stack.push(n)
        }
      } else {
        if (!existingEntryIndex.contains(curr.getPath)) {
          createFile(curr).foreach(consumer.accept)
        }
      }
    }
  }

  private def createDirectory(file: File): DirectoryEntry = {
    DirectoryEntry(file.getParent, file.getName, Instant.ofEpochMilli(file.lastModified()))
  }

  private def createFile(file: File): Option[FileEntry] = {
    Try {
      val md5 = if (populateMd5) {
        println(file.getPath)
        managed(new FileInputStream(file)).acquireAndGet { fis => Some(DigestUtils.md5Hex(fis))}
      } else {
        None
      }
      FileEntry(file.getParent, file.getName, md5, file.length(), Instant.ofEpochMilli(file.lastModified()))
    } match {
      case Success(fileEntry) => Some(fileEntry)
      case Failure(_) =>
        println(s"Error processing file ${file.getPath}" )
        None
    }
  }
}
