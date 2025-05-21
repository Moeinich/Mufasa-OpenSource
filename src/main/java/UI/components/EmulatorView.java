package UI.components;

import UI.ScriptSelectionUI;
import helpers.AbstractScript;
import helpers.Logger;
import helpers.ThreadManager;
import helpers.emulator.EmulatorHelper;
import helpers.emulator.EmulatorManager;
import helpers.utils.IsScriptRunning;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import scripts.ScriptExecutor;
import scripts.ScriptInfo;

import java.awt.image.BufferedImage;
import java.util.concurrent.*;

import static UI.components.utils.Observables.GAME_REFRESHRATE;
import static utils.Constants.*;

public class EmulatorView {
    private final Logger logger;
    private final LogArea logArea;
    private final EmulatorManager emulatorManager;
    private final EmulatorHelper emulatorHelper;
    public static final ListView<String> emulatorListView = new ListView<>();
    private static final ListView<String> emulatorDevListView = new ListView<>();
    private final ImageView imageView;
    private final IsScriptRunning isScriptRunning;
    private final ScriptSelectionUI scriptSelectionUI;
    private final ScriptExecutor scriptExecutor;
    private final ScriptInfo scriptInfo;

    // Reference to ThreadManager
    private final ThreadManager threadManager = ThreadManager.getInstance();

    // Scheduled task for updating the ImageView
    private ScheduledFuture<?> imageUpdateTask;

    public EmulatorView(ScriptInfo scriptInfo, Logger logger, EmulatorManager emulatorManager, EmulatorHelper emulatorHelper,
                        LogArea logArea, IsScriptRunning isScriptRunning, ScriptSelectionUI scriptSelectionUI,
                        ScriptExecutor scriptExecutor) {
        this.scriptInfo = scriptInfo;
        this.logger = logger;
        this.emulatorManager = emulatorManager;
        this.emulatorHelper = emulatorHelper;
        this.logArea = logArea;
        this.isScriptRunning = isScriptRunning;
        this.scriptSelectionUI = scriptSelectionUI;
        this.scriptExecutor = scriptExecutor;

        imageView = new ImageView();
        imageView.setFitWidth(670);
        imageView.setPreserveRatio(true);

        setupListView(emulatorListView); // Set the styling on the listview
        setupListView(emulatorDevListView);

        setupEmulatorListener(emulatorListView); // Start listener to update emulator list
        setupEmulatorListener(emulatorDevListView);
        setupListener(); // Set the listener for the image views on selected emulator
    }

    public static String getSelectedEmulator() {
        if (emulatorDevListView.getSelectionModel().getSelectedItem() != null) {
            return emulatorDevListView.getSelectionModel().getSelectedItem(); // Return the dev selected emulator
        } else if (emulatorListView.getSelectionModel().getSelectedItem() != null) {
            return emulatorListView.getSelectionModel().getSelectedItem(); // Return the regular selected emulator
        } else {
            return "none"; // Return "none" as in no selected emulator.
        }
    }

    public void setupListView(ListView<String> listView) {
        listView.setEditable(false);
        listView.getStyleClass().add("emulator-list-view");
        listView.getStylesheets().add(STYLESHEET);

        listView.setCellFactory(param -> new EmulatorListCell());
    }

    public static ListView<String> getListView() {
        return emulatorListView;
    }

    public static ListView<String> getEmulatorDevListView() {
        return emulatorDevListView;
    }


    public ImageView getImageView() {
        return imageView;
    }

