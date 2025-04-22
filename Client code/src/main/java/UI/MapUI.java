package UI;

import UI.components.EmulatorView;
import helpers.ThreadManager;
import helpers.utils.Area;
import helpers.utils.MapChunk;
import helpers.utils.Tile;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import osr.mapping.Minimap;
import osr.utils.ImageUtils;
import osr.walker.Walker;
import osr.walker.utils.ChunkCoordinates;
import osr.walker.utils.MapChunkHandler;
import osr.walker.utils.PositionResult;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static UI.components.MapUtils.parseChunksTF;
import static UI.components.MapUtils.toHexString;
import static osr.walker.utils.MapChunkHandler.getWorldCoordinateWithPlane;
import static utils.Constants.*;

public class MapUI {
    private final Minimap minimap;
    private final Walker walker;
    private final ImageUtils imageUtils;
    private final MapChunkHandler mapChunkHandler;

    private final List<java.awt.Point> clickedPoints = new ArrayList<>();
    private final double zoomFactor = 1.1;
    private final ImageView worldMapView = new ImageView();
    private final ImageView drawingView = new ImageView();
    private final ImageView originalMinimapView = new ImageView();
    private final ImageView cleanedMinimapView = new ImageView();
    // Buttons
    private final Button searchButton = new Button("Find Position");
    private final Button continualSearchButton = new Button("Start position updater");
    private final ToggleButton areaGenerator = new ToggleButton();
    private final ToggleButton pathGenerator = new ToggleButton();
    private final ToggleButton importGenerator = new ToggleButton();

    private final Button getMinimapButton = new Button("Get Minimap");
    private final Button saveMinimapButton = new Button("Enable saving");
    // Label things
    private final TextArea areaAndPathBox = new TextArea();
    private final TextArea importBox = new TextArea();

    private final TextField coordinateBox = new TextField("");
    private final TextField matchConfidenceBox = new TextField("");
    private final TextField timeSpentBox = new TextField("");
    private final Color activeColor = Color.web("#f3c244");
    private final Color inactiveColor = Color.WHITE;
    private final Label searchTimeLabel = new Label();
    // Boolean property for minimap save state
    private final BooleanProperty savingEnabled = new SimpleBooleanProperty(false);
    // Chunk stuff
    private final TextField chunksToLoad = new TextField();
    private final TextField planesToLoad = new TextField();
    // Tiling stuff
    int worldmapWidth;
    int worldmapHeight;
    // Parameters
    private Point lastClickedPoint;
    private Point firstClickPoint = null;
    private Point secondClickPoint = null;
    private double currentZoom = 1.0;
    private Point mousePressPoint;
    // Views
    private StackPane mapView;
    private ScrollPane worldMapScrollPane;
    private WritableImage drawingBuffer;
    // Timeline
    private ScheduledExecutorService executorService = ThreadManager.getInstance().getScheduler();

    public MapUI(Minimap minimap, Walker walker, ImageUtils imageUtils, MapChunkHandler mapChunkHandler) {
        this.minimap = minimap;
        this.walker = walker;
        this.imageUtils = imageUtils;
        this.mapChunkHandler = mapChunkHandler;
    }

    private void initializeUI() {
        setupCustomStyles();
        setupAreaGenerator();
        setupPathGeneratorButton();
        setupImportGenerator();
        setupMinimapButtons();
        setupSearchButton();
        setupPositionUpdater();
        setupCoordinateLabel();
        setupLabelBox();
        setupLabelBoxListener();

        // Configure the world map view to fit within a specific height
        worldMapView.setPreserveRatio(true);
        worldMapView.setSmooth(true);
        // Make drawingView's size follow worldMapView's size
        drawingView.fitWidthProperty().bind(worldMapView.fitWidthProperty());
        drawingView.fitHeightProperty().bind(worldMapView.fitHeightProperty());
        drawingView.setPreserveRatio(true);
        drawingView.setMouseTransparent(true);

        StackPane mapViewStack = new StackPane();
        mapViewStack.setAlignment(Pos.CENTER);
        mapViewStack.getChildren().addAll(worldMapView, drawingView);

        // Map view
        worldMapScrollPane = new ScrollPane(mapViewStack);
        worldMapScrollPane.setPrefViewportWidth(1280);
        worldMapScrollPane.setPrefViewportHeight(720);
        worldMapScrollPane.setPannable(true);
        worldMapScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        worldMapScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        // Setup layout for minimap views
        HBox minimapBox = new HBox(10, originalMinimapView, cleanedMinimapView);
        minimapBox.setAlignment(Pos.CENTER);

        // Chunk stuff!
        chunksToLoad.setPromptText("chunks");
        planesToLoad.setPromptText("Planes");
        Button loadChunks = new Button("Load chunks");
        loadChunks.setOnAction(event -> loadChunksToMap());

        // Setup map control buttons
        VBox mapControlButtons = new VBox(10, chunksToLoad, planesToLoad, loadChunks);
        mapControlButtons.setFillWidth(false);
        // Map Controls
        VBox mapControls = new VBox(5, new Label("Map Controls"), mapControlButtons, minimapBox);
        mapControls.setFillWidth(false);
        // Minimap Controls
        VBox minimapControls = new VBox(5, new Label("Minimap Controls"), saveMinimapButton, getMinimapButton);
        minimapControls.setFillWidth(false);
        // Search and Display
        VBox searchAndDisplay = new VBox(5, new Label("Search and Display"), searchButton, continualSearchButton, searchTimeLabel);
        searchAndDisplay.setFillWidth(false);
        // World interactions
        VBox worldInteractions = new VBox(10, areaGenerator, pathGenerator, importGenerator);
        worldInteractions.setFillWidth(false);

        VBox labelBox = new VBox(areaAndPathBox, importBox);
        // Coordinate field
        HBox coordinateArea = new HBox(5, coordinateBox, matchConfidenceBox, timeSpentBox);

        // Combine all control sections in a horizontal layout (each group in its own vertical box)
        VBox leftSideControls = new VBox(20, coordinateArea, mapControls, minimapControls, searchAndDisplay);
        leftSideControls.setMaxWidth(Control.USE_PREF_SIZE);
        leftSideControls.setMaxHeight(Control.USE_PREF_SIZE);

        HBox rightSideControls = new HBox(10, labelBox, worldInteractions);
        rightSideControls.setMaxWidth(Control.USE_PREF_SIZE);
        rightSideControls.setMaxHeight(Control.USE_PREF_SIZE);

        // Setup zooming functionalities
        setupZoom();
        // Setup click listeners
        setupImageClicks();

        // Add the world map scroll pane
        mapView = new StackPane();
        mapView.getChildren().addAll(worldMapScrollPane, rightSideControls, leftSideControls);
        StackPane.setAlignment(rightSideControls, Pos.TOP_RIGHT);
        StackPane.setAlignment(leftSideControls, Pos.TOP_LEFT);
        StackPane.setMargin(rightSideControls, new Insets(20, 20, 0, 0));
        StackPane.setMargin(leftSideControls, new Insets(20, 0, 0, 20));
        setImage(new MapChunk(new String[]{"50-50"}, "0"));
    }

