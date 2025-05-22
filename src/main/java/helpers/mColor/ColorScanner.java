package helpers.mColor;

import helpers.GetGameView;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static helpers.mColor.utils.RectangleHelper.filterContainedRectangles;

/*
Example stuff.

 WaterColor MColor(12030823, 35, 0.20, 0.91);

 FishSpotColors
    MColor(15389123, 2, 0.86, 1.88),
    MColor(14399121, 4, 0.30, 2.30),
    MColor(14727323, 4, 0.08, 0.68),
    MColor(11439730, 5, 0.14, 0.32),
    MColor(14795946, 5, 0.12, 3.15)

 */

/**
 * Utility class for scanning and identifying specific colors within an image.
 * Provides methods for finding points that match given color settings within a specified area.
 */
public class ColorScanner {
    private final GetGameView getGameView;

    public ColorScanner(GetGameView getGameView) {
        this.getGameView = getGameView;
    }
    /**
     * Finds and returns a list of points in the entire image that match any color criteria in the list.
     *
     * @param colorSettingsList The list of color settings for matching.
     * @return A list of points where the colors match any criteria specified in colorSettingsList.
     */
    public List<Point> findColors(String device, List<MColor> colorSettingsList) {
        BufferedImage image = getGameView.getBuffered(device);
        return findColorsInArea(image, colorSettingsList, new Rectangle(0, 0, image.getWidth(), image.getHeight()));
    }

    /**
     * Finds and returns a list of points in the entire image that match any color criteria in the list.
     *
     * @param colorSettingsList The list of color settings for matching.
     * @return A list of points where the colors match any criteria specified in colorSettingsList.
     */
    public List<Rectangle> findColorsAsRect(String device, List<MColor> colorSettingsList) {
        BufferedImage image = getGameView.getBuffered(device);
        return groupPointsIntoRectangles(findColorsInArea(image, colorSettingsList, new Rectangle(0, 0, image.getWidth(), image.getHeight())), 1);
    }

    /**
     * Finds and returns a list of points in the specified area of the image that match any color criteria in the list.
     *
     * @param colorSettingsList The list of color settings for matching.
     * @param area          The area within the image to limit the search for matching colors.
     * @return A list of points where the colors match any criteria specified in colorSettingsList.
     */
    public List<Point> findColors(String device, List<MColor> colorSettingsList, Rectangle area) {
        BufferedImage image = getGameView.getBuffered(device);
        return findColorsInArea(image, colorSettingsList, area);
    }

    /**
     * Finds and returns a list of points in the specified area of the image that match any color criteria in the list.
     *
     * @param colorSettingsList The list of color settings for matching.
     * @param area          The area within the image to limit the search for matching colors.
     * @return A list of points where the colors match any criteria specified in colorSettingsList.
     */
    public List<Rectangle> findColorsAsRect(String device, List<MColor> colorSettingsList, Rectangle area) {
        BufferedImage image = getGameView.getBuffered(device);
        return groupPointsIntoRectangles(findColorsInArea(image, colorSettingsList, area), 5);
    }

    /**
     * Scans the specified area within the image for pixels matching any given color criteria.
     *
     * @param image         The image to scan for matching colors.
     * @param colorSettingsList The list of color settings for matching.
     * @param area          The area within the image to search for matching colors.
     * @return A list of points where the colors match any criteria specified in colorSettingsList.
     */
    private List<Point> findColorsInArea(BufferedImage image, List<MColor> colorSettingsList, Rectangle area) {
        List<Point> matchingPoints = new ArrayList<>();

        for (int y = area.y; y < area.y + area.height; y++) {
            for (int x = area.x; x < area.x + area.width; x++) {
                int pixelColor = image.getRGB(x, y);
                // Check if the color matches any of the specified colors
                for (MColor colorSettings : colorSettingsList) {
                    if (matchesColor(pixelColor, colorSettings)) {
                        matchingPoints.add(new Point(x, y));
                        break; // Exit inner loop if a match is found to avoid duplicate entries
                    }
                }
            }
        }

        return matchingPoints;
    }

