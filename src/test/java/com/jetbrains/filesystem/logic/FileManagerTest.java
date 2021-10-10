package com.jetbrains.filesystem.logic;

import static com.jetbrains.filesystem.utils.SerializationUtils.serialize;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jetbrains.filesystem.exceptions.FileNotFoundException;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
public class FileManagerTest {
  private static final String BASE_PATH = "/test";
  private static final String ABSOLUTE_PATH = BASE_PATH + "/test.json";
  private static final String UNKNOWN_PATH = "non-existent-path";
  private static final String CONTENT = "This is my first file";

  @Test
  void readUnknownFile() {
    TreeNode root = new TreeNode("/", new ArrayList<>(), new ArrayList<>());
    StorageService storageService = mock(StorageService.class);
    when(storageService.readFromContainer()).thenReturn(serialize(root));

    FileManager fileManager = new FileManager(storageService);
    assertThatExceptionOfType(FileNotFoundException.class)
        .isThrownBy(() -> fileManager.read(UNKNOWN_PATH));
  }

  @Test
  void saveFile() {
    File expectedFile = new File(ABSOLUTE_PATH, CONTENT.getBytes());
    // The state of the root before saving the file.
    TreeNode oldRoot = new TreeNode("/");

    // The state of the root after saving the file.
    TreeNode node = new TreeNode("", new TreeNode("test", expectedFile));
    TreeNode newRoot = new TreeNode("/", node);

    StorageService storageService = mock(StorageService.class);
    when(storageService.readFromContainer())
        .thenReturn(serialize(oldRoot))
        .thenReturn(serialize(newRoot));
    FileManager fileManager = new FileManager(storageService);

    fileManager.save(expectedFile);
    verify(storageService).storeInContainer(serialize(newRoot));

    File file = fileManager.read(ABSOLUTE_PATH);
    verify(storageService, times(2)).readFromContainer();
    assertThat(file.getContent()).isEqualTo(expectedFile.getContent());
  }

  @Test
  void deleteFile() {
    File fileToDelete = new File(ABSOLUTE_PATH, CONTENT.getBytes());

    // The state of the root before deleting the file.
    TreeNode oldNode = new TreeNode("", new TreeNode("test", fileToDelete));
    TreeNode oldRoot = new TreeNode("/", oldNode);

    // The state of the root after deleting the file.
    TreeNode newNode = new TreeNode("", new TreeNode("test"));
    TreeNode newRoot = new TreeNode("/", newNode);

    StorageService storageService = mock(StorageService.class);
    when(storageService.readFromContainer()).thenReturn(serialize(oldRoot));

    FileManager fileManager = new FileManager(storageService);
    fileManager.delete(ABSOLUTE_PATH);
    verify(storageService).storeInContainer(serialize(newRoot));
    verify(storageService).readFromContainer();
  }

  @Test
  void deleteUnknownFile() {
    StorageService storageService = mock(StorageService.class);
    TreeNode root = new TreeNode("/");
    when(storageService.readFromContainer()).thenReturn(serialize(root));

    FileManager fileManager = new FileManager(storageService);

    assertThatExceptionOfType(NoSuchElementException.class)
        .isThrownBy(() -> fileManager.delete(UNKNOWN_PATH));
  }
}
