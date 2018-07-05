# File System Tools

A set of command line tools to help organizing a file system. The main
use case for this is an external hard drive or a backup folder.

## Workflow

The main idea is that we can access the file system once and all the
metadata into a catalog file. We can then perform some operations based
on the catalog such as finding duplicate files or listing the folders
that contain a specific file type.

After some modifications have been made on the file system, we can
incrementally update the catalog.

## Tools

### Catalog Operations

- `GenerateCatalog`: Generate the catalog and incrementally add new items
- `DeletionChecker`: Find catalog entries that were deleted and remove them from the
  catalog

### Search

- `FolderByName`: Find folders by name
- `FoldersContainingExtension`: Find the folders that contain a file type

### Deduplication

- `FindDuplicateFiles`: Find duplicated files
- `FindDuplicateFilesForFolder`: Find duplicated files from a target folder
- `FindIdenticalFolders`: Find identical folders
- `FindSimilarFolders`: Find folders that have a large number of common files

### Statistics

- `FoldersByFileCount`: Rank folders by file count
- `FilesBySize`: Rank files by size
- `TotalSize`: Compute the total size of the files in the catalog
- `ExtensionsByFileCount`: Find the most common extensions

## Scala Native Package

To build a Scala Native package, run:

```
sbt universal:packageBin
```

This will generate a zip file under `target/universal`, which you can extract anywhere and run the commands:

```
cd target/universal
unzip file-system-tools-*.zip
cd file-system-tools-*
./bin/generate-catalog --help
```

You can also [generate other kinds of packages](https://www.scala-sbt.org/sbt-native-packager/gettingstarted.html#create-a-package).

NOTE: You may have to set `JAVA_HOME` appropriately for the shell scripts to work.
