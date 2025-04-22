package helpers.grapher;

import helpers.grapher.utils.GraphEdge;
import helpers.grapher.utils.GraphNode;
import helpers.grapher.utils.dataclasses.Quadtree;
import helpers.Logger;
import helpers.ThreadManager;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Point2D;
import javafx.scene.image.PixelReader;
import javafx.scene.paint.Color;
import osr.walker.utils.MapChunkHandler;

import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static helpers.ThreadManager.AVAILABLE_CORES;

public class GraphGenerator {
    private final ExecutorService executorService = ThreadManager.getInstance().getUnifiedExecutor();
    private final Logger logger; 
    private final Graph graph;
    private final double MAX_DISTANCE = 50.0; // Max distance between nodes to create an edge
    private final double MIN_DISTANCE_BETWEEN_NODES = 20.0; // Minimum distance to consider a new node
    private final int surroundingRadius = 3; // The radius around the node to check
    private final int thickness = 4; // The "thickness" to check in the straight line from node to node if they can connect

    private final Quadtree quadtree;
    private final Random random = new Random();

    public GraphGenerator(Logger logger, int imageWidth, int imageHeight) {
        this.logger = logger;
        this.graph = new Graph();
        this.quadtree = new Quadtree(0, 0, 0, imageWidth, imageHeight);
    }

    public CompletableFuture<Graph> generateGraphAsync(BufferedImage image, String device) {
        CompletableFuture<Graph> future = new CompletableFuture<>();

        Task<Graph> graphGenerationTask = new Task<>() {
            @Override
            protected Graph call() {
                return generateGraph(image, device);
            }
        };

        graphGenerationTask.setOnSucceeded(event -> {
            Graph generatedGraph = graphGenerationTask.getValue();
            future.complete(generatedGraph); // Complete the future with the generated graph
        });

        graphGenerationTask.setOnFailed(event -> {
            Throwable throwable = graphGenerationTask.getException();
            logger.print("Graph generation failed: " + throwable.getMessage());
            future.completeExceptionally(throwable); // Complete the future exceptionally if an error occurs
        });

        // Run the task in a background thread
        Thread thread = new Thread(graphGenerationTask);
        thread.setDaemon(true);  // Ensures the thread will close when the application exits
        thread.start();

        return future;
    }

    public Graph generateGraph(BufferedImage image, String device) { // Added device parameter
        if (image == null) {
            logger.print("Invalid image provided.");
            return graph;
        }

        Long startTime = System.currentTimeMillis();
        logger.print("Starting to generate graph from provided image.");

        PixelReader pixelReader = SwingFXUtils.toFXImage(image, null).getPixelReader();
        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();

        // List to store newly created nodes
        List<GraphNode> newNodes = new ArrayList<>();

        // Process the image to find nodes and connect them
        int width = image.getWidth();
        int height = image.getHeight();
        logger.print("Generating nodes");
        for (int y = 0; y < height; y += 5) {
            for (int x = 0; x < width; x += 5) {
                Color color = pixelReader.getColor(x, y);
                if (isPotentialNode(color) && isSurroundingClear(pixelReader, x, y, width, height)) {
                    int randomOffsetX = random.nextInt(3) - 1;
                    int randomOffsetY = random.nextInt(3) - 1;
                    int worldX = x + randomOffsetX;
                    int worldY = y + randomOffsetY;

                    // Get the plane (Z-coordinate) for the current world coordinates
                    int plane = MapChunkHandler.getPlaneFromLocalCoordinates(device, worldX, worldY);

                    // Synchronize the node addition to ensure thread safety
                    if (plane >= 0 && isFarEnoughFromOtherNodes(worldX, worldY, plane)) {
                        GraphNode node = new GraphNode(graph.getNextAvailableNodeId(), worldX, worldY, plane);
                        graph.addNode(node);
                        quadtree.insert(node);
                        newNodes.add(node);
                    }
                }
            }
        }
        logger.print("Total nodes generated: " + newNodes.size());

        // Process potential edges within the image
        connectNodesWithinImage(newNodes, pixelReader, imageWidth, imageHeight);

        long endTime = System.currentTimeMillis();
        long durationMillis = endTime - startTime;
        double durationSeconds = durationMillis / 1000.0;

        logger.print("Graph generation completed.");
        logger.print("Generation took: " + durationMillis + "ms (" + durationSeconds + " seconds)");
        return graph;
    }

