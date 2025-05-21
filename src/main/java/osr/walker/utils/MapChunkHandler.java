package osr.walker.utils;

import helpers.Logger;
import helpers.utils.MapChunk;
import utils.SystemUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class MapChunkHandler {
    private final Logger logger;
    private static final ConcurrentHashMap<String, Map<String, ChunkData>> deviceChunkPositions = new ConcurrentHashMap<>();

    public MapChunkHandler(Logger logger) {
        this.logger = logger;
    }

    public static ConcurrentHashMap<String, Map<String, ChunkData>> getDeviceChunkPositions() {
        return deviceChunkPositions;
    }

    public static void removeDeviceChunkPosition(String deviceID) {
        deviceChunkPositions.remove(deviceID);
    }

    private static ChunkCoordinates findChunkCoordinates(String device, int localX, int localY) {
        Map<String, ChunkData> chunkPositions = deviceChunkPositions.get(device);
        if (chunkPositions == null) {
            System.out.println("No chunks stored for device: " + device);
            return new ChunkCoordinates(new Point(0, 0), -1);  // Indicating no chunk found
        }

        for (Map.Entry<String, ChunkData> entry : chunkPositions.entrySet()) {
            ChunkData chunkData = entry.getValue();
            Point topLeft = chunkData.getTopLeft();
            if (localX >= topLeft.x && localX < topLeft.x + 256 &&
                    localY >= topLeft.y && localY < topLeft.y + 256) {

                // Extract and parse the chunk key parts
                String[] chunkKeyParts = entry.getKey().split("-");
                int chunkX = Integer.parseInt(chunkKeyParts[0].trim());
                int chunkY = Integer.parseInt(chunkKeyParts[1].trim());
                int worldX = chunkX * 256 + (localX - topLeft.x);
                int worldY = chunkY * 256 + (topLeft.y - localY);

                int plane = chunkData.getPlane() != null ? Integer.parseInt(chunkData.getPlane()) : -1;

                return new ChunkCoordinates(new Point(worldX, worldY), plane);
            }
        }

        //System.out.println("Coordinates (" + localX + ", " + localY + ") do not match any chunk for device: " + device);
        return new ChunkCoordinates(new Point(0, 0), -1);
    }

    public static Point getWorldCoordinates(String device, int localX, int localY) {
        ChunkCoordinates coordinates = findChunkCoordinates(device, localX, localY);
        return new Point(coordinates.x, coordinates.y);
    }

    public static ChunkCoordinates getWorldCoordinateWithPlane(String device, int localX, int localY) {
        return findChunkCoordinates(device, localX, localY);
    }

    public static int getPlaneAtCoordinate(String device, int worldX, int worldY, int worldPlane) {
        ChunkCoordinates chunkCoordinates = getLocalCoordinatesFromWorld(device, worldX, worldY, worldPlane);

        if (chunkCoordinates.z == -1) {
            System.out.println("Invalid world coordinates or no chunk found for device: " + device);
            return -1;
        }

        return chunkCoordinates.z;
    }

    public static int getPlaneFromLocalCoordinates(String device, int localX, int localY) {
        // Retrieve the map of chunk positions for the given device
        Map<String, ChunkData> chunkPositions = deviceChunkPositions.get(device);

        if (chunkPositions == null) {
            System.out.println("No chunks stored for device: " + device);
            return -1;  // Indicating no chunk found
        }

        // Iterate over all chunks to find which one contains the local coordinates
        for (Map.Entry<String, ChunkData> entry : chunkPositions.entrySet()) {
            ChunkData chunkData = entry.getValue();
            Point topLeft = chunkData.getTopLeft();

            // Check if the local coordinates fall within this chunk
            if (localX >= topLeft.x && localX < topLeft.x + 256 &&
                    localY >= topLeft.y && localY < topLeft.y + 256) {

                // Extract and parse the plane information from the chunk key
                return chunkData.getPlane() != null ? Integer.parseInt(chunkData.getPlane()) : -1;
            }
        }

        // If no chunk is found for the given local coordinates, return -1
        //System.out.println("Coordinates (" + localX + ", " + localY + ") do not match any chunk for device: " + device);
        return -1;
    }

    public static ChunkCoordinates getLocalCoordinatesFromWorld(String device, int worldX, int worldY, int worldPlane) {
        Map<String, ChunkData> chunkPositions = deviceChunkPositions.get(device);

        if (chunkPositions == null) {
            System.out.println("No chunks stored for device: " + device);
            return new ChunkCoordinates(new Point(0, 0), -1);
        }

        for (Map.Entry<String, ChunkData> entry : chunkPositions.entrySet()) {
            ChunkData chunkData = entry.getValue();
            Point chunkTopLeft = chunkData.getTopLeft();

            // Parse chunk key
            String[] parts = entry.getKey().split("-");
            int chunkX = Integer.parseInt(parts[0].trim());
            int chunkY = Integer.parseInt(parts[1].trim());
            int plane = Integer.parseInt(parts[2].trim());

            // Check if we are on the right plane
            if (plane != worldPlane) continue;

            // Chunk boundaries for inverted Y-axis
            int startX = chunkX * 256;  // X-boundary start
            int endX = startX + 256 - 1;  // X-boundary end

            // Adjust Y-boundary for inverted Y-axis
            int startY = chunkY * 256;  // Chunk Y-start (inverted)
            int endY = startY - 256 + 1;  // Y-end (since Y is inverted, we subtract to go "downward")

            // Check if the world coordinates fall within this chunk
            if (worldX >= startX && worldX <= endX && worldY >= endY && worldY <= startY) {
                int localX = worldX - startX + chunkTopLeft.x;
                int localY = chunkTopLeft.y + (startY - worldY);  // Handle inverted Y-axis for local coordinates
                //System.out.println("Matching chunk found. Local coordinates: (" + localX + ", " + localY + ")");
                return new ChunkCoordinates(new Point(localX, localY), plane);
            }
        }

        // No matching chunk found
        //System.out.println("No matching chunk found for world coordinates (" + worldX + ", " + worldY + ")");
        return new ChunkCoordinates(new Point(0, 0), -1);
    }

    public BufferedImage stitchMap(String device, MapChunk mapChunk, boolean getCollision, boolean cleanChunkCache) {
        logger.print("Removing old chunk information (if present)");

        if (cleanChunkCache) {
            removeChunksForDevice(device);
        }

        logger.print("Generating map for device: " + device);

        // Ensure at least 1 chunk is provided
        if (mapChunk.getChunks().isEmpty()) {
            logger.log("MapChunk cannot have less than 1 parameter!", device);
            return null;
        }

        // If there is only 1 chunk, we calculate the bounding chunks around it
        Set<String> allChunks;
        if (mapChunk.getChunks().size() == 1) {
            String singleChunk = mapChunk.getChunks().get(0);
            allChunks = calculateBoundingChunksFromBox(singleChunk, singleChunk);
            // Create a new MapChunk with the updated chunks list
            MapChunk adjustedMapChunk = new MapChunk(allChunks.toArray(new String[0]), mapChunk.getPlanes().toArray(new String[0]));
            logger.print("Only one chunk provided, adding surrounding chunks: " + allChunks);
            return stitchChunks(device, adjustedMapChunk, getCollision);
        } else {
            // If there are 2 or more chunks, proceed as usual
            return stitchChunks(device, mapChunk, getCollision);
        }
    }

    private BufferedImage stitchChunks(String device, MapChunk mapChunk, boolean getCollision) {
        List<String> chunks = mapChunk.getChunks();
        List<String> planes = mapChunk.getPlanes();

        // Get all chunks including bounding chunks if needed
        Set<String> allChunks = (chunks.size() == 2)
                ? calculateBoundingChunksFromBox(chunks.get(0), chunks.get(1))
                : new HashSet<>(chunks);

        // Group chunks based on adjacency
        List<Set<String>> chunkGroups = groupChunks(allChunks);

        // Calculate dimensions for the image, accounting for the border
        Dimension dimensions = calculateDimensionsForStitchingWithBorder(chunkGroups, planes);
        BufferedImage finalImage = new BufferedImage(dimensions.width, dimensions.height, BufferedImage.TYPE_INT_RGB);
        Graphics2D gFinal = finalImage.createGraphics();

        // Prepare the canvas (black background)
        prepareCanvas(gFinal, dimensions);

        int currentHeight = 100;  // Start with yOffset of 100 to account for top border
        Map<String, ChunkData> chunkPositions = deviceChunkPositions.computeIfAbsent(device, k -> new HashMap<>());

        // Process each chunk group
        for (Set<String> group : chunkGroups) {
            Dimension groupDimensions = calculateGroupDimensions(group, planes);
            processGroup(gFinal, group, planes, currentHeight, chunkPositions, getCollision);
            currentHeight += groupDimensions.height;
        }

        gFinal.dispose();
        return finalImage;
    }

    private Dimension calculateDimensionsForStitchingWithBorder(List<Set<String>> chunkGroups, List<String> planes) {
        Dimension baseDimensions = calculateDimensionsForStitching(chunkGroups, planes);

        // Add 100-pixel border on all sides (left, right, top, bottom)
        int widthWithBorder = baseDimensions.width + 100;  // 100 on the right
        int heightWithBorder = baseDimensions.height + 200;  // 100 on the top and 100 on the bottom

        return new Dimension(widthWithBorder, heightWithBorder);
    }

    private Set<String> calculateBoundingChunksFromBox(String upperLeft, String lowerRight) {
        String[] upperLeftParts = upperLeft.split("-");
        String[] lowerRightParts = lowerRight.split("-");

        int upperLeftX = Integer.parseInt(upperLeftParts[0].trim());
        int upperLeftY = Integer.parseInt(upperLeftParts[1].trim());
        int lowerRightX = Integer.parseInt(lowerRightParts[0].trim());
        int lowerRightY = Integer.parseInt(lowerRightParts[1].trim());

        // Adjust coordinates to expand the bounding box
        int maxChunkX = 99;
        int maxChunkY = 199;
        upperLeftX = Math.max(upperLeftX - 1, 0);
        upperLeftY = Math.min(upperLeftY + 1, maxChunkY);
        lowerRightX = Math.min(lowerRightX + 1, maxChunkX);
        lowerRightY = Math.max(lowerRightY - 1, 0);

        return calculateBoundingChunks(upperLeftX, upperLeftY, lowerRightX, lowerRightY);
    }

    private void prepareCanvas(Graphics2D graphics, Dimension dimensions) {
        graphics.setColor(Color.BLACK);
        graphics.fillRect(0, 0, dimensions.width, dimensions.height);  // Fill entire canvas with black
    }

    private void processGroup(Graphics2D gFinal, Set<String> group, List<String> planes, int yOffset, Map<String, ChunkData> chunkPositions, boolean getCollision) {
        int[] boundingBox = calculateBoundingBox(group);

        // Calculate the number of vertical chunks in the group
        int verticalChunks = calculateVerticalChunks(group);

        // We iterate over each plane, and draw all the chunks for this plane first.
        int currentPlaneOffset = yOffset;  // Start with the yOffset for the group

        for (String plane : planes) {
            // Iterate over each chunk in the group
            for (String chunk : group) {
                // Draw each chunk for the current plane
                processChunk(gFinal, chunk, boundingBox, currentPlaneOffset, chunkPositions, plane, getCollision);
            }

            // After drawing the entire plane, update the offset for the next plane
            currentPlaneOffset += verticalChunks * 256;  // Move down by the height of all vertical chunks
        }
    }

    private int calculateVerticalChunks(Set<String> group) {
        // Extract unique Y coordinates from the chunk names to count vertical chunks
        Set<Integer> uniqueYCoordinates = new HashSet<>();

        for (String chunk : group) {
            String[] parts = chunk.split("-");
            int chunkY = Integer.parseInt(parts[1].trim());
            uniqueYCoordinates.add(chunkY);  // Add Y coordinate to the set
        }

        return uniqueYCoordinates.size();  // The number of unique Y coordinates represents the vertical chunks
    }

    private void processChunk(Graphics2D gFinal, String chunk, int[] boundingBox, int yOffset, Map<String, ChunkData> chunkPositions, String plane, boolean getCollision) {
        String[] parts = chunk.split("-");
        int chunkX = Integer.parseInt(parts[0].trim());
        int chunkY = Integer.parseInt(parts[1].trim());

        // Calculate the position to draw this chunk (X and Y positions)
        int drawX = (chunkX - boundingBox[0]) * 256 + 100;  // Horizontal position, starting 100px from the left
        int drawY = (boundingBox[1] - chunkY) * 256 + yOffset;  // Vertical position is adjusted by yOffset for planes

        chunkPositions.put(chunkX + "-" + chunkY + "-" + plane, new ChunkData(new Point(drawX, drawY), plane));

        // Construct the file path for the chunk and plane
        String filePath = constructFilePath(chunkX, chunkY, plane, getCollision);

        // Draw the chunk at the calculated position
        drawImage(gFinal, filePath, drawX, drawY);
    }

    private String constructFilePath(int chunkX, int chunkY, String plane, boolean getCollision) {
        String basePath = SystemUtils.getSystemPath();
        String folder = getCollision ? "chunks/collision/" : "chunks/map/";
        return basePath + folder + plane + "/" + chunkX + "-" + chunkY + ".png";
    }

    private Dimension calculateGroupDimensions(Set<String> group, List<String> planes) {
        int[] boundingBox = calculateBoundingBox(group);
        int width = (boundingBox[2] - boundingBox[0] + 1) * 256 + 100;
        int height = (boundingBox[1] - boundingBox[3] + 1) * 256 * planes.size() + (planes.size() - 1) * 100 + 100;
        return new Dimension(width, height);
    }

    private Dimension calculateDimensionsForStitching(List<Set<String>> chunkGroups, List<String> planes) {
        int totalWidth = 0;
        int totalHeight = 0;
        for (Set<String> group : chunkGroups) {
            Dimension groupDimension = calculateGroupDimensions(group, planes);
            totalWidth = Math.max(totalWidth, groupDimension.width);
            totalHeight += groupDimension.height;
        }
        return new Dimension(totalWidth, totalHeight);
    }

    private List<Set<String>> groupChunks(Set<String> chunks) {
        List<Set<String>> groups = new ArrayList<>();
        Set<String> visited = new HashSet<>();

        for (String chunk : chunks) {
            if (!visited.contains(chunk)) {
                Set<String> group = new HashSet<>();
                exploreChunkGroup(chunk, chunks, group, visited);
                groups.add(group);
            }
        }

        return groups;
    }

    private void exploreChunkGroup(String chunk, Set<String> chunks, Set<String> group, Set<String> visited) {
        if (visited.contains(chunk)) return;

        visited.add(chunk);
        group.add(chunk);

        String[] parts = chunk.split("-");
        int chunkX = Integer.parseInt(parts[0].trim());
        int chunkY = Integer.parseInt(parts[1].trim());

        for (String nextChunk : chunks) {
            if (!visited.contains(nextChunk)) {
                String[] nextParts = nextChunk.split("-");
                int nextX = Integer.parseInt(nextParts[0].trim());
                int nextY = Integer.parseInt(nextParts[1].trim());

                if (Math.abs(chunkX - nextX) <= 1 && Math.abs(chunkY - nextY) <= 1) {
                    exploreChunkGroup(nextChunk, chunks, group, visited);
                }
            }
        }
    }

    private int[] calculateBoundingBox(Set<String> group) {
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;

        for (String chunk : group) {
            String[] parts = chunk.split("-");
            int chunkX = Integer.parseInt(parts[0].trim());
            int chunkY = Integer.parseInt(parts[1].trim());

            if (chunkX < minX) minX = chunkX;
            if (chunkX > maxX) maxX = chunkX;
            if (chunkY < minY) minY = chunkY;
            if (chunkY > maxY) maxY = chunkY;
        }

        return new int[]{minX, maxY, maxX, minY}; // Remember Y-axis is inverted
    }

    private Set<String> calculateBoundingChunks(int upperLeftX, int upperLeftY, int lowerRightX, int lowerRightY) {
        Set<String> allChunks = new HashSet<>();

        for (int x = upperLeftX; x <= lowerRightX; x++) {
            for (int y = lowerRightY; y <= upperLeftY; y++) {
                allChunks.add(x + "-" + y);
            }
        }

        return allChunks;
    }

    private void drawImage(Graphics2D g, String filePath, int x, int y) {
        try {
            BufferedImage image = ImageIO.read(new File(filePath));
            if (image != null) {
                g.drawImage(image, x, y, null);
                image.flush();
            } else {
                logger.print("Failed to load image: " + filePath);
            }
        } catch (IOException e) {
            //logger.print("Error loading image from path: " + filePath);
        }
    }

    public static void removeChunksForDevice(String device) {
        if (device == null || device.isEmpty()) {
            System.out.println("Device is null or empty, cannot remove chunks.");
            return;
        }

        // Check if the device has any chunk data stored
        Map<String, ChunkData> removedChunks = deviceChunkPositions.remove(device);

        if (removedChunks != null) {
            System.out.println("Removed all chunks for device: " + device + ". Number of chunks removed: " + removedChunks.size());
        } else {
            System.out.println("No chunks found for device: " + device);
        }
    }
}