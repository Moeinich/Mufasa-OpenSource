package UI;

import helpers.Logger;
import helpers.ThreadManager;
import helpers.emulator.EmulatorManager;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.awt.image.BufferedImage;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static UI.components.EmulatorView.getEmulatorDevListView;
import static UI.components.EmulatorView.getSelectedEmulator;
import static UI.components.utils.Observables.GAME_REFRESHRATE;
import static utils.Constants.STYLESHEET;

public class DevColorPicker {
    private final EmulatorManager emulatorManager;
    private final Logger logger;

    // Things for the gameview image updater
    private ListView<String> emulatorListView;
    private ScheduledFuture<?> imageUpdateTask;
    ThreadManager threadManager = ThreadManager.getInstance();

    private Stage stage;

    private static final int ZOOM_SIZE = 10;
    private static final int ZOOM_SCALE = 20;

    private ImageView gameView;
    private ImageView zoomedImageView;

    public DevColorPicker(EmulatorManager emulatorManager, Logger logger) {
        this.emulatorManager = emulatorManager;
        this.logger = logger;
    }

    private void initializeUI() {
        stage = new Stage();
        stage.setTitle("Dev Color Picker");

        // Initialize components
        initializeImageView();
        gameView = createMainImageView();
        zoomedImageView = createZoomedImageView();

        // The two panes
        Pane leftPane = new Pane(gameView);
        VBox rightPane = createRightPane();

        // Create the root layout
        HBox root = new HBox();
        root.getChildren().addAll(leftPane, rightPane);

        // Set up the scene
        Scene scene = new Scene(root);
        scene.getStylesheets().add(STYLESHEET);
        stage.setScene(scene);
    }

    private ImageView createMainImageView() {
        ImageView imageView = new ImageView();
        imageView.setFitWidth(894);
        imageView.setFitHeight(540);
        imageView.setPreserveRatio(false);
        imageView.addEventHandler(MouseEvent.MOUSE_MOVED, this::handleMouseMoved);
        return imageView;
    }

    private ImageView createZoomedImageView() {
        ImageView zoomedImageView = new ImageView();
        zoomedImageView.setFitWidth(ZOOM_SIZE * ZOOM_SCALE);
        zoomedImageView.setFitHeight(ZOOM_SIZE * ZOOM_SCALE);
        zoomedImageView.setPreserveRatio(false);
        zoomedImageView.setScaleX(ZOOM_SCALE);
        zoomedImageView.setScaleY(ZOOM_SCALE);
        return zoomedImageView;
    }

    private VBox createRightPane() {
        VBox rightPane = new VBox();
        rightPane.setPrefWidth(300);
        rightPane.getChildren().addAll(zoomedImageView, emulatorListView);
        return rightPane;
    }

    private void handleMouseMoved(MouseEvent event) {
        double mouseX = event.getX();
        double mouseY = event.getY();
        updateZoomedImage(mouseX, mouseY);
    }

    private void updateZoomedImage(double mouseX, double mouseY) {
        Image sourceImage = gameView.getImage();
        if (sourceImage == null) return;

        PixelReader pixelReader = sourceImage.getPixelReader();
        if (pixelReader == null) return;

        int imageX = (int) (mouseX / gameView.getFitWidth() * sourceImage.getWidth());
        int imageY = (int) (mouseY / gameView.getFitHeight() * sourceImage.getHeight());

        int zoomStartX = Math.max(imageX - ZOOM_SIZE / 2, 0);
        int zoomStartY = Math.max(imageY - ZOOM_SIZE / 2, 0);
        int zoomEndX = Math.min(zoomStartX + ZOOM_SIZE, (int) sourceImage.getWidth());
        int zoomEndY = Math.min(zoomStartY + ZOOM_SIZE, (int) sourceImage.getHeight());

        // Adjust zoom start if it goes out of bounds
        if (zoomEndX - zoomStartX < ZOOM_SIZE) zoomStartX = Math.max(zoomEndX - ZOOM_SIZE, 0);
        if (zoomEndY - zoomStartY < ZOOM_SIZE) zoomStartY = Math.max(zoomEndY - ZOOM_SIZE, 0);

        WritableImage zoomedImage = new WritableImage(pixelReader, zoomStartX, zoomStartY, zoomEndX - zoomStartX, zoomEndY - zoomStartY);
        zoomedImageView.setImage(zoomedImage);
    }

