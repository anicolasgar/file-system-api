package com.jetbrains.filesystem.logic;

import static com.jetbrains.filesystem.utils.SerializationUtils.serialize;

import com.jetbrains.filesystem.exceptions.DirectoryNotFoundException;
import com.jetbrains.filesystem.exceptions.FileCorruptedException;
import com.jetbrains.filesystem.exceptions.FileNotFoundException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Optional;

/**
 * Service that manages the logic for the emulated file system.
 *
 * <p>It interacts with a {@link StorageService}, which is the responsible to store/retrieve the
 * current status of the emulated file system into/from a single container.
 */
class FileManager {
  private static final String BASE_LOGIC_PATH = "/";
  private final StorageService storageService;

  public FileManager(StorageService storageService) {
    this.storageService = storageService;
  }

  void save(File file) {
    String[] filePath = file.getAbsolutePath().split(BASE_LOGIC_PATH);
    TreeNode root = getOrCreateRoot();

    TreeNode node = findPath(filePath, root, true);
    node.getFiles().add(file);

    storageService.storeInContainer(serialize(root));
  }

  File read(String absolutePath) {
    String[] filePath = absolutePath.split(BASE_LOGIC_PATH);
    String fileName = filePath[filePath.length - 1];
    TreeNode root = getOrCreateRoot();

    TreeNode node = findPath(filePath, root, false);
    return node.getFiles().stream()
        .filter(f -> f.getFileName().equals(fileName))
        .findFirst()
        .orElseThrow(FileNotFoundException::new);
  }

  void delete(String absolutePath) {
    String[] filePath = absolutePath.split(BASE_LOGIC_PATH);
    String fileName = filePath[filePath.length - 1];
    TreeNode root = getOrCreateRoot();

    TreeNode node = findPath(filePath, root, false);

    File file =
        node.getFiles().stream()
            .filter(f -> f.getFileName().equals(fileName))
            .findFirst()
            .orElseThrow();
    node.getFiles().remove(file);
    storageService.storeInContainer(serialize(root));
  }

  private TreeNode findPath(String[] filePath, TreeNode root, boolean createIfDoesntExist) {
    TreeNode aux = root;
    for (int i = 0; i < filePath.length - 1; i++) {
      String currentPath = filePath[i];
      Optional<TreeNode> nodeFound =
          aux.getSubPaths().stream().filter(node -> node.getPath().equals(currentPath)).findFirst();
      if (nodeFound.isEmpty()) {
        // path doesn't exist
        if (createIfDoesntExist) {
          TreeNode node = new TreeNode(currentPath, new ArrayList<>(), new ArrayList<>());
          aux.getSubPaths().add(node);
          aux = node;
        } else {
          // todo: add tests for this flow
          throw new DirectoryNotFoundException();
        }
      } else {
        aux = nodeFound.orElseThrow();
      }
    }
    return aux;
  }

  private TreeNode getOrCreateRoot() {
    try {
      byte[] bytes = storageService.readFromContainer();
      if (bytes.length == 0) {
        return createRoot();
      }
      ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
      ObjectInput in = new ObjectInputStream(bis);
      Object obj = in.readObject();
      return obj == null ? createRoot() : (TreeNode) obj;

    } catch (IOException e) {
      System.out.println(e);
      throw new IllegalStateException();
    } catch (ClassNotFoundException e) {
      System.out.println(e);
      throw new FileCorruptedException();
    }
  }

  private TreeNode createRoot() {
    System.out.println("Creating base path...");
    return new TreeNode(BASE_LOGIC_PATH, new ArrayList<>(), new ArrayList<>());
  }
}
