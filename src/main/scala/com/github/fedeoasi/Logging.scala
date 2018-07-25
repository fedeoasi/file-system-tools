package com.github.fedeoasi

import wvlet.log.{LogFormatter, LogSupport, Logger}

trait Logging extends LogSupport {
  Logger.setDefaultFormatter(LogFormatter.AppLogFormatter)
}
