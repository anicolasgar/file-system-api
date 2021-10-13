package com.jetbrains.filesystem.logic;

import static com.jetbrains.filesystem.utils.SerializationUtils.serialize;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.jetbrains.filesystem.api.File;
import com.jetbrains.filesystem.exceptions.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class FileManagerTest {
  private static final String BASE_PATH = "/hello/world";
  private static final String ABSOLUTE_PATH = BASE_PATH + "/test.json";
  private static final String UNKNOWN_PATH = "non-existent-path";
  private static final String CONTENT1 = "This is one content";
  private static final String CONTENT2 = "This is another content";

  @Test
  void readUnknownFile() {
    StorageService storageService = mock(StorageService.class);
    FileManager fileManager = new FileManager(storageService);

    assertThatExceptionOfType(FileNotFoundException.class)
        .isThrownBy(() -> fileManager.read(UNKNOWN_PATH));
  }

  @Test
  void saveFile() {
    File file = new File(ABSOLUTE_PATH, CONTENT1.getBytes(StandardCharsets.UTF_8));

    StorageService storageService = mock(StorageService.class);
    FileManager fileManager = new FileManager(storageService);

    fileManager.save(file);
    verify(storageService).storeInContainer(serialize(file), 0);
  }

  @Test
  void saveSameFileTwice() {
    File file = new File(ABSOLUTE_PATH, CONTENT1.getBytes(StandardCharsets.UTF_8));
    File modifiedFile = new File(ABSOLUTE_PATH, CONTENT2.getBytes(StandardCharsets.UTF_8));

    StorageService storageService = mock(StorageService.class);
    FileManager fileManager = new FileManager(storageService);

    fileManager.save(file);
    fileManager.save(modifiedFile);
    verify(storageService).storeInContainer(serialize(file), 0);
    verify(storageService).dropFromContainer(0, serialize(file).length);
    verify(storageService).storeInContainer(serialize(modifiedFile), serialize(file).length);
  }

  @Test
  void deleteFile() {
    File file = new File(ABSOLUTE_PATH, CONTENT1.getBytes(StandardCharsets.UTF_8));

    StorageService storageService = mock(StorageService.class);
    FileManager fileManager = new FileManager(storageService);

    fileManager.save(file);
    verify(storageService).storeInContainer(serialize(file), 0);
    fileManager.delete(ABSOLUTE_PATH);
    verify(storageService).dropFromContainer(0, serialize(file).length);
  }

  @Test
  void deleteUnknownFile() {
    StorageService storageService = mock(StorageService.class);
    FileManager fileManager = new FileManager(storageService);

    assertThatExceptionOfType(FileNotFoundException.class)
        .isThrownBy(() -> fileManager.delete(UNKNOWN_PATH));
  }
}
