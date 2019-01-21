package com.github.fedeoasi.output

import java.io.Closeable
import java.nio.file.Path

import com.github.tototoshi.csv.CSVWriter
import wvlet.log.Logger

trait Output extends Closeable {
  def write(row: Seq[Any]): Unit
}

object Output {
  def apply(file: Option[Path], logger: Logger): Output = {
    file match {
      case Some(path) => new FileOutput(path)
      case None => new LoggingOutput(logger)
    }
  }
}

class LoggingOutput[T](logger: Logger) extends Output {
  override def write(row: Seq[Any]): Unit = {
    logger.info(row.map(_.toString).mkString(" "))
  }

  override def close(): Unit = {}
}

class FileOutput(path: Path) extends Output {
  private val writer = CSVWriter.open(path.toFile)

  override def write(row: Seq[Any]): Unit = writer.writeRow(row)

  override def close(): Unit = writer.close()
}
