package com.jetbrains.filesystem.logic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.jetbrains.filesystem.exceptions.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;

@Tag("integration")
class FileServiceImplIntegrationTest {
  private static final String BASE_PATH = "/test";
  private static final String FILE1 = "/file1";
  private static final String FILE2 = "/file2";
  private static final String FILE3 = "/file3";
  private static final String FILE4 = "/file4";

  private static Stream<Arguments> writeMultipleFilesTestCases() {
    /* { initialFiles, toDeleteFiles} */
    return Stream.of(
        arguments(List.of(FILE1, FILE2, FILE3, FILE4), List.of(FILE1, FILE4)),
        arguments(List.of(FILE1, FILE2, FILE3, FILE4), List.of(FILE1, FILE2, FILE3, FILE4)));
  }

  @ParameterizedTest
  @MethodSource("writeMultipleFilesTestCases")
  void writeMultipleFiles(List<String> initialFiles, List<String> toDeleteFiles) {
    StorageService storageService = new StorageService();
    FileManager fileManager = new FileManager(storageService);
    FileServiceImpl fileService = new FileServiceImpl(fileManager);

    for (String file : initialFiles) {
      fileService.write(BASE_PATH + file, file.getBytes(StandardCharsets.UTF_8));
    }

    for (String file : toDeleteFiles) {
      fileService.delete(BASE_PATH + file);
    }

    for (String file : initialFiles) {
      if (toDeleteFiles.contains(file)) {
        assertThatExceptionOfType(FileNotFoundException.class)
            .isThrownBy(() -> fileService.read(BASE_PATH + file).getContent());
      } else {
        assertThat(fileService.read(BASE_PATH + file).getContent())
            .isEqualTo(file.getBytes(StandardCharsets.UTF_8));
      }
    }
  }
}
