package osr.mapping.utils;

import helpers.GetGameView;
import helpers.Logger;
import helpers.openCV.ImageRecognition;
import helpers.patterns.DotPatterns;
import helpers.utils.GameObject;
import javafx.scene.shape.Circle;
import org.opencv.core.Point;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import osr.utils.ImageUtils;
import osr.utils.MatchedObjects;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static utils.Constants.GameObjectColorMap;

public class MinimapProjections {
    private static final double RAD_TO_DEG = 180.0 / Math.PI;
    private static final Rectangle COMPASS_RECTANGLE = new Rectangle(706, 14, 18, 19);
    private static final Point COMPASS_CENTER_POINT = new Point(715, 22);
    private static final Color COMPASS_TIP_COLOR = Color.decode("#830a0b");
    public static int MINIMAP_ROTATION_ANGLE = 0;
    private final Logger logger;
    private final GetGameView getGameView;
    private final ImageRecognition imageRecognition;
    private final Mat minimapTopBorder;

    public MinimapProjections(Logger logger, GetGameView getGameView, ImageRecognition imageRecognition, ImageUtils imageUtils) {
        this.logger = logger;
        this.getGameView = getGameView;
        this.imageRecognition = imageRecognition;

        minimapTopBorder = imageUtils.pathToMat("/osrsAssets/Minimap/minimaptop.png");
    }

    private static Circle getCircle(Rectangle topBorder) {
        double diameter = 158;
        double topLeftX = topBorder.x - 72 + 9;
        double topLeftY = topBorder.y + 2;

        // Calculate the center of the circle with the given offsets
        double centerX = topLeftX + (diameter / 2.0);
        double centerY = topLeftY + (diameter / 2.0);

        // Create and return the circle with the adjusted position and diameter
        return new Circle(centerX, centerY, diameter / 2.0);
    }

    public Circle getMinimapPosition(String device) {
        Mat sourceImage = null;
        try {
            // Load the images representing the borders of the minimap
            sourceImage = getGameView.getMat(device);

            // Find the top border within the source image
            Rectangle topBorder = imageRecognition.returnBestMatchObject(minimapTopBorder, sourceImage, 0.92);
            if (topBorder == null) {
                logger.devLog("Top border of the minimap was not found.");
                return null;
            }

            return getCircle(topBorder);
        } finally {
            if (sourceImage != null) {
                sourceImage.release();
            }
        }
    }

    public BufferedImage getMinimapImage(String device) {
        Circle minimapCircle = getMinimapPosition(device);
        // Retrieve the position of the minimap from getMinimapPosition
        if (minimapCircle == null || minimapCircle.getRadius() <= 0) {
            logger.devLog("Minimap position not found or invalid for device: " + device);
            return null;
        }
        determineCompassAngle(device);

        int centerX = (int) minimapCircle.getCenterX();
        int centerY = (int) minimapCircle.getCenterY();
        int radius = (int) minimapCircle.getRadius();
        Rectangle rect = new Rectangle(centerX - radius, centerY - radius, radius * 2, radius * 2);
        BufferedImage minimapImage = getGameView.getSubBuffered(device, rect);

        // Rotate the minimap based on the minimapRotationAngle
        if (MINIMAP_ROTATION_ANGLE % 360 != 0) { // Only rotate if angle is not 0 or a full rotation
            return rotateImage(minimapImage, MINIMAP_ROTATION_ANGLE);
        } else {
            // No rotation needed if minimapRotationAngle is 0 or a full 360
            return minimapImage;
        }
    }

    public List<Rectangle> findDots(BufferedImage minimap, GameObject... gameObjects) {
        // Precompute hex color array
        int[][] hexColors = getColorArray(minimap);
        List<Rectangle> foundDots = new ArrayList<>();
        List<int[][]> patterns = DotPatterns.get();

        // Iterate over each GameObject
        for (GameObject gameObject : gameObjects) {
            Set<Integer> colors = GameObjectColorMap.get(gameObject);

            if (patterns == null || colors == null) {
                throw new IllegalArgumentException("Invalid GameObject or uninitialized pattern/color map for: " + gameObject);
            }

            // Process each pattern for the GameObject
            for (int[][] pattern : patterns) {
                int patternHeight = pattern.length;
                int patternWidth = pattern[0].length;

                // Iterate over the minimap pixels
                for (int y = 0; y <= minimap.getHeight() - patternHeight; y++) {
                    for (int x = 0; x <= minimap.getWidth() - patternWidth; x++) {
                        if (matchesPattern(hexColors, x, y, pattern, colors)) {
                            // Add match and mark as processed
                            foundDots.add(new Rectangle(x, y, patternWidth, patternHeight));
                        }
                    }
                }
            }
        }

        return foundDots;
    }

