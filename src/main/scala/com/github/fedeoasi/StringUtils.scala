package com.github.fedeoasi

import scala.annotation.tailrec

object StringUtils {
  def longestPrefix(s1: String, s2: String): String = {
    @tailrec
    def loop(equalUpTo: Int): Int = {
      if (s1.length > equalUpTo && s2.length > equalUpTo && s1.charAt(equalUpTo) == s2.charAt(equalUpTo)) {
        loop(equalUpTo + 1)
      } else {
        equalUpTo
      }
    }
    s1.take(loop(0))
  }
}