    private void loadChunksToMap() {
        MapChunk mapChunk = parseChunksTF(chunksToLoad, planesToLoad);
        setImage(mapChunk);
    }


    private void markFoundPosition(int x, int y) {
        if (drawingBuffer == null) {
            return;
        }

        PixelWriter pixelWriter = drawingBuffer.getPixelWriter();

        // Define lineLength as a local variable
        int lineLength = 30;

        // Define the area that needs to be updated (1000x1000 area)
        int updateAreaSize = 1000;
        int halfUpdateAreaSize = updateAreaSize / 2;

        // Calculate the minimum and maximum X and Y values, ensuring they stay within bounds
        int minX = Math.max(0, x - halfUpdateAreaSize);
        int minY = Math.max(0, y - halfUpdateAreaSize);
        int maxX = Math.min((int) drawingBuffer.getWidth(), x + halfUpdateAreaSize);
        int maxY = Math.min((int) drawingBuffer.getHeight(), y + halfUpdateAreaSize);

        // Clear only the specific 1000x1000 area
        clearDrawingBuffer(pixelWriter, minX, minY, maxX - minX, maxY - minY);

        // Color for the rectangle's stroke
        Color strokeColor = Color.rgb(243, 194, 68);

        // Draw a 4x4 rectangle stroke around the area
        drawRectangle(pixelWriter, x, y, strokeColor);

        // Draw lines extending from the edges of the 4x4 rectangle
        drawLine(pixelWriter, x + 2, y, x + 2, y - lineLength, strokeColor);
        drawLine(pixelWriter, x + 2, y + 4, x + 2, y + 4 + lineLength, strokeColor);
        drawLine(pixelWriter, x, y + 2, x - lineLength, y + 2, strokeColor);
        drawLine(pixelWriter, x + 4, y + 2, x + 4 + lineLength, y + 2, strokeColor);

        drawingView.setImage(drawingBuffer);
    }

    private void clearDrawingBuffer(PixelWriter pixelWriter, int minX, int minY, int width, int height) {
        Color transparent = Color.TRANSPARENT;

        for (int y = minY; y < minY + height; y++) {
            for (int x = minX; x < minX + width; x++) {
                pixelWriter.setColor(x, y, transparent);
            }
        }
    }

    private void setupPositionUpdater() {
        continualSearchButton.setOnAction(event -> {
            if (executorService.isShutdown() || executorService.isTerminated()) {
                executorService = Executors.newSingleThreadScheduledExecutor();
                executorService.scheduleWithFixedDelay(this::updatePosition, 0, 200, TimeUnit.MILLISECONDS);
                continualSearchButton.setText("Stop position updater");
            } else {
                executorService.shutdown();
                continualSearchButton.setText("Start position updater");
            }
        });
    }

    private void setupSearchButton() {
        searchButton.setOnAction(event -> updatePosition());
    }

    private void updatePosition() {
        long startTime = System.currentTimeMillis();
        PositionResult result = walker.getPlayerPosition(EmulatorView.getSelectedEmulator());
        long endTime = System.currentTimeMillis();

        if (result.x != 0 && result.y != 0) {
            System.out.println("Final Local Coordinates: (" + result.x + ", " + result.y + ")");
            markFoundPosition(result.x, result.y);
            centerScrollPaneOnMark(result.x, result.y);
        } else {
            System.out.println("Local coordinates not found for world coordinates: (" + result.x + ", " + result.y + ")");
        }

        double confidence = result.getConfidence();
        Point worldCoordinates = result.getWorldCoordinates(EmulatorView.getSelectedEmulator()).getPoint();
        System.out.println("World coordinates: " + worldCoordinates.x + "," + worldCoordinates.y);
        coordinateBox.setText(String.format("%d, %d, %d", worldCoordinates.x, worldCoordinates.y, result.z));
        matchConfidenceBox.setText(String.format("%.0f%%", confidence * 100));
        timeSpentBox.setText((endTime - startTime) + " ms");

        Point localCoordinates = MapChunkHandler.getLocalCoordinatesFromWorld(EmulatorView.getSelectedEmulator(), worldCoordinates.x, worldCoordinates.y, result.z).getPoint();
        System.out.println("Local converted back: " + localCoordinates.x + "," + localCoordinates.y);
    }

