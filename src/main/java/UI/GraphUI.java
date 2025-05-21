package UI;

import helpers.grapher.Graph;
import helpers.grapher.GraphGenerator;
import helpers.grapher.utils.GraphEdge;
import helpers.grapher.utils.GraphNode;
import helpers.grapher.utils.dataclasses.GraphData;
import com.google.gson.Gson;
import helpers.Logger;
import helpers.ThreadManager;
import helpers.utils.MapChunk;
import helpers.utils.Tile;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import osr.walker.Walker;
import osr.walker.utils.ChunkCoordinates;
import osr.walker.utils.MapChunkHandler;
import utils.SystemUtils;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;

import static UI.components.EmulatorView.getSelectedEmulator;
import static UI.components.MapUtils.parseChunksTF;
import static helpers.grapher.GraphHelpers.convertToGraph;
import static helpers.grapher.GraphHelpers.convertToGraphData;
import static osr.walker.utils.MapChunkHandler.getWorldCoordinateWithPlane;
import static osr.walker.utils.MapChunkHandler.getLocalCoordinatesFromWorld;
import static utils.Constants.*;

public class GraphUI {
    private final Logger logger;
    private final MapChunkHandler mapChunkHandler;
    private final Walker walker;

    ScheduledExecutorService scheduler = ThreadManager.getInstance().getScheduler();

    // Map stuff
    private final ImageView worldMapView = new ImageView();
    private final ImageView drawingView = new ImageView();
    private double initialX, initialY;
    private final double DRAG_THRESHOLD = 5.0; // Adjust the threshold as needed
    private boolean isDragging = false;
    private long pressTime; // To store the time when the mouse was pressed
    // Node stuff
    private final Group graphGroup = new Group();
    private final double closeNodeThreshold = 8.0;
    // Node settings
    private final String MODE_ALL_NEAREST_NODES = "All Nearest nodes";
    private final String MODE_LAST_NODE = "Last node";
    private final String MODE_NEAREST_NODE = "Nearest node";
    private final ComboBox<String> autoConnectModeDropdown = new ComboBox<>();
    // Image changer
    ToggleButton autoConnectButton = new ToggleButton("Enable auto connect");
    // Tiling stuff
    int worldmapWidth;
    int worldmapHeight;
    // FX stuff
    private StackPane mapView;
    private ScrollPane worldMapScrollPane;
    private WritableImage drawingImage;
    private boolean isCollisionMapDisplayed = false;
    private Graph graph = new Graph();
    private GraphNode selectedNode = null;
    private GraphNode lastAddedNode = null;

    private final Button loadGraphs = new Button("Load Graph");
    private final Button showCollisionMap = new Button("Show Collision map");
    private final Button generateNodesButton = new Button("Generate Nodes");
    private final TextField coordinateBox = new TextField();
    private final TextField chunksToLoad = new TextField();
    private final TextField planesToLoad = new TextField();

    public GraphUI(Logger logger, MapChunkHandler mapChunkHandler, Walker walker) {
        this.logger = logger;
        this.mapChunkHandler = mapChunkHandler;
        this.walker = walker;
    }

    private void initializeUI() {
        setImage(new MapChunk(new String[]{"50-50"}, "0"), false, false);

        // Setup the map view
        setupMapView();

        // Create Save and Load buttons
        Button saveButton = new Button("Save Graph");
        saveButton.setOnAction(event -> saveGraph());

        loadGraphs.setOnAction(event -> loadGraph());

        // Chunk stuff!
        chunksToLoad.setPromptText("Chunks");
        planesToLoad.setPromptText("Planes");
        Button loadChunks = new Button("Load chunks");
        loadChunks.setOnAction(event -> loadChunksToMap());

        autoConnectButton.setOnAction(event -> {
            if (autoConnectButton.isSelected()) {
                autoConnectButton.setText("Disable auto connect");
            } else {
                autoConnectButton.setText("Enable auto connect");
            }
        });

        generateNodesButton.setOnAction(event -> generateNodes());
        showCollisionMap.setOnAction(event -> showCollisionMap());

        // Configure auto connect mode dropdown
        autoConnectModeDropdown.setItems(FXCollections.observableArrayList(MODE_ALL_NEAREST_NODES, MODE_LAST_NODE, MODE_NEAREST_NODE));
        autoConnectModeDropdown.getSelectionModel().select(MODE_ALL_NEAREST_NODES);

        // Visualize path generation
        Button pathGenerator = new Button("Visualize Path");
        pathGenerator.setOnAction(event -> visualizePath());
        coordinateBox.setPromptText("X,Y,Z end tile");
        coordinateBox.setMaxWidth(100);

        // Add buttons to an HBox
        HBox buttonBox = new HBox(10); // HBox with spacing of 10
        buttonBox.getChildren().addAll(chunksToLoad, planesToLoad, loadChunks, saveButton, loadGraphs);
        buttonBox.setAlignment(Pos.CENTER_RIGHT); // Ensure contents are right-aligned

        HBox graphControls = new HBox(10);
        graphControls.getChildren().addAll(autoConnectModeDropdown, autoConnectButton, showCollisionMap, generateNodesButton, coordinateBox, pathGenerator);
        graphControls.setAlignment(Pos.CENTER_LEFT); // Ensure contents are left-aligned

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS); // Allow spacer to grow and push content to the edges

