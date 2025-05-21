package UI.components;

import helpers.utils.Tile;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import osr.walker.Walker;

public class MM2MSProjection {
    private final Walker walker;

    private final int tileSize = 40; // Base tile size (before scaling)
    private final int rows = 10, cols = 10;

    // Isometric projection angles
    private final double isoAngle = Math.toRadians(30); // 30 degrees for isometric
    private final double cosIso = Math.cos(isoAngle);
    private final double sinIso = Math.sin(isoAngle);

    public MM2MSProjection(Walker walker) {
        this.walker = walker;
    }

    // Method to draw the grid with isometric projection
    public void drawGrid(String device, GraphicsContext gc) {
        gc.clearRect(0, 0, gc.getCanvas().getWidth(), gc.getCanvas().getHeight()); // Clear the canvas before drawing

        Tile playerPos = walker.getPlayerPosition(device).getTile(device); // Player's current tile position

        // Define the center of the canvas (where the player's tile should be)
        double centerX = gc.getCanvas().getWidth() / 2.0;
        double centerY = gc.getCanvas().getHeight() / 2.0;

        gc.setStroke(Color.GREEN);  // Set the grid color
        gc.setLineWidth(1);

        // Loop through the grid, centering it around the player's position
        for (int row = -rows / 2; row < rows / 2; row++) {
            for (int col = -cols / 2; col < cols / 2; col++) {

                // Calculate the tile's position relative to the player
                double relativeX = playerPos.x() + col;
                double relativeY = playerPos.y() + row;

                // Apply isometric projection
                double isoX = (relativeX - relativeY) * cosIso * tileSize;
                double isoY = (relativeX + relativeY) * sinIso * tileSize;

                // Convert the tile's position to screen coordinates (relative to the center)
                double tileCenterX = centerX + isoX;
                double tileCenterY = centerY + isoY;

                // Define the four corners of the tile in isometric projection
                double[] xPoints = new double[4];
                double[] yPoints = new double[4];

                // Top-left corner
                xPoints[0] = tileCenterX - (cosIso * tileSize) / 2.0;
                yPoints[0] = tileCenterY - (sinIso * tileSize) / 2.0;

                // Top-right corner
                xPoints[1] = tileCenterX + (cosIso * tileSize) / 2.0;
                yPoints[1] = tileCenterY - (sinIso * tileSize) / 2.0;

                // Bottom-right corner
                xPoints[2] = tileCenterX + (cosIso * tileSize) / 2.0;
                yPoints[2] = tileCenterY + (sinIso * tileSize) / 2.0;

                // Bottom-left corner
                xPoints[3] = tileCenterX - (cosIso * tileSize) / 2.0;
                yPoints[3] = tileCenterY + (sinIso * tileSize) / 2.0;

                // Draw the polygon representing the tile
                gc.strokePolygon(xPoints, yPoints, 4);

                // Optionally, label the tile coordinates
                String tileLabel = relativeX + ", " + relativeY;
                gc.setFill(Color.WHITE);
                gc.fillText(tileLabel, tileCenterX - 10, tileCenterY);
            }
        }
    }
}