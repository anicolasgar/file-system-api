package com.nicolasgarcia.filesystem.logic;

import static com.nicolasgarcia.filesystem.utils.SerializationUtils.serialize;

import com.nicolasgarcia.filesystem.api.File;
import com.nicolasgarcia.filesystem.exceptions.FileCorruptedException;
import com.nicolasgarcia.filesystem.exceptions.FileNotFoundException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.StreamCorruptedException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service that manages the logic for the emulated file system.
 *
 * <p>It interacts with a {@link StorageService}, which is the responsible to store/retrieve the
 * current status of the emulated file system into/from a single container.
 */
class FileManager {
  private static final Logger LOG = LoggerFactory.getLogger(FileManager.class);
  private final StorageService storageService;
  private final SegmentationTableService segmentationTableService;
  private final AtomicInteger nextAvailableBit;
  private final AtomicInteger nextSegmentNumber;

  public FileManager(StorageService storageService) {
    this.storageService = storageService;
    this.segmentationTableService = new SegmentationTableService();
    this.nextAvailableBit = new AtomicInteger(0);
    this.nextSegmentNumber = new AtomicInteger(0);
  }

  void save(File file) {
    byte[] serializedFile = serialize(file);

    Optional<FileMetaData> oldFileMetaData = segmentationTableService.find(file.getAbsolutePath());

    oldFileMetaData.ifPresent(
        fileMetaData ->
            storageService.dropFromContainer(fileMetaData.getFrom(), fileMetaData.getTo()));

    int from = nextAvailableBit.getAndAdd(serializedFile.length);
    FileMetaData fileMetaData =
        new FileMetaData(
            file.getFileName(),
            file.getAbsolutePath(),
            from,
            from + serializedFile.length,
            nextSegmentNumber.getAndIncrement());
    segmentationTableService.addOrReplace(fileMetaData, true);
    storageService.storeInContainer(serializedFile, from);
  }

  File read(String absolutePath) {
    FileMetaData fileMetaData =
        segmentationTableService.find(absolutePath).orElseThrow(FileNotFoundException::new);
    return findFile(fileMetaData.getFrom(), fileMetaData.getTo());
  }

  private File findFile(int fromPosition, int toPosition) {
    try {
      byte[] bytes = storageService.readFromContainer(fromPosition, toPosition);
      if (bytes.length == 0) {
        throw new FileNotFoundException();
      }
      ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
      ObjectInput in = new ObjectInputStream(bis);
      Object obj = in.readObject();
      if (obj == null) {
        throw new FileNotFoundException();
      }
      return (File) obj;

    } catch (StreamCorruptedException e) {
      LOG.warn("File logically deleted - Compaction needed", e);
      throw new FileNotFoundException();
    } catch (IOException e) {
      LOG.warn("Failed creating ObjectInputStream", e);
      throw new IllegalStateException();
    } catch (ClassNotFoundException e) {
      LOG.warn("Failed to deserialize file", e);
      throw new FileCorruptedException();
    }
  }

  void delete(String absolutePath) {
    FileMetaData fileMetaData =
        segmentationTableService.find(absolutePath).orElseThrow(FileNotFoundException::new);
    segmentationTableService.delete(fileMetaData);
    storageService.dropFromContainer(fileMetaData.getFrom(), fileMetaData.getTo());
  }

  Map<String, String> metrics() {
    Map<String, String> stats = new HashMap<>();
    long containerSize = storageService.getContainerSize();

    stats.put("can_write", String.valueOf(storageService.isAllowedToWriteInContainer()));
    stats.put("container_size", String.valueOf(containerSize));
    stats.put(
        "empty_fragments", String.valueOf(segmentationTableService.getFragmentedSpace().size()));
    return stats;
  }

  synchronized void compactMemory() {
    LOG.info("Started memory compaction...");
    // We should iterate until we don't move files anymore.
    while (doCompactMemory() > 0) {}

    // Now we should drop all the empty files at the end of the container.
    segmentationTableService
        .findLastFragmentedSpace()
        .ifPresent(
            fileMetaData -> {
              storageService.resizeContainer(fileMetaData);

              nextAvailableBit.set(fileMetaData.getFrom());
              segmentationTableService.deleteFragmentedSpace(fileMetaData);
            });
    LOG.info("Memory compaction done");
  }

  private int doCompactMemory() {
    segmentationTableService.mergeContiguousFragmentedSpace();
    List<FileMetaData> sortedSegmentationTable =
        segmentationTableService.getSortedSegmentationTable();
    List<FileMetaData> newFileMetaDataList = new ArrayList<>();
    Queue<FileMetaData> newFragmentedSpace =
        new PriorityQueue<>(Comparator.comparingInt(FileMetaData::getSegmentNumber));

    while (!segmentationTableService.getFragmentedSpace().isEmpty()) {
      FileMetaData emptySpace = segmentationTableService.pollFragmentedSpace();
      Optional<FileMetaData> fileMetaDataToMoveOpt =
          findFileToMove(sortedSegmentationTable, emptySpace);

      fileMetaDataToMoveOpt.ifPresentOrElse(
          fileMetaDataToMove -> {
            byte[] fileToMoveBytes =
                storageService.readFromContainer(
                    fileMetaDataToMove.getFrom(), fileMetaDataToMove.getTo());

            FileMetaData newEmptySpace =
                generateNewEmptySpace(fileMetaDataToMove, fileToMoveBytes, emptySpace);
            newFragmentedSpace.offer(newEmptySpace);

            // Update the new meta data for the moved file.
            FileMetaData newFileMetaData =
                generateNewMetaData(fileMetaDataToMove, fileToMoveBytes, emptySpace);

            newFileMetaDataList.add(newFileMetaData);
          },
          () -> newFragmentedSpace.offer(emptySpace));
    }
    newFileMetaDataList.forEach(f -> segmentationTableService.addOrReplace(f, false));
    segmentationTableService.addFragmentedSpace(newFragmentedSpace);
    return newFileMetaDataList.size();
  }

  private FileMetaData generateNewEmptySpace(
      FileMetaData fileMetaData, byte[] fileToMoveBytes, FileMetaData emptySpace) {
    // Drop the file in old address store it in the new one.
    storageService.dropFromContainer(fileMetaData.getFrom(), fileMetaData.getTo());
    storageService.storeInContainer(fileToMoveBytes, emptySpace.getFrom());

    return new FileMetaData(
        emptySpace.getFileName(),
        emptySpace.getAbsolutePath(),
        emptySpace.getFrom() + fileToMoveBytes.length,
        emptySpace.getTo(),
        fileMetaData.getSegmentNumber());
  }

  private static FileMetaData generateNewMetaData(
      FileMetaData fileMetaData, byte[] fileToMoveBytes, FileMetaData emptySpace) {
    return new FileMetaData(
        fileMetaData.getFileName(),
        fileMetaData.getAbsolutePath(),
        emptySpace.getFrom(),
        emptySpace.getFrom() + fileToMoveBytes.length,
        emptySpace.getSegmentNumber());
  }

  private static Optional<FileMetaData> findFileToMove(
      List<FileMetaData> sortedSegmentationTable, FileMetaData emptySpace) {
    return sortedSegmentationTable.stream()
        .filter(f -> emptySpace.getTo() <= f.getFrom())
        .findFirst();
  }
}