    private int[][] getColorArray(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[][] colorArray = new int[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                colorArray[y][x] = image.getRGB(x, y) & 0xFFFFFF; // Extract RGB and discard alpha channel
            }
        }

        return colorArray;
    }

    private int[][] getPixelArray(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[][] pixels = new int[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pixels[y][x] = image.getRGB(x, y);
            }
        }

        return pixels;
    }

    public BufferedImage inpaintRectangles(BufferedImage minimap, List<Rectangle> rectangles) {
        int[][] pixelArray = getPixelArray(minimap);
        BufferedImage result = new BufferedImage(minimap.getWidth(), minimap.getHeight(), minimap.getType());
        Set<Integer> excludedColors = new HashSet<>();
        excludedColors.addAll(GameObjectColorMap.get(GameObject.PLAYER));
        excludedColors.addAll(GameObjectColorMap.get(GameObject.NPC));
        excludedColors.addAll(GameObjectColorMap.get(GameObject.ITEM));

        // Copy the original image into the result
        for (int y = 0; y < minimap.getHeight(); y++) {
            for (int x = 0; x < minimap.getWidth(); x++) {
                result.setRGB(x, y, minimap.getRGB(x, y));
            }
        }

        // Inpaint the specified rectangles
        for (Rectangle rect : rectangles) {
            int avgRed = 0, avgGreen = 0, avgBlue = 0, count = 0;

            // Extend the border area to collect colors
            int borderExtension = 3; // 1 pixel larger on each side
            for (int y = rect.y - borderExtension; y <= rect.y + rect.height + borderExtension; y++) {
                for (int x = rect.x - borderExtension; x <= rect.x + rect.width + borderExtension; x++) {
                    if (x >= 0 && x < minimap.getWidth() && y >= 0 && y < minimap.getHeight() &&
                            (y < rect.y || y >= rect.y + rect.height || x < rect.x || x >= rect.x + rect.width)) {
                        int rgb = pixelArray[y][x];
                        if (!excludedColors.contains(rgb & 0xFFFFFF)) { // Ignore excluded colors
                            Color color = new Color(rgb);
                            avgRed += color.getRed();
                            avgGreen += color.getGreen();
                            avgBlue += color.getBlue();
                            count++;
                        }
                    }
                }
            }

            // Calculate average color
            if (count > 0) {
                avgRed /= count;
                avgGreen /= count;
                avgBlue /= count;
            }

            // Fill the extended rectangle with a gradient or slight randomization
            for (int y = rect.y - 1; y < rect.y + rect.height + 1; y++) {
                for (int x = rect.x - 1; x < rect.x + rect.width + 1; x++) {
                    if (x >= 0 && x < minimap.getWidth() && y >= 0 && y < minimap.getHeight()) {
                        int randomOffset = (int) (Math.random() * 10 - 5); // Random variation: -5 to +5
                        int red = Math.min(255, Math.max(0, avgRed + randomOffset));
                        int green = Math.min(255, Math.max(0, avgGreen + randomOffset));
                        int blue = Math.min(255, Math.max(0, avgBlue + randomOffset));

                        // Optional: Add a gradient effect based on the position
                        int gradientFactor = (int) ((double) (x - rect.x) / rect.width * 20 - 10); // Smooth gradient
                        red = Math.min(255, Math.max(0, red + gradientFactor));
                        green = Math.min(255, Math.max(0, green + gradientFactor));
                        blue = Math.min(255, Math.max(0, blue + gradientFactor));

                        Color fillColor = new Color(red, green, blue);
                        result.setRGB(x, y, fillColor.getRGB());
                    }
                }
            }
        }

        return result;
    }

    private boolean matchesPattern(int[][] colorArray, int startX, int startY, int[][] pattern, Set<Integer> colors) {
        int totalPixels = 0;
        int matchedPixels = 0;

        for (int py = 0; py < pattern.length; py++) {
            for (int px = 0; px < pattern[py].length; px++) {
                int expectedValue = pattern[py][px];
                if (expectedValue == 0) continue;

                totalPixels++;
                int color = colorArray[startY + py][startX + px];
                if (colors.contains(color)) {
                    matchedPixels++;
                }

                // Early exit if further matching is impossible
                if (matchedPixels + (totalPixels - matchedPixels) < totalPixels * 0.8) {
                    return false;
                }
            }
        }

        return ((double) matchedPixels / totalPixels) * 100 >= 60.0;
    }

    // Method to rotate the BufferedImage
    private BufferedImage rotateImage(BufferedImage image, double angle) {
        // Create an AffineTransform for rotating the image
        AffineTransform transform = new AffineTransform();
        transform.rotate(Math.toRadians(angle), image.getWidth() / 2.0, image.getHeight() / 2.0);

        // Apply the rotation to the image
        AffineTransformOp op = new AffineTransformOp(transform, AffineTransformOp.TYPE_BILINEAR);
        BufferedImage rotatedImage = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
        op.filter(image, rotatedImage);

        return rotatedImage;
    }


    // MAT stuff
    public Mat getMinimapMat(String device, Circle minimapCircle) {
        determineCompassAngle(device);

        // Retrieve the position of the minimap from getMinimapPosition
        if (minimapCircle == null) {
            logger.devLog("Minimap position not found.");
            return null;
        }

        // Calculate the bounding rectangle for the minimap circle directly using the radius
        int centerX = (int) minimapCircle.getCenterX();
        int centerY = (int) minimapCircle.getCenterY();
        int radius = (int) minimapCircle.getRadius();

        // Define bounding rectangle without additional calculations
        Rect boundingRect = new Rect(centerX - radius, centerY - radius, radius * 2, radius * 2);

        Mat minimapMat;
        try {
            // Directly get the submat for the minimap area
            minimapMat = getGameView.getSubmat(device, boundingRect);

            // Rotate based on the minimapRotationAngle determined from compass direction
            if (MINIMAP_ROTATION_ANGLE % 360 != 0) { // Only rotate if angle is not 0 or a full rotation
                double adjustedAngle = MINIMAP_ROTATION_ANGLE > 180
                        ? MINIMAP_ROTATION_ANGLE - 360  // Rotate counterclockwise for angles > 180
                        : MINIMAP_ROTATION_ANGLE;       // Rotate clockwise for angles <= 180

                Mat rotatedMat = rotateMinimap(minimapMat, -adjustedAngle); // Rotate back

                minimapMat.release(); // Release the original Mat after rotation
                return rotatedMat;    // Return the rotated Mat
            } else {
                // No rotation needed if minimapRotationAngle is 0 or a full 360
                return minimapMat; // Return the original Mat
            }
        } catch (Exception e) {
            logger.devLog("Error occurred while processing the minimap: " + e.getMessage());
            return null;
        }
    }

    private Mat rotateMinimap(Mat mat, double angle) {
        // Find the center of the image
        Point center = new Point((double) mat.width() / 2, (double) mat.height() / 2);

        // Get the rotation matrix for the given angle
        Mat rotationMatrix = Imgproc.getRotationMatrix2D(center, angle, 1.0);

        // Calculate the size of the rotated image
        Size rotatedSize = new Size(mat.width(), mat.height());

        // Create a new Mat to store the rotated result
        Mat rotatedMat = new Mat();

        // Apply the rotation to the original Mat
        Imgproc.warpAffine(mat, rotatedMat, rotationMatrix, rotatedSize);

        return rotatedMat;
    }

    public List<MatchedObjects> getMinimapMatches(Mat minimapMat, GameObject... types) {
        List<MatchedObjects> allMatches = new ArrayList<>();
        if (minimapMat != null) {
            Set<Integer> combinedHexColors = new HashSet<>();
            for (GameObject type : types) {
                Set<Integer> hexColors = GameObjectColorMap.get(type);
                if (hexColors == null) {
                    logger.devLog("Type " + type + " is not recognized.");
                    continue;
                }
                combinedHexColors.addAll(hexColors);
            }

            if (!combinedHexColors.isEmpty()) {
                MatchedObjects matchedObject = findAndMarkColorAreas(minimapMat, combinedHexColors);
                Rectangle middleRect = getMiddleRectangle(minimapMat);
                matchedObject.getRectangles().add(middleRect);
                allMatches.add(matchedObject);
            }
        } else {
            logger.devLog("Could not retrieve the minimap image.");
        }
        return allMatches;
    }

    public MatchedObjects findAndMarkColorAreas(Mat img, Set<Integer> bgrColors) {
        Mat combinedMask = generateCombinedMask(img, bgrColors);
        List<MatOfPoint> contours = findContours(combinedMask);
        List<Rect> rectangles = filterAndDrawRectangles(contours);
        List<Rectangle> awtRectangles = convertRectsToRectangles(rectangles);
        return new MatchedObjects(awtRectangles, bgrColors.toString()); // Modify as needed to fit context
    }

    private List<Rectangle> convertRectsToRectangles(List<Rect> rects) {
        List<Rectangle> awtRectangles = new ArrayList<>();
        for (Rect rect : rects) {
            awtRectangles.add(new Rectangle(rect.x, rect.y, rect.width, rect.height));
        }
        return awtRectangles;
    }

    private Mat generateCombinedMask(Mat bgrImage, Set<Integer> bgrColors) {
        Mat combinedMask = Mat.zeros(bgrImage.size(), CvType.CV_8UC1);

        for (int color : bgrColors) {
            Scalar bgrColor = intToBGR(color);
            Scalar lower = new Scalar(
                    Math.max(bgrColor.val[0], 0),
                    Math.max(bgrColor.val[1], 0),
                    Math.max(bgrColor.val[2], 0)
            );
            Scalar upper = new Scalar(
                    Math.min(bgrColor.val[0], 255),
                    Math.min(bgrColor.val[1], 255),
                    Math.min(bgrColor.val[2], 255)
            );

            Mat mask = new Mat();
            Core.inRange(bgrImage, lower, upper, mask);

            Core.bitwise_or(combinedMask, mask, combinedMask);
            mask.release();
        }

        return combinedMask;
    }

    private Scalar intToBGR(int color) {
        int blue = color & 0xFF;
        int green = (color >> 8) & 0xFF;
        int red = (color >> 16) & 0xFF;
        return new Scalar(blue, green, red);
    }

    private List<MatOfPoint> findContours(Mat combinedMask) {
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(combinedMask, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        hierarchy.release();
        return contours;
    }

    private List<Rect> filterAndDrawRectangles(List<MatOfPoint> contours) {
        List<Rect> rectangles = new ArrayList<>();

        for (MatOfPoint contour : contours) {
            Rect rect = Imgproc.boundingRect(contour);
            if (shouldIncludeRect(rect, contour)) {
                rectangles.add(rect);
            }
        }
        return rectangles;
    }

    private boolean shouldIncludeRect(Rect rect, MatOfPoint contour) {
        double minArea = 2;
        double maxArea = 100;
        int maxWidth = 30;
        int maxHeight = 30;

        double area = Imgproc.contourArea(contour);

        return area >= minArea && area <= maxArea &&
                rect.width <= maxWidth && rect.height <= maxHeight;
    }

    private Rectangle getMiddleRectangle(Mat mat) {
        int width = mat.width();
        int height = mat.height();

        int rectSize = 4; // 4x4 rectangle
        int centerX = width / 2;
        int centerY = height / 2;

        int rectX = centerX - rectSize / 2;
        int rectY = centerY - rectSize / 2;

        return new Rectangle(rectX, rectY, rectSize, rectSize);
    }

    public void determineCompassAngle(String device) {
        // Extract only the compass area from game view
        BufferedImage gameView = getGameView.getSubBuffered(device, COMPASS_RECTANGLE);

        Point farthestCompassTip = null;
        double maxDistance = -1;

        int centerX = (int) (COMPASS_CENTER_POINT.x - COMPASS_RECTANGLE.x);
        int centerY = (int) (COMPASS_CENTER_POINT.y - COMPASS_RECTANGLE.y);

        // Loop through each pixel in the compassRect to find the farthest compassTipColor
        for (int x = 0; x < gameView.getWidth(); x++) {
            for (int y = 0; y < gameView.getHeight(); y++) {
                int pixelColorValue = gameView.getRGB(x, y);

                // Extract RGB components from the integer pixel color
                int red = (pixelColorValue >> 16) & 0xFF;
                int green = (pixelColorValue >> 8) & 0xFF;
                int blue = pixelColorValue & 0xFF;

                // Check if this pixel matches the compassTipColor
                if (isMatchingColor(red, green, blue)) {
                    int dx = x - centerX;
                    int dy = y - centerY;
                    double distance = dx * dx + dy * dy;

                    // Update if this point is farther than the previous points
                    if (distance > maxDistance) {
                        maxDistance = distance;
                        farthestCompassTip = new Point(x + COMPASS_RECTANGLE.x, y + COMPASS_RECTANGLE.y);
                    }
                }
            }
        }

        if (farthestCompassTip != null) {
            int dx = (int) (farthestCompassTip.x - COMPASS_CENTER_POINT.x);
            int dy = (int) (farthestCompassTip.y - COMPASS_CENTER_POINT.y);
            MINIMAP_ROTATION_ANGLE = (int) Math.round(Math.atan2(-dy, dx) * RAD_TO_DEG) - 90;

            // Ensure angle is between 0 and 360
            if (MINIMAP_ROTATION_ANGLE < 0) {
                MINIMAP_ROTATION_ANGLE += 360;
            }

            //System.out.println("Detected compass angle: " + MINIMAP_ROTATION_ANGLE);
        } else {
            System.out.println("Compass tip color not found in compass area.");
        }
    }

    // Helper function to match color with a tolerance for slight variations
    private boolean isMatchingColor(int red, int green, int blue) {
        int tolerance = 10; // Adjust tolerance as needed
        return Math.abs(red - COMPASS_TIP_COLOR.getRed()) <= tolerance &&
                Math.abs(green - COMPASS_TIP_COLOR.getGreen()) <= tolerance &&
                Math.abs(blue - COMPASS_TIP_COLOR.getBlue()) <= tolerance;
    }
}
