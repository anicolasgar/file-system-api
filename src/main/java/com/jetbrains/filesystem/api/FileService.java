package com.jetbrains.filesystem.api;

import com.jetbrains.filesystem.logic.File;
import com.jetbrains.filesystem.exceptions.FileNotFoundException;

/** Service for managing file system's operations. */
public interface FileService {
  /**
   * Creates an empty file located in the provided absolute path.
   *
   * @param absolutePath The path where the file will be stored.
   * @return {@link File} The created file.
   */
  File create(String absolutePath);

  /**
   * Overwrites the content of a file.
   *
   * <p>Note that a new file will be created with the given content, in case it doesn't exist in the
   * provided path.
   *
   * @param absolutePath The location of the file.
   * @param content The content the file will have.
   */
  void write(String absolutePath, byte[] content);

  /**
   * Creates an empty file located in the provided absolute path.
   *
   * @param absolutePath The path where the file is located.
   * @return {@link File} The existing file.
   * @throws FileNotFoundException if the file doesn't exist.
   */
  File read(String absolutePath);

  /** Pending. */
  void append(String absolutePath, byte[] content);

  /**
   * Deletes the specified file.
   *
   * @param absolutePath The path where the file is located.
   * @throws FileNotFoundException if the file doesn't exist.
   */
  void delete(String absolutePath);

  /** Pending. */
  void rename(String oldFileName, String newFileName);

  /** Pending. */
  void move(String oldPath, String newPath);
}
