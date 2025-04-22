package osr.mapping;

import helpers.GetGameView;
import helpers.Logger;
import helpers.Color.ColorFinder;
import helpers.testGrounds.ColorScanner;
import helpers.visualFeedback.FeedbackObservables;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import osr.utils.OverlayType;

import java.awt.Point;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static helpers.testGrounds.utils.MColorLibrary.FISH_SPOT_COLORS_NEW;

public class OverlayFinder {
    private final List<Color> greenOverlay = Arrays.asList(
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
    private final List<Color> blueOverlay = List.of(
            Color.decode("#25ffff") //from OverlayColor
    );
    private final List<Color> redOverlay = Arrays.asList(
            Color.decode("#ca8818"),
            Color.decode("#c98818"),
            Color.decode("#c98718"),
            Color.decode("#d38f1a"),
            Color.decode("#cb8a19")
    );

    private final Logger logger;
    private final GetGameView getGameView;
    private final GameTabs gameTabs;
    private final ColorFinder colorFinder;
    private final ColorScanner colorScanner;

    public OverlayFinder(Logger logger, GetGameView getGameView, GameTabs gameTabs, ColorFinder colorFinder, ColorScanner colorScanner) {
        this.logger = logger;
        this.getGameView = getGameView;
        this.gameTabs = gameTabs;
        this.colorFinder = colorFinder;
        this.colorScanner = colorScanner;
    }

    public Point getGameCenter(String device) {
        return getGameView.getGameviewCenter(device);
    }

    private List<Color> getOverlayColor(OverlayType itemType) {
        switch (itemType) {
            case AGILITY:
                return greenOverlay;
            case GROUND_ITEM:
                return redOverlay;
            case FISHING:
                return blueOverlay;
            default:
                throw new IllegalArgumentException("Unknown item type");
        }
    }

    public List<Rectangle> findOverlays(String device, OverlayType overlayType, Rectangle searchArea, double eps, int minPts) {
        FeedbackObservables.rectangleObservable.setValue(device, searchArea);
        List<Color> overlay = getOverlayColor(overlayType);
        if (overlayType == OverlayType.FISHING) {
            return findFishingSpots(device);
        } else {
            return colorFinder.processColorClustersInRect(device, overlay, 2, searchArea, eps, minPts);
        }
    }

    public List<Rectangle> findFishingSpots(String device) {
        List<Rectangle> spots = colorScanner.findColorsAsRectNEW(device, FISH_SPOT_COLORS_NEW);

        if (spots == null) {
            logger.devLog("findFishingSpots: findColorsAsRectNEW returned null!");
            return Collections.emptyList();
        }

        // Filter out spots smaller than 6 pixels wide
        List<Rectangle> filteredSpots = spots.stream()
                .filter(spot -> spot.getWidth() >= 6)
                .collect(Collectors.toList());

        logger.devLog("Fishing spots found before filtering: " + spots.size() + ", after filtering: " + filteredSpots.size());

        return filteredSpots;
    }

    public List<Rectangle> findFishingSpotsROI(String device, Rectangle searchArea) {
        List<Rectangle> allFishingSpots = findFishingSpots(device);

        if (allFishingSpots.isEmpty()) {
            logger.devLog("findFishingSpotsROI: No fishing spots found after calling findFishingSpots.");
            return Collections.emptyList();
        }

        // Only keep spots inside ROI
        return allFishingSpots.stream()
                .filter(searchArea::intersects)
                .collect(Collectors.toList());
    }

    public Rectangle findNearestOverlay(String device, OverlayType overlayType, Rectangle searchArea, double eps, int minPts) {
        FeedbackObservables.rectangleObservable.setValue(device, searchArea);
        List<Rectangle> itemOverlayPoints = findOverlays(device, overlayType, searchArea, eps, minPts);
        Rectangle nearestRectangle = findNearestRectangle(itemOverlayPoints, getGameCenter(device));
        return nearestRectangle != null ? nearestRectangle : new Rectangle();
    }

    public Rectangle findSecondNearestOverlay(String device, OverlayType overlayType, Rectangle searchArea, double eps, int minPts) {
        FeedbackObservables.rectangleObservable.setValue(device, searchArea);
        List<Rectangle> itemOverlayPoints = findOverlays(device, overlayType, searchArea, eps, minPts);
        Rectangle nearestRectangle = findNearestRectangle(itemOverlayPoints, getGameCenter(device));
        if (nearestRectangle == null) {
            return new Rectangle();
        }
        Rectangle secondNearestRectangle = findSecondNearestRectangle(itemOverlayPoints, getGameCenter(device), nearestRectangle);
        return secondNearestRectangle != null ? secondNearestRectangle : new Rectangle();
    }

    // OLD STUFF
    public List<Polygon> findFishing(Color color, String device, boolean SaveMask) {
        List<Polygon> polygons = new ArrayList<>();
        Mat src = getGameView.getMat(device);

        // Get the mask for the defined color
        Mat mask = getColorMask(src, color, device, SaveMask);

        // Find contours on the mask
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(mask, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

        // Approximate contours to polygons
        MatOfPoint2f approxCurve = new MatOfPoint2f();
        for (MatOfPoint contour : contours) {
            MatOfPoint2f contour2f = new MatOfPoint2f(contour.toArray());
            double approxDistance = Imgproc.arcLength(contour2f, true) * 0.02;
            Imgproc.approxPolyDP(contour2f, approxCurve, approxDistance, true);

            // Convert approxCurve to MatOfPoint
            MatOfPoint points = new MatOfPoint(approxCurve.toArray());

            // Convert points to a Polygon
            int[] xPoints = new int[(int) points.total()];
            int[] yPoints = new int[(int) points.total()];
            for (int i = 0; i < points.total(); i++) {
                xPoints[i] = (int) points.get(i, 0)[0];
                yPoints[i] = (int) points.get(i, 0)[1];
            }

            polygons.add(new Polygon(xPoints, yPoints, points.rows()));
        }

        return clusterPolygons(polygons, 30);
    }

    public Polygon findNearestFishing(java.awt.Color color, String device, boolean SaveMask) {
        List<Polygon> polygons = findFishing(color, device, SaveMask);

        // Get the center point of the image as floating-point values
        Mat src = getGameView.getMat(device);
        org.opencv.core.Point imageCenter = new org.opencv.core.Point((double) src.width() / 2.0, (double) src.height() / 2.0);

        Polygon nearestPolygon = null;
        double minDistance = Double.MAX_VALUE;

        // Iterate over the polygons to find the nearest one
        for (Polygon polygon : polygons) {
            Point centroid = calculateCentroid(polygon);
            double distance = Math.hypot(centroid.x - imageCenter.x, centroid.y - imageCenter.y);
            if (distance < minDistance) {
                minDistance = distance;
                nearestPolygon = polygon;
            }
        }

        return nearestPolygon;
    }

    public Rectangle findNearestFishingNEW(String device, Rectangle searchArea) {
        // Get all fishing spots within the search area
        List<Rectangle> filteredFishingSpots = findFishingSpotsROI(device, searchArea);

        if (filteredFishingSpots == null || filteredFishingSpots.isEmpty()) {
            return null;
        }

        // Get the center of the game view
        Point gameCenter = getGameCenter(device);

        if (gameCenter == null) {
            return null;
        }

        // Sort fishing spots by distance to game center
        filteredFishingSpots.sort((r1, r2) -> {
            double d1 = Math.hypot(r1.getCenterX() - gameCenter.x, r1.getCenterY() - gameCenter.y);
            double d2 = Math.hypot(r2.getCenterX() - gameCenter.x, r2.getCenterY() - gameCenter.y);
            return Double.compare(d1, d2);
        });

        return filteredFishingSpots.get(0);
    }

    public Rectangle findSecondNearestFishingNEW(String device, Rectangle searchArea) {
        logger.devLog("findSecondNearestFishingNEW called with device: " + device + " and searchArea: " + searchArea);

        // Null checks
        if (device == null || searchArea == null) {
            logger.devLog("findSecondNearestFishingNEW: device or searchArea is null!");
            return null;
        }

        // Get all fishing spots
        List<Rectangle> filteredFishingSpots = findFishingSpotsROI(device, searchArea);

        logger.devLog("Filtered fishing spots count: " + (filteredFishingSpots != null ? filteredFishingSpots.size() : "null"));

        // Ensure we have at least two valid spots
        if (filteredFishingSpots == null || filteredFishingSpots.size() < 2) {
            logger.devLog("findSecondNearestFishingNEW: Not enough fishing spots found.");
            return null;
        }

        // Get the center of the game view
        Point gameCenter = getGameCenter(device);
        logger.devLog("Game center: " + gameCenter);

        if (gameCenter == null) {
            logger.devLog("findSecondNearestFishingNEW: Game center is null!");
            return null;
        }

        // Sort fishing spots by distance to game center
        filteredFishingSpots.sort((r1, r2) -> {
            double d1 = Math.hypot(r1.getCenterX() - gameCenter.x, r1.getCenterY() - gameCenter.y);
            double d2 = Math.hypot(r2.getCenterX() - gameCenter.x, r2.getCenterY() - gameCenter.y);
            return Double.compare(d1, d2);
        });

        logger.devLog("Returning second nearest fishing spot: " + filteredFishingSpots.get(1));
        return filteredFishingSpots.get(1);
    }

    public Polygon findSecondNearestFishing(java.awt.Color color, String device, boolean SaveMask) {
        List<Polygon> polygons = findFishing(color, device, SaveMask);

        // Get the center point of the image as floating-point values
        Mat src = getGameView.getMat(device);
        org.opencv.core.Point imageCenter = new org.opencv.core.Point((double) src.width() / 2.0, (double) src.height() / 2.0);

        Polygon nearestPolygon = null;
        Polygon secondNearestPolygon = null;
        double minDistance = Double.MAX_VALUE;
        double secondMinDistance = Double.MAX_VALUE;

        // Iterate over the polygons to find the nearest and second nearest ones
        for (Polygon polygon : polygons) {
            Point centroid = calculateCentroid(polygon);
            double distance = Math.hypot(centroid.x - imageCenter.x, centroid.y - imageCenter.y);
            if (distance < minDistance) {
                secondMinDistance = minDistance;
                minDistance = distance;
                secondNearestPolygon = nearestPolygon;
                nearestPolygon = polygon;
            } else if (distance < secondMinDistance) {
                secondMinDistance = distance;
                secondNearestPolygon = polygon;
            }
        }

        return secondNearestPolygon;
    }

    public Point calculateCentroid(Polygon polygon) {
        double xSum = 0, ySum = 0;
        for (int i = 0; i < polygon.npoints; i++) {
            xSum += polygon.xpoints[i];
            ySum += polygon.ypoints[i];
        }
        return new Point((int) (xSum / polygon.npoints), (int) (ySum / polygon.npoints));
    }

    public Mat getColorMask(Mat src, Color color, String device, boolean saveMat) {
        // Convert Color to HSV
        float[] hsvValues = new float[3];
        Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), hsvValues);

        // Convert HSV values to OpenCV HSV range
        Scalar colorLower = new Scalar((hsvValues[0] * 180) - 9, 100, 100);
        Scalar colorUpper = new Scalar((hsvValues[0] * 180) + 9, 255, 255);

        // Convert to HSV color space for color thresholding
        Mat hsv = new Mat();
        Imgproc.cvtColor(src, hsv, Imgproc.COLOR_BGR2HSV);

        // Threshold the HSV image to get only the colors in the specified range
        Mat mask = new Mat();
        Core.inRange(hsv, colorLower, colorUpper, mask);
        excludeAreasFromMask(mask, device); // Exclude the game tabs etc from the mask

        // Assuming 'mask' is your Mat object that you want to save:
        if (saveMat) {
            boolean isSaved = saveMatToDownloads(mask, "mask.png");
            if (isSaved) {
                logger.devLog("Mask saved successfully.");
            } else {
                logger.devLog("Failed to save the mask.");
            }
        }
        return mask;
    }

