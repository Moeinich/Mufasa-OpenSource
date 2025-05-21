package osr.walker.utils;

import helpers.Logger;
import helpers.utils.Tile;
import org.opencv.core.Point;
import osr.mapping.Minimap;

import java.awt.*;

public class TranslatePosition {
    private final Minimap minimap;
    private final Logger logger;

    public TranslatePosition(Logger logger, Minimap minimap) {
        this.minimap = minimap;
        this.logger = logger;
    }

    public Point worldToMM(Point worldPoint, Point playerPoint, String device) {
        java.awt.Point minimapCenter = minimap.getMinimapCenter(device);

        // Translate the world coordinates to minimap coordinates
        int translatedX = (int) (worldPoint.x - playerPoint.x + minimapCenter.x);
        int translatedY = (int) (playerPoint.y - worldPoint.y + minimapCenter.y);

        java.awt.Point tPoint = new java.awt.Point(translatedX, translatedY);
        if (!minimap.isPointOn(tPoint, device)) {
            logger.debugLog("tap point is not on the minimap!", device);
            return new Point(-1, -1);
        }

        return new Point(translatedX, translatedY);
    }

    public java.awt.Point worldToMM(Tile worldPoint, Tile playerPoint, String device) {
        java.awt.Point minimapCenter = minimap.getMinimapCenter(device);

        // Translate the world coordinates to minimap coordinates
        int translatedX = worldPoint.x - playerPoint.x + minimapCenter.x;
        int translatedY = playerPoint.y - worldPoint.y + minimapCenter.y;

        java.awt.Point tPoint = new java.awt.Point(translatedX, translatedY);
        if (!minimap.isPointOn(tPoint, device)) {
            logger.debugLog("tap point is not on the minimap!", device);
            return new java.awt.Point(-1, -1);
        }

        return new java.awt.Point(translatedX, translatedY);
    }

    // Define constants for tile size and max distance
    private static final int TILE_SIZE = 10; // Each tile is 10x10 pixels
    private static final int MAX_DISTANCE = 10; // Maximum number of tiles you can pass through to the target

    public Rectangle MM2MS(Tile currentDestination, Tile endDestination) {
        // Calculate the difference in tile coordinates
        int tileOffsetX = endDestination.x - currentDestination.x;
        int tileOffsetY = endDestination.y - currentDestination.y;

        // Check if the target tile is within the allowable range of tiles
        if (Math.abs(tileOffsetX) > MAX_DISTANCE || Math.abs(tileOffsetY) > MAX_DISTANCE) {
            return null;  // Return null if the destination is too far
        }

        // Calculate the pixel coordinates of the target tile relative to the player
        int pixelX = tileOffsetX * TILE_SIZE;  // How many pixels we need to move on the x-axis
        int pixelY = tileOffsetY * TILE_SIZE;  // How many pixels we need to move on the y-axis

        // Return the rectangle representing the 10x10 pixel area for the target tile
        return new Rectangle(pixelX, pixelY, TILE_SIZE, TILE_SIZE);
    }
}
