package com.jetbrains.filesystem.logic;

import java.util.ArrayList;
import java.util.Optional;

class FileManager {
    private static final String BASE_PATH = "/";
    private final TreeNode basePath;

    public FileManager() {
        this.basePath = new TreeNode(BASE_PATH, new ArrayList<>(), new ArrayList<>());
    }

    public void save(File file) {
        String[] filePath = file.getAbsolutePath().split(BASE_PATH);
        TreeNode node = findPath(filePath, true);
        node.getFiles().add(file);
    }

    public File read(String absolutePath) {
        String[] filePath = absolutePath.split(BASE_PATH);
        String fileName = filePath[filePath.length - 1];
        TreeNode node = findPath(filePath, false);
        return node.getFiles().stream()
                .filter(f -> f.getFileName().equals(fileName))
                .findFirst()
                .orElseThrow();
    }

    public void delete(String absolutePath) {
        String[] filePath = absolutePath.split(BASE_PATH);
        String fileName = filePath[filePath.length - 1];
        TreeNode node = findPath(filePath, false);

        File file =
                node.getFiles().stream()
                        .filter(f -> f.getFileName().equals(fileName))
                        .findFirst()
                        .orElseThrow();
        node.getFiles().remove(file);
    }

    private TreeNode findPath(String[] filePath, boolean createIfDoesntExist) {
        TreeNode aux = basePath;
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
                    // throw exception?
                }
            } else {
                aux = nodeFound.orElseThrow();
            }
        }
        return aux;
    }
}
