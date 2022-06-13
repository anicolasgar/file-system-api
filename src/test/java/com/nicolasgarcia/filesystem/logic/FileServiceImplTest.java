package com.nicolasgarcia.filesystem.logic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nicolasgarcia.filesystem.api.File;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@Tag("unit")
class FileServiceImplTest {
  private static final String ABSOLUTE_PATH = "/nico/projects/jetbrains/testing/test.json";
  private static final String CONTENT_STRING = "This is my first file";
  private static final byte[] CONTENT_BYTES = CONTENT_STRING.getBytes();
  private static final File FILE = new File(ABSOLUTE_PATH, CONTENT_BYTES);

  @Test
  void create() {
    FileManager fileManager = mock(FileManager.class);
    FileServiceImpl fileService = new FileServiceImpl(fileManager);
    fileService.create(ABSOLUTE_PATH);

    ArgumentCaptor<File> argumentCaptor = ArgumentCaptor.forClass(File.class);
    verify(fileManager).save(argumentCaptor.capture());
    assertThat(argumentCaptor.getValue().getAbsolutePath()).isEqualTo(ABSOLUTE_PATH);
    assertThat(argumentCaptor.getValue().getContent()).isNull();
  }

  @Test
  void write() {
    FileManager fileManager = mock(FileManager.class);
    FileServiceImpl fileService = new FileServiceImpl(fileManager);
    fileService.write(ABSOLUTE_PATH, CONTENT_BYTES);

    ArgumentCaptor<File> argumentCaptor = ArgumentCaptor.forClass(File.class);
    verify(fileManager).save(argumentCaptor.capture());
    assertThat(argumentCaptor.getValue().getAbsolutePath()).isEqualTo(ABSOLUTE_PATH);
    assertThat(argumentCaptor.getValue().getContent()).isEqualTo(CONTENT_BYTES);
  }

  @Test
  void read() {
    FileManager fileManager = mock(FileManager.class);
    FileServiceImpl fileService = new FileServiceImpl(fileManager);
    fileService.read(ABSOLUTE_PATH);
    verify(fileManager).read(ABSOLUTE_PATH);
  }

  @Test
  void append() {
    byte[] moreContent = "more-content".getBytes();
    byte[] newContent = new byte[CONTENT_BYTES.length + moreContent.length];
    System.arraycopy(CONTENT_BYTES, 0, newContent, 0, CONTENT_BYTES.length);
    System.arraycopy(moreContent, 0, newContent, CONTENT_BYTES.length, moreContent.length);

    FileManager fileManager = mock(FileManager.class);
    when(fileManager.read(ABSOLUTE_PATH)).thenReturn(FILE);

    FileServiceImpl fileService = new FileServiceImpl(fileManager);
    fileService.append(ABSOLUTE_PATH, moreContent);

    ArgumentCaptor<File> argumentCaptor = ArgumentCaptor.forClass(File.class);
    verify(fileManager).save(argumentCaptor.capture());
    assertThat(argumentCaptor.getValue().getAbsolutePath()).isEqualTo(ABSOLUTE_PATH);
    assertThat(argumentCaptor.getValue().getContent()).isEqualTo(newContent);
  }

  @Test
  void delete() {
    FileManager fileManager = mock(FileManager.class);
    FileServiceImpl fileService = new FileServiceImpl(fileManager);
    fileService.delete(ABSOLUTE_PATH);
    verify(fileManager).delete(ABSOLUTE_PATH);
  }

  @Test
  void rename() {
    // TODO: With the given implementation, it is the same than #move.

  }

  @Test
  void move() {
    FileManager fileManager = mock(FileManager.class);
    FileServiceImpl fileService = new FileServiceImpl(fileManager);
    when(fileManager.read(ABSOLUTE_PATH)).thenReturn(FILE);

    String newAbsolutePath = "/another/path/test.json";
    fileService.move(ABSOLUTE_PATH, newAbsolutePath);

    ArgumentCaptor<File> argumentCaptor = ArgumentCaptor.forClass(File.class);
    verify(fileManager).delete(ABSOLUTE_PATH);
    verify(fileManager).save(argumentCaptor.capture());
    assertThat(argumentCaptor.getValue().getAbsolutePath()).isEqualTo(newAbsolutePath);
    assertThat(argumentCaptor.getValue().getContent()).isEqualTo(CONTENT_BYTES);
  }
}
