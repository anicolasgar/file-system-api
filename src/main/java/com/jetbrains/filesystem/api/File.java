package com.jetbrains.filesystem.api;

import java.io.Serializable;
import java.util.Locale;

/** Represents a file for the emulated file system. */
public class File implements Serializable {
  /**
   * The absolute path of the file.
   *
   * <p>Note that this path is absolute regarding the emulated file system but relative to the
   * `StorageService#BASE_PHYSICAL_PATH`.
   */
  private final String absolutePath;
  /** The content of the file. */
  private final byte[] content;

  public File(String absolutePath, byte[] content) {
    this.absolutePath = normalize(absolutePath);
    this.content = content;
  }

  public String getFileName() {
    String[] filePath = absolutePath.split("/");
    return filePath[filePath.length - 1];
  }

  public String getAbsolutePath() {
    return absolutePath;
  }

  public byte[] getContent() {
    return content;
  }

  private String normalize(String path) {
    return path.toLowerCase(Locale.ROOT);
  }
}
