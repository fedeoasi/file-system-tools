# File System Tools

A set of command line tools to help organize a file system. The main
use case for this is an external hard drive or a backup folder.

## Workflow

The main idea is that we can access the file system once and all the
metadata into a catalog file. We can then perform some operations based
on the catalog such as finding duplicate files or listing the folders
that contain a specific file type.

After some modifications have been made on the file system, we can
incrementally update the catalog.

## Tools

The single entry point for all the operations provided by file-system
tools is a class called `CliMain`. It exposes as commands all the
operations described below.

Using this entry point without any arguments will print out descriptive
usage instructions. Using the `run` sbt command will run `CliMain`.

In an SBT console:
```
# Show a list of commands along with their description
run

# Run a specific command with arguments
run <COMMAND> <ARGS>
```

### Catalog Operations

- `generate-catalog`: Generate the catalog and incrementally add new items
- `deletion-checker`: Find catalog entries that were deleted and remove them
  from the catalog
- `merge-catalogs`: Merge two catalogs into an output catalog

### Search

- `folder-by-name`: Find folders by name
- `find-file-by-md5`: Find the folders that contain a file type
- `folders-having-extension`: Prints folders that contain files with a given extension.

### Deduplication

- `find-duplicate-files`: Find duplicate files in a catalog
- `find-identical-folders`: Find identical folders in a catalog
- `folder-similarity`: Ranks folder pairs by MD5 sum similarity
- `folder-pair-similarity`: Compute the MD5 sum similarity between a pair of folders
- `lookup-duplicates`: Lookup duplicates within two given folders

### Statistics

- `extensions-by-file-count`: Find the most common extensions
- `files-by-size`: Rank files by size
- `folders-by-file-count`: Rank folders by file count
- `folders-by-file-size`: Rank folders by file size
- `total-size`: Compute the catalog total size

## Scala Native Package

To build a Scala Native package, run:

```
sbt universal:packageBin
```

This will generate a zip file under `target/universal`, which you can
extract anywhere and run the commands:

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

#### Generate a catalog for the external hard drive starting from a root folder:

```
run generate-catalog --inputFolder <ROOT_FOLDER> --catalog <CATALOG_NAME>
```

Note: GenerateCatalog computes the md5 sums for all files by default.
You can disable the sums generation using the flag `--populate-md5 false`.
The deduplication tools only work when the md5 sums have been generated.

#### Files by Size

```
run files-by-size --catalog <CATALOG_NAME>
```

The command above prints the biggest files. Use this tool to find files
to delete. This will save disk space and speeds up the md5 computation
when generating the catalog.

If you delete some files, you can then update the catalog using `deletion-checker`.

```
run deletion-checker --catalog <CATALOG_NAME>
```

Note: You can pass an optional folder to restrict the search for deleted
files using the `--folder` parameter.

#### Folders by File Count

```
run folders-by-file-count --catalog <CATALOG_NAME> --folder <ROOT_FOLDER>
```

The above command prints the folders containing the most files. Deleting
folders with many files that are not needed will speed up the generation
and analysis for the rest of the catalog.

#### Folders by File Size

```
run folders-by-file-size --catalog <CATALOG_NAME> --folder <ROOT_FOLDER>
```

The above command prints the folders whose sum of file size is largest.
Deleting folders with big files that are not needed will speed up the
generation and analysis for the rest of the catalog.

#### File Counts by Extension

```
run extensions-by-file-count --catalog <CATALOG_NAME>
```

The above prints file extensions ranked by number of files. This helps
identifying undesired extensions that use a lot of files (e.g., svn files,
local caching, etc) and how many images or documents are on the drive.

#### Find Duplicate Files

(Requires md5 sums)

```
run find-duplicate-files --catalog <CATALOG_NAME>
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
