package com.github.fedeoasi.utils

import java.io.{File, FileInputStream}
import java.nio.file.{Files, Path}
import java.time.Instant
import java.util.function.Consumer

import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import com.github.fedeoasi.Model.{DirectoryEntry, FileEntry, FileSystemEntry}
import com.github.fedeoasi.catalog.EntryIndex
import com.github.fedeoasi.output.Logging
import org.apache.commons.codec.digest.DigestUtils
import resource.managed

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

/** Walks the file system tree and exposes all new entries one by one to a `Consumer`.
  *
  * Supports incremental walks by using an index of existing entries supplied as `existingEntryIndex` parameter.
  */
class FileSystemWalk(directory: Path, existingEntryIndex: EntryIndex, populateMd5: Boolean = true) extends Logging {
  require(directory.toFile.isDirectory)

  def traverse(consumer: Consumer[FileSystemEntry])(implicit materializer: ActorMaterializer): Unit = {
    val runStream = StreamConverters.fromJavaStream(() => Files.walk(directory))
      .map(toFileSystemEntry)
      .runWith(Sink.foreach(entry => entry.foreach(consumer.accept)))
    Await.ready(runStream, 6.hours)
  }

  private def createDirectory(file: File): DirectoryEntry = {
    DirectoryEntry(file.getParent, file.getName, Instant.ofEpochMilli(file.lastModified()))
  }

  private def createFile(file: File): Option[FileEntry] = {
    Try {
      val md5 = if (populateMd5) {
        info(file.getPath)
        managed(new FileInputStream(file)).acquireAndGet { fis => Some(DigestUtils.md5Hex(fis))}
      } else {
        None
      }
      FileEntry(file.getParent, file.getName, md5, file.length(), Instant.ofEpochMilli(file.lastModified()))
    } match {
      case Success(fileEntry) => Some(fileEntry)
      case Failure(_) =>
        info(s"Error processing file ${file.getPath}" )
        None
    }
  }

  private def toFileSystemEntry(path: Path): Option[FileSystemEntry] = {
    if (!existingEntryIndex.contains(path.toFile.getPath)) {
      if (path.toFile.isDirectory) {
        Some(createDirectory(path.toFile))
      } else {
        createFile(path.toFile)
      }
    } else {
      None
    }
  }

  private class PathConsumer(entryConsumer: Consumer[FileSystemEntry]) extends Consumer[Path] {
    override def accept(path: Path): Unit = {
      if (!existingEntryIndex.contains(path.toFile.getPath)) {
        if (path.toFile.isDirectory) {
          entryConsumer.accept(createDirectory(path.toFile))
        } else {
          createFile(path.toFile).foreach(entryConsumer.accept)
        }
      }
    }
  }
}
