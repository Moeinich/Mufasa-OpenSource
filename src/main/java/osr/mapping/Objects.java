package osr.mapping;

import helpers.GetGameView;
import helpers.Logger;
import helpers.openCV.ImageRecognition;
import helpers.openCV.utils.MatchedRectangle;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import osr.utils.ImageUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Objects {
    private final Logger logger;
    private final ImageRecognition imageRecognition;
    private final GetGameView getGameView;
    private final ImageUtils imageUtils;

    public Objects(Logger logger, ImageRecognition imageRecognition, GetGameView getGameView, ImageUtils imageUtils) {
        this.logger = logger;
        this.imageRecognition = imageRecognition;
        this.getGameView = getGameView;
        this.imageUtils = imageUtils;
    }

    public List<Rectangle> within(String device, int tileRadius, boolean returnAll, String filePath) {
        List<MatchedRectangle> foundObjects = getObjects(device, filePath, returnAll);

        Point center = getGameView.getGameviewCenter(device);
        List<Rectangle> withinRadiusObjects = new ArrayList<>();

        for (MatchedRectangle obj : foundObjects) {
            if (isWithinRadius(center, obj, tileRadius)) {
                withinRadiusObjects.add(obj);
                if (!returnAll) {
                    break; // Stop after finding the first within radius if not returning all
                }
            }
        }

        return withinRadiusObjects;
    }

    public List<Rectangle> within(String device, Rectangle rect, boolean returnAll, String filePath) {
        List<MatchedRectangle> foundObjects = getObjects(device, filePath, returnAll);

        List<Rectangle> rectanglesWithin = new ArrayList<>();
        for (MatchedRectangle obj : foundObjects) {
            if (rect.contains(obj.getX(), obj.getY())) {
                rectanglesWithin.add(obj);
                if (!returnAll) {
                    break; // Break the loop if only the first object is needed
                }
            }
        }

        return rectanglesWithin;
    }

    public Rectangle getBestMatch(String device, String filePath, double threshold) {
        Mat fileToSearchFor = null;
        try {
            fileToSearchFor = imageUtils.pathToMat(filePath);
            Mat gameView = getGameView.getMat(device);
            return imageRecognition.returnBestMatchObject(fileToSearchFor, gameView, threshold);
        } finally {
            if (fileToSearchFor != null) {
                fileToSearchFor.release();
            }
        }
    }

    public Rectangle getNearest(String device, String filePath) {
        List<MatchedRectangle> foundObjects = getObjects(device, filePath, true);
        Point center = getGameView.getGameviewCenter(device);

        MatchedRectangle nearest = null;
        double minDistance = Double.MAX_VALUE;  // Start with the largest possible distance

        for (MatchedRectangle rect : foundObjects) {
            // Calculate the center of the rectangle
            Point rectCenter = new Point(rect.x + rect.width / 2, rect.y + rect.height / 2);
            // Calculate the distance between the center of the rectangle and the game view center
            double distance = Math.sqrt(Math.pow(rectCenter.x - center.x, 2) + Math.pow(rectCenter.y - center.y, 2));
            // Update the nearest rectangle if the current one is closer
            if (distance < minDistance) {
                minDistance = distance;
                nearest = rect;
            }
        }
        return nearest;
    }

    private List<MatchedRectangle> getObjects(String device, String filePath, boolean returnAll) {
        Mat imageToFind = null;
        try {
            Mat imageToSearchIn = getGameView.getMat(device);
            imageToFind = imageUtils.pathToMat(filePath);

            if (imageToFind.channels() == 4) {
                Imgproc.cvtColor(imageToFind, imageToFind, Imgproc.COLOR_BGRA2BGR);
            }

            if (imageToSearchIn.channels() == 4) {
                Imgproc.cvtColor(imageToSearchIn, imageToSearchIn, Imgproc.COLOR_BGRA2BGR);
            }

            List<MatchedRectangle> foundObjects;
            if (!returnAll) {
                MatchedRectangle bestMatch = imageRecognition.returnBestMatchObject(imageToFind, imageToSearchIn, 0.9);
                foundObjects = bestMatch != null ? List.of(bestMatch) : new ArrayList<>();
            } else {
                foundObjects = imageRecognition.returnAllMatchObjects(imageToFind, imageToSearchIn, 0.9);
            }

            logger.devLog("Found: " + foundObjects.size() + " objects");
            return foundObjects;
        } finally {
            if (imageToFind != null) {
                imageToFind.release();
            }
        }
    }

    private boolean isWithinRadius(Point center, MatchedRectangle obj, int tileRadius) {
        // Calculate the center of the MatchedRectangle
        int rectCenterX = (int) (obj.getX() + obj.getWidth() / 2);
        int rectCenterY = (int) (obj.getY() + obj.getHeight() / 2);

        // Calculate the distance between the centers
        double distance = Math.sqrt(Math.pow(rectCenterX - center.x, 2) + Math.pow(rectCenterY - center.y, 2));

        // Check if the distance is within the tile radius (considering each tile is 3x3)
        return distance <= tileRadius * 3;
    }
}