    public boolean saveMatToDownloads(Mat mask, String filename) {
        String homeDir = System.getProperty("user.home");
        String downloadsPath = homeDir + (System.getProperty("os.name").toLowerCase().contains("win") ? "\\Downloads\\" : "/Downloads/");
        String fullPath = downloadsPath + filename;

        // Use OpenCV to save the image
        return Imgcodecs.imwrite(fullPath, mask);
    }

    public List<Polygon> clusterPolygons(List<Polygon> allPolygons, double distanceThreshold) {
        List<Point> centroids = calculateCentroids(allPolygons);
        List<List<Point>> clusters = new ArrayList<>(); // This will hold lists of points in the same cluster

        // Iterate through centroids to cluster them
        for (Point centroid : centroids) {
            boolean isClustered = false;
            for (List<Point> cluster : clusters) {
                if (isCloseToCluster(centroid, cluster, distanceThreshold)) {
                    cluster.add(centroid);
                    isClustered = true;
                    break;
                }
            }
            if (!isClustered) {
                List<Point> newCluster = new ArrayList<>();
                newCluster.add(centroid);
                clusters.add(newCluster);
            }
        }
        return selectOrCombinePolygons(clusters, allPolygons);
    }

    public List<Point> calculateCentroids(List<Polygon> polygons) {
        List<Point> centroids = new ArrayList<>();
        for (Polygon poly : polygons) {
            double xSum = 0, ySum = 0;
            int[] xPoints = poly.xpoints;
            int[] yPoints = poly.ypoints;
            int nPoints = poly.npoints;
            for (int i = 0; i < nPoints; i++) {
                xSum += xPoints[i];
                ySum += yPoints[i];
            }
            Point centroid = new Point((int) (xSum / nPoints), (int) (ySum / nPoints));
            centroids.add(centroid);
        }
        return centroids;
    }

