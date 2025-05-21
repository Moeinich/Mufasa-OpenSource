package osr.walker;

import helpers.grapher.Graph;
import helpers.grapher.utils.GraphNode;
import helpers.grapher.utils.dataclasses.GraphData;
import com.google.gson.Gson;
import helpers.CacheManager;
import helpers.Logger;
import helpers.utils.MapChunk;
import helpers.utils.Tile;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import osr.mapping.Minimap;
import osr.mapping.utils.BankAreas;
import osr.mapping.utils.BankPositions;
import osr.mapping.utils.PlayerHelper;
import osr.utils.ImageUtils;
import osr.utils.NamedArea;
import osr.walker.utils.*;
import scripts.APIClasses.ClientAPI;
import scripts.ScriptInfo;
import utils.SystemUtils;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static helpers.grapher.GraphHelpers.convertToGraph;
import static helpers.grapher.GraphHelpers.findNearestNode;
import static utils.Constants.convertTileArrayToOpenCVPointArray;

public class Walker {
    private final TranslatePosition translatePosition;
    private final ClientAPI clientAPI;
    private final Logger logger;
    private final Minimap minimap;
    private final MapIR mapIR;
    private final ScriptInfo scriptInfo;
    private final PlayerHelper playerHelper;
    private final MapChunkHandler mapChunkHandler;
    private final ImageUtils imageUtils;
    private final CacheManager cacheManager;
    private Graph graph = null;
    private Mat bankMap = null;

    public Walker(TranslatePosition translatePosition, ClientAPI clientAPI, Logger logger, Minimap minimap, MapIR mapIR, ScriptInfo scriptInfo, PlayerHelper playerHelper, MapChunkHandler mapChunkHandler, ImageUtils imageUtils, CacheManager cacheManager) {
        this.translatePosition = translatePosition;
        this.clientAPI = clientAPI;
        this.logger = logger;
        this.minimap = minimap;
        this.mapIR = mapIR;
        this.scriptInfo = scriptInfo;
        this.playerHelper = playerHelper;
        this.mapChunkHandler = mapChunkHandler;
        this.imageUtils = imageUtils;
        this.cacheManager = cacheManager;

        loadGraph();
    }

    public void setup(String device, MapChunk mapChunk) {
        BufferedImage mapImage = mapChunkHandler.stitchMap(device, mapChunk, false, true);
        // Save the updated MapInfo back to the cache manager
        cacheManager.setMapInfo(device, new MapInfo(imageUtils.bufferedImageToMat(mapImage)));
    }

    public void setup(String device, Mat map) {
        // Create or update the MapInfo object with the new map
        cacheManager.setMapInfo(device, new MapInfo(map));
    }

    /**
     * @param device the device we should get minimap from
     * @return local position on stitched map as PositionResult
     */
    public PositionResult getBankPosition(String device) {
        if (bankMap == null) {
            // If bankMap is null, set it up using the mapChunk and stitchMap method
            MapChunk mapChunk = new MapChunk(new String[]{
                    "51-49", "50-49", "40-52", "40-51", "41-52", "41-51", "38-48",
                    "43-53", "44-53", "19-55", "19-56", "49-53", "45-51", "46-51",
                    "45-50", "46-50", "48-50", "48-54", "48-53", "47-52", "46-52",
                    "45-52", "19-58", "48-56", "49-56", "49-54", "27-56", "27-55",
                    "26-54", "26-53", "27-54", "27-53", "23-53", "22-59", "22-60",
                    "23-58", "24-58", "42-54", "43-54", "44-54", "23-56", "22-56",
                    "22-55", "23-55", "50-53", "51-53", "25-61", "24-54", "25-54",
                    "40-48", "41-48", "47-77", "47-78", "46-152", "46-151", "47-152",
                    "47-151", "27-48", "28-48", "25-48",
            }, "0", "1");
            BufferedImage bufferMap = mapChunkHandler.stitchMap("bankDevice", mapChunk, false, true);
            Image fxImage = SwingFXUtils.toFXImage(bufferMap, null);
            bankMap = imageUtils.convertFXImageToMat(fxImage);
        }

        Mat minimapMat = minimap.getCleanMinimapMat(device,false, true);
        return mapIR.getPosition(minimapMat, bankMap);
    }

