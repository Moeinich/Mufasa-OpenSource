package helpers.Color;

import helpers.GetGameView;
import helpers.ThreadManager;
import helpers.Color.utils.ColorPointPair;
import helpers.Color.utils.ColorRectanglePair;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ColorFinder {
    private final GetGameView getGameView;
    private final DBSCAN dbscan = new DBSCAN();
    private final ExecutorService executor = ThreadManager.getInstance().getUnifiedExecutor();

    public final List<Color> greenOverlay = Arrays.asList(
            Color.decode("#20ff25"),
            Color.decode("#7e9c7c"),
            Color.decode("#1fff25"),
            Color.decode("#456620"),
            Color.decode("#21ff27"),
            Color.decode("#21f124"),
            Color.decode("#20f625"),
            Color.decode("#29d325"),
            Color.decode("#22ff28"),
            Color.decode("#b8cd9b"),
            Color.decode("#b2c791"),
            Color.decode("#a1a95a"),
            Color.decode("#a6ac5e"),
            Color.decode("#20ff26"),
            Color.decode("#21ff26"),
            Color.decode("#29dd2c")
    );
    public final List<Color> blueOverlay = List.of(
            Color.decode("#25ffff") //from OverlayColor
    );
    public final List<Color> redOverlay = Arrays.asList(
            Color.decode("#ca8818"),
            Color.decode("#c98818"),
            Color.decode("#c98718"),
            Color.decode("#d38f1a"),
            Color.decode("#cb8a19")
    );

    public ColorFinder(GetGameView getGameView) {
        this.getGameView = getGameView;
    }

    public boolean isRedTinted(String device, Rectangle checkRectangle, double threshold) {
        // Get the sub-image from the specified rectangle
        BufferedImage image = getGameView.getSubBuffered(device, checkRectangle);

        long totalRed = 0;
        long totalGreen = 0;
        long totalBlue = 0;
        int width = image.getWidth();
        int height = image.getHeight();
        int totalPixels = width * height;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);

                // Extract red, green, and blue components
                int red = (rgb >> 16) & 0xFF;
                int green = (rgb >> 8) & 0xFF;
                int blue = rgb & 0xFF;

                totalRed += red;
                totalGreen += green;
                totalBlue += blue;
            }
        }

        // Calculate average intensities
        double avgRed = totalRed / (double) totalPixels;
        double avgGreen = totalGreen / (double) totalPixels;
        double avgBlue = totalBlue / (double) totalPixels;

        // Calculate red intensity ratio
        double redRatio = avgRed / ((avgGreen + avgBlue) / 2.0);

        // Debugging log for calculated values
        System.out.printf("AvgRed: %.2f, AvgGreen: %.2f, AvgBlue: %.2f, RedRatio: %.2f%n", avgRed, avgGreen, avgBlue, redRatio);

        // Return true if red ratio exceeds the threshold
        return redRatio > threshold;
    }

    public boolean isBlackTinted(String device, Rectangle checkRectangle, double threshold) {
        // Get the sub-image from the specified rectangle
        BufferedImage image = getGameView.getSubBuffered(device, checkRectangle);

        long totalIntensity = 0;
        int width = image.getWidth();
        int height = image.getHeight();
        int totalPixels = width * height;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);

                // Extract red, green, and blue components
                int red = (rgb >> 16) & 0xFF;
                int green = (rgb >> 8) & 0xFF;
                int blue = rgb & 0xFF;

                // Calculate intensity (average of red, green, and blue)
                int intensity = (red + green + blue) / 3;
                totalIntensity += intensity;
            }
        }

        // Calculate average intensity
        double avgIntensity = totalIntensity / (double) totalPixels;

        // Debugging log for calculated values
        System.out.printf("AvgIntensity: %.2f, Threshold: %.2f%n", avgIntensity, threshold);

        // Return true if average intensity is less than the threshold
        return avgIntensity < threshold;
    }

    public List<Rectangle> findNPCs(String device, List<Color> targetColors, int tolerance, double eps, int minPts) {
        BufferedImage image = getGameView.getBuffered(device);
        if (image == null) {
            System.out.println("Failed to retrieve image from device: " + device);
            return Collections.emptyList();
        }

        System.out.println("Starting NPC detection on device: " + device);
        System.out.println("Target colors: " + targetColors);
        System.out.println("Tolerance: " + tolerance);

        // Search for all matching points in the entire image
        Point point1 = new Point(0, 0);
        Point point2 = new Point(image.getWidth() - 1, image.getHeight() - 1);
        List<Point> allMatchingPoints = searchForColorsInImage(image, targetColors, tolerance, point1, point2);

        System.out.println("Total matching points found: " + allMatchingPoints.size());

        if (allMatchingPoints.isEmpty()) {
            System.out.println("No matching colors found in the image.");
            return Collections.emptyList();
        }

        // Apply DBSCAN clustering to group nearby points
        List<Set<Point>> clusters = dbscan.applyDBSCAN(allMatchingPoints, eps, minPts);

        System.out.println("Total clusters found: " + clusters.size());

        if (clusters.isEmpty()) {
            System.out.println("No clusters found with the specified parameters.");
            return Collections.emptyList();
        }

        // Verify each cluster contains at least 70% of target colors
        List<Rectangle> npcRectangles = new ArrayList<>();
        int npcCount = 0;
        for (Set<Point> cluster : clusters) {
            Set<Color> clusterColors = getDistinctColorsInCluster(cluster, image, tolerance, targetColors);
            System.out.println("Cluster size: " + cluster.size() + ", Distinct colors found: " + clusterColors);

            // Calculate how many target colors are needed (70% of total)
            int requiredMatches = (int) Math.ceil(targetColors.size() * 0.7);

            // Count how many target colors are present in the cluster
            long matchingColorsCount = clusterColors.stream().filter(targetColors::contains).count();

            // Check if the cluster has at least 70% of the target colors
            if (matchingColorsCount >= requiredMatches) {
                Rectangle boundingBox = getBoundingBox(cluster);
                npcRectangles.add(boundingBox);
                npcCount++;
                System.out.println("NPC " + npcCount + " detected at: " + boundingBox);
            }
        }

        System.out.println("NPC detection completed. Total NPCs found: " + npcRectangles.size());

        return npcRectangles;
    }

    /**
     * Retrieves the distinct colors present in a cluster.
     *
     * @param cluster      The set of points in the cluster.
     * @param image        The BufferedImage being analyzed.
     * @param tolerance    The tolerance for color matching.
     * @return A set of distinct colors found in the cluster.
     */
    private Set<Color> getDistinctColorsInCluster(Set<Point> cluster, BufferedImage image, int tolerance, List<Color> targetColors) {
        Set<Color> distinctColors = new HashSet<>();
        for (Point point : cluster) {
            int rgb = image.getRGB(point.x, point.y);
            // Check if this color matches any target color within tolerance
            for (Color targetColor : targetColors) { // You may need to pass targetColors as a parameter
                if (isColorMatch(rgb, targetColor, tolerance)) {
                    distinctColors.add(targetColor);
                    break;
                }
            }
        }
        return distinctColors;
    }

    public List<Point> findColorAtPosition(String device, Color targetColor, Point point1, Point point2, int tolerance) {
        return searchForColorsInImage(getGameView.getBuffered(device), List.of(targetColor), tolerance, point1, point2);
    }

    public boolean isColorAtPoint(String device, Color targetColor, Point pointToSearch, int tolerance) {
        BufferedImage bufferedImage = getGameView.getBuffered(device);
        // Ensure the point is within the bounds of the image
        if (pointToSearch.x >= 0 && pointToSearch.x < bufferedImage.getWidth() &&
                pointToSearch.y >= 0 && pointToSearch.y < bufferedImage.getHeight()) {

            int pixelColor = bufferedImage.getRGB(pointToSearch.x, pointToSearch.y);
            return isColorMatch(pixelColor, targetColor, tolerance);
        }

        // Return false if the point is outside the image bounds
        return false;
    }

    public boolean isAnyColorInRect(String device, List<Color> targetColors, Rectangle rect, int tolerance) {
        BufferedImage bufferedImage = getGameView.getBuffered(device);
        return isAnyColorInSingleRect(bufferedImage, targetColors, rect, tolerance);
    }

    public boolean isAnyColorInRect(String device, Set<Integer> targetColors, Rectangle rect, int tolerance) {
        BufferedImage bufferedImage = getGameView.getBuffered(device);
        return isAnyColorInSingleRect(bufferedImage, targetColors, rect, tolerance);
    }

    public boolean areAllColorsInPairs(String device, List<ColorRectanglePair> colorRectPairs, int tolerance) {
        BufferedImage bufferedImage = getGameView.getBuffered(device);
        for (ColorRectanglePair pair : colorRectPairs) {
            if (!isAnyColorInSingleRect(bufferedImage, pair.getColors(), pair.getRectangle(), tolerance)) {
                return false; // Return false if any pair does not match
            }
        }
        return true;
    }

    public boolean areAllPixelsMatchingInPair(String device, List<ColorPointPair> colorPointPairs) {
        for (ColorPointPair pair : colorPointPairs) {
            if (!isPixelColor(device, pair.getPoint(), pair.getColor(), pair.getTolerance())) {
                return false;
            }
        }
        return true;
    }

    public boolean isAnyColorInRects(String device, List<Color> targetColors, List<Rectangle> rects, int tolerance) {
        BufferedImage bufferedImage = getGameView.getBuffered(device);

        // Loop through each rectangle in the list and use the helper method
        for (Rectangle rect : rects) {
            if (isAnyColorInSingleRect(bufferedImage, targetColors, rect, tolerance)) {
                return true; // Return true if any match is found
            }
        }

        return false; // Return false if no match is found in any rectangle
    }

    // Helper method to handle color matching in a single rectangle
    private boolean isAnyColorInSingleRect(BufferedImage bufferedImage, List<Color> targetColors, Rectangle rect, int tolerance) {
        if (bufferedImage == null) {
            return false;
        }

        for (int x = rect.x; x < rect.x + rect.width; x++) {
            for (int y = rect.y; y < rect.y + rect.height; y++) {
                int pixelColor = bufferedImage.getRGB(x, y);

                // Loop through each target color to check for a match
                for (Color targetColor : targetColors) {
                    if (isColorMatch(pixelColor, targetColor, tolerance)) {
                        return true;
                    }
                }
            }
        }
        return false; // No match found in this rectangle
    }

    private boolean isAnyColorInSingleRect(BufferedImage bufferedImage, Set<Integer> targetColors, Rectangle rect, int tolerance) {
        // Ensure the rectangle is fully within the image bounds
        int startX = Math.max(0, rect.x);
        int startY = Math.max(0, rect.y);
        int endX   = Math.min(rect.x + rect.width,  bufferedImage.getWidth());
        int endY   = Math.min(rect.y + rect.height, bufferedImage.getHeight());

        if (endX <= startX || endY <= startY) {
            return false;
        }

        for (int x = startX; x < endX; x++) {
            for (int y = startY; y < endY; y++) {
                // Mask off the alpha channel so we only compare RGB
                int pixelColor = 0xFFFFFF & bufferedImage.getRGB(x, y);

                // Check all target colors
                for (int targetColor : targetColors) {
                    if (isColorMatch(pixelColor, targetColor, tolerance)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean isAllColorsInRect(String device, List<Color> targetColors, Rectangle rect, int tolerance) {
        BufferedImage bufferedImage = getGameView.getBuffered(device);
        Set<Color> foundColors = new HashSet<>();

        for (int x = rect.x; x < rect.x + rect.width; x++) {
            for (int y = rect.y; y < rect.y + rect.height; y++) {
                int pixelColor = bufferedImage.getRGB(x, y);
                for (Color targetColor : targetColors) {
                    if (isColorMatch(pixelColor, targetColor, tolerance)) {
                        foundColors.add(targetColor);
                    }
                }
            }
        }

        return foundColors.containsAll(targetColors);
    }

    public boolean isColorInRect(String device, Color targetColor, Rectangle rect, int tolerance) {
        BufferedImage bufferedImage = getGameView.getBuffered(device);

        // Now proceed with checking each pixel color within the rectangle
        for (int x = rect.x; x < rect.x + rect.width; x++) {
            for (int y = rect.y; y < rect.y + rect.height; y++) {
                int pixelColor = bufferedImage.getRGB(x, y);

                if (isColorMatch(pixelColor, targetColor, tolerance)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean performPlayerCheck(String device,
                                      List<ColorPointPair> blackChecks,
                                      List<ColorPointPair> topColorChecks,
                                      List<ColorPointPair> whiteChecks) {
        BufferedImage bufferedImage = getGameView.getBuffered(device);

        // Check if all blackChecks match
        for (ColorPointPair pair : blackChecks) {
            if (!isPixelColor(bufferedImage, pair)) {
                return false; // If any blackCheck fails, condition is not satisfied.
            }
        }

        // Check if any topColorChecks match
        for (ColorPointPair pair : topColorChecks) {
            if (isPixelColor(bufferedImage, pair)) {
                return false; // If any topColorCheck matches, condition is not satisfied.
            }
        }

        // Check if all whiteChecks match
        for (ColorPointPair pair : whiteChecks) {
            if (!isPixelColor(bufferedImage, pair)) {
                return false; // If any whiteCheck fails, condition is not satisfied.
            }
        }

        return true; // All conditions satisfied.
    }

    private boolean isPixelColor(BufferedImage bufferedImage, ColorPointPair pair) {
        Point pixel = pair.getPoint();
        int targetRGB = pair.getColor().getRGB();
        int pixelRGB = bufferedImage.getRGB(pixel.x, pixel.y);

        return isRGBWithinTolerance(pixelRGB, targetRGB, pair.getTolerance());
    }

    public boolean isPixelPairMatching(String device, List<ColorPointPair> colorPointPairs) {
        BufferedImage bufferedImage = getGameView.getBuffered(device);

        for (ColorPointPair pair : colorPointPairs) {
            Point pixel = pair.getPoint();
            int targetRGB = pair.getColor().getRGB(); // Get the target color's RGB value
            int tolerance = pair.getTolerance();

            int pixelRGB = bufferedImage.getRGB(pixel.x, pixel.y);

            if (isRGBWithinTolerance(pixelRGB, targetRGB, tolerance)) {
                return true; // Return true as soon as one pair matches.
            }
        }
        return false; // Return false if no pairs match.
    }

    public boolean isPixelColor(String device, Point pixel, Color targetColor, int tolerance) {
        BufferedImage bufferedImage = getGameView.getBuffered(device);

        int pixelRGB = bufferedImage.getRGB(pixel.x, pixel.y);
        int targetRGB = targetColor.getRGB(); // Get the target color's RGB value

        return isRGBWithinTolerance(pixelRGB, targetRGB, tolerance);
    }

    private boolean isRGBWithinTolerance(int pixelRGB, int targetRGB, int tolerance) {
        int pixelRed = (pixelRGB >> 16) & 0xFF;
        int pixelGreen = (pixelRGB >> 8) & 0xFF;
        int pixelBlue = pixelRGB & 0xFF;

        int targetRed = (targetRGB >> 16) & 0xFF;
        int targetGreen = (targetRGB >> 8) & 0xFF;
        int targetBlue = targetRGB & 0xFF;

        return Math.abs(pixelRed - targetRed) <= tolerance &&
                Math.abs(pixelGreen - targetGreen) <= tolerance &&
                Math.abs(pixelBlue - targetBlue) <= tolerance;
    }


    public List<Point> searchForColorsWithExclusions(
            BufferedImage bImage, List<Color> colors, int tolerance,
            List<Color> exclusionColors, int exclusionTolerance, Point point1, Point point2) {

        int minX = Math.min(point1.x, point2.x);
        int minY = Math.min(point1.y, point2.y);
        int maxX = Math.max(point1.x, point2.x);
        int maxY = Math.max(point1.y, point2.y);

        List<Point> allMatchingPoints = Collections.synchronizedList(new ArrayList<>());

        // Collect Futures
        List<Future<?>> futures = new ArrayList<>();

        for (int x = minX; x <= maxX; x++) {
            int finalX = x;
            futures.add(executor.submit(() -> {
                for (int y = minY; y <= maxY; y++) {
                    // Check for interruption
                    if (Thread.currentThread().isInterrupted()) {
                        return; // Exit early if interrupted
                    }

                    int pixel = bImage.getRGB(finalX, y);
                    if (!isExcluded(pixel, exclusionColors, exclusionTolerance)) {
                        for (Color color : colors) {
                            if (isColorMatch(pixel, color, tolerance)) {
                                synchronized (allMatchingPoints) {
                                    allMatchingPoints.add(new Point(finalX, y));
                                }
                                break; // Exit the color loop early if a match is found
                            }
                        }
                    }
                }
            }));
        }

        // Wait for all tasks to complete
        for (Future<?> future : futures) {
            try {
                future.get(); // Blocks until the task is done
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Restore interrupt status
                return Collections.emptyList();
            } catch (ExecutionException e) {
                e.printStackTrace(); // Log other exceptions
            }
        }

        return allMatchingPoints;
    }

    public List<Point> searchForColorsInImage(BufferedImage bImage, List<Color> colors, int tolerance, Point point1, Point point2) {
        int minX = Math.min(point1.x, point2.x);
        int minY = Math.min(point1.y, point2.y);
        int maxX = Math.max(point1.x, point2.x);
        int maxY = Math.max(point1.y, point2.y);

        List<Point> allMatchingPoints = Collections.synchronizedList(new ArrayList<>());

        // Submit task and store the Future
        Future<?> future = executor.submit(() ->
                IntStream.range(minX, maxX + 1).forEach(x -> {
                    // Check for interruption before processing each row
                    if (Thread.currentThread().isInterrupted()) {
                        return; // Exit the stream early if interrupted
                    }
                    IntStream.range(minY, maxY + 1).forEach(y -> {
                        if (Thread.currentThread().isInterrupted()) {
                            return; // Exit the stream early if interrupted
                        }
                        int pixel = bImage.getRGB(x, y);
                        for (Color color : colors) {
                            if (isColorMatch(pixel, color, tolerance)) {
                                allMatchingPoints.add(new Point(x, y));
                                break;
                            }
                        }
                    });
                })
        );

        // Wait for the task to complete
        try {
            future.get(); // This blocks until the task is done
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Collections.emptyList(); // Return an empty list
        } catch (ExecutionException e) {
            e.printStackTrace(); // Log other exceptions
        }

        return allMatchingPoints;
    }

    public List<Point> processColorPoints(String deviceID, List<Color> colors, int tolerance) {
        return processColorPoints(deviceID, colors, tolerance, null, 0);
    }

    public List<Point> processColorPoints(String deviceID, List<Color> colors, int tolerance, List<Color> exclusionColors, int exclusionTolerance) {
        BufferedImage bImage = getGameView.getBuffered(deviceID);
        Point point1 = new Point(0, 0);
        Point point2 = new Point(bImage.getWidth() - 1, bImage.getHeight() - 1);

        // Use the method with exclusions if exclusion colors are provided
        if (exclusionColors != null) {
            return searchForColorsWithExclusions(bImage, colors, tolerance, exclusionColors, exclusionTolerance, point1, point2);
        } else {
            return searchForColorsInImage(bImage, colors, tolerance, point1, point2);
        }
    }

    public List<Point> processColorPointsInRect(String deviceID, List<Color> colors, Rectangle searchArea, int tolerance) {
        return processColorPointsInRect(deviceID, colors, searchArea, tolerance, null, 0);
    }

    public List<Point> processColorPointsInRect(String deviceID, List<Color> colors, Rectangle searchArea, int tolerance, List<Color> exclusionColors, int exclusionTolerance) {
        BufferedImage bImage = getGameView.getBuffered(deviceID);

        // Calculate top-left and bottom-right points based on the search area
        Point point1 = new Point(searchArea.x, searchArea.y);
        Point point2 = new Point(searchArea.x + searchArea.width - 1, searchArea.y + searchArea.height - 1);

        // Use the method with exclusions if exclusion colors are provided
        if (exclusionColors != null) {
            return searchForColorsWithExclusions(bImage, colors, tolerance, exclusionColors, exclusionTolerance, point1, point2);
        } else {
            return searchForColorsInImage(bImage, colors, tolerance, point1, point2);
        }
    }

    public List<Rectangle> processColorClusters(String deviceID, List<Color> colors, int tolerance) {
        return processColorClusters(deviceID, colors, tolerance, null, 0);
    }

    public List<Rectangle> processColorClusters(String deviceID, List<Color> colors, int tolerance, List<Color> exclusionColors, int exclusionTolerance) {
        BufferedImage bImage = getGameView.getBuffered(deviceID);
        List<Point> allMatchingPoints;

        if (exclusionColors != null) {
            allMatchingPoints = searchForColorsWithExclusions(bImage, colors, tolerance, exclusionColors, exclusionTolerance, new Point(0, 0), new Point(bImage.getWidth() - 1, bImage.getHeight() - 1));
        } else {
            allMatchingPoints = searchForColorsInImage(bImage, colors, tolerance, new Point(0, 0), new Point(bImage.getWidth() - 1, bImage.getHeight() - 1));
        }

        List<Set<Point>> clusters = dbscan.applyDBSCAN(allMatchingPoints, 10.0, 20);

        return clusters.stream().map(this::getBoundingBox).collect(Collectors.toList());
    }

    public List<Rectangle> processColorClustersInRect(String deviceID, List<Color> colors, int tolerance, Rectangle searchArea, double eps, int minPts) {
        return processColorClustersInRect(deviceID, colors, tolerance, searchArea, null, 0, eps, minPts);
    }

    public List<Rectangle> processColorClustersInRect(String deviceID, List<Color> colors, int tolerance, Rectangle searchArea, List<Color> exclusionColors, int exclusionTolerance, double eps, int minPts) {
        BufferedImage bImage = getGameView.getBuffered(deviceID);

        // Adjust the search area coordinates to match the top-left and bottom-right points
        Point point1 = new Point(searchArea.x, searchArea.y);
        Point point2 = new Point(searchArea.x + searchArea.width - 1, searchArea.y + searchArea.height - 1);

        List<Point> allMatchingPoints;

        if (exclusionColors != null) {
            allMatchingPoints = searchForColorsWithExclusions(bImage, colors, tolerance, exclusionColors, exclusionTolerance, point1, point2);
        } else {
            allMatchingPoints = searchForColorsInImage(bImage, colors, tolerance, point1, point2);
        }

        List<Set<Point>> clusters = dbscan.applyDBSCAN(allMatchingPoints, eps, minPts);
        return clusters.stream().map(this::getBoundingBox).collect(Collectors.toList());
    }

    private Rectangle getBoundingBox(Set<Point> cluster) {
        int minX = cluster.stream().mapToInt(p -> p.x).min().orElse(0);
        int minY = cluster.stream().mapToInt(p -> p.y).min().orElse(0);
        int maxX = cluster.stream().mapToInt(p -> p.x).max().orElse(0);
        int maxY = cluster.stream().mapToInt(p -> p.y).max().orElse(0);

        return new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    private boolean isColorMatch(int rgb, Color target, int tolerance) {
        int r1 = (rgb >> 16) & 0xFF;
        int g1 = (rgb >> 8) & 0xFF;
        int b1 = rgb & 0xFF;

        return Math.abs(r1 - target.getRed()) <= tolerance &&
                Math.abs(g1 - target.getGreen()) <= tolerance &&
                Math.abs(b1 - target.getBlue()) <= tolerance;
    }

    private boolean isColorMatch(int colorA, int colorB, int tolerance) {
        int rA = (colorA >> 16) & 0xFF;
        int gA = (colorA >> 8)  & 0xFF;
        int bA = (colorA)       & 0xFF;

        int rB = (colorB >> 16) & 0xFF;
        int gB = (colorB >> 8)  & 0xFF;
        int bB = (colorB)       & 0xFF;

        return (Math.abs(rA - rB) <= tolerance) &&
                (Math.abs(gA - gB) <= tolerance) &&
                (Math.abs(bA - bB) <= tolerance);
    }

    private boolean isExcluded(int rgb, List<Color> exclusionColors, int tolerance) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;

        for (Color color : exclusionColors) {
            if (Math.abs(r - color.getRed()) <= tolerance &&
                    Math.abs(g - color.getGreen()) <= tolerance &&
                    Math.abs(b - color.getBlue()) <= tolerance) {
                return true;  // Return true if the color matches any exclusion color
            }
        }
        return false;
    }

    public List<Rectangle> findBankPinTiles(String device, List<Color> targetColors, Rectangle roi, int tileSize, int tolerance, double coverageThreshold) {
        BufferedImage image = getGameView.getBuffered(device);
        List<Rectangle> foundTiles = new ArrayList<>();

        int horizontalSpacing = 30;
        int verticalSpacing = 8;

        outerloop:
        for (int x = roi.x; x <= roi.x + roi.width - tileSize; x += 5) {
            for (int y = roi.y; y <= roi.y + roi.height - tileSize; y += 5) {
                Rectangle initialTile = new Rectangle(x, y, tileSize, tileSize);
                double coverage = calculateTileCoverage(image, initialTile, targetColors, tolerance);

                if (coverage >= coverageThreshold) {
                    System.out.println("Initial tile found at (" + x + ", " + y + ") with " + (coverage * 100) + "% coverage.");

                    if (!isTopLeftPixelMatch(image, initialTile, targetColors, tolerance)) {
                        initialTile = adjustToTopLeftColorMatch(image, initialTile, targetColors, tolerance);
                        if (initialTile == null) {
                            continue;
                        }
                    }

                    int startX = initialTile.x;
                    int startY = initialTile.y;

                    int[][] layout = {
                            {0, 1, 2},    // Top row (3 tiles, add 4th tile manually)
                            {0, 1, 2},    // Middle row
                            {0, 1, 2}     // Bottom row
                    };

                    Rectangle thirdTile = null;

                    // Calculate the positions for the first 9 tiles
                    for (int row = 0; row < layout.length; row++) {
                        for (int col : layout[row]) {
                            int tileX = startX + (tileSize + horizontalSpacing) * col;
                            int tileY = startY + (tileSize + verticalSpacing) * row;
                            Rectangle tile = new Rectangle(tileX, tileY, tileSize, tileSize);

                            double tileCoverage = calculateTileCoverage(image, tile, targetColors, tolerance);
                            System.out.println("Checking tile at (" + tileX + ", " + tileY + "): Coverage = " + (tileCoverage * 100) + "%");

                            if (tileCoverage >= coverageThreshold && isTopLeftPixelMatch(image, tile, targetColors, tolerance)) {
                                foundTiles.add(tile);
                                System.out.println("Confirmed tile at (" + tileX + ", " + tileY + ") with " + (tileCoverage * 100) + "% coverage.");

                                // Store the position of the third tile in the top row
                                if (row == 0 && col == 2) {
                                    thirdTile = tile;
                                }
                            }
                        }
                    }

                    // Manually add the fourth tile on the top row based on the third tile’s position
                    if (thirdTile != null) {
                        int fourthTileX = thirdTile.x + tileSize + 20; // 84 pixels to the right
                        int fourthTileY = thirdTile.y;
                        Rectangle fourthTile = new Rectangle(fourthTileX, fourthTileY, tileSize, tileSize);
                        foundTiles.add(fourthTile);
                        System.out.println("Manually added fourth tile at (" + fourthTileX + ", " + fourthTileY + ")");
                    }

                    break outerloop;
                }
            }
        }

        return foundTiles;
    }

    private boolean isTopLeftPixelMatch(BufferedImage image, Rectangle rect, List<Color> targetColors, int tolerance) {
        int topLeftColor = image.getRGB(rect.x, rect.y);
        return targetColors.stream().anyMatch(targetColor -> isColorMatch(topLeftColor, targetColor, tolerance));
    }

    private Rectangle adjustToTopLeftColorMatch(BufferedImage image, Rectangle rect, List<Color> targetColors, int tolerance) {
        for (int dx = 0; dx < rect.width; dx++) {
            for (int dy = 0; dy < rect.height; dy++) {
                int x = rect.x + dx;
                int y = rect.y + dy;
                int pixelColor = image.getRGB(x, y);

                if (targetColors.stream().anyMatch(targetColor -> isColorMatch(pixelColor, targetColor, tolerance))) {
                    // Return a new rectangle adjusted so that (x, y) is the top-left corner
                    return new Rectangle(x, y, rect.width, rect.height);
                }
            }
        }
        System.out.println("Failed to adjust top-left for tile at (" + rect.x + ", " + rect.y + ")");
        return null; // Return null if no adjustment is possible
    }

    // Method to calculate coverage within a 64x64 area
    private double calculateTileCoverage(BufferedImage image, Rectangle tileRect, List<Color> targetColors, int tolerance) {
        int matchingPixels = 0;
        int totalPixels = tileRect.width * tileRect.height;

        for (int x = tileRect.x; x < tileRect.x + tileRect.width; x++) {
            for (int y = tileRect.y; y < tileRect.y + tileRect.height; y++) {
                int pixelColor = image.getRGB(x, y);
                for (Color targetColor : targetColors) {
                    if (isColorMatch(pixelColor, targetColor, tolerance)) {
                        matchingPixels++;
                        break;
                    }
                }
            }
        }

        return (double) matchingPixels / totalPixels;
    }
}