    public boolean isCloseToCluster(Point centroid, List<Point> cluster, double distanceThreshold) {
        for (Point member : cluster) {
            double distance = Math.sqrt(Math.pow(member.x - centroid.x, 2) + Math.pow(member.y - centroid.y, 2));
            if (distance < distanceThreshold) {
                return true;
            }
        }
        return false;
    }

    public List<Polygon> selectOrCombinePolygons(List<List<Point>> clusters, List<Polygon> allPolygons) {
        List<Polygon> clusteredPolygons = new ArrayList<>();
        for (List<Point> cluster : clusters) {
            Polygon largestPolygon = null;
            double maxArea = 0;
            for (Point centroid : cluster) {
                for (Polygon poly : allPolygons) {
                    if (polygonContains(poly, centroid)) {
                        double area = calculatePolygonArea(poly);
                        if (area > maxArea) {
                            maxArea = area;
                            largestPolygon = poly;
                        }
                    }
                }
            }
            if (largestPolygon != null) {
                clusteredPolygons.add(largestPolygon);
            }
        }
        return clusteredPolygons;
    }

    public boolean polygonContains(Polygon poly, Point point) {
        return poly.contains(point.x, point.y);
    }

    public double calculatePolygonArea(Polygon poly) {
        double area = 0.0;
        int j = poly.npoints - 1;
        for (int i = 0; i < poly.npoints; i++) {
            area += (poly.xpoints[j] + poly.xpoints[i]) * (poly.ypoints[j] - poly.ypoints[i]);
            j = i;  // j is previous vertex to i
        }
        return 0.5 * Math.abs(area);
    }

