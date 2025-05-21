package utils;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.DosFileAttributeView;

import static utils.Constants.OS_MAC;
import static utils.Constants.OS_WIN;

public class SystemUtils {
    private static String operatingSystem;

    public static String getOperatingSystem() {
        if (operatingSystem == null) {
            operatingSystem = System.getProperty("os.name").toLowerCase();
        }
        return operatingSystem;
    }

    public static String getSystemPath() {
        String os = getOperatingSystem();

        Path path;
        if (os.contains(OS_WIN)) {
            String appDataLocal = System.getenv("LOCALAPPDATA");
            if (appDataLocal == null) {
                throw new IllegalStateException("LOCALAPPDATA environment variable is not set.");
            }
            path = Paths.get(appDataLocal, "MufasaOpenSource");
        } else if (os.contains(OS_MAC)) {
            String userHome = System.getProperty("user.home");
            if (userHome == null) {
                throw new IllegalStateException("user.home system property is not set.");
            }
            path = Paths.get(userHome, ".MufasaOpenSource");
        } else {
            throw new UnsupportedOperationException("Unsupported operating system: " + os);
        }

        // Ensure the directory exists
        try {
            Files.createDirectories(path);
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to create directory: " + path, e);
        }

        // Return the absolute path as a String without appending File.separator
        return path.toAbsolutePath() + File.separator;
    }

    private static void createFolder(String folderPath) {
        Path path = Paths.get(folderPath);
        boolean isNewFolder = false;

        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path);
                System.out.println("Folder created: " + folderPath);
                isNewFolder = true;
            } catch (IOException e) {
                System.err.println("Failed to create the folder: " + folderPath);
                e.printStackTrace();
            }
        } else {
            System.out.println("Folder already exists: " + folderPath);
        }

        // Check if the operating system is Windows
        if (System.getProperty("os.name").startsWith("Windows")) {
            try {
                // Set the folder to be hidden
                DosFileAttributeView attrs = Files.getFileAttributeView(path, DosFileAttributeView.class);
                if (isNewFolder || !attrs.readAttributes().isHidden()) {
                    attrs.setHidden(true);
                    if (!isNewFolder) {
                        System.out.println("Existing folder set to hidden: " + folderPath);
                    }
                }
            } catch (IOException e) {
                System.err.println("Failed to set the folder as hidden: " + folderPath);
                e.printStackTrace();
            }
        }
    }

    public static void initializeFolders() {
        // Check for the necessary folders and create them if they don't exist based on the OS
        String folderPath = getSystemPath();

        System.out.println("Initializing folders at path: " + folderPath); // debug print

        // Create the main folder if it doesn't exist
        createFolder(folderPath);

        // Create subfolders
        createFolder(folderPath + "logging");
        createFolder(folderPath + "screenshots");
        createFolder(folderPath + "scripts");
        createFolder(folderPath + "platform-tools");
        createFolder(folderPath + "libs");
        createFolder(folderPath + "items");
        createFolder(folderPath + "hopper");
        createFolder(folderPath + "chunks");
        createFolder(folderPath + "objects");
        createFolder(folderPath + "graphs");

        // Hide files if needed
        hideFileIfPresent(folderPath + "breaks.json");
        hideFileIfPresent(folderPath + "creds.json");
        hideFileIfPresent(folderPath + "Mufasa.jar");
    }

    // Method to get the the folder paths based on the OS
    public static String getScreenshotFolderPath() {
        return (getSystemPath() + "screenshots");
    }

    public static String getLocalScriptFolderPath() {
        return (getSystemPath() + "scripts");
    }

    public static String getLogsFolderPath() {
        return (getSystemPath() + "logging");
    }

    public static String getLibsFolderPath() {
        return (getSystemPath() + "libs");
    }

    public static String getADBFolderPath() {
        return (getSystemPath() + "platform-tools");
    }

    public static String getItemsFolderPath() {
        return (getSystemPath() + "items");
    }

    public static String getWorldHopperFolderPath() {
        return (getSystemPath() + "hopper");
    }

    public static String getChunksFolderPath() {
        return (getSystemPath() + "chunks");
    }

    public static String getObjectsFolderPath() {
        return (getSystemPath() + "objects");
    }

    public static String getObjectsFolderPath(int ZLevel) {
        return (getSystemPath() + "objects" + File.separator + ZLevel);
    }

    public static String getMapChunksFolderPath(int ZLevel) {
        return (getSystemPath() + "chunks" + File.separator + "map" + File.separator + ZLevel);
    }

    public static String getCollisionChunksFolderPath(int ZLevel) {
        return (getSystemPath() + "chunks" + File.separator + "collision" + File.separator + ZLevel);
    }

    public static String getHeightMapChunksFolderPath(int ZLevel) {
        return (getSystemPath() + "chunks" + File.separator + "heightMaps" + File.separator + ZLevel);
    }

    public static String getGraphsFolderPath() {
        return (getSystemPath() + "graphs");
    }

    private static void hideFileIfPresent(String filePath) {
        Path path = Paths.get(filePath);
        if (Files.exists(path) && System.getProperty("os.name").startsWith("Windows")) {
            try {
                DosFileAttributeView attrs = Files.getFileAttributeView(path, DosFileAttributeView.class);
                if (!attrs.readAttributes().isHidden()) {
                    attrs.setHidden(true);
                    System.out.println("File set to hidden: " + filePath);
                }
            } catch (IOException e) {
                System.err.println("Failed to set the file as hidden: " + filePath);
                e.printStackTrace();
            }
        }
    }

    // Method to open a folder based on the OS
    public static void openFolder(String folderPath) throws IOException {
        File folder = new File(folderPath);
        if (folder.exists() && folder.isDirectory()) {
            Desktop.getDesktop().open(folder);
        } else {
            System.err.println("Folder does not exist: " + folderPath);
        }
    }
}