    /**
     * Checks if a given pixel color matches the target color within specified tolerance,
     * hue, and saturation modifications.
     *
     * @param pixelColor    The color of the current pixel to be checked.
     * @param colorSettings The target color and matching parameters (tolerance, hue, and saturation).
     * @return true if the pixel color matches the target color criteria; false otherwise.
     */
    private boolean matchesColor(int pixelColor, MColor colorSettings) {
        Color targetColor = colorSettings.getColor();
        Color currentColor = new Color(pixelColor);

        // Calculate RGB differences for initial quick check
        int redDiff = Math.abs(targetColor.getRed() - currentColor.getRed());
        int greenDiff = Math.abs(targetColor.getGreen() - currentColor.getGreen());
        int blueDiff = Math.abs(targetColor.getBlue() - currentColor.getBlue());

        // If RGB differences are within tolerance, proceed to HSB comparison
        if (redDiff <= colorSettings.getTolerance() &&
                greenDiff <= colorSettings.getTolerance() &&
                blueDiff <= colorSettings.getTolerance()) {

            // Convert both colors to HSB
            float[] targetHSB = Color.RGBtoHSB(targetColor.getRed(), targetColor.getGreen(), targetColor.getBlue(), null);
            float[] currentHSB = Color.RGBtoHSB(currentColor.getRed(), currentColor.getGreen(), currentColor.getBlue(), null);

            // Calculate differences in hue and saturation
            float hueDiff = Math.abs(targetHSB[0] - currentHSB[0]);
            float satDiff = Math.abs(targetHSB[1] - currentHSB[1]);

            // Check if hue and saturation differences are within specified modifiers
            return hueDiff <= colorSettings.getHueMod() && satDiff <= colorSettings.getSatMod();
        }

        return false; // RGB difference too large, skip HSB comparison
    }

    /**
     * Helper method to convert list of points into bounding rectangles.
     * This method groups points that are close to each other and creates rectangles around them.
     *
     * @param points  The list of points where colors matched.
     * @param maxDistance The maximum distance between points to consider them part of the same group.
     * @return A list of rectangles representing grouped areas of matched points.
     */
    private List<Rectangle> groupPointsIntoRectangles(List<Point> points, int maxDistance) {
        List<Rectangle> rectangles = new ArrayList<>();
        List<List<Point>> clusters = clusterPoints(points, maxDistance);

        for (List<Point> cluster : clusters) {
            int minX = Integer.MAX_VALUE;
            int minY = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int maxY = Integer.MIN_VALUE;

            for (Point p : cluster) {
                minX = Math.min(minX, p.x);
                minY = Math.min(minY, p.y);
                maxX = Math.max(maxX, p.x);
                maxY = Math.max(maxY, p.y);
            }

            rectangles.add(new Rectangle(minX, minY, maxX - minX, maxY - minY));
        }
        rectangles = filterContainedRectangles(rectangles);

        return trimEachRectangle(rectangles);
    }

    private List<Rectangle> trimEachRectangle(List<Rectangle> rectangles) {
        List<Rectangle> trimmedRectangles = new ArrayList<>();

        for (Rectangle rect : rectangles) {
            int trimmedX = rect.x + 3;
            int trimmedY = rect.y + 3;
            int trimmedWidth = Math.max(0, rect.width - 6);  // Ensure width is not negative
            int trimmedHeight = Math.max(0, rect.height - 6); // Ensure height is not negative

            trimmedRectangles.add(new Rectangle(trimmedX, trimmedY, trimmedWidth, trimmedHeight));
        }

        return trimmedRectangles;
    }

    /**
     * Clusters points that are close to each other within a given distance.
     *
     * @param points The list of points to be clustered.
     * @param maxDistance The maximum distance between points to be considered in the same cluster.
     * @return A list of clusters, where each cluster is a list of points close to each other.
     */
    private List<List<Point>> clusterPoints(List<Point> points, int maxDistance) {
        List<List<Point>> clusters = new ArrayList<>();
        boolean[] visited = new boolean[points.size()];

        for (int i = 0; i < points.size(); i++) {
            if (!visited[i]) {
                List<Point> cluster = new ArrayList<>();
                clusterPointsRecursive(points, visited, i, cluster, maxDistance);
                clusters.add(cluster);
            }
        }

        return clusters;
    }