    public PositionResult getPlayerPosition(String device) {
        return getPlayerPosition(device, 92);
    }

    public PositionResult getPlayerPosition(String device, int minimapRegionSize) {
        MapInfo deviceInfo = cacheManager.getMapInfo(device);
        Mat minimapMat = minimap.getCleanMinimapMat(device, minimapRegionSize,false, true);
        if (minimapMat == null) {
            return new PositionResult(new Point(0, 0), 0.0, 0, 0);
        }
        return mapIR.getPosition(minimapMat, deviceInfo.getMap());
    }

    /**
     * Finds and returns the nearest bank area to the player's current position.
     *
     * @param device The device identifier.
     * @return The nearest NamedArea representing the bank, or null if no banks are found on the same plane.
     */
    public Tile getNearestBank(String device) {
        // Retrieve the player's current position
        Tile playerTile = getPlayerPosition(device).getWorldCoordinates(device).getTile();

        // Retrieve the list of all bank areas
        List<NamedArea> bankAreas = BankAreas.getBankAreas();

        NamedArea nearestBank = null;
        double minDistance = Double.MAX_VALUE;

        // Iterate through each bank area to find the nearest one
        for (NamedArea bankArea : bankAreas) {
            // Ensure both the player and the bank are on the same plane
            if (bankArea.topTile.z != playerTile.z) {
                continue;
            }

            // Calculate the distance from the player to the center of the bank area
            double distance = calculateDistanceToBankCenter(playerTile, bankArea);

            // Update the nearest bank if a closer one is found
            if (distance < minDistance) {
                minDistance = distance;
                nearestBank = bankArea;
            }
        }
        return BankPositions.getRandomPosition(nearestBank != null ? nearestBank.getName() : null);
    }

    public Point getWorldPosition(String device) {
        return getWorldPosition(device, 92);
    }

    public Point getWorldPosition(String device, int minimapRegionSize) {
        MapInfo deviceInfo = cacheManager.getMapInfo(device);
        Mat minimapMat = minimap.getCleanMinimapMat(device, minimapRegionSize, false, true);
        return mapIR.getPosition(minimapMat, deviceInfo.getMap()).getWorldCoordinatesOCV(device);
    }

    public Point worldToMM(Point worldPoint, Point playerPoint, String device) {
        return translatePosition.worldToMM(worldPoint, playerPoint, device);
    }

    public boolean stepToPoint(Point worldmapPoint, String device, Runnable actionWhileStepping) {
        Point currentPlayerPosition = getWorldPosition(device);
        return moveTo(worldmapPoint, currentPlayerPosition, device, actionWhileStepping, 30, false, 0);
    }

    public void stepToPointBank(String realEmulatorDevice, Tile destination) {
        Tile playerPos = getBankPosition(realEmulatorDevice).getWorldCoordinates("bankDevice").getTile();
        java.awt.Point minimapCenter = minimap.getMinimapCenter(realEmulatorDevice);

        int translatedX = destination.x - playerPos.x + minimapCenter.x;
        int translatedY = playerPos.y - destination.y + minimapCenter.y;

        clientAPI.tap(translatedX, translatedY);
    }

    public boolean walkTo(Point worldmapTile, Point playerPoint, String device, Runnable actionWhileWalking) {
        return moveTo(worldmapTile, playerPoint, device, actionWhileWalking, 30, true, 10);
    }

    public Boolean walkPath(Point[] path, Runnable whileRunning, String device) {
        PositionSupplier positionSupplier = () -> getWorldPosition(device);
        return walkPath(path, positionSupplier, whileRunning, device);
    }