        HBox buttonContainer = new HBox(10);
        buttonContainer.getChildren().addAll(graphControls, spacer, buttonBox);

        // Create a VBox to stack buttonBox and worldMapScrollPane
        VBox mainLayout = new VBox();
        mainLayout.getChildren().addAll(buttonContainer, worldMapScrollPane);
        VBox.setVgrow(worldMapScrollPane, Priority.ALWAYS); // Make scroll pane take available space

        // Add nodes and edges handling
        setupNodeInteraction();

        // Set main layout as the root of the scene
        mapView = new StackPane();
        mapView.getChildren().add(mainLayout);
    }

    private void visualizePath() {
        // Step 1: Extract and parse the coordinates from the coordinateBox
        String input = coordinateBox.getText().trim();
        if (input.isEmpty()) {
            showDialog("Input Error", "Please enter coordinates in the format X,Y,Z.", Alert.AlertType.INFORMATION);
            return;
        }

        String[] parts = input.split("\\s*,\\s*");
        if (parts.length != 3) {
            showDialog("Input Error", "Invalid format. Please enter coordinates as X,Y,Z.", Alert.AlertType.INFORMATION);
            return;
        }

        int targetX, targetY, targetZ;
        try {
            targetX = Integer.parseInt(parts[0]);
            targetY = Integer.parseInt(parts[1]);
            targetZ = Integer.parseInt(parts[2]);
        } catch (NumberFormatException e) {
            showDialog("Input Error", "Coordinates must be integers. Please check your input.", Alert.AlertType.INFORMATION);
            return;
        }

        Tile targetTile = new Tile(targetX, targetY, targetZ);

        // Proceed to build the tile path
        buildAndVisualizePath(targetTile);
    }

    private void buildAndVisualizePath(Tile targetTile) {
        CompletableFuture.runAsync(() -> {
            try {
                // Step 2: Build the tile path synchronously
                Tile[] tilePath = walker.buildTilePath(getSelectedEmulator(), targetTile, true);

                // Step 3: Convert the array to a mutable list
                List<Tile> tilePathList = new ArrayList<>(Arrays.asList(tilePath));

                // Step 4: Add the end destination tile to ensure it's included
                tilePathList.add(targetTile);

                // Step 5: Transform tiles to local coordinates
                List<Point2D> localPath = new ArrayList<>();
                for (Tile tile : tilePathList) {
                    ChunkCoordinates localCoords = getLocalCoordinatesFromWorld(
                            getSelectedEmulator(),
                            tile.x(),
                            tile.y(),
                            tile.z()
                    );
                    if (localCoords != null) {
                        localPath.add(new Point2D(localCoords.x, localCoords.y));
                    } else {
                        logger.print("Invalid local coordinates for tile: " + tile);
                    }
                }

                // Step 6: Draw the path on the map on the JavaFX Application Thread
                Platform.runLater(() -> drawPath(localPath));

            } catch (Exception e) {
                Platform.runLater(() -> {
                    showDialog("Path Visualization Error", "An error occurred while building the tile path: " + e.getMessage(), Alert.AlertType.INFORMATION);
                    logger.print("Error in visualizePath: " + e.getMessage());
                });
            }
        }, scheduler); // Provide your custom executor here
    }

    /**
     * Draws a path on the drawingView by connecting a list of local coordinates and marking each point with a black circle.
     *
     * @param localPath List of points representing the path in local coordinates.
     */
    private void drawPath(List<Point2D> localPath) {
        if (localPath.isEmpty()) {
            logger.print("No path to draw.");
            return;
        }

        // Clear previous paths and drawings
        clearDrawingBuffer();

        // Draw lines between consecutive points
        for (int i = 0; i < localPath.size() - 1; i++) {
            Point2D start = localPath.get(i);
            Point2D end = localPath.get(i + 1);
            drawLineOnImage((int) start.getX(), (int) start.getY(), (int) end.getX(), (int) end.getY());
        }

        // Draw black circles on each point
        for (Point2D point : localPath) {
            drawCircleOnImage((int) point.getX(), (int) point.getY(), 3, Color.BLACK);
        }

        // Optionally, highlight start and end points with distinct colors
        highlightStartAndEndPointsOnImage(localPath);

        // Update the drawing view to reflect changes
        drawingView.setImage(drawingImage);

        logger.print("Path visualized with " + localPath.size() + " points.");
    }

    /**
     * Clears the drawing buffer by setting all pixels to transparent.
     */
    private void clearDrawingBuffer() {
        PixelWriter pixelWriter = drawingImage.getPixelWriter();
        for (int x = 0; x < drawingImage.getWidth(); x++) {
            for (int y = 0; y < drawingImage.getHeight(); y++) {
                pixelWriter.setColor(x, y, Color.TRANSPARENT);
            }
        }
        logger.print("Drawing buffer cleared.");
    }

    /**
     * Highlights the start and end points on the WritableImage.
     *
     * @param localPath The list of local coordinates representing the path.
     */
    private void highlightStartAndEndPointsOnImage(List<Point2D> localPath) {
        if (localPath.isEmpty()) return;

        Point2D startPoint = localPath.get(0);
        Point2D endPoint = localPath.get(localPath.size() - 1);

        drawCircleOnImage((int) startPoint.getX(), (int) startPoint.getY(), 5, Color.GREEN);
        drawCircleOnImage((int) endPoint.getX(), (int) endPoint.getY(), 5, Color.RED);
    }

    /**
     * Draws a filled circle on the WritableImage.
     *
     * @param centerX X-coordinate of the circle's center.
     * @param centerY Y-coordinate of the circle's center.
     * @param radius Radius of the circle.
     * @param color  Color of the circle.
     */
    private void drawCircleOnImage(int centerX, int centerY, int radius, Color color) {
        PixelWriter pixelWriter = drawingImage.getPixelWriter();
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int y = centerY - radius; y <= centerY + radius; y++) {
                if (x >= 0 && x < drawingImage.getWidth() && y >= 0 && y < drawingImage.getHeight()) {
                    int dx = x - centerX;
                    int dy = y - centerY;
                    if (dx * dx + dy * dy <= radius * radius) {
                        pixelWriter.setColor(x, y, color);
                    }
                }
            }
        }
    }

    /**
     * Draws a line between two points on the WritableImage.
     *
     * @param x0 Starting x-coordinate.
     * @param y0 Starting y-coordinate.
     * @param x1 Ending x-coordinate.
     * @param y1 Ending y-coordinate.
     */
    private void drawLineOnImage(int x0, int y0, int x1, int y1) {
        PixelWriter pixelWriter = drawingImage.getPixelWriter();

        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;

        while (true) {
            if (x0 >= 0 && x0 < drawingImage.getWidth() && y0 >= 0 && y0 < drawingImage.getHeight()) {
                pixelWriter.setColor(x0, y0, Color.BLUE);
            }

            if (x0 == x1 && y0 == y1) break;
            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x0 += sx;
            }
            if (e2 < dx) {
                err += dx;
                y0 += sy;
            }
        }
    }

    private void generateNodes() {
        // Disable the button and change its text
        generateNodesButton.setText("Generating....");
        generateNodesButton.setDisable(true);

        // Start generating the graph asynchronously
        BufferedImage image = mapChunkHandler.stitchMap("none", parseChunksTF(chunksToLoad, planesToLoad), true, true);
        GraphGenerator graphGenerator = new GraphGenerator(logger, image.getWidth(), image.getHeight());
        CompletableFuture<Graph> futureGraph = graphGenerator.generateGraphAsync(image, "none");

        // Use thenAccept to process the graph once it is ready
        futureGraph.thenAccept(generatedGraph -> {
            // This code will be run in the background thread, so we need to move UI updates to the JavaFX thread
            logger.print("Processing each node into world coordinates..");
            Platform.runLater(() -> {
                // Convert all generated nodes to world coordinates
                for (GraphNode generatedNode : generatedGraph.getNodes()) {
                    // Convert local coordinates to world coordinates
                    ChunkCoordinates worldCoords = getWorldCoordinateWithPlane(getSelectedEmulator(),
                            generatedNode.getNodeX(), generatedNode.getNodeY());

                    // Update the node with the world coordinates
                    generatedNode.setCoordinates(worldCoords.x, worldCoords.y, worldCoords.z);
                }
                logger.print("Done converting nodes..");

                graph = generatedGraph;

                // Save the generated graph before displaying it
                saveGeneratedGraph();

                // Clear the current graph visualization
                graphGroup.getChildren().clear();

                // Incrementally draw the nodes and edges on the map, and update the button after drawing is complete
                drawNodesAndEdgesIncrementally(() -> {
                    // Re-enable the button and reset its text after drawing is finished
                    generateNodesButton.setText("Generate Nodes");
                    generateNodesButton.setDisable(false);
                });
            });
        }).exceptionally(ex -> {
            // Handle any exceptions that occurred during graph generation
            Platform.runLater(() -> {
                logger.print("Error generating graph: " + ex.getMessage());

                // Re-enable the button and reset its text
                generateNodesButton.setText("Generate Nodes");
                generateNodesButton.setDisable(false);
            });
            return null;
        });
    }

    private void saveGeneratedGraph() {
        String appPath = SystemUtils.getSystemPath() + "/graphs"; // Customize as needed
        File directory = new File(appPath);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        String fileName = "generated_graph.graph";
        File file = new File(directory, fileName);
        saveGraph(file);
    }

    private void loadChunksToMap() {
        MapChunk mapChunk = parseChunksTF(chunksToLoad, planesToLoad);
        walker.setup(getSelectedEmulator(), mapChunk);
        setImage(mapChunk, false, false);
    }

    private void showCollisionMap() {
        MapChunk mapChunk = parseChunksTF(chunksToLoad, planesToLoad);
        if (isCollisionMapDisplayed) {
            // Switch to regular map
            showCollisionMap.setText("Show collision map");
            setImage(mapChunk, true, false);
        } else {
            // Switch to collision map
            showCollisionMap.setText("Hide collision map");
            setImage(mapChunk, true, true);
        }
        isCollisionMapDisplayed = !isCollisionMapDisplayed;
    }

    private void setupNodeInteraction() {
        worldMapScrollPane.setOnMousePressed(event -> {
            initialX = event.getScreenX();
            initialY = event.getScreenY();
            isDragging = false; // Reset dragging flag
            pressTime = System.currentTimeMillis(); // Capture the time when the mouse is pressed
        });

        worldMapScrollPane.setOnMouseDragged(event -> {
            double deltaX = Math.abs(event.getScreenX() - initialX);
            double deltaY = Math.abs(event.getScreenY() - initialY);

            // Check if the movement exceeds the drag threshold
            if (deltaX > DRAG_THRESHOLD || deltaY > DRAG_THRESHOLD) {
                isDragging = true; // Mark as dragging if movement exceeds threshold
            }
        });

        worldMapScrollPane.setOnMouseReleased(event -> {
            long releaseTime = System.currentTimeMillis();
            long clickDuration = releaseTime - pressTime;

            // If dragging or the click is held too long, do nothing
            if (!isDragging && clickDuration < 200) { // 200 ms can be adjusted as needed
                handleMouseClick(event);
            }
        });
    }

    private void handleMouseClick(MouseEvent event) {
        // Get the actual click position relative to the entire map
        double viewportX = worldMapScrollPane.getHvalue() * (worldMapView.getImage().getWidth() - worldMapScrollPane.getViewportBounds().getWidth());
        double viewportY = worldMapScrollPane.getVvalue() * (worldMapView.getImage().getHeight() - worldMapScrollPane.getViewportBounds().getHeight());

        // Convert screen coordinates to local coordinates in the map's image
        Point2D localCoords = worldMapView.parentToLocal(new Point2D(event.getX() + viewportX, event.getY() + viewportY));
        int localX = (int) localCoords.getX();
        int localY = (int) localCoords.getY();

        // Convert local coordinates to global/world coordinates
        ChunkCoordinates globalCoordsWithPlane = getWorldCoordinateWithPlane(getSelectedEmulator(), localX, localY);

        if (event.getButton() == MouseButton.SECONDARY) {
            addNode(globalCoordsWithPlane.x, globalCoordsWithPlane.y, globalCoordsWithPlane.z);
        }

        if (event.getButton() == MouseButton.PRIMARY) {
            GraphNode nearestNode = findNearestNode(globalCoordsWithPlane.x, globalCoordsWithPlane.y, globalCoordsWithPlane.z); // Use findNearestNode with threshold

            if (nearestNode != null) {
                logger.print("Found nearest node and selected it");
                handleNodeSelection(nearestNode);
            }
        }
    }

    private void addNode(int globalX, int globalY, int plane) {
        // Find the nearest node within the threshold
        GraphNode nearestNode = findNearestNode(globalX, globalY, plane);

        if (nearestNode != null) {
            // If a node is within the threshold, remove it
            logger.print("Node already exists at (" + nearestNode.getNodeX() + ", " + nearestNode.getNodeY() + ", " + plane + "), removing it.");
            removeNode(nearestNode);
            return;
        }

        // Proceed to add a new node if no existing node was found within the threshold
        String nodeId = graph.getNextAvailableNodeId();
        GraphNode node = new GraphNode(nodeId, globalX, globalY, plane);
        graph.addNode(node);

        // Convert global coordinates to local for drawing
        ChunkCoordinates chunkCoordinates = getLocalCoordinatesFromWorld(getSelectedEmulator(), globalX, globalY, plane);

        // Draw the node on the map using local coordinates
        Rectangle2D dirtyArea = calculateNodeBoundingBox(chunkCoordinates.x, chunkCoordinates.y);
        if (chunkCoordinates.z != -1) {
            drawNodeOnImage(chunkCoordinates.x, chunkCoordinates.y, dirtyArea);
        }

        if (!graphGroup.getChildren().contains(node)) {
            graphGroup.getChildren().add(node);
        }

        // Handle auto-connect functionality as before
        if (autoConnectButton.isSelected()) {
            String selectedMode = autoConnectModeDropdown.getSelectionModel().getSelectedItem();
            if (MODE_ALL_NEAREST_NODES.equals(selectedMode)) {
                connectToAllNearestNodes(node);
            } else if (MODE_LAST_NODE.equals(selectedMode) && lastAddedNode != null) {
                connectToLastNode(node);
            } else if (MODE_NEAREST_NODE.equals(selectedMode)) {
                connectToNearestNode(node);
            }
        }

        lastAddedNode = node;
        drawingView.setImage(drawingImage);
        logger.print("Node added: " + nodeId + " at (" + globalX + ", " + globalY + ", " + plane + ")");
    }

    private void removeNode(GraphNode node) {
        // Convert global coordinates to local for clearing
        ChunkCoordinates chunkCoordinates = getLocalCoordinatesFromWorld(getSelectedEmulator(), node.getNodeX(), node.getNodeY(), node.getNodeZ());

        // Remove edges connected to the node
        removeEdgesConnectedToNode(node);

        // Remove the node from the graph
        graph.removeNode(node);

        // Remove the node from the display
        graphGroup.getChildren().remove(node);

        // Clear the node from the drawing buffer
        Rectangle2D dirtyArea = calculateNodeBoundingBox(chunkCoordinates.x, chunkCoordinates.y);
        refreshDrawingBuffer(dirtyArea);

        logger.print("Node removed: " + node.getNodeId() + " at (" + node.getNodeX() + ", " + node.getNodeY() + ", " + node.getNodeZ() + ")");
    }

    private void removeEdgesConnectedToNode(GraphNode node) {
        List<GraphEdge> edgesToRemove = new ArrayList<>();
        logger.print("Checking edges connected to node: " + node.getNodeId());
        for (GraphEdge edge : graph.getEdges()) {
            if (edge.getStartNode().equals(node) || edge.getEndNode().equals(node)) {
                edgesToRemove.add(edge);
                logger.print("Edge marked for removal: " + edge.getStartNode().getNodeId() + " - " + edge.getEndNode().getNodeId());
            }
        }

        for (GraphEdge edge : edgesToRemove) {
            logger.print("Removing edge from: " + edge.getStartNode().getNodeId() + " and " + edge.getEndNode().getNodeId());
            graph.removeEdge(edge);
            graphGroup.getChildren().remove(edge);
        }

        if (edgesToRemove.isEmpty()) {
            logger.print("No edges found connected to node: " + node.getNodeId());
        }
    }

    private void connectToAllNearestNodes(GraphNode node) {
        double thresholdDistance = 50;

        for (GraphNode otherNode : graph.getNodes()) {
            if (otherNode != node && otherNode.getNodeZ() == node.getNodeZ()) { // Ensure the same plane
                // Calculate the distance between the nodes
                double distance = node.distance(otherNode);
                if (distance < thresholdDistance && findEdge(otherNode, node) == null) {
                    GraphEdge edge = new GraphEdge(otherNode, node);
                    graph.addEdge(edge);
                    if (!graphGroup.getChildren().contains(edge)) {
                        graphGroup.getChildren().add(edge);
                    }

                    // Convert global coordinates to local coordinates before drawing the edge
                    ChunkCoordinates startChunkCoordinates = getLocalCoordinatesFromWorld(getSelectedEmulator(), node.getNodeX(), node.getNodeY(), node.getNodeZ());
                    ChunkCoordinates endChunkCoordinates = getLocalCoordinatesFromWorld(getSelectedEmulator(), otherNode.getNodeX(), otherNode.getNodeY(), otherNode.getNodeZ());

                    // Draw the edge using local coordinates
                    Rectangle2D edgeBox = calculateEdgeBoundingBox(startChunkCoordinates.x, startChunkCoordinates.y, endChunkCoordinates.x, endChunkCoordinates.y);
                    drawEdgeOnImage(startChunkCoordinates.x, startChunkCoordinates.y, endChunkCoordinates.x, endChunkCoordinates.y, edgeBox);
                }
            }
        }
    }

    private void connectToLastNode(GraphNode node) {
        if (lastAddedNode != null && lastAddedNode.getNodeZ() == node.getNodeZ() && findEdge(lastAddedNode, node) == null) {
            GraphEdge edge = new GraphEdge(lastAddedNode, node);
            graph.addEdge(edge);
            graphGroup.getChildren().add(edge);

            // Convert global coordinates to local coordinates before drawing the edge
            ChunkCoordinates startChunkCoordinates = getLocalCoordinatesFromWorld(getSelectedEmulator(), lastAddedNode.getNodeX(), lastAddedNode.getNodeY(), lastAddedNode.getNodeZ());
            ChunkCoordinates endChunkCoordinates = getLocalCoordinatesFromWorld(getSelectedEmulator(), node.getNodeX(), node.getNodeY(), node.getNodeZ());

            // Draw the edge using local coordinates
            Rectangle2D edgeBox = calculateEdgeBoundingBox(startChunkCoordinates.x, startChunkCoordinates.y, endChunkCoordinates.x, endChunkCoordinates.y);
            drawEdgeOnImage(startChunkCoordinates.x, startChunkCoordinates.y, endChunkCoordinates.x, endChunkCoordinates.y, edgeBox);

            logger.print("Edge created between: " + lastAddedNode.getNodeId() + " and " + node.getNodeId());
        }
    }

    private void connectToNearestNode(GraphNode node) {
        // Find the nearest node in global coordinates on the same plane
        GraphNode nearestNode = findNearestNode(node.getNodeX(), node.getNodeY(), node.getNodeZ());

        if (nearestNode != null && nearestNode != node && findEdge(nearestNode, node) == null) {
            // Create and add the edge
            GraphEdge edge = new GraphEdge(nearestNode, node);
            graph.addEdge(edge);
            graphGroup.getChildren().add(edge);

            // Convert global coordinates to local coordinates before drawing the edge
            ChunkCoordinates startChunkCoordinates = getLocalCoordinatesFromWorld(getSelectedEmulator(), nearestNode.getNodeX(), nearestNode.getNodeY(), nearestNode.getNodeZ());
            ChunkCoordinates endChunkCoordinates = getLocalCoordinatesFromWorld(getSelectedEmulator(), node.getNodeX(), node.getNodeY(), node.getNodeZ());

            // Draw the edge using local coordinates
            Rectangle2D edgeBox = calculateEdgeBoundingBox(startChunkCoordinates.x, startChunkCoordinates.y, endChunkCoordinates.x, endChunkCoordinates.y);
            drawEdgeOnImage(startChunkCoordinates.x, startChunkCoordinates.y, endChunkCoordinates.x, endChunkCoordinates.y, edgeBox);

            logger.print("Edge created between: " + nearestNode.getNodeId() + " and " + node.getNodeId());
        }
    }

    private void handleNodeSelection(GraphNode node) {
        if (selectedNode == null) {
            // No node is currently selected, select the clicked node
            node.setSelected(true);
            selectedNode = node;
        } else if (selectedNode != node) {
            // Check for existing edge between selectedNode and node
            GraphEdge existingEdge = findEdge(selectedNode, node);
            if (existingEdge != null) {
                // Remove the existing edge
                graph.removeEdge(existingEdge);
                graphGroup.getChildren().remove(existingEdge); // Remove from display
                Rectangle2D edgeBox = calculateEdgeBoundingBox(selectedNode.getNodeX(), selectedNode.getNodeY(), node.getNodeX(), node.getNodeY());
                refreshDrawingBuffer(edgeBox); // Clear the affected area
                logger.print("Edge removed between: " + selectedNode.getNodeId() + " and " + node.getNodeId());
            } else {
                // No edge exists, create a new one
                GraphEdge edge = new GraphEdge(selectedNode, node);
                graph.addEdge(edge);
                graphGroup.getChildren().add(edge); // Add to display
                Rectangle2D edgeBox = calculateEdgeBoundingBox(selectedNode.getNodeX(), selectedNode.getNodeY(), node.getNodeX(), node.getNodeY());
                drawEdgeOnImage(selectedNode.getNodeX(), selectedNode.getNodeY(), node.getNodeX(), node.getNodeY(), edgeBox);
                logger.print("Edge created between: " + selectedNode.getNodeId() + " and " + node.getNodeId());
            }
            // Deselect both nodes
            selectedNode.setSelected(false);
            node.setSelected(false);
            selectedNode = null;
        } else {
            // The same node was clicked twice, deselect it
            node.setSelected(false);
            selectedNode = null;
        }

        // Update the drawing view with the new image
        drawingView.setImage(drawingImage);
    }

    private GraphNode findNearestNode(int globalX, int globalY, int plane) {
        GraphNode nearestNode = null;
        double minDistance = closeNodeThreshold; // Set the minimum distance to the threshold
        GraphNode tempNode = new GraphNode("temp", globalX, globalY, plane);

        for (GraphNode existingNode : graph.getNodes()) {
            if (existingNode.getNodeZ() == plane) {
                double distance = existingNode.distance(tempNode);
                if (distance < minDistance) {
                    nearestNode = existingNode;
                    minDistance = distance;
                }
            }
        }

        return nearestNode;
    }

    private GraphEdge findEdge(GraphNode node1, GraphNode node2) {
        for (GraphEdge edge : graph.getEdges()) {
            if ((edge.getStartNode() == node1 && edge.getEndNode() == node2) ||
                    (edge.getStartNode() == node2 && edge.getEndNode() == node1)) {
                return edge;
            }
        }
        return null; // No edge found
    }

    private void drawNodeOnImage(double localX, double localY, Rectangle2D boundingBox) {
        // Avoid drawing on (0, 0)
        if (localX == 0 && localY == 0) {
            return;
        }

        int radius = 3;
        int radiusSquared = radius * radius;
        PixelWriter pixelWriter = drawingImage.getPixelWriter();

        // Precompute effective bounding box for the circle
        int minX = Math.max((int) boundingBox.getMinX(), (int) (localX - radius));
        int maxX = Math.min((int) boundingBox.getMaxX(), (int) (localX + radius));
        int minY = Math.max((int) boundingBox.getMinY(), (int) (localY - radius));
        int maxY = Math.min((int) boundingBox.getMaxY(), (int) (localY + radius));

        // Loop only within the clipped bounds
        for (int i = minX; i <= maxX; i++) {
            for (int j = minY; j <= maxY; j++) {
                double dx = localX - i;
                double dy = localY - j;
                if (dx * dx + dy * dy <= radiusSquared) {
                    pixelWriter.setColor(i, j, Color.RED);
                }
            }
        }
    }

    private void drawEdgeOnImage(double x1, double y1, double x2, double y2, Rectangle2D boundingBox) {
        PixelWriter pixelWriter = drawingImage.getPixelWriter();
        int width = (int) drawingImage.getWidth();
        int height = (int) drawingImage.getHeight();

        // Precompute bounding box clipped to image bounds
        double minX = Math.max(boundingBox.getMinX(), 0);
        double minY = Math.max(boundingBox.getMinY(), 0);
        double maxX = Math.min(boundingBox.getMaxX(), width - 1);
        double maxY = Math.min(boundingBox.getMaxY(), height - 1);

        // Use Bresenham's line algorithm
        int dx = Math.abs((int) x2 - (int) x1);
        int dy = Math.abs((int) y2 - (int) y1);
        int sx = (int) x1 < (int) x2 ? 1 : -1;
        int sy = (int) y1 < (int) y2 ? 1 : -1;
        int err = dx - dy;

        while (true) {
            // Check if current point is within the bounding box
            if (x1 >= minX && x1 <= maxX && y1 >= minY && y1 <= maxY) {
                pixelWriter.setColor((int) x1, (int) y1, Color.YELLOW);
            }

            if ((int) x1 == (int) x2 && (int) y1 == (int) y2) break;

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

    private void refreshDrawingBuffer(Rectangle2D dirtyArea) {
        PixelWriter pixelWriter = drawingImage.getPixelWriter();

        // Clear the affected area
        for (int x = (int) dirtyArea.getMinX(); x <= (int) dirtyArea.getMaxX(); x++) {
            for (int y = (int) dirtyArea.getMinY(); y <= (int) dirtyArea.getMaxY(); y++) {
                if (x >= 0 && x < drawingImage.getWidth() && y >= 0 && y < drawingImage.getHeight()) {
                    pixelWriter.setColor(x, y, Color.TRANSPARENT);
                }
            }
        }

        // Redraw nodes within the affected area
        for (GraphNode node : graph.getNodes()) {
            // Fetch the chunk coordinates and plane for the current node
            ChunkCoordinates chunkCoordinates = getLocalCoordinatesFromWorld(getSelectedEmulator(), node.getNodeX(), node.getNodeY(), node.getNodeZ());

            Rectangle2D nodeBox = calculateNodeBoundingBox(chunkCoordinates.x, chunkCoordinates.y);

            // Only redraw if the node is within the affected area and on the same plane
            if (dirtyArea.intersects(nodeBox) && chunkCoordinates.z == node.getNodeZ() && chunkCoordinates.z != -1) {
                drawNodeOnImage(chunkCoordinates.x, chunkCoordinates.y, dirtyArea);
            }
        }

        // Redraw edges within the affected area
        for (GraphEdge edge : graph.getEdges()) {
            // Ensure edges are redrawn only if both nodes are on the same plane
            if (edge.getStartNode().getNodeZ() == edge.getEndNode().getNodeZ()) {
                ChunkCoordinates startChunkCoordinates = getLocalCoordinatesFromWorld(getSelectedEmulator(), edge.getStartNode().getNodeX(), edge.getStartNode().getNodeY(), edge.getStartNode().getNodeZ());
                ChunkCoordinates endChunkCoordinates = getLocalCoordinatesFromWorld(getSelectedEmulator(), edge.getEndNode().getNodeX(), edge.getEndNode().getNodeY(), edge.getEndNode().getNodeZ());

                Rectangle2D edgeBox = calculateEdgeBoundingBox(startChunkCoordinates.x, startChunkCoordinates.y, endChunkCoordinates.x, endChunkCoordinates.y);

                if (dirtyArea.intersects(edgeBox)) {
                    drawEdgeOnImage(startChunkCoordinates.x, startChunkCoordinates.y, endChunkCoordinates.x, endChunkCoordinates.y, dirtyArea);
                }
            }
        }

        drawingView.setImage(drawingImage);
    }

    private void setupMapView() {
        // Configure the world map view to fit within a specific height
        worldMapView.setPreserveRatio(true);
        worldMapView.setSmooth(true);
        // Make drawingView's size follow worldMapView's size
        drawingView.fitWidthProperty().bind(worldMapView.fitWidthProperty());
        drawingView.fitHeightProperty().bind(worldMapView.fitHeightProperty());
        drawingView.setPreserveRatio(true);

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
    }

    private void setImage(MapChunk mapChunk, boolean dontRefreshBuffer, boolean getCollision) {
        // Stitch the map and get a BufferedImage
        BufferedImage bufferedMap = mapChunkHandler.stitchMap(getSelectedEmulator(), mapChunk, getCollision, true);

        // Convert BufferedImage to JavaFX Image
        Image fxImage = SwingFXUtils.toFXImage(bufferedMap, null);

        // Set the JavaFX Image to the ImageView
        worldMapView.setImage(fxImage);

        // Initialize the drawing buffer to match the dimensions of the new image
        if (!dontRefreshBuffer) {
            initializeDrawingBuffer((int) fxImage.getWidth(), (int) fxImage.getHeight());
            drawingView.setImage(drawingImage);
        }

        // Update the world map dimensions
        worldmapWidth = (int) fxImage.getWidth();
        worldmapHeight = (int) fxImage.getHeight();

        walker.setup(getSelectedEmulator(), parseChunksTF(chunksToLoad, planesToLoad));
    }

    private void initializeDrawingBuffer(int width, int height) {
        drawingImage = new WritableImage(width, height);
        PixelWriter pixelWriter = drawingImage.getPixelWriter();
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                pixelWriter.setColor(x, y, Color.TRANSPARENT);
            }
        }
        logger.print("Drawing buffer initialized with dimensions: " + width + "x" + height);
    }

    private void saveGraph() {
        String appPath = SystemUtils.getSystemPath() + "/graphs";

        File directory = new File(appPath);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        String fileName = "map.graph";
        File file = new File(directory, fileName);
        saveGraph(file);
    }

    private void saveGraph(File file) {
        try {
            Gson gson = new Gson();
            GraphData graphData = convertToGraphData(graph);
            String json = gson.toJson(graphData);

            Files.write(file.toPath(), json.getBytes());
            logger.print("Graph saved to " + file.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadGraph(File file) {
        try {
            String json = new String(Files.readAllBytes(file.toPath())); // Read file contents
            Gson gson = new Gson();
            GraphData graphData = gson.fromJson(json, GraphData.class); // Parse JSON to GraphData

            // Use GraphHelpers to convert GraphData to Graph
            graph = convertToGraph(graphData);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadGraph() {
        String appPath = SystemUtils.getSystemPath() + "/graphs";
        String fileName = "map.graph";

        loadGraphs.setText("Loading..");
        loadGraphs.setDisable(true);

        // Create the file object for the new file
        File file = new File(appPath, fileName);

        // Check if the file exists and load it asynchronously
        if (file.exists()) {
            CompletableFuture.runAsync(() -> loadGraph(file), scheduler) // Use the custom executor
                    .thenRunAsync(() -> {
                        // Once the graph is loaded, draw nodes and edges incrementally
                        drawNodesAndEdgesIncrementally(() -> {
                            // Re-enable the button and reset its text after drawing is finished
                            loadGraphs.setText("Load Graph");
                            loadGraphs.setDisable(false);
                        });
                    }, scheduler) // Ensure this continuation also uses the custom executor
                    .exceptionally(ex -> {
                        // Handle any exceptions that occurred during graph loading
                        Platform.runLater(() -> logger.print("Error loading graph: " + ex.getMessage()));
                        return null;
                    });
        } else {
            logger.print("No graph file found");
        }
    }

    private void drawNodesAndEdgesIncrementally(Runnable onComplete) {
        Task<Void> drawTask = new Task<>() {
            @Override
            protected Void call() {
                logger.print("Drawing nodes...");

                // Draw the nodes incrementally
                for (GraphNode node : graph.getNodes()) {
                    ChunkCoordinates localCoords = getLocalCoordinatesFromWorld(
                            getSelectedEmulator(), node.getNodeX(), node.getNodeY(), node.getNodeZ());

                    if (localCoords != null && localCoords.x != 0 && localCoords.y != 0) {
                        Platform.runLater(() -> {
                            // Calculate bounding box
                            Rectangle2D nodeBox = calculateNodeBoundingBox(localCoords.x, localCoords.y);
                            if (localCoords.z != -1) {
                                drawNodeOnImage(localCoords.x, localCoords.y, nodeBox);
                            }
                            // Add the node to the graphGroup for visualization
                            graphGroup.getChildren().add(node);
                        });
                    }
                }

                logger.print("Drawing edges...");

                // Draw the edges incrementally
                for (GraphEdge edge : graph.getEdges()) {
                    GraphNode startNode = edge.getStartNode();
                    GraphNode endNode = edge.getEndNode();
                    ChunkCoordinates startLocalCoords = getLocalCoordinatesFromWorld(
                            getSelectedEmulator(), startNode.getNodeX(), startNode.getNodeY(), startNode.getNodeZ());
                    ChunkCoordinates endLocalCoords = getLocalCoordinatesFromWorld(
                            getSelectedEmulator(), endNode.getNodeX(), endNode.getNodeY(), endNode.getNodeZ());

                    if (startLocalCoords != null && endLocalCoords != null &&
                            startLocalCoords.x != 0 && startLocalCoords.y != 0 &&
                            endLocalCoords.x != 0 && endLocalCoords.y != 0) {

                        Platform.runLater(() -> {
                            // Drawing the edge
                            Rectangle2D edgeBox = calculateEdgeBoundingBox(
                                    startLocalCoords.x, startLocalCoords.y,
                                    endLocalCoords.x, endLocalCoords.y);

                            drawEdgeOnImage(startLocalCoords.x, startLocalCoords.y,
                                    endLocalCoords.x, endLocalCoords.y, edgeBox);

                            graphGroup.getChildren().add(edge);
                        });
                    } else {
                        logger.print("Invalid local coordinates for edge between nodes: " + startNode + " and " + endNode);
                    }
                }

                logger.print("Done drawing nodes & edges!");

                // When finished, call the onComplete callback on the JavaFX thread
                Platform.runLater(onComplete);

                return null;
            }
        };

        Thread drawThread = new Thread(drawTask);
        drawThread.setDaemon(true);
        drawThread.start();
    }

    private Rectangle2D calculateNodeBoundingBox(double x, double y) {
        return new Rectangle2D(x - (double) 3, y - (double) 3, (double) 3 * 2, (double) 3 * 2);
    }

    private Rectangle2D calculateEdgeBoundingBox(double x1, double y1, double x2, double y2) {
        double minX = Math.min(x1, x2) - (double) 1;
        double minY = Math.min(y1, y2) - (double) 1;
        double width = Math.abs(x1 - x2) + (double) 1 * 2;
        double height = Math.abs(y1 - y2) + (double) 1 * 2;
        return new Rectangle2D(minX, minY, width, height);
    }

    public void display() {
        initializeUI();
        Stage popupStage = new Stage();
        popupStage.initModality(Modality.NONE);
        popupStage.setTitle("Mufasa Graphing Tool");
        popupStage.setResizable(true);
        popupStage.getIcons().add(MUFASA_LOGO);

        Scene scene = new Scene(mapView);
        scene.getStylesheets().add(STYLESHEET);
        popupStage.setScene(scene);

        popupStage.show();
    }

}