    /**
     * Recursively finds all points close to the given point and adds them to the cluster.
     *
     * @param points The list of points to search within.
     * @param visited An array to track visited points.
     * @param index The index of the current point in the points list.
     * @param cluster The current cluster being formed.
     * @param maxDistance The maximum distance between points in the same cluster.
     */
    private void clusterPointsRecursive(List<Point> points, boolean[] visited, int index, List<Point> cluster, int maxDistance) {
        visited[index] = true;
        cluster.add(points.get(index));

        for (int i = 0; i < points.size(); i++) {
            if (!visited[i]) {
                Point p1 = points.get(index);
                Point p2 = points.get(i);

                // Only cluster points that are within maxDistance in horizontal or vertical directions
                boolean isAdjacent = (p1.distance(p2) <= maxDistance) &&
                        ((Math.abs(p1.x - p2.x) <= maxDistance && p1.y == p2.y) ||
                                (Math.abs(p1.y - p2.y) <= maxDistance && p1.x == p2.x));

                if (isAdjacent) {
                    clusterPointsRecursive(points, visited, i, cluster, maxDistance);
                }
            }
        }
    }

    public List<Rectangle> findColorsAsRectNEW(String device, List<MColor> colorSettingsList) {
        BufferedImage image = getGameView.getBuffered(device);
        return groupPointsIntoRectanglesNEW(findColorsInArea(image, colorSettingsList, new Rectangle(0, 0, image.getWidth(), image.getHeight())), 1);
    }

    private List<Rectangle> groupPointsIntoRectanglesNEW(List<Point> points, int maxDistance) {
        List<Rectangle> rectangles = new ArrayList<>();
        List<List<Point>> clusters = clusterPoints(points, maxDistance);

        for (List<Point> cluster : clusters) {
            int minX = Integer.MAX_VALUE;
            int minY = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int maxY = Integer.MIN_VALUE;

            for (Point p : cluster) {
                minX = Math.min(minX, p.x);
                minY = Math.min(minY, p.y);
                maxX = Math.max(maxX, p.x);
                maxY = Math.max(maxY, p.y);
            }

            rectangles.add(new Rectangle(minX, minY, maxX - minX, maxY - minY));
        }

        rectangles = filterContainedRectangles(rectangles);
        rectangles = mergeAndTrimRectangles(rectangles, 40, 40); // Merge small rectangles into larger ones
        rectangles = removeFakeRectangles(rectangles); // Remove fake rectangles
        rectangles = enforceMaxTwoSpotsPerRadius(rectangles, 100); // Ensure max 2 spots per radius

        return rectangles;
    }

    private List<Rectangle> mergeAndTrimRectangles(List<Rectangle> rectangles, int maxWidth, int maxHeight) {
        List<Rectangle> mergedRectangles = new ArrayList<>();
        boolean[] merged = new boolean[rectangles.size()];

        for (int i = 0; i < rectangles.size(); i++) {
            if (merged[i]) continue;

            Rectangle base = rectangles.get(i);
            Rectangle combined = new Rectangle(base);

            for (int j = i + 1; j < rectangles.size(); j++) {
                if (merged[j]) continue;

                Rectangle candidate = rectangles.get(j);

                // Check if rectangles are adjacent or overlapping and can be merged
                if (canMerge(combined, candidate, maxWidth, maxHeight)) {
                    combined = combined.union(candidate); // Combine the two rectangles
                    merged[j] = true; // Mark as merged
                }
            }

            // Ensure the merged rectangle still adheres to size constraints
            if (combined.width <= maxWidth && combined.height <= maxHeight) {
                mergedRectangles.add(combined);
            } else {
                // Split oversized rectangle into smaller valid parts
                mergedRectangles.addAll(splitOversizedRectangle(combined, maxWidth, maxHeight));
            }
        }

        return trimEachRectangleNEW(mergedRectangles); // Apply trimming after merging
    }

    private boolean canMerge(Rectangle rect1, Rectangle rect2, int maxWidth, int maxHeight) {
        // Combine rectangles
        Rectangle combined = rect1.union(rect2);

        // Ensure the combined rectangle does not exceed max dimensions
        return combined.width <= maxWidth && combined.height <= maxHeight;
    }

    private List<Rectangle> splitOversizedRectangle(Rectangle rect, int maxWidth, int maxHeight) {
        List<Rectangle> splitRectangles = new ArrayList<>();

        // Split horizontally
        for (int y = rect.y; y < rect.y + rect.height; y += maxHeight) {
            for (int x = rect.x; x < rect.x + rect.width; x += maxWidth) {
                int width = Math.min(maxWidth, rect.x + rect.width - x);
                int height = Math.min(maxHeight, rect.y + rect.height - y);
                splitRectangles.add(new Rectangle(x, y, width, height));
            }
        }

        return splitRectangles;
    }

