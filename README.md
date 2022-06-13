# file-system-api
 
## Instructions
Design and implement a library to emulate the typical file system API, which will actually store files and folders in one single container file. Do not compress the data, minimize used RAM, minimise disk operations.

Create-write-read-append-delete-rename-move operations should be supported.
Keep balance between container file size and used resources.
Provide a metric and routine for container file maintenance - if required.

Provide unit tests for all features.

Include at least one complete functional test: store all project tree, erase 70% of all files, compact container, add all files again into new subfolder. Close container file, reopen and verify all content.

## :bookmark: Considerations
- Before running the tests, make sure that the folder `/tmp` exists, and it has writing permission.

## :mag: Performance and scalability analysis of the final solution (CPU/RAM/DISK).
In terms of RAM, the system will always have a collection of metadata in memory. Each metadata object contains the minimum necessary information to retrieve the file stored in disk.

Also, in a different collection in memory, the system keeps track of all the free fragments that should be compacted later. For every read / write, the system gets only the very specific bytes for the requested files.

The disk operations are also minimised. Every time a file is stored, it is stored in the last part of the container. This makes it cheaper and easier to write, but with the disadvantage that any already free space is not reused. Because of this, regular compaction processes are necessary.

With regard to the CPU, the only expensive operation is the compaction, where it first merges all the contiguous free buckets and then moves all the necessary files.

## :chart_with_downwards_trend: Not covered
The following features are not supported / logic is not implemented:
- Symbolic links.
- Empty folders are not cleaned-up.
- Some hardcoded strings could be extracted to a properties file or similar (e.g BASE_PHYSICAL_PATH, CONTAINER_NAME, etc).
- All the metadata is not persisted. In case the service is shut down, the whole filesystem should be re-generated.
- Even though most of the structures used are thread safe, everything runs in the main thread. There’re still some pending tweaks to make it safe to have a thread pool and execute tasks in parallel.
- Don’t support different privileges / rights.
- Test coverage should be improved. Some edge cases + tests for some methods are missing.
- Only 3 different metrics were provided:
  - Whether the container can be written or not.
  - The size of the container.
  - The number of empty fragments.


