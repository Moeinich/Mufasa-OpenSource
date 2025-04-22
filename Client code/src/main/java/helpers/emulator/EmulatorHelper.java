package helpers.emulator;

import helpers.Logger;
import helpers.adb.ADBHandler;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.scene.control.ListView;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class EmulatorHelper {
    private final String DEVICE_SUFFIX = "device";
    private final Logger logger;
    private final ADBHandler adbHandler;
    private final ObservableSet<String> onlineEmulators = FXCollections.observableSet(ConcurrentHashMap.newKeySet());
    Path adbPath;

    public EmulatorHelper(Logger logger, ADBHandler adbHandler) {
        this.logger = logger;
        this.adbHandler = adbHandler;

        adbPath = adbHandler.getADBPath();
        // Validate the ADB path
        if (adbPath == null || !Files.exists(adbPath) || !adbHandler.isADBInstalledAtPath(adbPath)) {
            String errorMessage = "ADB is not installed at the specified path: " + adbPath;
            logger.devLog(errorMessage);
            throw new IllegalStateException(errorMessage);
        } else {
            logger.devLog("ADB found at: " + adbPath.toAbsolutePath());
        }
    }

    public ObservableSet<String> getOnlineEmulators() {
        List<String> outputLines = adbHandler.executeADBCommandWithOutput("devices", "");
        Set<String> currentEmulators = new HashSet<>();
        outputLines.stream()
                .filter(line -> line.endsWith(DEVICE_SUFFIX))
                .map(line -> line.split("\\t")[0])
                .forEach(currentEmulators::add);

        // Update the ObservableSet onlineEmulators in place
        onlineEmulators.retainAll(currentEmulators);
        onlineEmulators.addAll(currentEmulators);

        return onlineEmulators;
    }

    public void updateOnlineEmulators(ListView<String> emulatorListView) {
        try {
            getOnlineEmulators();  // This will update the observable onlineEmulators set directly
        } catch (IllegalStateException e) {
            logger.devLog(e.getMessage());
            updateListView(emulatorListView, FXCollections.observableArrayList("Failed collecting online emulators"));
            return;
        }

        updateListView(emulatorListView, FXCollections.observableArrayList(onlineEmulators), emulatorListView.getSelectionModel().getSelectedItem());
    }

    private void updateListView(ListView<String> emulatorListView, ObservableList<String> items) {
        Platform.runLater(() -> {
            ObservableList<String> currentItems = emulatorListView.getItems();
            if (!currentItems.equals(items)) {
                currentItems.setAll(items);
            }
        });
    }

    private void updateListView(ListView<String> emulatorListView, ObservableList<String> items, String currentSelection) {
        Platform.runLater(() -> {
            ObservableList<String> currentItems = emulatorListView.getItems();
            if (!currentItems.equals(items)) {
                currentItems.setAll(items);
                if (currentSelection != null && items.contains(currentSelection)) {
                    emulatorListView.getSelectionModel().select(currentSelection);
                }
            }
        });
    }
}