    private List<Rectangle> trimEachRectangleNEW(List<Rectangle> rectangles) {
        List<Rectangle> trimmedRectangles = new ArrayList<>();

        for (Rectangle rect : rectangles) {
            int trimmedX = rect.x + 8;
            int trimmedY = rect.y + 8;
            int trimmedWidth = Math.max(0, rect.width - 16);  // Ensure width is not negative
            int trimmedHeight = Math.max(0, rect.height - 16); // Ensure height is not negative

            // Only add valid rectangles
            if (trimmedWidth > 0 && trimmedHeight > 0) {
                trimmedRectangles.add(new Rectangle(trimmedX, trimmedY, trimmedWidth, trimmedHeight));
            }
        }

        return trimmedRectangles;
    }

    private List<Rectangle> removeFakeRectangles(List<Rectangle> rectangles) {
        List<Rectangle> cleanedRectangles = new ArrayList<>();
        Set<Rectangle> toRemove = new HashSet<>();

        for (int i = 0; i < rectangles.size(); i++) {
            Rectangle current = rectangles.get(i);

            for (int j = 0; j < rectangles.size(); j++) {
                if (i == j) continue;

                Rectangle other = rectangles.get(j);

                // Check if rectangles overlap or are very close
                if (isCloseOrOverlapping(current, other)) {
                    // Determine which rectangle is "fake" based on size and position
                    if (current.width * current.height <= other.width * other.height) {
                        toRemove.add(current); // Mark smaller rectangle for removal
                    } else {
                        toRemove.add(other); // Mark the other rectangle for removal
                    }
                }
            }
        }

        // Add rectangles that are not marked for removal
        for (Rectangle rect : rectangles) {
            if (!toRemove.contains(rect)) {
                cleanedRectangles.add(rect);
            }
        }

        return cleanedRectangles;
    }

    private boolean isCloseOrOverlapping(Rectangle rect1, Rectangle rect2) {
        // Expand rect1 slightly to include "closeness" criteria
        Rectangle expanded = new Rectangle(
                rect1.x - 3, rect1.y - 3, rect1.width + 6, rect1.height + 6
        );

        // Check if rect2 intersects with the expanded rect1
        return expanded.intersects(rect2);
    }

    private List<Rectangle> enforceMaxTwoSpotsPerRadius(List<Rectangle> rectangles, int radius) {
        List<Rectangle> cleanedRectangles = new ArrayList<>();
        List<List<Rectangle>> groups = groupRectanglesByProximity(rectangles, radius);

        for (List<Rectangle> group : groups) {
            if (group.size() <= 2) {
                cleanedRectangles.addAll(group); // Keep groups with 2 or fewer rectangles
            } else {
                // Sort by area (width * height) in descending order
                group.sort((r1, r2) -> Integer.compare(r2.width * r2.height, r1.width * r1.height));
                cleanedRectangles.add(group.get(0)); // Add the largest rectangle
                cleanedRectangles.add(group.get(1)); // Add the second-largest rectangle
            }
        }

        return cleanedRectangles;
    }

    private List<List<Rectangle>> groupRectanglesByProximity(List<Rectangle> rectangles, int radius) {
        List<List<Rectangle>> groups = new ArrayList<>();
        boolean[] visited = new boolean[rectangles.size()];

        for (int i = 0; i < rectangles.size(); i++) {
            if (visited[i]) continue;

            List<Rectangle> group = new ArrayList<>();
            Rectangle base = rectangles.get(i);

            for (int j = 0; j < rectangles.size(); j++) {
                if (i == j || visited[j]) continue;

                Rectangle other = rectangles.get(j);

                // Check if the rectangles are within the given radius
                if (isWithinRadius(base, other, radius)) {
                    group.add(other);
                    visited[j] = true;
                }
            }

            group.add(base); // Add the base rectangle
            visited[i] = true;
            groups.add(group);
        }

        return groups;
    }

    private boolean isWithinRadius(Rectangle rect1, Rectangle rect2, int radius) {
        int centerX1 = rect1.x + rect1.width / 2;
        int centerY1 = rect1.y + rect1.height / 2;
        int centerX2 = rect2.x + rect2.width / 2;
        int centerY2 = rect2.y + rect2.height / 2;

        // Calculate Euclidean distance between the centers
        int distance = (int) Math.sqrt(Math.pow(centerX1 - centerX2, 2) + Math.pow(centerY1 - centerY2, 2));
        return distance <= radius;
    }

}