    /**
     * Sets up the listener for emulator selection and initiates image updates.
     */
    public void setupListener() {
        emulatorListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            emulatorManager.monitorEmulators();

            if (imageView == null) {
                logger.print("ImageView has been garbage collected");
                return;
            }

            if (oldValue != null) {
                // Cancel any existing scheduled task for the old emulator and clear the ImageView
                stopImageUpdater(oldValue);
            }

            if (newValue != null) {
                logArea.setCurrentEmulatorID(newValue);
                logger.print("New emulator selected: " + newValue);
                startImageUpdater(newValue);
            }
        });
    }

    private void startImageUpdater(String emulatorID) {
        logger.print("Starting image updater for: " + emulatorID);

        if (imageUpdateTask != null && !imageUpdateTask.isCancelled()) {
            imageUpdateTask.cancel(true);
            logger.print("Existing image update task canceled for: " + emulatorID + " at " + System.currentTimeMillis());
        }

        final String capturedEmulatorID = emulatorID;

        imageUpdateTask = threadManager.getScheduler().scheduleWithFixedDelay(() -> {
            try {
                String currentSelectedEmulator = getSelectedEmulator();
                if (!capturedEmulatorID.equals(currentSelectedEmulator)) {
                    logger.print("Selected emulator has changed from " + capturedEmulatorID + " to " + currentSelectedEmulator);
                    return;
                }

                BufferedImage latestScreenshot = emulatorManager.getLatestScreenshot(emulatorID);
                if (latestScreenshot != null) {
                    Platform.runLater(() -> updateImageView(emulatorID, latestScreenshot));
                } else {
                    logger.print("No new screenshot available for: " + emulatorID + ", retrying with backoff..");
                }
            } catch (Exception e) {
                logger.err("Error while updating screenshot for " + emulatorID + ": " + e.getMessage());
                e.printStackTrace();
            }
        }, 250, GAME_REFRESHRATE.get(), TimeUnit.MILLISECONDS);
    }

    private void updateImageView(String emulatorID, BufferedImage latestImage) {
        if (latestImage == null) {
            return;
        }

        // Ensure that the emulatorID matches the currently selected emulator
        String currentSelectedEmulator = getSelectedEmulator();
        if (!emulatorID.equals(currentSelectedEmulator)) {
            logger.err("Attempted to update ImageView with emulatorID: " + emulatorID + ", but current selection is: " + currentSelectedEmulator);
            return;
        }

        Platform.runLater(() -> {
            try {
                imageView.setImage(SwingFXUtils.toFXImage(latestImage, null));
            } catch (Exception e) {
                logger.err("Failed to update ImageView for device: " + emulatorID + ". Error: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    /**
     * Stops the image updater task for the specified emulator and clears the ImageView.
     *
     * @param emulatorID The identifier of the emulator.
     */
    private void stopImageUpdater(String emulatorID) {
        logger.print("Stopping image updater for: " + emulatorID);

        if (imageUpdateTask != null && !imageUpdateTask.isCancelled()) {
            imageUpdateTask.cancel(true);
            logger.print("Image update task canceled for: " + emulatorID);
            imageUpdateTask = null;
        }

        // Clear the ImageView if it's the currently selected emulator
        String currentSelectedEmulator = getSelectedEmulator();
        if (emulatorID.equals(currentSelectedEmulator)) {
            Platform.runLater(() -> {
                imageView.setImage(null);
                logger.print("ImageView cleared for: " + emulatorID);
            });
        }
    }

    /**
     * Sets up the emulator listener to periodically update the emulator list.
     *
     * @param listView The ListView displaying the emulators.
     */
    public void setupEmulatorListener(ListView<String> listView) {
        threadManager.getScheduler().scheduleAtFixedRate(() -> emulatorHelper.updateOnlineEmulators(listView), 5, 10, TimeUnit.SECONDS);
        // Display a message if the list is empty
        listView.setPlaceholder(new Label("No online emulators"));
    }

    /**
     * Shuts down the EmulatorView, canceling all scheduled tasks.
     */
    public void shutdown() {
        logger.print("Shutting down EmulatorView");

        // Cancel the image update task if it's running
        if (imageUpdateTask != null && !imageUpdateTask.isCancelled()) {
            imageUpdateTask.cancel(true);
            logger.print("Image update task has been canceled.");
            imageUpdateTask = null;
        }

        // Clear the ImageView
        Platform.runLater(() -> {
            imageView.setImage(null);
            logger.print("ImageView cleared during shutdown.");
        });
    }

    private class EmulatorListCell extends ListCell<String> {
        private final ImageView icon = new ImageView();
        private final ImageView stopIcon = new ImageView(STOP_ICON);
        private final HBox content = new HBox();
        private final Label label = new Label();
        private final Region spacer = new Region();

        public EmulatorListCell() {
            getStyleClass().add("emulator-list-cell");

            // Configure label
            label.getStyleClass().add("emulator-label"); // Add a specific style class if needed

            // Configure stopIcon
            stopIcon.setPreserveRatio(true);
            stopIcon.setFitHeight(20);
            stopIcon.setVisible(false);

            // Configure icon
            icon.setPreserveRatio(true);
            icon.setFitHeight(20);

            // Configure content HBox
            content.getChildren().addAll(label, spacer, icon, stopIcon);
            content.setSpacing(10);
            content.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(spacer, Priority.ALWAYS);

            // Set up click handlers
            icon.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY) {
                    handleIconClick();
                }
            });

            stopIcon.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY) {
                    handleStopIconClick();
                }
            });
        }

        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                setGraphic(null);
                setText(null);
                return;
            }

            label.setText(item);
            setGraphic(content);

            // Set text color based on selection and focus
            if (isSelected()) {
                label.setStyle("-fx-text-fill: black;"); // Selected state text color
                setStyle("-fx-background-color: #e57c23;"); // Selected background color
            } else {
                label.setStyle("-fx-text-fill: white;"); // Default text color
                setStyle("-fx-background-color: #1B1817;"); // Default background color
            }

            // Bind stopIcon visibility to isRunning property
            BooleanProperty isRunning = isScriptRunning.isScriptRunningProperty(item);
            stopIcon.visibleProperty().bind(isRunning);

            // Bind icon image based on isRunning and isPaused properties
            icon.imageProperty().bind(Bindings.createObjectBinding(() -> {
                if (isRunning.get()) {
                    return scriptExecutor.pausedProperty().get() ? PLAY_ICON : PAUSE_ICON;
                } else {
                    return PLAY_ICON;
                }
            }, isRunning, scriptExecutor.pausedProperty()));
        }

        private void handleIconClick() {
            String currentEmulator = getItem();
            if (currentEmulator == null) {
                return;
            }

            BooleanProperty isRunning = isScriptRunning.isScriptRunningProperty(currentEmulator);
            if (!isRunning.get()) {
                scriptSelectionUI.display(currentEmulator);
            } else {
                AbstractScript currentScript = scriptInfo.getCurrentAbstractScript(currentEmulator);
                boolean isPaused = currentScript != null && currentScript.isPaused();
                scriptExecutor.pauseOrResumeScriptOnEmulator(currentEmulator);
                if (isPaused) {
                    logger.log("Resumed script", currentEmulator);
                } else {
                    logger.log("Paused script", currentEmulator);
                }
            }
        }

        private void handleStopIconClick() {
            String currentEmulator = getItem();
            if (currentEmulator == null) {
                return;
            }

            BooleanProperty isRunning = isScriptRunning.isScriptRunningProperty(currentEmulator);
            if (isRunning.get()) {
                scriptExecutor.stopScriptOnEmulator(currentEmulator);
                isRunning.set(false);
            }
        }
    }
}