package com.github.fedeoasi

import com.github.fedeoasi.Model.DirectoryEntry

object FolderByName {
  /** Finds a folder by name (case insensitive)
    *
    * @param args [FOLDER]
    */
  def main(args: Array[String]): Unit = {
    val entries = EntryPersistence.read(Constants.DefaultCatalogFilename)
    val matchedFolders = entries.collect { case d: DirectoryEntry if d.name.equalsIgnoreCase(args(0)) => d }
    println(matchedFolders.mkString("\n"))
  }
}