    private void excludeAreasFromMask(Mat mask, String device) {
        // Define the exclusion rectangles
        List<Rect> exclusionRects = new ArrayList<>();
        exclusionRects.add(new Rect(new org.opencv.core.Point(676, 204), new org.opencv.core.Point(894, 0))); // Minimap
        exclusionRects.add(new Rect(new org.opencv.core.Point(15, 191), new org.opencv.core.Point(51, 490))); // UI buttons panel
        exclusionRects.add(new Rect(new org.opencv.core.Point(856, 221), new org.opencv.core.Point(881, 497))); // UI buttons panel
        exclusionRects.add(new Rect(new org.opencv.core.Point(19, 6), new org.opencv.core.Point(459, 27))); // Chatbox panel
        exclusionRects.add(new Rect(new org.opencv.core.Point(0, 0), new org.opencv.core.Point(533, 33))); // Chatbox panel closed
        exclusionRects.add(new Rect(new org.opencv.core.Point(538, 29), new org.opencv.core.Point(657, 68))); // XP bar
        exclusionRects.add(new Rect(new org.opencv.core.Point(430, 219), new org.opencv.core.Point(460, 301))); // Location where own player is
        exclusionRects.add(new Rect(new org.opencv.core.Point(0, 0), new org.opencv.core.Point(48, 540))); // Left side of side buttons
        exclusionRects.add(new Rect(new org.opencv.core.Point(842, 0), new org.opencv.core.Point(894, 540))); // Right side of side buttons

        // Exclude inventory area if it's open
        if (gameTabs.isTabOpen(device, "Inventory")) {
            exclusionRects.add(new Rect(new org.opencv.core.Point(639, 218), new org.opencv.core.Point(846, 498))); // Inventory area
        }

        // Apply exclusion to each rectangle
        for (Rect rect : exclusionRects) {
            Mat zeros = Mat.zeros(rect.height, rect.width, mask.type());
            zeros.copyTo(mask.submat(rect));
        }
    }

    private Rectangle findNearestRectangle(List<Rectangle> rectangles, Point referencePoint) {
        Rectangle nearestRectangle = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Rectangle rect : rectangles) {
            Point rectCenter = new Point(rect.x + rect.width / 2, rect.y + rect.height / 2);
            double distance = referencePoint.distance(rectCenter);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestRectangle = rect;
            }
        }

        return nearestRectangle;
    }

    private Rectangle findSecondNearestRectangle(List<Rectangle> rectangles, Point referencePoint, Rectangle nearestRectangle) {
        Rectangle secondNearestRectangle = null;
        double secondNearestDistance = Double.MAX_VALUE;

        for (Rectangle rect : rectangles) {
            if (rect.equals(nearestRectangle)) {
                continue;
            }

            Point rectCenter = new Point(rect.x + rect.width / 2, rect.y + rect.height / 2);
            double distance = referencePoint.distance(rectCenter);
            if (distance < secondNearestDistance) {
                secondNearestDistance = distance;
                secondNearestRectangle = rect;
            }
        }

        return secondNearestRectangle;
    }
}
