package com.jetbrains.filesystem.utils;

import static org.assertj.core.api.Assertions.assertThat;

import com.jetbrains.filesystem.api.File;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class SerializationUtilsTest {
  @Test
  void serialize() throws IOException {
    File file = new File("path", "".getBytes(StandardCharsets.UTF_8));

    ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
    ObjectOutputStream oos = new ObjectOutputStream(baos);
    oos.writeObject(file);
    oos.flush();
    assertThat(SerializationUtils.serialize(file)).isEqualTo(baos.toByteArray());
  }
}
