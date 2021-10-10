package com.jetbrains.filesystem.logic;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class StorageServiceTest {

  @Test
  void storeAndReadFromContainer() {
    String test = "teststring";
    StorageService storageService = new StorageService();
    storageService.storeInContainer(test.getBytes(StandardCharsets.UTF_8));
    assertThat(storageService.readFromContainer()).isEqualTo(test.getBytes());
  }
}