    private boolean isSurroundingClear(PixelReader pixelReader, int x, int y, int width, int height) {
        // Define the bounds of the surrounding area to check
        int minX = Math.max(0, x - surroundingRadius);
        int maxX = Math.min(width - 1, x + surroundingRadius);
        int minY = Math.max(0, y - surroundingRadius);
        int maxY = Math.min(height - 1, y + surroundingRadius);

        // Retrieve nodes from the quadtree within the surrounding area
        List<GraphNode> nearbyNodes = quadtree.retrieveInArea(minX, maxX, minY, maxY);

        // Check if any of these nodes are close enough to interfere with the current pixel
        for (GraphNode node : nearbyNodes) {
            double distance = Math.sqrt(Math.pow(node.getNodeX() - x, 2) + Math.pow(node.getNodeY() - y, 2));
            if (distance <= surroundingRadius) {
                return false;
            }
        }

        // Preload the color values for the surrounding area to minimize PixelReader calls
        Map<Point2D, Color> colorMap = new HashMap<>();
        for (int offsetX = -surroundingRadius; offsetX <= surroundingRadius; offsetX++) {
            for (int offsetY = -surroundingRadius; offsetY <= surroundingRadius; offsetY++) {
                int checkX = x + offsetX;
                int checkY = y + offsetY;
                if (checkX >= 0 && checkX < width && checkY >= 0 && checkY < height) {
                    colorMap.put(new Point2D(checkX, checkY), pixelReader.getColor(checkX, checkY));
                }
            }
        }

        // Check the pixels directly around the current pixel for black (non-passable) pixels
        for (int offsetX = -surroundingRadius; offsetX <= surroundingRadius; offsetX++) {
            for (int offsetY = -surroundingRadius; offsetY <= surroundingRadius; offsetY++) {
                int checkX = x + offsetX;
                int checkY = y + offsetY;

                // Ignore out-of-bounds pixels
                if (checkX < 0 || checkX >= width || checkY < 0 || checkY >= height) {
                    continue;
                }

                // Retrieve from preloaded color map
                Color color = colorMap.get(new Point2D(checkX, checkY));
                if (color.equals(Color.BLACK)) {
                    return false;
                }
            }
        }

        return true;
    }


    private boolean isFarEnoughFromOtherNodes(int x, int y, int z) {
        GraphNode tempNode = new GraphNode("temp", x, y, z);
        List<GraphNode> nearbyNodes = quadtree.retrieve(new ArrayList<>(), tempNode);

        for (GraphNode node : nearbyNodes) {
            if (node.distance(tempNode) < MIN_DISTANCE_BETWEEN_NODES) {
                return false;
            }
        }
        return true;
    }

    private void connectNodesWithinImage(List<GraphNode> nodes, PixelReader pixelReader, int imageWidth, int imageHeight) {
        List<Future<List<GraphEdge>>> futures = new ArrayList<>();

        try {
            int chunkSize = (nodes.size() / AVAILABLE_CORES) + 1;

            logger.print("Starting to submit tasks for edge connections.");

            for (int i = 0; i < nodes.size(); i += chunkSize) {
                List<GraphNode> chunk = nodes.subList(i, Math.min(nodes.size(), i + chunkSize));

                logger.print("Processing chunk: " + i + " to " + (i + chunkSize) + " with " + chunk.size() + " nodes.");

                futures.add(executorService.submit(() -> connectNodesInChunk(chunk, pixelReader, imageWidth, imageHeight)));
            }

            List<GraphEdge> allEdges = new ArrayList<>();  // Combine results from all threads
            for (Future<List<GraphEdge>> future : futures) {
                allEdges.addAll(future.get());  // Get each thread's results and add to the master list
            }
            // Safely add all edges to the graph (synchronized)
            synchronized (graph) {
                for (GraphEdge edge : allEdges) {
                    graph.addEdge(edge);
                }
            }

            logger.print("Total edges created: " + allEdges.size());

        } catch (Exception e) {
            logger.print("Error during graph connection: " + e.getMessage());
            e.printStackTrace();
        } finally {
            executorService.shutdown();
        }
    }

