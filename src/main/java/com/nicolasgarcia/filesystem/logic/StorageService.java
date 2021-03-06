package com.nicolasgarcia.filesystem.logic;

import com.nicolasgarcia.filesystem.exceptions.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import org.assertj.core.util.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A package-private service that interacts with the real file system. */
class StorageService {
  private static final Logger LOG = LoggerFactory.getLogger(StorageService.class);
  /** The base path of the emulated file system. */
  @VisibleForTesting static final String BASE_PHYSICAL_PATH = "/tmp/";
  /** The name of the file where all the emulated file system is stored. */
  @VisibleForTesting static final String CONTAINER_NAME = "jetbrains-assignment";

  public StorageService() {
    storeInContainer(new byte[] {}, 0);
  }

  void storeInContainer(byte[] content, int position) {
    // TODO: The subfolders should exist.
    try (RandomAccessFile raf = new RandomAccessFile(BASE_PHYSICAL_PATH + CONTAINER_NAME, "rw")) {
      raf.seek(position);
      raf.write(content);
    } catch (java.io.FileNotFoundException e) {
      LOG.warn("File not found", e);
      throw new FileNotFoundException();
    } catch (IOException e) {
      LOG.warn("Failed to store in container", e);
      throw new IllegalStateException();
    }
  }

  byte[] readAllFromContainer() {
    try {
      RandomAccessFile f = new RandomAccessFile(BASE_PHYSICAL_PATH + CONTAINER_NAME, "r");
      byte[] output = new byte[(int) f.length()];
      f.readFully(output);
      return output;
    } catch (IOException e) {
      LOG.warn("File not found", e);
      throw new FileNotFoundException();
    }
  }

  byte[] readFromContainer(int from, int to) {
    try {
      RandomAccessFile raf = new RandomAccessFile(BASE_PHYSICAL_PATH + CONTAINER_NAME, "r");
      byte[] output = new byte[to - from];
      raf.seek(from);
      raf.readFully(output, 0, to - from);
      return output;
    } catch (IOException e) {
      LOG.warn("File not found", e);
      throw new FileNotFoundException();
    }
  }

  void dropFromContainer(int from, int to) {
    byte[] emptyData = new byte[to - from];
    Arrays.fill(emptyData, (byte) 0);
    storeInContainer(emptyData, from);
  }

  boolean isAllowedToWriteInContainer() {
    java.io.File f = new java.io.File(BASE_PHYSICAL_PATH + CONTAINER_NAME);
    return f.exists() && f.canWrite() && !f.isDirectory();
  }

  long getContainerSize() {
    try {
      RandomAccessFile raf = new RandomAccessFile(BASE_PHYSICAL_PATH + CONTAINER_NAME, "r");
      return raf.length();
    } catch (java.io.FileNotFoundException e) {
      LOG.warn("File not found", e);
      throw new FileNotFoundException();
    } catch (IOException e) {
      LOG.warn("An error has occurred", e);
      throw new IllegalStateException();
    }
  }

  void resizeContainer(FileMetaData fileMetaData) {
    try (FileChannel outChan =
        new FileOutputStream(BASE_PHYSICAL_PATH + CONTAINER_NAME, true).getChannel()) {
      outChan.truncate(fileMetaData.getFrom());
    } catch (IOException e) {
      LOG.warn("An error has occurred", e);
      throw new IllegalStateException();
    }
  }
}
