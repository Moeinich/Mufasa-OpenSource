package osr.utils;

import helpers.utils.Tile;

import java.awt.*;
import java.util.Random;

public class NamedArea extends Rectangle {
    private String name;
    public Tile topTile;
    public Tile bottomTile;
    private final Random random = new Random();

    // Fixed margin to trim from each side
    private static final int FIXED_MARGIN = 10;

    // Maximum number of attempts for rejection sampling
    private static final int MAX_ATTEMPTS = 1000;

    public NamedArea(String name, Tile topTile, Tile bottomTile) {
        this.name = name;
        this.topTile = topTile;
        this.bottomTile = bottomTile;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Generates and returns a random Tile within the area defined by topTile and bottomTile,
     * trimming a fixed margin from the edges and favoring the center.
     *
     * @return A randomly generated Tile within the trimmed and weighted area.
     */
    public Tile getRandomTileWithinArea() {
        // Determine the minimum and maximum x coordinates after trimming
        int minX = Math.min(topTile.x, bottomTile.x) + FIXED_MARGIN;
        int maxX = Math.max(topTile.x, bottomTile.x) - FIXED_MARGIN;

        // Determine the minimum and maximum y coordinates after trimming
        int minY = Math.min(topTile.y, bottomTile.y) + FIXED_MARGIN;
        int maxY = Math.max(topTile.y, bottomTile.y) - FIXED_MARGIN;

        // Calculate the center of the trimmed area
        int centerX = (minX + maxX) / 2;
        int centerY = (minY + maxY) / 2;

        // Calculate the maximum distance from the center within the trimmed area
        double maxDistance = Math.sqrt(Math.pow(centerX - minX, 2) + Math.pow(centerY - minY, 2));

        int attempts = 0;
        while (attempts < MAX_ATTEMPTS) {
            // Generate random x and y within the trimmed boundaries
            int randomX = getRandomIntInRange(minX, maxX);
            int randomY = getRandomIntInRange(minY, maxY);

            // Calculate the distance from the random point to the center
            double distance = Math.sqrt(Math.pow(randomX - centerX, 2) + Math.pow(randomY - centerY, 2));

            // Calculate acceptance probability (higher for points closer to center)
            double acceptanceProbability = 1.0 - (distance / maxDistance);

            // Generate a random number between 0 and 1
            double randomProbability = random.nextDouble();

            // Accept the point based on the acceptance probability
            if (randomProbability <= acceptanceProbability) {
                return new Tile(randomX, randomY, topTile.z);
            }

            attempts++;
        }

        // If no tile is accepted after MAX_ATTEMPTS, return the center tile as fallback
        return new Tile(centerX, centerY, topTile.z);
    }

    /**
     * Generates a random integer between min (inclusive) and max (inclusive).
     *
     * @param min The minimum value.
     * @param max The maximum value.
     * @return A random integer between min and max.
     */
    private int getRandomIntInRange(int min, int max) {
        if (min > max) {
            throw new IllegalArgumentException("min cannot be greater than max.");
        }
        // The +1 ensures that max is inclusive
        return random.nextInt((max - min) + 1) + min;
    }
}
