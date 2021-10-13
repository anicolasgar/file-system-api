package com.jetbrains.filesystem.logic;

import static com.jetbrains.filesystem.utils.SerializationUtils.serialize;

import com.jetbrains.filesystem.exceptions.FileCorruptedException;
import com.jetbrains.filesystem.exceptions.FileNotFoundException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.StreamCorruptedException;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
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
  private static final String BASE_LOGIC_PATH = "/";
  private final StorageService storageService;
  private final SegmentationTable segmentationTable;
  private final AtomicInteger nextAvailableBit;
  private final AtomicInteger nextSegmentNumber;

  public FileManager(StorageService storageService) {
    this.storageService = storageService;
    this.segmentationTable = new SegmentationTable();
    this.nextAvailableBit = new AtomicInteger(0);
    this.nextSegmentNumber = new AtomicInteger(0);
  }

  void save(File file) {
    byte[] serializedFile = serialize(file);

    Optional<FileMetaData> oldFileMetaData = segmentationTable.find(file.getAbsolutePath());

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
    segmentationTable.addOrReplace(fileMetaData);
    storageService.storeInContainer(serializedFile, from);
  }

  File read(String absolutePath) {
    FileMetaData fileMetaData =
        segmentationTable.find(absolutePath).orElseThrow(FileNotFoundException::new);
    return findFile(fileMetaData.getFrom(), fileMetaData.getTo());
  }

  private File findFile(int fromPosition, int toPosition) {
    try {
      byte[] bytes = storageService.readFromContainer(fromPosition, toPosition - fromPosition);
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
        segmentationTable.find(absolutePath).orElseThrow(FileNotFoundException::new);
    segmentationTable.delete(fileMetaData);
    storageService.dropFromContainer(fileMetaData.getFrom(), fileMetaData.getTo());
  }

  Map<String, String> metrics() {
    Map<String, String> stats = new HashMap<>();
    long containerSize = storageService.getContainerSize();

    stats.put("can_write", String.valueOf(storageService.isAllowedToWriteInContainer()));
    stats.put("container_size", String.valueOf(containerSize));
    stats.put("empty_fragments", String.valueOf(segmentationTable.getFragmentedSpace().size()));
    return stats;
  }

  void compactMemory() {
    // todo: Implement this!
  }

  static class SegmentationTable {
    /** A mapping from depth to their associated files, which are indexed by file name. */
    private final Map<Integer, Map<String, FileMetaData>> data = new ConcurrentHashMap<>();
    /** A queue that contains all the free fragments between files. */
    private final Queue<FileMetaData> fragmentedSpace =
        new PriorityQueue<>(Comparator.comparingInt(FileMetaData::getSegmentNumber));

    public Map<Integer, Map<String, FileMetaData>> getData() {
      return data;
    }

    public Queue<FileMetaData> getFragmentedSpace() {
      return fragmentedSpace;
    }

    Optional<FileMetaData> find(String absolutePath) {
      String[] filePath = absolutePath.split(BASE_LOGIC_PATH);
      String fileName = filePath[filePath.length - 1];
      Map<String, FileMetaData> filesFromDepth =
          data.getOrDefault(getDepth(absolutePath), new ConcurrentHashMap<>());
      return Optional.ofNullable(filesFromDepth.get(fileName))
          .filter(file -> file.getAbsolutePath().equals(absolutePath));
    }

    void addOrReplace(FileMetaData fileMetaData) {
      int depth = getDepth(fileMetaData.getAbsolutePath());
      Map<String, FileMetaData> filesFromDepth =
          data.getOrDefault(depth, new ConcurrentHashMap<>());

      if (filesFromDepth.containsKey(fileMetaData.getFileName())) {
        fragmentedSpace.offer(fileMetaData);
      }

      filesFromDepth.put(fileMetaData.getFileName(), fileMetaData);
      data.put(depth, filesFromDepth);
    }

    void delete(FileMetaData file) {
      int depth = getDepth(file.getAbsolutePath());
      Map<String, FileMetaData> filesFromDepth =
          data.getOrDefault(depth, new ConcurrentHashMap<>());
      filesFromDepth.remove(file.getFileName());
      data.put(depth, filesFromDepth);
      fragmentedSpace.offer(file);
    }

    static int getDepth(String absolutePath) {
      String[] filePath = absolutePath.split(BASE_LOGIC_PATH);
      return filePath.length - 2;
    }
  }
}
