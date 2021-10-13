package com.jetbrains.filesystem.logic;

import static com.jetbrains.filesystem.logic.StorageService.BASE_PHYSICAL_PATH;
import static com.jetbrains.filesystem.logic.StorageService.CONTAINER_NAME;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("integration")
class StorageServiceTest {
  @BeforeEach
  void beforeMethod() {
    java.io.File file = new java.io.File(BASE_PHYSICAL_PATH + CONTAINER_NAME);
    file.delete();
  }

  @Test
  void storeAndReadFromContainer() {
    byte[] test = "teststring".getBytes(StandardCharsets.UTF_8);
    StorageService storageService = new StorageService();
    storageService.storeInContainer(test, 0);
    assertThat(storageService.readFromContainer(0, test.length)).isEqualTo(test);
  }

  @Test
  void storeAndReadMultipleFilesFromContainer() {
    byte[] test1 = "testing1".getBytes(StandardCharsets.UTF_8);
    byte[] test2 = "testing2".getBytes(StandardCharsets.UTF_8);
    StorageService storageService = new StorageService();
    storageService.storeInContainer(test1, 0);
    storageService.storeInContainer(test2, test1.length);

    byte[] expectedArray = new byte[test1.length + test2.length];
    System.arraycopy(test1, 0, expectedArray, 0, test1.length);
    System.arraycopy(test2, 0, expectedArray, test1.length, test2.length);

    assertThat(storageService.readAllFromContainer()).isEqualTo(expectedArray);
  }
}
