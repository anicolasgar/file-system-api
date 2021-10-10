package com.jetbrains.filesystem.logic;

import com.jetbrains.filesystem.api.FileService;

public class FileServiceImpl implements FileService {

  private final FileManager fileManager;

  public FileServiceImpl(FileManager fileManager) {
    this.fileManager = fileManager;
  }

  @Override
  public File create(String absolutePath) {
    File file = new File(absolutePath, null);
    fileManager.save(file);
    return file;
  }

  @Override
  public void write(String absolutePath, byte[] content) {
    File file = new File(absolutePath, content);
    fileManager.save(file);
  }

  @Override
  public File read(String absolutePath) {
    return fileManager.read(absolutePath);
  }

  @Override
  public void append(String absolutePath, byte[] content) {
    File oldFile = fileManager.read(absolutePath);

    byte[] newContent = new byte[oldFile.getContent().length + content.length];
    System.arraycopy(oldFile.getContent(), 0, newContent, 0, oldFile.getContent().length);
    System.arraycopy(content, 0, newContent, oldFile.getContent().length, content.length);

    fileManager.save(new File(absolutePath, newContent));
  }

  @Override
  public void delete(String absolutePath) {
    fileManager.delete(absolutePath);
  }

  @Override
  public void rename(String oldAbsolutePath, String newAbsolutePath) {
    // todo: Similar than #move?
  }

  @Override
  public void move(String oldAbsolutePath, String newAbsolutePath) {
    File file = fileManager.read(oldAbsolutePath);
    fileManager.delete(oldAbsolutePath);
    fileManager.save(new File(newAbsolutePath, file.getContent()));
  }
}