    private List<GraphEdge> connectNodesInChunk(List<GraphNode> nodesChunk, PixelReader pixelReader, int imageWidth, int imageHeight) {
        List<GraphEdge> localEdges = new ArrayList<>();

        for (GraphNode startNode : nodesChunk) {
            int minX = Math.max(0, startNode.getNodeX() - (int) MAX_DISTANCE);
            int maxX = Math.min(imageWidth - 1, startNode.getNodeX() + (int) MAX_DISTANCE);
            int minY = Math.max(0, startNode.getNodeY() - (int) MAX_DISTANCE);
            int maxY = Math.min(imageHeight - 1, startNode.getNodeY() + (int) MAX_DISTANCE);

            List<GraphNode> nearbyNodes = quadtree.retrieveInArea(minX, maxX, minY, maxY);

            for (GraphNode endNode : nearbyNodes) {
                if (!startNode.equals(endNode) && !isEdgePresent(startNode, endNode)) {
                    if (canConnectNodes(startNode, endNode, pixelReader, imageWidth, imageHeight)) {
                        double distance = startNode.distance(endNode);
                        if (distance < MAX_DISTANCE) {
                            GraphEdge edge = new GraphEdge(startNode, endNode);
                            localEdges.add(edge);
                        }
                    }
                }
            }
        }

        logger.print("Chunk processed with " + localEdges.size() + " edges found.");
        return localEdges;
    }

    private boolean isEdgePresent(GraphNode nodeA, GraphNode nodeB) {
        for (GraphEdge edge : graph.getEdges()) {
            if ((edge.getStartNode().equals(nodeA) && edge.getEndNode().equals(nodeB)) ||
                    (edge.getStartNode().equals(nodeB) && edge.getEndNode().equals(nodeA))) {
                return true;
            }
        }
        return false;
    }

    private boolean isPotentialNode(Color color) {
        return !color.equals(Color.BLACK);
    }

    private boolean canConnectNodes(GraphNode startNode, GraphNode endNode, PixelReader pixelReader, int imageWidth, int imageHeight) {
        int x1 = startNode.getNodeX();
        int y1 = startNode.getNodeY();
        int x2 = endNode.getNodeX();
        int y2 = endNode.getNodeY();

        int minX = Math.max(0, Math.min(x1, x2) - thickness);
        int maxX = Math.min(imageWidth - 1, Math.max(x1, x2) + thickness);
        int minY = Math.max(0, Math.min(y1, y2) - thickness);
        int maxY = Math.min(imageHeight - 1, Math.max(y1, y2) + thickness);

        if (isDirectPathClear(x1, y1, x2, y2, pixelReader)) {
            return true;
        }

        return bfsPathExists(x1, y1, x2, y2, pixelReader, minX, maxX, minY, maxY);
    }

    private boolean isDirectPathClear(int x1, int y1, int x2, int y2, PixelReader pixelReader) {
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int sx = x1 < x2 ? 1 : -1;
        int sy = y1 < y2 ? 1 : -1;
        int err = dx - dy;

        while (true) {
            // Check if the current pixel is white
            if (!pixelReader.getColor(x1, y1).equals(Color.WHITE)) {
                return false; // Encountered a non-white pixel, direct path is not clear
            }

            if (x1 == x2 && y1 == y2) {
                break; // Reached the end point
            }

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

        return true; // Direct path is clear
    }

    private boolean bfsPathExists(int startX, int startY, int endX, int endY, PixelReader pixelReader, int minX, int maxX, int minY, int maxY) {
        boolean[][] visited = new boolean[maxX - minX + 1][maxY - minY + 1];
        Queue<int[]> queue = new LinkedList<>();
        queue.add(new int[]{startX, startY});
        visited[startX - minX][startY - minY] = true;

        int[][] directions = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

        while (!queue.isEmpty()) {
            int[] current = queue.poll();
            int x = current[0];
            int y = current[1];

            // If we've reached the end node, return true
            if (x == endX && y == endY) {
                return true;
            }

            // Explore the neighbors (up, down, left, right)
            for (int[] dir : directions) {
                int newX = x + dir[0];
                int newY = y + dir[1];

                if (newX >= minX && newX <= maxX && newY >= minY && newY <= maxY &&
                        !visited[newX - minX][newY - minY]) {

                    Color color = pixelReader.getColor(newX, newY);
                    if (color.equals(Color.WHITE)) {
                        visited[newX - minX][newY - minY] = true;
                        queue.add(new int[]{newX, newY});
                    }
                }
            }
        }

        return false; // No connected path found
    }
}