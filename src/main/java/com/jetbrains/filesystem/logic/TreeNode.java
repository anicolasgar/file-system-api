package com.jetbrains.filesystem.logic;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Represents a node in the emulated file system. */
final class TreeNode implements Serializable {
  private final String path;
  private final List<TreeNode> subPaths;
  private final List<File> files;

  TreeNode(String path) {
    this(path, new ArrayList<>(), new ArrayList<>());
  }

  TreeNode(String path, TreeNode node) {
    this(path);
    this.subPaths.add(node);
  }

  TreeNode(String path, File file) {
    this(path);
    this.files.add(file);
  }

  TreeNode(String path, List<TreeNode> subPaths, List<File> files) {
    this.path = normalize(path);
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

  private String normalize(String path) {
    return path.toLowerCase(Locale.ROOT);
  }
}
