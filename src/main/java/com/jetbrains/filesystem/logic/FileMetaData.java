package com.jetbrains.filesystem.logic;

import com.jetbrains.filesystem.api.File;

/**
 * Contains meta-data from a specific (existing) {@link File}.
 *
 * <p>It can also represent free memory slots.
 */
class FileMetaData {
  private final String fileName;
  private final String absolutePath;
  private final int from;
  private final int to;
  private final int segmentNumber;

  public FileMetaData(String fileName, String absolutePath, int from, int to, int segmentNumber) {
    this.fileName = fileName;
    this.absolutePath = absolutePath;
    this.from = from;
    this.to = to;
    this.segmentNumber = segmentNumber;
  }

  public String getFileName() {
    return fileName;
  }

  public String getAbsolutePath() {
    return absolutePath;
  }

  public int getFrom() {
    return from;
  }

  public int getTo() {
    return to;
  }

  public int getSegmentNumber() {
    return segmentNumber;
  }

  public boolean isContiguous(FileMetaData emptySpace) {
    return this.to == emptySpace.getFrom() || this.from == emptySpace.getTo();
  }
}