    private Boolean walkPath(Point[] path, PositionSupplier positionSupplier, Runnable whileRunning, String device) {
        int nearestPointIndex = findNearestPointIndex(positionSupplier.getPosition(), path);

        if (isPointVeryClose(positionSupplier.getPosition(), path[nearestPointIndex], 2) && nearestPointIndex + 1 < path.length) {
            nearestPointIndex++;
        }

        return followPathFromIndex(path, nearestPointIndex, positionSupplier, whileRunning, device);
    }

    public boolean moveTo(Point worldTile, Point playerPoint, String device, Runnable action, int maxAttempts, boolean randomize, int margin) {
        Point worldmapPointCV = new Point(worldTile.x, worldTile.y);
        Point minimapPoint = worldToMM(worldmapPointCV, playerPoint, device);

        if (randomize) {
            int randomXOffset = (int) (Math.random() * 5) - 2;
            int randomYOffset = (int) (Math.random() * 5) - 2;
            minimapPoint = new Point(minimapPoint.x + randomXOffset, minimapPoint.y + randomYOffset);
        }

        if (!playerHelper.isRunEnabled(device)) {
            int currentRunEnergy = playerHelper.readRun(device);
            if (currentRunEnergy >= 40) {
                playerHelper.toggleRun(device);
            }
        }

        clientAPI.tapMinimap((int) (minimapPoint.x), (int) (minimapPoint.y), device);
        return checkAndReact(maxAttempts, margin, device, action, worldmapPointCV);
    }

    private boolean checkAndReact(int maxAttempts, int margin, String device, Runnable action, Point worldmapPointCV) {
        boolean isWithinMargin = false;
        int attempts = 0;

        while (!isWithinMargin && attempts < maxAttempts && !scriptInfo.getCancellationToken(device).isCancellationRequested()) {
            if (action != null) {
                logger.print("Performing action while moving");
                action.run();
            }

            Point currentPlayerPosition = getWorldPosition(device);
            double dx = Math.abs(currentPlayerPosition.x - worldmapPointCV.x);
            double dy = Math.abs(currentPlayerPosition.y - worldmapPointCV.y);
            isWithinMargin = (dx <= margin && dy <= margin);

            if (!isWithinMargin) {
                attempts++;
                double distance = Math.sqrt(dx * dx + dy * dy);
                long sleepTime = (distance > 10) ? 400 : (distance > 5) ? 250 : 100;
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.out.println("Interruption: " + e.getMessage());
                    break;
                }
            }
        }

