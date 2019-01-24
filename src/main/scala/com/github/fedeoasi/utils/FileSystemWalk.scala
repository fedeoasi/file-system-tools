package com.github.fedeoasi.utils

import java.io.{File, FileInputStream}
import java.nio.file.{Files, Path}
import java.time.Instant
import java.util.function.Consumer

import akka.stream.{ActorMaterializer, ClosedShape}
import akka.stream.scaladsl._
import com.github.fedeoasi.Model.{DirectoryEntry, FileEntry, FileSystemEntry}
import com.github.fedeoasi.catalog.EntryIndex
import com.github.fedeoasi.output.Logging
import org.apache.commons.codec.digest.DigestUtils
import resource.managed

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

import scala.concurrent.ExecutionContext.Implicits.global

/** Walks the file system tree and exposes all new entries one by one to a `Consumer`.
  *
  * Supports incremental walks by using an index of existing entries supplied as `existingEntryIndex` parameter.
  */
class FileSystemWalk(directory: Path, existingEntryIndex: EntryIndex, populateMd5: Boolean = true) extends Logging {
  require(directory.toFile.isDirectory)

  private val countingSink = Sink.fold[Long, FileSystemEntry](0)((acc, _) => acc + 1)

  def traverse(consumer: Consumer[FileSystemEntry])(implicit materializer: ActorMaterializer): Long = {
    val consumerSink = Sink.foreach[FileSystemEntry](consumer.accept)
    val graph = RunnableGraph.fromGraph(GraphDSL.create(countingSink, consumerSink)((_, _)) { implicit builder =>
      (s1, s2) =>
        import GraphDSL.Implicits._

        val broadcast = builder.add(Broadcast[FileSystemEntry](2))
        val source = StreamConverters.fromJavaStream(() => Files.walk(directory))
          .mapConcat(e => toImmutable(toFileSystemEntry(e).toIterable))

        source ~> broadcast.in
        broadcast.out(0) ~> s1
        broadcast.out(1) ~> s2
        ClosedShape
    })

    val (futureCount, futureDone) = graph.run()
    val overallFuture = for {
      count <- futureCount
      done <- futureDone
    } yield (count, done)

    val (count, _) = Await.result(overallFuture, 6.hours)
    count
  }

  private def toImmutable[A](elements: Iterable[A]): scala.collection.immutable.Iterable[A] =
    new scala.collection.immutable.Iterable[A] {
      override def iterator: Iterator[A] = elements.toIterator
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
}
