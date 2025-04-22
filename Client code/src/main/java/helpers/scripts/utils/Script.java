package helpers.scripts.utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Script {
    private final Node root; // Root of the directory tree

    public Script() {
        this.root = new Node("/", null); // root directory
    }

    // Adds a file to the script, preserving its directory structure
    public void addFile(String path, byte[] data) {
        String[] parts = path.split("/");
        Node current = root;

        for (int i = 0; i < parts.length; i++) {
            if (!current.children.containsKey(parts[i])) {
                current.children.put(parts[i], new Node(parts[i], current));
            }
            current = current.children.get(parts[i]);

            // If it's the last part of the path, it's a file, not a directory
            if (i == parts.length - 1) {
                current.data = data;
            }
        }
    }

    // Retrieve a file
    public byte[] getFile(String path) {
        String[] parts = path.split("/");
        Node current = root;

        for (String part : parts) {
            current = current.children.get(part);
            if (current == null) {
                return null; // File not found
            }
        }

        return current.data;
    }

    // Node class representing each file/directory
    private static class Node {
        String name;
        byte[] data; // null for directories
        Map<String, Node> children;
        Node parent;

        Node(String name, Node parent) {
            this.name = name;
            this.data = null;
            this.children = new HashMap<>();
            this.parent = parent;
        }
    }

    // Retrieve a class file
    public byte[] getClassFile(String classFilePath) {
        if (classFilePath.endsWith(".class")) {
            return getFile(classFilePath);
        }
        return null;
    }

    // Retrieve a resource
    public byte[] getResource(String resourcePath) {
        if (!resourcePath.endsWith(".class")) {
            return getFile(resourcePath);
        }
        System.out.print("Failed to get the resource!");
        return null;
    }

    // List all class files in the script
    public Set<String> listAllClassFiles() {
        Set<String> classFiles = new HashSet<>();
        listFilesRecursive(root, "", classFiles, true);
        return classFiles;
    }

    // List all resources in the script
    public Set<String> listAllResources() {
        Set<String> resources = new HashSet<>();
        listFilesRecursive(root, "", resources, false);
        return resources;
    }

    // Helper method for recursive file listing
    private void listFilesRecursive(Node node, String path, Set<String> fileList, boolean classFilesOnly) {
        if (node != root) {
            path += node.name;
            if (node.data != null) {
                if (classFilesOnly && node.name.endsWith(".class")) {
                    fileList.add(path);
                } else if (!classFilesOnly && !node.name.endsWith(".class")) {
                    fileList.add(path);
                }
            }
            if (!node.children.isEmpty()) {
                path += "/";
            }
        }
        for (Node child : node.children.values()) {
            listFilesRecursive(child, path, fileList, classFilesOnly);
        }
    }
}