        return isWithinMargin;
    }

    public boolean isReachable(Tile destinationTile, String device) {
        Point mmPoint = worldToMM(new Point(destinationTile.x, destinationTile.y), getWorldPosition(device), device);
        return minimap.isPointOn(new java.awt.Point((int) mmPoint.x, (int) mmPoint.y), device);
    }

    private int findNearestPointIndex(Point currentPlayerPosition, Point[] path) {
        double minDistance = Double.MAX_VALUE;
        int index = 0;
        for (int i = 0; i < path.length; i++) {
            double distance = calculateDistanceToPoint(currentPlayerPosition, path[i]);
            if (distance < minDistance) {
                minDistance = distance;
                index = i;
            }
        }
        return index;
    }

    private boolean isPointVeryClose(Point currentPlayerPosition, Point point, double threshold) {
        return calculateDistanceToPoint(currentPlayerPosition, point) < threshold;
    }

    private Boolean followPathFromIndex(Point[] path, int startIndex, PositionSupplier positionSupplier, Runnable whileRunning, String device) {
        for (int i = startIndex; i < path.length; i++) {
            Point destination = path[i];
            if (!moveTo(destination, positionSupplier.getPosition(), device, whileRunning, 40, true, 20)) {
                logger.print("Failed to reach the destination in your path: " + destination.x + ", " + destination.y);
                if (i + 1 < path.length && minimap.isPointOn(new java.awt.Point((int) path[i + 1].x, (int) path[i + 1].y), device)) {
                    logger.print("Skipping to next point as it is visible on the minimap");
                    continue;
                }
                return false;
            }
        }
        return true;
    }

    private double calculateDistanceToPoint(Point p1, Point p2) {
        return Math.sqrt(Math.pow(p2.x - p1.x, 2) + Math.pow(p2.y - p1.y, 2));
    }

    public boolean webWalk(Tile destinationTile, Runnable whileRunning, String device, boolean stepToEnd) {
        Point[] path = convertTileArrayToOpenCVPointArray(buildTilePath(device, destinationTile, !stepToEnd));

        if (path.length > 1) {
            walkPath(path, whileRunning, device);

            if (stepToEnd) {
                Point endPoint = new Point(destinationTile.x, destinationTile.y);
                return stepToPoint(endPoint, device, null);
            } else {
                return true;
            }
        } else {
            logger.print("Invalid path on: " + device);
            return false;
        }
    }

    // Shared method to find the shortest path and return GraphNode[] (internal logic)
    private GraphNode[] findShortestPath(String device, Tile endTile) {
        Point startPosition = getWorldPosition(device);
        logger.print("world position: " + startPosition.x + "," + startPosition.y);
        GraphNode startNode = findNearestNode(graph, (int) startPosition.x, (int) startPosition.y);
        logger.print("Start node: " + startNode.getNodeX() + "," + startNode.getNodeY() + "," + startNode.getNodeZ());
        GraphNode endNode = findNearestNode(graph, endTile.x(), endTile.y());

        // Find the shortest path as a GraphNode[] array
        GraphNode[] shortestPathNodes = graph.findShortestPath(startNode, endNode);

        // Return the shortest path or null if no path is found
        return (shortestPathNodes == null || shortestPathNodes.length == 0) ? null : shortestPathNodes;
    }

    public Tile[] buildTilePath(String device, Tile endTile, boolean includeEndTile) {
        GraphNode[] shortestPathNodes = findShortestPath(device, endTile);

        // If no path is found, return an empty array
        if (shortestPathNodes == null) {
            return new Tile[0];
        }

        // Determine the correct size for the tiles array based on whether the endTile should be included
        int tileArraySize = shortestPathNodes.length + (includeEndTile ? 1 : 0);
        Tile[] tiles = new Tile[tileArraySize];

        // Convert the GraphNode[] array into a Tile[] array
        for (int i = 0; i < shortestPathNodes.length; i++) {
            GraphNode node = shortestPathNodes[i];
            tiles[i] = new Tile(node.getNodeX(), node.getNodeY(), node.getNodeZ());
        }

        // Add the endTile to the last position if needed
        if (includeEndTile) {
            tiles[shortestPathNodes.length] = endTile;
        }

        return tiles;
    }

    private void loadGraph() {
        logger.print("Loading webwalker graphs");
        String graphFilePath = SystemUtils.getSystemPath() + "graphs/map.graph";

        try {
            String json = new String(Files.readAllBytes(Path.of(graphFilePath)));
            Gson gson = new Gson();
            GraphData graphData = gson.fromJson(json, GraphData.class);
            graph = convertToGraph(graphData);
            logger.print("Done loading graphs");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FunctionalInterface
    private interface PositionSupplier {
        Point getPosition();
    }

    /**
     * Calculates the Euclidean distance between the player's tile and the center of the bank area.
     *
     * @param playerTile The player's current tile.
     * @param area       The bank area to calculate the distance to.
     * @return The Euclidean distance as a double.
     */
    private double calculateDistanceToBankCenter(Tile playerTile, NamedArea area) {
        // Calculate the center coordinates of the bank area
        int centerX = (area.topTile.x + area.bottomTile.x) / 2;
        int centerY = (area.topTile.y + area.bottomTile.y) / 2;

        // Calculate the difference in coordinates
        int deltaX = playerTile.x - centerX;
        int deltaY = playerTile.y - centerY;

        // Return the Euclidean distance
        return Math.sqrt(deltaX * deltaX + deltaY * deltaY);
    }
}