package com.jetbrains.filesystem.logic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.NoSuchElementException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
public class FileManagerTest {
    private static final String ABSOLUTE_PATH = "/nico/projects/jetbrains/testing/test.json";
    private static final String UNKNOWN_PATH = "non-existent-path";
    private static final String CONTENT = "This is my first file";

    @Test
    void readUnknownFile() {
        FileManager fileManager = new FileManager();
        assertThatExceptionOfType(NoSuchElementException.class)
                .isThrownBy(() -> fileManager.read(UNKNOWN_PATH));
    }

    @Test
    void saveFile() {
        FileManager fileManager = new FileManager();
        File expectedFile = new File(ABSOLUTE_PATH, CONTENT.getBytes());
        fileManager.save(expectedFile);
        File file = fileManager.read(ABSOLUTE_PATH);
        assertThat(file).isEqualTo(expectedFile);
    }

    @Test
    void deleteFile() {
        FileManager fileManager = new FileManager();
        fileManager.save(new File(ABSOLUTE_PATH, null));
        fileManager.delete(ABSOLUTE_PATH);
        assertThatExceptionOfType(NoSuchElementException.class)
                .isThrownBy(() -> fileManager.read(ABSOLUTE_PATH));
    }

    @Test
    void deleteUnknownFile() {
        FileManager fileManager = new FileManager();
        assertThatExceptionOfType(NoSuchElementException.class)
                .isThrownBy(() -> fileManager.delete(UNKNOWN_PATH));
    }
}
