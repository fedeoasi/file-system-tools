package com.github.fedeoasi.cli

import com.github.fedeoasi._

object CliMain {
  val commands: Map[String, List[CliAware]] = Map(
    "Catalog creation and update" -> List(GenerateCatalog, DeletionChecker, MergeCatalogs),
    "Lookup" -> List(FolderByName, FindFileByMd5, FoldersContainingExtension),
    "Finding duplicates" -> List(DuplicateFilesFinder, FolderPairSimilarity, FolderSimilarity, FindIdenticalFolders,
      FindDuplicateFilesWithinSecondaryFolder),
    "Catalog statistics" -> List(ExtensionsByFileCount, FilesBySize, FoldersByFileCount, FoldersByFileSize)
  )

  def main(args: Array[String]): Unit = {
    if(args.length == 0) {
      printUsage()
    } else {
      val commandName = args(0)
      commands.values.flatten.find(_.command.name == commandName) match {
        case None =>
          println(s"Unknown command: $commandName\n")
          printUsage()
        case Some(command) =>
          val commandArgs = args.drop(1)
          command.main(commandArgs)
      }
    }
  }

  private def printUsage(): Unit = {
    println("Catalog-driven utilities for finding duplicate files.\n")
    commands.foreach(group => {
      println(group._1 + ":")
      group._2.map("  " + _.command.toString).foreach(println)
      println()
    })
    println("Use \"<command> --help\" for more information about a given command.")
  }
}