    private void setupCoordinateLabel() {
        coordinateBox.setOnMouseClicked(event -> {
            // Get the content of the coordinateBox
            String content = coordinateBox.getText();

            // Copy the content to the clipboard
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent clipboardContent = new ClipboardContent();
            clipboardContent.putString(content);
            clipboard.setContent(clipboardContent);
        });
    }

    private void setupLabelBox() {
        areaAndPathBox.setOnMouseClicked(event -> {
            // Get the content of the coordinateBox
            String content = areaAndPathBox.getText();

            // Copy the content to the clipboard
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent clipboardContent = new ClipboardContent();
            clipboardContent.putString(content);
            clipboard.setContent(clipboardContent);
        });
    }

    private void centerScrollPaneOnMark(int x, int y) {
        if (worldMapView.getImage() == null) return;

        double zoomAdjustmentFactor = currentZoom;

        double scaledX = x * zoomAdjustmentFactor;
        double scaledY = y * zoomAdjustmentFactor;

        double imageWidth = worldmapWidth * zoomAdjustmentFactor;
        double imageHeight = worldmapHeight * zoomAdjustmentFactor;

        double viewportWidth = worldMapScrollPane.getViewportBounds().getWidth();
        double viewportHeight = worldMapScrollPane.getViewportBounds().getHeight();

        // Calculate the center position of the viewport in the image's coordinate system
        double centerX = scaledX - viewportWidth / 2;
        double centerY = scaledY - viewportHeight / 2;

        // Calculate scroll values
        double scrollX = centerX / (imageWidth - viewportWidth);
        double scrollY = centerY / (imageHeight - viewportHeight);

        // Clamp values between 0 and 1
        scrollX = Math.max(0, Math.min(scrollX, 1));
        scrollY = Math.max(0, Math.min(scrollY, 1));

        // Only update the scroll position if it has changed
        if (worldMapScrollPane.getHvalue() != scrollX) {
            worldMapScrollPane.setHvalue(scrollX);
        }
        if (worldMapScrollPane.getVvalue() != scrollY) {
            worldMapScrollPane.setVvalue(scrollY);
        }
    }

    private void setupMinimapButtons() {

        savingEnabled.set(false); // Ensure it's explicitly set even if it's already the default
        saveMinimapButton.setText("Enable saving");
        saveMinimapButton.setStyle("-fx-background-color: white;"); // Set to white as it is disabled by default
        saveMinimapButton.setOnAction(event -> {
            savingEnabled.set(!savingEnabled.get());  // Toggle the state
            if (savingEnabled.get()) {
                saveMinimapButton.setText("Disable saving");
                saveMinimapButton.setStyle(""); // Reset to default styling or specify enabled styling
            } else {
                saveMinimapButton.setText("Enable saving");
                saveMinimapButton.setStyle("-fx-background-color: white;"); // Set to white when disabled
            }
        });

        // Use the stored emulatorId in your action handlers
        getMinimapButton.setOnAction(event -> {
            if (EmulatorView.getSelectedEmulator() == null) {
                showDialog("An error has occured", "No emulator is selected. Please select an emulator before trying to retrieve your minimap.", Alert.AlertType.INFORMATION);
                return;
            }

            Image minimapImage = imageUtils.matToImage(minimap.getMinimap(EmulatorView.getSelectedEmulator()));
            if (minimapImage != null) {
                originalMinimapView.setImage(minimapImage);
            }

            Image cleanMinimapImage = minimap.getCleanMinimap(EmulatorView.getSelectedEmulator(), savingEnabled.get(), true, true);
            if (cleanMinimapImage != null) {
                cleanedMinimapView.setImage(cleanMinimapImage);
            }
        });
    }

