package com.nicolasgarcia.filesystem.logic;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.stream.Collectors;

/** A service for managing segmentation. */
class SegmentationTableService {
  private static final String BASE_LOGIC_PATH = "/";
  /** A mapping from depth to their associated files, which are indexed by file name. */
  private final Map<Integer, Map<String, FileMetaData>> data = new ConcurrentHashMap<>();
  /** A queue that contains all the free fragments between files. */
  private final Queue<FileMetaData> fragmentedSpace = new PriorityBlockingQueue<>();

  Queue<FileMetaData> getFragmentedSpace() {
    return fragmentedSpace;
  }

  void addFragmentedSpace(Queue<FileMetaData> newFragmentedSpace) {
    this.fragmentedSpace.addAll(newFragmentedSpace);
  }

  public FileMetaData pollFragmentedSpace() {
    return this.fragmentedSpace.poll();
  }

  Optional<FileMetaData> find(String absolutePath) {
    String[] filePath = absolutePath.split(BASE_LOGIC_PATH);
    String fileName = filePath[filePath.length - 1];
    Map<String, FileMetaData> filesFromDepth =
        data.getOrDefault(getDepth(absolutePath), new ConcurrentHashMap<>());
    return Optional.ofNullable(filesFromDepth.get(fileName))
        .filter(file -> file.getAbsolutePath().equals(absolutePath));
  }

  /**
   * Adds or replace a {@link FileMetaData}.
   *
   * <p>There are some cases (e.g memory compaction) where it's not needed to update the fragment
   * space because is already done on their side. This could cause inconsistencies, is advisable to
   * avoid it.
   */
  void addOrReplace(FileMetaData fileMetaData, boolean updateFragmentedSpace) {
    int depth = getDepth(fileMetaData.getAbsolutePath());
    Map<String, FileMetaData> filesFromDepth = data.getOrDefault(depth, new ConcurrentHashMap<>());

    // We update the fragmented table with the old fileMetaData.
    if (updateFragmentedSpace && filesFromDepth.containsKey(fileMetaData.getFileName())) {
      fragmentedSpace.offer(filesFromDepth.get(fileMetaData.getFileName()));
    }

    filesFromDepth.put(fileMetaData.getFileName(), fileMetaData);
    data.put(depth, filesFromDepth);
  }

  void delete(FileMetaData file) {
    int depth = getDepth(file.getAbsolutePath());
    Map<String, FileMetaData> filesFromDepth = data.getOrDefault(depth, new ConcurrentHashMap<>());
    filesFromDepth.remove(file.getFileName());
    data.put(depth, filesFromDepth);
    fragmentedSpace.offer(file);
  }

  /**
   * Merges all the contiguous free buckets. It's commonly used as a first step before memory
   * compaction.
   */
  void mergeContiguousFragmentedSpace() {
    // We should iterate until we merged every fragmented space.
    while (doMergeContiguousFragmentedSpace() > 1) {}
  }

  private int doMergeContiguousFragmentedSpace() {
    Queue<FileMetaData> newFragmentedSpace =
        new PriorityQueue<>(Comparator.comparingInt(FileMetaData::getSegmentNumber));

    while (!fragmentedSpace.isEmpty()) {
      FileMetaData previousEmptySpace = fragmentedSpace.poll();
      if (fragmentedSpace.size() == 1 && !previousEmptySpace.isContiguous(fragmentedSpace.poll())) {
        newFragmentedSpace.offer(previousEmptySpace);
        break;
      } else if (fragmentedSpace.size() == 0) {
        // we don't have more elements to compare.
        newFragmentedSpace.offer(previousEmptySpace);
        break;
      }

      FileMetaData emptySpace = fragmentedSpace.poll();
      if (previousEmptySpace.isContiguous(emptySpace)) {
        FileMetaData fileMetaData =
            new FileMetaData(
                "",
                "",
                previousEmptySpace.getFrom(),
                emptySpace.getTo(),
                previousEmptySpace.getSegmentNumber());
        newFragmentedSpace.offer(fileMetaData);
      } else {
        newFragmentedSpace.offer(previousEmptySpace);
        newFragmentedSpace.offer(emptySpace);
      }
    }
    fragmentedSpace.addAll(newFragmentedSpace);
    return newFragmentedSpace.size();
  }

  List<FileMetaData> getSortedSegmentationTable() {
    return data.values().stream()
        .map(Map::values)
        .flatMap(Collection::stream)
        .sorted(Comparator.comparingInt(FileMetaData::getSegmentNumber))
        .collect(Collectors.toList());
  }

  Optional<FileMetaData> findLastFragmentedSpace() {
    return fragmentedSpace.stream().max(Comparator.comparing(FileMetaData::getFrom));
  }

  void deleteFragmentedSpace(FileMetaData fileMetaData) {
    fragmentedSpace.remove(fileMetaData);
  }

  private static int getDepth(String absolutePath) {
    String[] filePath = absolutePath.split(BASE_LOGIC_PATH);
    return filePath.length - 2;
  }
}
