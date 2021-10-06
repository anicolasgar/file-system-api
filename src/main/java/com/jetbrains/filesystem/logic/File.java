package com.jetbrains.filesystem.logic;

public class File {
    private final String absolutePath;
    private final byte[] content;

    public File(String absolutePath, byte[] content) {
        this.absolutePath = absolutePath;
        this.content = content;
    }

    public String getFileName() {
        String[] filePath = absolutePath.split("/");
        return filePath[filePath.length - 1];
    }

    public String getAbsolutePath() {
        return absolutePath;
    }

    public byte[] getContent() {
        return content;
    }
}
