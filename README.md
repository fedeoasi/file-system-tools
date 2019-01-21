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

- `CliMain`: A single entry point for all the operations described below.  Using this entry point without any arguments will print out descriptive usage instructions.

### Catalog Operations

- `GenerateCatalog`: Generate the catalog and incrementally add new items
- `DeletionChecker`: Find catalog entries that were deleted and remove them from the
  catalog

### Search

- `FolderByName`: Find folders by name
- `FoldersContainingExtension`: Find the folders that contain a file type

### Deduplication

- `FindDuplicateFiles`: Find duplicated files
- `FindIdenticalFolders`: Find identical folders
- `FindSimilarFolders`: Find folders that have a large number of common files
- FolderPairSimilarity: Compute the similarity between a pair of folders

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

## Use Case: Organizing an External Hard Drive

### Goals

- Minimize the number of files
- Have at most one copy of a file
- Have a common directory structure (photos, projects, documents)
- Minimize the disk space (lower priority)

### Workflow

#### Generate a catalog for the external hard drive starting from a root
  folder:

```
runMain com.github.fedeoasi.GenerateCatalog --inputFolder <ROOT_FOLDER>
  --catalog <CATALOG_NAME>
```

Note: GenerateCatalog computes the md5 sums for all files by default.
You can disable the sums generation using the flag `--populate-md5 false`.
The deduplication tools only work when the md5 sums have been generated.

#### Files by Size

```
runMain com.github.fedeoasi.FilesBySize --catalog <CATALOG_NAME>
```

The command above prints the biggest files. Use this tool to find files
to delete. This will save disk space and speeds up the md5 computation
when generating the catalog.

If you delete some files, you can then update the catalog using `DeletionChecker`.

```
runMain com.github.fedeoasi.DeletionChecker --catalog <CATALOG_NAME>
```

Note: You can pass an optional folder to restrict the search for deleted
files using the `--folder` parameter.

#### Folders by File Count

```
runMain com.github.fedeoasi.FoldersByFileCount --catalog <CATALOG_NAME>
--folder <ROOT_FOLDER>
```

The above command prints the folders containing the most files. Deleting
folders with many files that are not needed will speed up the generation
and analysis for the rest of the catalog.

#### Folders by File Size

```
runMain com.github.fedeoasi.FoldersByFileSize --catalog <CATALOG_NAME>
--folder <ROOT_FOLDER>
```

The above command prints the folders whose sum of file size is largest.
Deleting folders with big files that are not needed will speed up the
generation and analysis for the rest of the catalog.

#### File Counts by Extension

```
runMain com.github.fedeoasi.ExtensionsByFileCount --catalog <CATALOG_NAME>
```

The above prints file extensions ranked by number of files. This helps
identifying undesired extensions that use a lot of files (e.g., svn files,
local caching, etc) and how many images or documents are on the drive.

#### Find Duplicate Files

(Requires md5 sums)

```
runMain com.github.fedeoasi.FindDuplicateFiles --catalog <CATALOG_NAME>
```

The above prints a list of files that are duplicated (one or more
identical copies) sorted by size.

Note: A `folder` parameter can be used to limit the duplicate search to files in
a given folder. The copies will be searched for in the rest of the catalog
but only files that appear in the given folder will be reported.

You can pass an extension through the `extension` parameter so that
only files of the given extension will be analyzed. This can be used for
example to deduplicate images (e.g., jpg).

A `show-duplicates` parameter determines whether the copies should be printed
along with the original file that is duplicated.
