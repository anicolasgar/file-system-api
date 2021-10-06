package com.jetbrains.filesystem.logic;

import java.util.List;

final class TreeNode {
    private final String path;
    private final List<TreeNode> subPaths;
    private final List<File> files;

    TreeNode(String path, List<TreeNode> subPaths, List<File> files) {
        this.path = path;
        this.subPaths = subPaths;
        this.files = files;
    }

    String getPath() {
        return path;
    }

    List<TreeNode> getSubPaths() {
        return subPaths;
    }

    List<File> getFiles() {
        return files;
    }
}
