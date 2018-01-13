# file-system-tools

A set of command line tools to help organizing a file system. The main
use case for this is an external hard drive or a backup folder.

# Workflow

The main idea is that we can access the file system once and all the
metadata into a file. We can then perform some operations on the metadata
file such as finding duplicate files or the folders that contain a
specific file type.

After some modifications have been made on the file system, we can
incrementally update the metadata file.

## Operations


### Generate the metadata file and incrementally add new items

runMain com.github.fedeoasi.GenerateMetadata <FOLDER>

- Find entries that have been deleted and remove them from the metadata
  file
- Find the folders that contain a file type
- Find the folders that contain the most number of files
- Find duplicate files
- Find folders that contain the most duplicate files
- Find duplicated folders
- Find the most common extensions
