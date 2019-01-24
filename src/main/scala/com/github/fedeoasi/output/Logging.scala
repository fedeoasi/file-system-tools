package com.github.fedeoasi.output

import wvlet.log.{LogFormatter, LogSupport, Logger}

trait Logging extends LogSupport {
  customizeLogger()

  protected def customizeLogger(): Unit = {
    Logger.setDefaultFormatter(LogFormatter.AppLogFormatter)
  }
}
