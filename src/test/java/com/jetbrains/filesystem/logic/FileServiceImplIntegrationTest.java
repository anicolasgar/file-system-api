package com.jetbrains.filesystem.logic;

import static com.jetbrains.filesystem.logic.StorageService.BASE_PHYSICAL_PATH;
import static com.jetbrains.filesystem.logic.StorageService.CONTAINER_NAME;
import static com.jetbrains.filesystem.utils.SerializationUtils.serialize;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.jetbrains.filesystem.api.File;
import com.jetbrains.filesystem.exceptions.FileNotFoundException;
import com.jetbrains.filesystem.utils.SerializationUtils;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@Tag("integration")
class FileServiceImplIntegrationTest {
  private static final String CONTENT = "some content";
  private static final String BASE_PATH = "/some/path";
  private static final File FILE1 =
      new File(BASE_PATH + "/file1", CONTENT.getBytes(StandardCharsets.UTF_8));
  private static final File FILE2 =
      new File(BASE_PATH + "/another/path/file2", CONTENT.getBytes(StandardCharsets.UTF_8));
  private static final File FILE3 =
      new File(BASE_PATH + "/another/file3", CONTENT.getBytes(StandardCharsets.UTF_8));
  private static final File FILE4 =
      new File(BASE_PATH + "/file4", CONTENT.getBytes(StandardCharsets.UTF_8));
  private static final File FILE5 =
      new File(BASE_PATH + "/test/file5", CONTENT.getBytes(StandardCharsets.UTF_8));

  @BeforeEach
  void beforeMethod() {
    java.io.File file = new java.io.File(BASE_PHYSICAL_PATH + CONTAINER_NAME);
    file.delete();
  }

  private static Stream<Arguments> writeMultipleFilesTestCases() {
    /* { initialFiles, toDeleteFiles, expectedEmptyFragments, containerSizeAfterCompacting } */
    return Stream.of(
        // only add files.
        arguments(
            List.of(FILE1, FILE2, FILE3, FILE4),
            List.of(),
            0,
            getContentLength(List.of(FILE1, FILE2, FILE3, FILE4))),
        // add files and delete a subgroup of them.
        arguments(
            List.of(FILE1, FILE2, FILE3, FILE4, FILE5),
            List.of(FILE4, FILE5, FILE1, FILE2),
            4,
            getContentLength(List.of(FILE3))),
        // add the same file multiple times.
        arguments(
            List.of(FILE1, FILE1, FILE1, FILE1), List.of(), 3, getContentLength(List.of(FILE1))),
        // add the same file multiple times, and then delete it.
        arguments(List.of(FILE1, FILE1, FILE1, FILE1), List.of(FILE1), 4, 0),
        arguments(
            List.of(FILE1, FILE2, FILE3, FILE4, FILE5),
            List.of(FILE1, FILE2, FILE3, FILE4, FILE5),
            5,
            0));
  }

  @ParameterizedTest
  @MethodSource("writeMultipleFilesTestCases")
  void writeMultipleFiles(
      List<File> initialFiles,
      List<File> toDeleteFiles,
      int expectedEmptyFragments,
      int containerSizeAfterCompacting) {
    StorageService storageService = new StorageService();
    FileManager fileManager = new FileManager(storageService);
    FileServiceImpl fileService = new FileServiceImpl(fileManager);

    for (File file : initialFiles) {
      fileService.write(file.getAbsolutePath(), file.getContent());
    }

    for (File file : toDeleteFiles) {
      fileService.delete(file.getAbsolutePath());
    }

    for (File file : initialFiles) {
      if (toDeleteFiles.contains(file)) {
        assertThatExceptionOfType(FileNotFoundException.class)
            .isThrownBy(() -> fileService.read(file.getAbsolutePath()).getContent());
      } else {
        assertThat(fileService.read(file.getAbsolutePath()).getContent())
            .isEqualTo(file.getContent());
      }
    }

    Map<String, String> metrics = fileService.metrics();
    assertThat(metrics.get("empty_fragments")).isEqualTo(String.valueOf(expectedEmptyFragments));
    assertThat(fileService.metrics().get("container_size"))
        .isEqualTo(String.valueOf(getContentLength(initialFiles)));

    fileManager.compactMemory();
    assertThat(fileService.metrics().get("container_size"))
        .isEqualTo(String.valueOf(containerSizeAfterCompacting));
  }

  @Test
  void writeAndCompact() {
    StorageService storageService = new StorageService();
    FileManager fileManager = new FileManager(storageService);
    FileServiceImpl fileService = new FileServiceImpl(fileManager);

    // there's nothing stored in the container
    assertThat(fileService.metrics().get("empty_fragments")).isEqualTo(String.valueOf(0));
    assertThat(fileService.metrics().get("container_size")).isEqualTo(String.valueOf(0));

    // write and delete 5 times.
    for (int i = 0; i < 5; i++) {
      fileService.write(FILE2.getAbsolutePath(), FILE2.getContent());
      fileService.delete(FILE2.getAbsolutePath());
    }
    // write another file.
    fileService.write(FILE1.getAbsolutePath(), FILE1.getContent());

    assertThat(fileService.metrics().get("empty_fragments")).isEqualTo(String.valueOf(5));
    assertThat(fileService.metrics().get("container_size"))
        .isEqualTo(String.valueOf(serialize(FILE1).length + serialize(FILE2).length * 5));

    // compact the container
    fileManager.compactMemory();
    assertThat(fileService.metrics().get("empty_fragments")).isEqualTo(String.valueOf(0));
    assertThat(fileService.metrics().get("container_size"))
        .isEqualTo(String.valueOf(serialize(FILE1).length));

    // write another file.
    fileService.write(FILE3.getAbsolutePath(), FILE3.getContent());
    assertThat(fileService.metrics().get("empty_fragments")).isEqualTo(String.valueOf(0));
    assertThat(fileService.metrics().get("container_size"))
        .isEqualTo(String.valueOf(getContentLength(List.of(FILE1, FILE3))));
  }

  private static int getContentLength(List<File> files) {
    return files.stream()
        .map(SerializationUtils::serialize)
        .map(b -> b.length)
        .reduce(0, Integer::sum);
  }
}
