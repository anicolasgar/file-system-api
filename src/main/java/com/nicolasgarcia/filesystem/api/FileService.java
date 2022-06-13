package com.nicolasgarcia.filesystem.api;

import com.nicolasgarcia.filesystem.exceptions.FileNotFoundException;
import java.util.Map;

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

  /**
   * Appends with a new content, the current content of a file.
   *
   * @param absolutePath The location of the file.
   * @param content The content that will be appended.
   */
  void append(String absolutePath, byte[] content);

  /**
   * Deletes the specified file.
   *
   * @param absolutePath The path where the file is located.
   * @throws FileNotFoundException if the file doesn't exist.
   */
  void delete(String absolutePath);

  /**
   * Renamed the associated file.
   *
   * <p>Note that this method replicates {@link #move(String, String)}. In a following interation,
   * it will be improved.
   *
   * @param oldAbsolutePath The path where the file is located.
   * @param newAbsolutePath The desired new path.
   * @throws FileNotFoundException if the file doesn't exist.
   */
  void rename(String oldAbsolutePath, String newAbsolutePath);

  /**
   * Moves the associated file from one path to another.
   *
   * @param oldPath The path where the file is located.
   * @param newPath The desired new path.
   * @throws FileNotFoundException if the file doesn't exist.
   */
  void move(String oldPath, String newPath);

  /**
   * A set of metrics to monitor the status of the service.
   *
   * @return The set of metrics.
   */
  Map<String, String> metrics();
}
