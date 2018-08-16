package com.github.fedeoasi

import wvlet.log.{LogFormatter, LogSupport, Logger}

trait Logging extends LogSupport {
  customizeLogger()

  protected def customizeLogger(): Unit = {
    Logger.setDefaultFormatter(LogFormatter.AppLogFormatter)
  }
}
