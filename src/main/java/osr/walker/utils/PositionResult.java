package osr.walker.utils;

import helpers.utils.Tile;
import org.opencv.core.Point;

public class PositionResult {
    private final Point foundPoint;
    private final double confidence;
    private final int width;
    private final int height;
    public final int x;
    public final int y;
    public int z;

    public PositionResult(Point foundPoint, double confidence, int width, int height) {
        this.foundPoint = foundPoint;
        this.confidence = confidence;
        this.width = width;
        this.height = height;
        this.x = (int) foundPoint.x;
        this.y = (int) foundPoint.y;
        this.z = -1;
    }

    public Point getPosition() {
        return foundPoint;
    }

    public double getConfidence() {
        return confidence;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public Tile getTile(String device) {
        ChunkCoordinates chunkCoordinates = MapChunkHandler.getWorldCoordinateWithPlane(device, (int) foundPoint.x, (int) foundPoint.y);
        z = chunkCoordinates.z;
        return chunkCoordinates.getTile();
    }

    public int x() {
        return (int) foundPoint.x;
    }

    public int y() {
        return (int) foundPoint.y;
    }

    /**
     * Translates the position's coordinates to full world coordinates.
     *
     * @return The translated world coordinates.
     */
    public ChunkCoordinates getWorldCoordinates(String device) {
        ChunkCoordinates chunkCoordinates = MapChunkHandler.getWorldCoordinateWithPlane(device, (int) foundPoint.x, (int) foundPoint.y);
        z = chunkCoordinates.z;
        return chunkCoordinates;
    }

    public void updateZ(String device) {
        ChunkCoordinates chunkCoordinates = MapChunkHandler.getWorldCoordinateWithPlane(device, (int) foundPoint.x, (int) foundPoint.y);
        z = chunkCoordinates.z;
    }

    /**
     * This just returns it as an OCV point (For walker purposes)
     * @param device
     * @return org.opencv.core.Point
     */
    public Point getWorldCoordinatesOCV(String device) {
        Tile coordinates = getWorldCoordinates(device).getTile();
        return new Point(coordinates.x, coordinates.y);
    }

    /**
     * Returns the local chunk coordinates for the position's coordinates.
     *
     * @return The local chunk coordinates.
     */
    public ChunkCoordinates getLocalChunkCoordinate(String device) {
        return MapChunkHandler.getLocalCoordinatesFromWorld(device, x, y, z);
    }

    public int getPlaneFromCoordinate(String device) {
        return MapChunkHandler.getPlaneAtCoordinate(device, x, y, z);
    }
}
