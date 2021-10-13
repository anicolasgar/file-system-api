package com.jetbrains.filesystem.logic;

import static com.jetbrains.filesystem.logic.StorageService.BASE_PHYSICAL_PATH;
import static com.jetbrains.filesystem.logic.StorageService.CONTAINER_NAME;
import static com.jetbrains.filesystem.utils.SerializationUtils.serialize;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.jetbrains.filesystem.exceptions.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;

@Tag("integration")
class FileServiceImplIntegrationTest {
  private static final String BASE_PATH = "/some/path";
  private static final String FILE1 = "/file1";
  private static final String FILE2 = "/another/path/file2";
  private static final String FILE3 = "/another/file3";
  private static final String FILE4 = "/file4";
  private static final String FILE5 = "/test/file5";

  @BeforeEach
  void beforeMethod() {
    java.io.File file = new java.io.File(BASE_PHYSICAL_PATH + CONTAINER_NAME);
    file.delete();
  }

  private static Stream<Arguments> writeMultipleFilesTestCases() {
    /* { initialFiles, toDeleteFiles, empty_fragments} */
    return Stream.of(
        // only add files.
        arguments(List.of(FILE1, FILE2, FILE3, FILE4), List.of(), 0),
        // add files and delete a subgroup.
        arguments(
            List.of(FILE1, FILE2, FILE3, FILE4, FILE5), List.of(FILE1, FILE5, FILE2, FILE4), 4),
        // add the same file multiple times.
        // todo: Let's be smarter here: If the file is the same than stored, ignore the request.
        arguments(List.of(FILE1, FILE1, FILE1, FILE1), List.of(), 3),
        // add the same file multiple times, and then delete it.
        arguments(List.of(FILE1, FILE1, FILE1, FILE1), List.of(FILE1), 4),
        arguments(
            List.of(FILE1, FILE2, FILE3, FILE4, FILE5),
            List.of(FILE1, FILE2, FILE3, FILE4, FILE5),
            5));
  }

  @ParameterizedTest
  @MethodSource("writeMultipleFilesTestCases")
  void writeMultipleFiles(
      List<String> initialFiles, List<String> toDeleteFiles, int emptyFragments) {
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

    Map<String, String> metrics = fileService.metrics();
    assertThat(metrics.get("empty_fragments")).isEqualTo(String.valueOf(emptyFragments));
    System.out.println(metrics.get("container_size"));
  }

  @Test
  void writeAndCompact() {
    StorageService storageService = new StorageService();
    FileManager fileManager = new FileManager(storageService);
    FileServiceImpl fileService = new FileServiceImpl(fileManager);
    Map<String, String> metrics = fileService.metrics();

    // there's nothing stored in the container
    assertThat(metrics.get("empty_fragments")).isEqualTo(String.valueOf(0));
    assertThat(metrics.get("container_size")).isEqualTo(String.valueOf(0));

    // write and delete 5 times.
    for (int i = 0; i < 5; i++) {
      fileService.write(BASE_PATH + FILE1, FILE1.getBytes(StandardCharsets.UTF_8));
      fileService.delete(BASE_PATH + FILE1);
    }

    Map<String, String> newMetrics = fileService.metrics();
    File file = new File(BASE_PATH + FILE1, FILE1.getBytes(StandardCharsets.UTF_8));
    assertThat(newMetrics.get("empty_fragments")).isEqualTo(String.valueOf(5));
    assertThat(newMetrics.get("container_size"))
        .isEqualTo(String.valueOf(serialize(file).length * 5));

    // compact the container
    // fileManager.compactMemory();
    // assertThat(newMetrics.get("container_size")).isEqualTo(String.valueOf(0));
  }
}