    private void setupAreaGenerator() {
        Image iconImage = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/assets/icons/Rectangle.png")));
        ImageView areaImageView = new ImageView(iconImage);
        areaImageView.setFitWidth(20);
        areaImageView.setPreserveRatio(true);

        // Set initial state and icon
        areaGenerator.setSelected(false);
        areaGenerator.setGraphic(areaImageView);
        areaGenerator.setStyle("-fx-background-color: " + toHexString(inactiveColor) + ";");

        // Add a listener to handle toggle state changes
        areaGenerator.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                clearDrawnGraphics();
                areaAndPathBox.setText("");
                importBox.setText("");
                areaGenerator.setStyle("-fx-background-color: " + toHexString(activeColor) + ";");
                if (pathGenerator.isSelected()) {
                    pathGenerator.setSelected(false);
                }
                if (importGenerator.isSelected()) {
                    importGenerator.setSelected(false);
                }
                clearClickedPoints();
            } else {
                areaGenerator.setStyle("-fx-background-color: " + toHexString(inactiveColor) + ";");
                areaAndPathBox.setText("");
                importBox.setText("");
                clearDrawnGraphics();
            }
        });
    }

    private void setupImportGenerator() {
        Image iconImage = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/assets/icons/Import.png")));
        ImageView areaImageView = new ImageView(iconImage);
        areaImageView.setFitWidth(20);
        areaImageView.setPreserveRatio(true);

        // Set initial state and icon
        importGenerator.setSelected(false);
        importGenerator.setGraphic(areaImageView);
        importGenerator.setStyle("-fx-background-color: " + toHexString(inactiveColor) + ";");

        // Add a listener to handle toggle state changes
        importGenerator.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                clearDrawnGraphics();
                areaAndPathBox.setText("");
                importBox.setText("");
                importGenerator.setStyle("-fx-background-color: " + toHexString(activeColor) + ";");
                if (pathGenerator.isSelected()) {
                    pathGenerator.setSelected(false);
                }
                if (areaGenerator.isSelected()) {
                    areaGenerator.setSelected(false);
                }
                clearClickedPoints();
            } else {
                importGenerator.setStyle("-fx-background-color: " + toHexString(inactiveColor) + ";");
                areaAndPathBox.setText("");
                importBox.setText("");
                clearDrawnGraphics();
            }
        });
    }

    private void setupPathGeneratorButton() {
        Image pathIconImage = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/assets/icons/Path.png")));
        ImageView pathImageView = new ImageView(pathIconImage);
        pathImageView.setFitWidth(20);
        pathImageView.setPreserveRatio(true);

        // Set initial state and icon for the path generator button
        pathGenerator.setSelected(false);
        pathGenerator.setGraphic(pathImageView);
        pathGenerator.setStyle("-fx-background-color: " + toHexString(inactiveColor) + ";");

        // Add a listener to handle toggle state changes for the path generator button
        pathGenerator.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                clearDrawnGraphics();
                clickedPoints.clear();
                areaAndPathBox.setText("");
                importBox.setText("");
                pathGenerator.setStyle("-fx-background-color: " + toHexString(activeColor) + ";");
                if (areaGenerator.isSelected()) {
                    areaGenerator.setSelected(false);
                }
                if (importGenerator.isSelected()) {
                    importGenerator.setSelected(false);
                }
                // Additional actions when the button is selected
            } else {
                pathGenerator.setStyle("-fx-background-color: " + toHexString(inactiveColor) + ";");
                clearDrawnGraphics(); // Clear the graphics for path generation, if needed
                areaAndPathBox.setText("");
                importBox.setText("");
            }
        });
    }

    private void setupImageClicks() {
        // Handle mouse press
        worldMapScrollPane.setOnMousePressed(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                mousePressPoint = new Point((int) event.getX(), (int) event.getY());
            }
        });

        // Handle mouse release (click)
        worldMapScrollPane.setOnMouseReleased(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                Point mouseReleasePoint = new Point((int) event.getX(), (int) event.getY());

                // Check if the mouse has moved significantly
                if (Math.abs(mousePressPoint.x - mouseReleasePoint.x) < 5 && Math.abs(mousePressPoint.y - mouseReleasePoint.y) < 5) {
                    // Process as a click since there was minimal movement
                    processClick(event);
                }
            }
        });
    }

    private void handlePointPathGeneration(int x, int y) {
        // Add the clicked point
        clickedPoints.add(new Point(x, y));

        // Incrementally update the labelBox content
        updatePointPathText();
    }

    private void handleAreaMapping(int x, int y) {
        Point currentClickPoint = new Point(x, y);

        if (firstClickPoint == null) {
            firstClickPoint = currentClickPoint;
        } else if (secondClickPoint == null) {
            secondClickPoint = currentClickPoint;

            // Calculate top-left and bottom-right points
            Point topLeft = new Point(Math.min(firstClickPoint.x, secondClickPoint.x), Math.min(firstClickPoint.y, secondClickPoint.y));
            Point bottomRight = new Point(Math.max(firstClickPoint.x, secondClickPoint.x), Math.max(firstClickPoint.y, secondClickPoint.y));

            // Update the labelBox content
            ChunkCoordinates topLeftCoords = getWorldCoordinateWithPlane(EmulatorView.getSelectedEmulator(), topLeft.x, topLeft.y);
            ChunkCoordinates bottomRightCoords = getWorldCoordinateWithPlane(EmulatorView.getSelectedEmulator(), bottomRight.x, bottomRight.y);

            Tile topLeftTile = new Tile(topLeftCoords.x, topLeftCoords.y, topLeftCoords.z);
            Tile bottomRightTile = new Tile(bottomRightCoords.x, bottomRightCoords.y, bottomRightCoords.z);

            String areaText = String.format(
                    "new Area(\n\tnew Tile(%d, %d, %d), \n\tnew Tile(%d, %d, %d)\n);",
                    topLeftTile.x(), topLeftTile.y(), topLeftTile.z(),
                    bottomRightTile.x(), bottomRightTile.y(), bottomRightTile.z()
            );

            areaAndPathBox.setText(areaText);

            // Reset for a new area
            firstClickPoint = null;
            secondClickPoint = null;
        }
    }

    private void drawPoint(PixelWriter pixelWriter, int x, int y) {
        Color highlightColor = Color.rgb(243, 194, 68); // JavaFX color equivalent of #f3c244

        // Draw a 3x3 square centered on the point
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                int drawX = x + i;
                int drawY = y + j;

                // Ensure the coordinates are within the image bounds
                if (drawX >= 0 && drawX < drawingBuffer.getWidth() && drawY >= 0 && drawY < drawingBuffer.getHeight()) {
                    pixelWriter.setColor(drawX, drawY, highlightColor);
                }
            }
        }
    }

    private void drawLine(PixelWriter pixelWriter, Point p1, Point p2) {
        int x1 = p1.x;
        int y1 = p1.y;
        int x2 = p2.x;
        int y2 = p2.y;

        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int sx = x1 < x2 ? 1 : -1;
        int sy = y1 < y2 ? 1 : -1;
        int err = dx - dy;

        while (true) {
            // Ensure the coordinates are within the image bounds
            if (x1 >= 0 && x1 < drawingBuffer.getWidth() && y1 >= 0 && y1 < drawingBuffer.getHeight()) {
                pixelWriter.setColor(x1, y1, Color.YELLOW);
            }

            if (x1 == x2 && y1 == y2) break;

            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x1 += sx;
            }
            if (e2 < dx) {
                err += dx;
                y1 += sy;
            }
        }
    }

    private void drawRectangle(PixelWriter pixelWriter, int x, int y, Color color) {
        // Draw the top and bottom sides of the rectangle
        for (int i = 0; i < 4; i++) {
            pixelWriter.setColor(x + i, y, color); // Top side
            pixelWriter.setColor(x + i, y + 4 - 1, color); // Bottom side
        }

        // Draw the left and right sides of the rectangle
        for (int i = 0; i < 4; i++) {
            pixelWriter.setColor(x, y + i, color); // Left side
            pixelWriter.setColor(x + 4 - 1, y + i, color); // Right side
        }
    }

    private void drawLine(PixelWriter pixelWriter, int x1, int y1, int x2, int y2, Color color) {
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int sx = x1 < x2 ? 1 : -1;
        int sy = y1 < y2 ? 1 : -1;
        int err = dx - dy;

        while (true) {
            if (x1 >= 0 && x1 < drawingBuffer.getWidth() && y1 >= 0 && y1 < drawingBuffer.getHeight()) {
                pixelWriter.setColor(x1, y1, color);
            }

            if (x1 == x2 && y1 == y2) break;

            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x1 += sx;
            }
            if (e2 < dx) {
                err += dx;
                y1 += sy;
            }
        }
    }

    /**
     * Draws a rectangle on the PixelWriter using two Tile objects.
     * The Tiles are first converted to local ChunkCoordinates before drawing.
     *
     * @param pixelWriter The PixelWriter to draw on.
     * @param topTile     The Tile representing the top-left (or one corner) of the rectangle.
     * @param bottomTile  The Tile representing the bottom-right (or the opposite corner) of the rectangle.
     */
    private void drawRectangle(PixelWriter pixelWriter, Tile topTile, Tile bottomTile) {
        clearDrawnGraphics();

        // Define the color for the rectangle edges
        Color rectangleColor = Color.rgb(243, 194, 68); // Equivalent to #f3c244

        // Convert topTile to local ChunkCoordinates
        ChunkCoordinates topLocal = MapChunkHandler.getLocalCoordinatesFromWorld(
                EmulatorView.getSelectedEmulator(),
                topTile.x(),
                topTile.y(),
                topTile.z()
        );

        // Convert bottomTile to local ChunkCoordinates
        ChunkCoordinates bottomLocal = MapChunkHandler.getLocalCoordinatesFromWorld(
                EmulatorView.getSelectedEmulator(),
                bottomTile.x(),
                bottomTile.y(),
                bottomTile.z()
        );

        // Extract x and y from ChunkCoordinates
        int x1 = topLocal.x;
        int y1 = topLocal.y;
        int x2 = bottomLocal.x;
        int y2 = bottomLocal.y;

        // Determine the minimum and maximum coordinates for the rectangle
        int minX = Math.min(x1, x2);
        int maxX = Math.max(x1, x2);
        int minY = Math.min(y1, y2);
        int maxY = Math.max(y1, y2);

        // Draw the top and bottom edges of the rectangle
        for (int x = minX; x <= maxX; x++) {
            // Draw top edge
            if (isWithinBounds(x, minY)) {
                pixelWriter.setColor(x, minY, rectangleColor);
            }
            // Draw bottom edge
            if (isWithinBounds(x, maxY)) {
                pixelWriter.setColor(x, maxY, rectangleColor);
            }
        }

        // Draw the left and right edges of the rectangle
        for (int y = minY; y <= maxY; y++) {
            // Draw left edge
            if (isWithinBounds(minX, y)) {
                pixelWriter.setColor(minX, y, rectangleColor);
            }
            // Draw right edge
            if (isWithinBounds(maxX, y)) {
                pixelWriter.setColor(maxX, y, rectangleColor);
            }
        }
    }

    /**
     * Checks if the given (x, y) coordinates are within the drawing buffer bounds.
     *
     * @param x The x-coordinate to check.
     * @param y The y-coordinate to check.
     * @return True if (x, y) is within bounds; otherwise, false.
     */
    private boolean isWithinBounds(int x, int y) {
        return x >= 0 && x < drawingBuffer.getWidth() && y >= 0 && y < drawingBuffer.getHeight();
    }

    private void handleSinglePointSelection(int x, int y) {
        if (drawingBuffer == null) {
            return;
        }

        // Get the pixel writer for the WritableImage
        PixelWriter pixelWriter = drawingBuffer.getPixelWriter();

        // Write the coordinates for the single point
        ChunkCoordinates wmPoint = getWorldCoordinateWithPlane(EmulatorView.getSelectedEmulator(), x, y);
        Tile tileCoordinates = new Tile(wmPoint.x, wmPoint.y, wmPoint.z);
        String text = String.format("%d, %d", tileCoordinates.x(), tileCoordinates.y());
        coordinateBox.setText(text);

        // Draw a 3x3 square centered on the clicked point
        Color highlightColor = Color.rgb(243, 194, 68); // JavaFX color equivalent of #f3c244

        // Draw the 3x3 square using the PixelWriter
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                int drawX = x + i;
                int drawY = y + j;

                // Ensure the coordinates are within the image bounds
                if (drawX >= 0 && drawX < drawingBuffer.getWidth() && drawY >= 0 && drawY < drawingBuffer.getHeight()) {
                    pixelWriter.setColor(drawX, drawY, highlightColor);
                }
            }
        }

        // Set the modified WritableImage back to the ImageView
        drawingView.setImage(drawingBuffer);
    }

    private void setupZoom() {
        // Bind the drawingView and regionView to the worldMapView scaling
        drawingView.scaleXProperty().bind(worldMapView.scaleXProperty());
        drawingView.scaleYProperty().bind(worldMapView.scaleYProperty());

        // Add a scroll listener for zooming
        worldMapScrollPane.addEventFilter(ScrollEvent.SCROLL, event -> {
            event.consume();
            double deltaY = event.getDeltaY();

            // Calculate scale factor
            double scaleFactor = (deltaY > 0) ? zoomFactor : 1 / zoomFactor;

            // Compute mouse cursor's position relative to ImageView
            Point2D mouseSceneCoords = new Point2D(event.getSceneX(), event.getSceneY());
            Point2D mouseCoordsInImageView = worldMapView.sceneToLocal(mouseSceneCoords);

            // Scale the ImageView
            currentZoom *= scaleFactor;
            currentZoom = Math.min(Math.max(currentZoom, 0.5), 6.0);
            worldMapView.setScaleX(currentZoom);
            worldMapView.setScaleY(currentZoom);

            // Calculate translation factors
            Point2D mouseCoordsAfterScale = worldMapView.localToScene(mouseCoordsInImageView);
            double deltaX = mouseSceneCoords.getX() - mouseCoordsAfterScale.getX();
            double deltaYz = mouseSceneCoords.getY() - mouseCoordsAfterScale.getY();

            // Apply translation to ImageView
            worldMapView.setTranslateX(worldMapView.getTranslateX() + deltaX);
            worldMapView.setTranslateY(worldMapView.getTranslateY() + deltaYz);
            // Apply the same translation to drawingView
            drawingView.setTranslateX(drawingView.getTranslateX() + deltaX);
            drawingView.setTranslateY(drawingView.getTranslateY() + deltaYz);
        });
    }

    private void updatePointPathText() {
        StringBuilder pointPathText = new StringBuilder("Tile[] path = new Tile[] {\n");

        for (java.awt.Point point : clickedPoints) {
            ChunkCoordinates wmPoint = getWorldCoordinateWithPlane(EmulatorView.getSelectedEmulator(), point.x, point.y);
            Tile wmCoordinates = new Tile(wmPoint.x, wmPoint.y, wmPoint.z);
            pointPathText.append(String.format("\tnew Tile(%d, %d, %d),\n", wmCoordinates.x(), wmCoordinates.y(), wmCoordinates.z()));
        }

        // Remove the trailing ",\n" if there are points
        if (!clickedPoints.isEmpty()) {
            pointPathText.setLength(pointPathText.length() - 2);
        }

        pointPathText.append("\n};");

        areaAndPathBox.setText(pointPathText.toString());
    }

    private void parseAndRenderLabelBoxContent(String content) {
        // Trim the content to avoid issues with leading/trailing whitespace
        content = content.trim();

        // Determine if the content is a Tile path
        if (content.startsWith("Tile[]") || content.startsWith("{") || content.contains("new Tile[]")) {
            // Handle path input
            List<Tile> path = parsePath(content);
            if (path != null) {
                drawPath(path);
            } else {
                showDialog("Error", "Invalid path format in label box.", Alert.AlertType.ERROR);
            }
        } else if (content.startsWith("Area") || content.startsWith("new Area") || content.startsWith("(")) {
            // Handle area input
            Area area = parseArea(content);
            if (area != null) {
                drawArea(area);
            } else {
                showDialog("Error", "Invalid area format in label box.", Alert.AlertType.ERROR);
            }
        } else {
            showDialog("Error", "Invalid format. Must be either a path or area.", Alert.AlertType.ERROR);
        }
    }

    private List<Tile> parsePath(String content) {
        try {
            // Trim the content to avoid issues with leading/trailing whitespace
            content = content.trim();

            // Handle optional "Tile[] <name> =" or "new Tile[]" or "Tile[]"
            if (content.startsWith("Tile[]")) {
                int equalsIndex = content.indexOf("=");
                if (equalsIndex != -1) {
                    // Extract everything after '='
                    content = content.substring(equalsIndex + 1).trim();
                }

                // Remove optional "new Tile[]"
                if (content.startsWith("new Tile[]")) {
                    content = content.substring("new Tile[]".length()).trim();
                }
            } else if (content.startsWith("new Tile[]")) {
                // Remove optional "new Tile[]"
                content = content.substring("new Tile[]".length()).trim();
            }

            // Handle cases with or without a trailing semicolon
            if (content.endsWith(";")) {
                content = content.substring(0, content.length() - 1).trim();
            }

            // Ensure content starts with "{" and ends with "}"
            if (!content.startsWith("{") || !content.endsWith("}")) {
                showDialog("Error", "Format error: Missing curly braces.", Alert.AlertType.ERROR);
                return null; // Invalid format
            }

            // Remove the outermost curly braces
            content = content.substring(1, content.length() - 1).trim();

            // Handle empty paths
            if (content.isEmpty()) {
                showDialog("Error", "Path is empty.", Alert.AlertType.ERROR);
                return new ArrayList<>(); // Valid empty path
            }

            // Split the tiles by "new Tile("
            String[] tileStrings = content.split("new Tile\\(");
            List<Tile> path = new ArrayList<>();

            for (String tileString : tileStrings) {
                tileString = tileString.trim();
                if (tileString.isEmpty()) continue;

                // Ensure the tile ends with ")"
                int endIndex = tileString.indexOf(")");
                if (endIndex == -1) {
                    showDialog("Error", "Format error: Missing closing parenthesis in tile.", Alert.AlertType.ERROR);
                    return null; // Invalid format
                }

                // Extract coordinates
                String tileData = tileString.substring(0, endIndex).trim();
                String[] coords = tileData.split(",");
                if (coords.length != 3) {
                    showDialog("Error", "Format error: Incorrect number of coordinates in tile.", Alert.AlertType.ERROR);
                    return null; // Invalid format
                }

                try {
                    // Parse coordinates
                    int x = Integer.parseInt(coords[0].trim());
                    int y = Integer.parseInt(coords[1].trim());
                    int z = Integer.parseInt(coords[2].trim());
                    path.add(new Tile(x, y, z));
                } catch (NumberFormatException e) {
                    showDialog("Error", "Format error: Non-integer coordinate found.", Alert.AlertType.ERROR);
                    return null; // Invalid format
                }
            }
            return path;
        } catch (Exception e) {
            e.printStackTrace();
            return null; // Handle unexpected errors
        }
    }

    private Area parseArea(String content) {
        try {
            // Trim and clean content
            content = content.trim();

            // Handle optional "Area <name> =" or "new Area"
            if (content.startsWith("Area")) {
                int equalsIndex = content.indexOf("=");
                if (equalsIndex != -1) {
                    content = content.substring(equalsIndex + 1).trim();
                }
            }

            if (content.startsWith("new Area")) {
                content = content.substring("new Area".length()).trim();
            }

            // Handle cases with or without a trailing semicolon
            if (content.endsWith(";")) {
                content = content.substring(0, content.length() - 1).trim();
            }

            // Ensure content starts and ends with parentheses
            if (!content.startsWith("(") || !content.endsWith(")")) {
                showDialog("Error", "Format error: Missing parentheses.", Alert.AlertType.ERROR);
                return null; // Invalid format
            }

            // Remove the outermost parentheses
            content = content.substring(1, content.length() - 1).trim();

            // Split by "new Tile("
            String[] tileStrings = content.split("new Tile\\(");
            if (tileStrings.length != 3) { // Expect two tiles + leading split
                showDialog("Error", "Format error: Area must have exactly two tiles.", Alert.AlertType.ERROR);
                return null; // Invalid format
            }

            // Parse the top-left tile
            Tile topLeft = parseTileString(tileStrings[1]);
            if (topLeft == null) {
                showDialog("Error", "Format error: Invalid top-left tile.", Alert.AlertType.ERROR);
                return null;
            }

            // Parse the bottom-right tile
            Tile bottomRight = parseTileString(tileStrings[2]);
            if (bottomRight == null) {
                showDialog("Error", "Format error: Invalid bottom-right tile.", Alert.AlertType.ERROR);
                return null;
            }

            // Return the parsed Area
            return new Area(topLeft, bottomRight);
        } catch (Exception e) {
            e.printStackTrace();
            showDialog("Error", "Unexpected error while parsing area.", Alert.AlertType.ERROR);
            return null;
        }
    }

    private Tile parseTileString(String tileString) {
        try {
            // Ensure the tile ends with ")"
            int endIndex = tileString.indexOf(")");
            if (endIndex == -1) {
                return null; // Invalid format
            }

            // Extract coordinates
            String[] coords = tileString.substring(0, endIndex).trim().split(",");
            if (coords.length != 3) {
                return null; // Invalid format
            }

            // Parse and return the Tile
            return new Tile(
                    Integer.parseInt(coords[0].trim()),
                    Integer.parseInt(coords[1].trim()),
                    Integer.parseInt(coords[2].trim())
            );
        } catch (Exception e) {
            e.printStackTrace();
            return null; // Handle unexpected errors
        }
    }

    private void drawPath(List<Tile> path) {
        clearDrawnGraphics();
        PixelWriter pixelWriter = drawingBuffer.getPixelWriter();

        if (path == null || path.isEmpty()) {
            System.out.println("Path is null or empty. Nothing to draw.");
            return;
        }

        // List to hold all local coordinates
        List<ChunkCoordinates> localCoordinatesList = new ArrayList<>();

        // Convert all Tiles to ChunkCoordinates
        for (Tile tile : path) {
            ChunkCoordinates chunkCoordinates = MapChunkHandler.getLocalCoordinatesFromWorld(
                    EmulatorView.getSelectedEmulator(),
                    tile.x(),
                    tile.y(),
                    tile.z()
            );
            localCoordinatesList.add(chunkCoordinates);

            // Draw the point at local coordinates
            drawPoint(pixelWriter, chunkCoordinates.x, chunkCoordinates.y);
        }

        // Draw lines between consecutive local coordinates
        for (int i = 0; i < localCoordinatesList.size() - 1; i++) {
            ChunkCoordinates p1 = localCoordinatesList.get(i);
            ChunkCoordinates p2 = localCoordinatesList.get(i + 1);

            Point point1 = new Point(p1.x, p1.y);
            Point point2 = new Point(p2.x, p2.y);

            drawLine(pixelWriter, point1, point2);
        }

        System.out.println("Path drawn successfully with " + localCoordinatesList.size() + " points.");
    }

    private void drawArea(Area area) {
        if (drawingBuffer == null) {
            return;
        }

        PixelWriter pixelWriter = drawingBuffer.getPixelWriter();
        drawRectangle(pixelWriter, area.getTopTile(), area.getBottomTile());
    }

    private void setupLabelBoxListener() {
        areaAndPathBox.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.trim().isEmpty()) {
                parseAndRenderLabelBoxContent(newValue.trim());
            } else {
                clearDrawnGraphics(); // Clear the map if the labelBox is cleared
            }
        });

        importBox.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.trim().isEmpty()) {
                parseAndRenderLabelBoxContent(newValue.trim());
            } else {
                clearDrawnGraphics();
            }
        });
    }

    private void setupCustomStyles() {
        // Configure labelBox
        areaAndPathBox.setEditable(true);
        areaAndPathBox.setWrapText(true);
        areaAndPathBox.setMaxWidth(200);
        areaAndPathBox.setMaxHeight(105);
        areaAndPathBox.setStyle("-fx-control-inner-background: black; -fx-text-fill: white; -fx-border-radius: 5; -fx-background-radius: 5;");
        areaAndPathBox.setVisible(false); // Initially hidden

        // Configure labelBox
        importBox.setEditable(true);
        importBox.setWrapText(true);
        importBox.setMaxWidth(200);
        importBox.setMaxHeight(105);
        importBox.setStyle("-fx-control-inner-background: black; -fx-text-fill: white; -fx-border-radius: 5; -fx-background-radius: 5;");
        importBox.setVisible(false); // Initially hidden

        // Coordinate box
        coordinateBox.setEditable(false);
        coordinateBox.setMinWidth(125);
        coordinateBox.setStyle("-fx-control-inner-background: black; -fx-text-fill: white; -fx-border-radius: 5; -fx-background-radius: 5;");
        matchConfidenceBox.setEditable(false);
        matchConfidenceBox.setMaxWidth(45);
        matchConfidenceBox.setStyle("-fx-control-inner-background: black; -fx-text-fill: white; -fx-border-radius: 5; -fx-background-radius: 5;");
        timeSpentBox.setEditable(false);
        timeSpentBox.setMaxWidth(60);
        timeSpentBox.setStyle("-fx-control-inner-background: black; -fx-text-fill: white; -fx-border-radius: 5; -fx-background-radius: 5;");

        // Update the labelBox visibility
        areaGenerator.selectedProperty().addListener((observable, oldValue, newValue) -> updateLabelBoxVisibility());
        pathGenerator.selectedProperty().addListener((observable, oldValue, newValue) -> updateLabelBoxVisibility());
        importGenerator.selectedProperty().addListener((observable, oldValue, newValue) -> updateLabelBoxVisibility());
    }

    private void updateLabelBoxVisibility() {
        // Set labelBox visible if area/path ToggleButtons is selected
        areaAndPathBox.setVisible(areaGenerator.isSelected() || pathGenerator.isSelected() && !importGenerator.isSelected());
        areaAndPathBox.setManaged(areaGenerator.isSelected() || pathGenerator.isSelected() && !importGenerator.isSelected());

        // Set the importBox visible if importGenerator togglebutton is selected
        importBox.setVisible(!areaGenerator.isSelected() && !pathGenerator.isSelected() && importGenerator.isSelected());
        importBox.setManaged(!areaGenerator.isSelected() && !pathGenerator.isSelected() && importGenerator.isSelected());
    }

    public void display() {
        initializeUI();

        Stage popupStage = new Stage();
        popupStage.initModality(Modality.NONE);
        popupStage.setTitle("Mufasa Mapping UI");
        popupStage.setResizable(true);
        popupStage.getIcons().add(MUFASA_LOGO_ANIMATED);
        Scene scene = new Scene(mapView);
        scene.getStylesheets().add(STYLESHEET);
        popupStage.setScene(scene);

        popupStage.show();
    }

    // HELPER METHODS
    private void processClick(MouseEvent event) {
        // Convert the click coordinates from scene space to the ImageView's local space
        double viewportX = worldMapScrollPane.getHvalue() * (worldMapView.getImage().getWidth() - worldMapScrollPane.getViewportBounds().getWidth());
        double viewportY = worldMapScrollPane.getVvalue() * (worldMapView.getImage().getHeight() - worldMapScrollPane.getViewportBounds().getHeight());
        Point2D localCoords = worldMapView.parentToLocal(new Point2D(event.getX() + viewportX, event.getY() + viewportY));

        // Convert to tile coordinates
        lastClickedPoint = new Point((int) localCoords.getX(), (int) localCoords.getY());

        if (pathGenerator.isSelected()) {
            handlePointPathGeneration(lastClickedPoint.x, lastClickedPoint.y);
        } else if (areaGenerator.isSelected()) {
            handleAreaMapping(lastClickedPoint.x, lastClickedPoint.y);
        } else {
            clearDrawnGraphics();
            handleSinglePointSelection(lastClickedPoint.x, lastClickedPoint.y);
        }
    }

    private void clearDrawnGraphics() {
        if (drawingBuffer == null) {
            return;
        }

        // Get the pixel writer for the WritableImage
        PixelWriter pixelWriter = drawingBuffer.getPixelWriter();

        // Create a transparent color to clear the image
        Color transparent = new Color(0, 0, 0, 0);

        // Clear the entire WritableImage by setting all pixels to transparent
        for (int y = 0; y < drawingBuffer.getHeight(); y++) {
            for (int x = 0; x < drawingBuffer.getWidth(); x++) {
                pixelWriter.setColor(x, y, transparent);
            }
        }

        // Set the modified WritableImage back to the ImageView
        drawingView.setImage(drawingBuffer);
    }

    private void clearClickedPoints() {
        firstClickPoint = null;
        secondClickPoint = null;
    }

    private void setImage(MapChunk mapChunk) {
        // Stitch the map and get a BufferedImage
        BufferedImage bufferedMap = mapChunkHandler.stitchMap(EmulatorView.getSelectedEmulator(), mapChunk, false, true);

        // Convert BufferedImage to JavaFX Image
        Image fxImage = SwingFXUtils.toFXImage(bufferedMap, null);

        // Set the JavaFX Image to the ImageView
        worldMapView.setImage(fxImage);

        // Initialize the drawing buffer to match the dimensions of the new image
        initializeDrawingBuffer((int) fxImage.getWidth(), (int) fxImage.getHeight());
        drawingView.setImage(drawingBuffer);

        // Update the world map dimensions
        worldmapWidth = (int) fxImage.getWidth();
        worldmapHeight = (int) fxImage.getHeight();

        // Set the walker up for this as well.
        walker.setup(EmulatorView.getSelectedEmulator(), parseChunksTF(chunksToLoad, planesToLoad));

        // Save the BufferedImage to the Downloads folder
//        try {
//            File downloadsDir = new File(System.getProperty("user.home"), "Downloads");
//            File outputFile = new File(downloadsDir, "stitched_map.png");
//            ImageIO.write(bufferedMap, "png", outputFile);
//            System.out.println("Image saved to: " + outputFile.getAbsolutePath());
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    private void initializeDrawingBuffer(int width, int height) {
        drawingBuffer = new WritableImage(width, height);
        PixelWriter pixelWriter = drawingBuffer.getPixelWriter();
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                pixelWriter.setColor(x, y, Color.TRANSPARENT);
            }
        }
    }
}