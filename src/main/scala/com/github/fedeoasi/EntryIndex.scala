package com.github.fedeoasi

import java.io.File

class EntryIndex(val filesByDir: Map[String, Seq[String]]) {
  def contains(entryPath: String): Boolean = {
    if (filesByDir.contains(entryPath)) {
      true
    } else {
      val (folder, filename) = folderAndName(entryPath)
      filesByDir.get(folder) match {
        case Some(files) if files.contains(filename) => true
        case _ => false
      }
    }
  }

  def folderAndName(entryPath: String): (String, String) = {
    val file = new File(entryPath)
    (file.getParent, file.getName)
  }
}
