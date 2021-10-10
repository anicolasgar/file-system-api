package com.jetbrains.filesystem.logic;

import com.jetbrains.filesystem.exceptions.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

/** A package-private service that interacts with the real file system. */
class StorageService {
  /** The base path of the emulated file system. */
  private static final String BASE_PHYSICAL_PATH = "/users/nicolasgarcia/test/";
  /** The name of the file where all the emulated file system is stored. */
  private static final String CONTAINER_NAME = "jetbrains-assignment";

  public StorageService() {
    storeInContainer(new byte[] {});
  }

  // todo: create subfolders of physical path in case it doesn't exist.
  void storeInContainer(byte[] content) {
    java.io.File root = new java.io.File(BASE_PHYSICAL_PATH + CONTAINER_NAME);
    try {
      if (!root.exists()) {
        root.createNewFile();
      }

      try (FileOutputStream stream = new FileOutputStream(BASE_PHYSICAL_PATH + CONTAINER_NAME)) {
        stream.write(content);
      }
    } catch (IOException e) {
      System.out.println(e);
      // todo: Do something else?
    }
  }

  byte[] readFromContainer() {
    try {
      RandomAccessFile f = new RandomAccessFile(BASE_PHYSICAL_PATH + CONTAINER_NAME, "r");
      byte[] output = new byte[(int) f.length()];
      f.readFully(output);
      return output;
    } catch (IOException e) {
      // todo: wrap IOException.
      throw new FileNotFoundException();
    }
  }
}