    private void initializeImageView() {
        emulatorListView = getEmulatorDevListView();
        gameView.setFitWidth(894);
        gameView.setFitHeight(540);
        emulatorListView.setPrefWidth(1150 - 894); // Adjust to fit within the 1150px limit
        emulatorListView.setMaxWidth(1150 - 894);

        emulatorListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            emulatorManager.monitorEmulators();

            if (oldValue != null) {
                // Cancel any existing scheduled task for the old emulator and clear the ImageView
                stopImageUpdater(oldValue);
            }

            if (newValue != null) {
                logger.print("New emulator selected: " + newValue);
                startImageUpdater(newValue);
            }
        });
    }

    /**
     * Starts a new image updater task for the specified emulator.
     *
     * @param emulatorID The identifier of the emulator.
     */
    private void startImageUpdater(String emulatorID) {
        logger.print("Starting image updater for: " + emulatorID + " at " + System.currentTimeMillis());

        // Cancel any existing update task to avoid multiple concurrent tasks
        if (imageUpdateTask != null && !imageUpdateTask.isCancelled()) {
            imageUpdateTask.cancel(true);
            logger.print("Existing image update task canceled for: " + emulatorID + " at " + System.currentTimeMillis());
        }

        // Schedule a new task to periodically update the ImageView
        imageUpdateTask = threadManager.getScheduler().scheduleWithFixedDelay(() -> {
            try {
                BufferedImage latestScreenshot = emulatorManager.getLatestScreenshot(getSelectedEmulator()) != null
                        ? emulatorManager.getLatestScreenshot(getSelectedEmulator())
                        : null;

                if (latestScreenshot != null) {
                    updateImageView(emulatorID, latestScreenshot);
                } else {
                    logger.print("No new screenshot available for: " + emulatorID + ", will retry in " + GAME_REFRESHRATE.get() + " ms");
                }
            } catch (Exception e) {
                logger.err("Error while updating screenshot for " + emulatorID + ": " + e.getMessage());
                e.printStackTrace();
            }
        }, (GAME_REFRESHRATE.get() * 4L), (GAME_REFRESHRATE.get() * 4L), TimeUnit.MILLISECONDS);
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

        // Clear the ImageView
        Platform.runLater(() -> {
            gameView.setImage(null);
            logger.print("ImageView cleared for: " + emulatorID);
        });
    }

    /**
     * Updates the ImageView with the latest image from the emulator.
     *
     * @param emulatorID  The identifier of the emulator.
     * @param latestImage The latest screenshot to be displayed.
     */
    private void updateImageView(String emulatorID, BufferedImage latestImage) {
        if (latestImage == null) {
            return;
        }

        threadManager.getUnifiedExecutor().submit(() -> Platform.runLater(() -> {
            try {
                gameView.setImage(SwingFXUtils.toFXImage(latestImage, null));
            } catch (Exception e) {
                logger.err("Failed to update ImageView for device: " + emulatorID + ". Error: " + e.getMessage());
                e.printStackTrace();
            }
        }));
    }

    public void display() {
        if (stage == null) {
            initializeUI();

            stage.setOnCloseRequest(event -> {
                if (imageUpdateTask != null && !imageUpdateTask.isCancelled()) {
                    imageUpdateTask.cancel(true);
                    logger.print("Image update task canceled on window close.");
                }
            });
        }
        stage.show();
    }
}