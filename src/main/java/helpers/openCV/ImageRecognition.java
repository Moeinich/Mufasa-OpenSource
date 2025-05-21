package helpers.openCV;

import helpers.Logger;
import helpers.Color.ColorFinder;
import helpers.openCV.utils.MatchedRectangle;
import org.opencv.core.Point;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ImageRecognition {
    private final Logger logger;
    private final ColorFinder colorFinder;

    public ImageRecognition(Logger logger, ColorFinder colorFinder) {
        this.logger = logger;
        this.colorFinder = colorFinder;
    }

    /*
     ** Game object stuff
     */

    public MatchedRectangle returnBestMatchObject(File imageToSearchFor, Mat imageToSearchIn, double threshold) {
        Mat imageToSearchForMat = Imgcodecs.imread(imageToSearchFor.getAbsolutePath(), Imgcodecs.IMREAD_UNCHANGED);

        List<MatchedRectangle> matches = performTemplateMatchForGameObjects(imageToSearchForMat, imageToSearchIn, threshold);

        double bestMatchValue = Double.MIN_VALUE;
        MatchedRectangle bestMatchRect = null;

        for (MatchedRectangle matchRect : matches) {
            double matchValue = matchRect.getMatchValue();

            if (matchValue > bestMatchValue && matchValue <= 1.1) {
                bestMatchValue = matchValue;
                bestMatchRect = matchRect;
            }
        }

        return bestMatchRect;
    }

    public MatchedRectangle returnBestMatchObject(Mat imageToSearchFor, Mat imageToSearchIn, double threshold) {
        List<MatchedRectangle> matches = performTemplateMatchForGameObjects(imageToSearchFor, imageToSearchIn, threshold);

        double bestMatchValue = Double.MIN_VALUE;
        MatchedRectangle bestMatchRect = null;

        for (MatchedRectangle matchRect : matches) {
            double matchValue = matchRect.getMatchValue();

            if (matchValue > bestMatchValue && matchValue <= 1.1) {
                bestMatchValue = matchValue;
                bestMatchRect = matchRect;

                // Print the current best match score
                logger.devLog("Current Best Match Score: " + bestMatchValue);
            }
        }

        //logger.devLog("No best match found.");
        return bestMatchRect;
    }

    private List<MatchedRectangle> getTopColorMatches(String device, Mat imageToSearchFor, Mat imageToSearchIn, double threshold, Color targetColor, int cropX, int cropY, int maxMatchesCheck) {
        //logger.print("Looking for color: " + targetColor.toString());
        List<MatchedRectangle> matches = performTemplateMatchForGameObjects(imageToSearchFor, imageToSearchIn, threshold);

        // Sort matches by match value in descending order
        matches.sort(Comparator.comparingDouble(MatchedRectangle::getMatchValue).reversed());

        // Select the top matches based on the maxMatches parameter
        List<MatchedRectangle> topMatches = matches.subList(0, Math.min(maxMatchesCheck, matches.size()));
        List<MatchedRectangle> colorMatchedRectangles = new ArrayList<>();

        for (MatchedRectangle matchRect : topMatches) {
            double matchValue = matchRect.getMatchValue();

            if (matchValue <= 1.1) {
                // Adjust the rectangle coordinates to account for the offset within the full game view
                Rectangle adjustedRect = new Rectangle(
                        matchRect.x + cropX,
                        matchRect.y + cropY,
                        matchRect.width,
                        matchRect.height
                );

                boolean colorCheck = colorFinder.isColorInRect(device, targetColor, adjustedRect, 5);

                // Log the match score and color check result
                logger.devLog("Match Value: " + matchValue + " | Color Check: " + colorCheck);

                // If the color check passes, add it to the list of color-matched rectangles
                if (colorCheck) {
                    colorMatchedRectangles.add(matchRect);
                }
            }
        }
        return colorMatchedRectangles;
    }

    public MatchedRectangle returnBestMatchWithColor(String device, Mat imageToSearchFor, Mat imageToSearchIn, double threshold, Color targetColor, int cropX, int cropY, int maxMatchesCheck) {
        List<MatchedRectangle> colorMatchedRectangles = getTopColorMatches(device, imageToSearchFor, imageToSearchIn, threshold, targetColor, cropX, cropY, maxMatchesCheck);

        // Return the best match by match value or null if no matches found
        return colorMatchedRectangles.stream()
                .max(Comparator.comparingDouble(MatchedRectangle::getMatchValue))
                .orElse(null);
    }

    public List<MatchedRectangle> returnBestMatchesWithColor(String device, Mat imageToSearchFor, Mat imageToSearchIn, double threshold, Color targetColor, int cropX, int cropY, int maxMatchesCheck) {
        // Get the list of top color-matched rectangles
        return getTopColorMatches(device, imageToSearchFor, imageToSearchIn, threshold, targetColor, cropX, cropY, maxMatchesCheck);
    }

    public List<MatchedRectangle> returnAllMatchObjects(Mat imageToSearchFor, Mat imageToSearchIn, double threshold) {
        return performTemplateMatchForGameObjects(imageToSearchFor, imageToSearchIn, threshold);
    }

    public List<MatchedRectangle> returnAllMatchObjects(File imageToSearchFor, Mat imageToSearchIn, double threshold) {
        Mat imageToSearchForMat = Imgcodecs.imread(imageToSearchFor.getAbsolutePath(), Imgcodecs.IMREAD_UNCHANGED);
        return performTemplateMatchForGameObjects(imageToSearchForMat, imageToSearchIn, threshold);
    }

    public List<MatchedRectangle> performTemplateMatchForGameObjects(Mat imageToSearchFor, Mat imageToSearchIn, double threshold) {
        List<MatchedRectangle> matchRectangles = new ArrayList<>();

        // Split the source image into its channels
        List<Mat> sourceChannels = new ArrayList<>();
        Core.split(imageToSearchFor, sourceChannels);

        Mat imageToSearchForBGR = new Mat();
        Mat mask = new Mat();

        // Check if the source image has an alpha channel
        if (imageToSearchFor.channels() >= 4) {
            // Use the first three channels (BGR) for matching
            Core.merge(new ArrayList<>(sourceChannels.subList(0, 3)), imageToSearchForBGR);

            // Use the fourth channel (alpha) for masking
            Mat alphaChannel = sourceChannels.get(3);
            Core.compare(alphaChannel, new Scalar(0), mask, Core.CMP_GT);
        } else {
            // If no alpha channel, use the source image as is
            imageToSearchForBGR = imageToSearchFor;
        }

        // Perform the template matching
        Mat result = new Mat();
        Imgproc.matchTemplate(imageToSearchIn, imageToSearchForBGR, result, Imgproc.TM_CCOEFF_NORMED, mask);

        // Find all matches above the threshold and below the maximum score cap
        for (int y = 0; y < result.rows(); y++) {
            for (int x = 0; x < result.cols(); x++) {
                double matchValue = result.get(y, x)[0];
                if (matchValue >= threshold && matchValue <= 1.1) {
                    // Avoid matching the same location again
                    Imgproc.floodFill(result, new Mat(), new Point(x, y), new Scalar(0), new Rect(), new Scalar(0), new Scalar(0.1));
                    // Add the match to the list
                    matchRectangles.add(new MatchedRectangle(x, y, imageToSearchFor.width(), imageToSearchFor.height(), matchValue));
                }
            }
        }
        return matchRectangles;
    }

    public List<MatchedRectangle> performTemplateMatchForGameObjectsWithMask(Mat imageToSearchFor, Mat imageToSearchIn, double threshold) {
        List<MatchedRectangle> matchRectangles = new ArrayList<>();

        // Split the source image into its channels
        List<Mat> sourceChannels = new ArrayList<>();
        Core.split(imageToSearchFor, sourceChannels);

        Mat imageToSearchForBGR = new Mat();
        Mat mask = new Mat();

        // Check if the source image has an alpha channel
        if (imageToSearchFor.channels() >= 4) {
            // Use the first three channels (BGR) for matching
            Core.merge(new ArrayList<>(sourceChannels.subList(0, 3)), imageToSearchForBGR);

            // Use the fourth channel (alpha) for masking
            mask = sourceChannels.get(3);
            Core.compare(mask, new Scalar(0), mask, Core.CMP_GT);
        } else {
            // If no alpha channel, use the source image as is
            imageToSearchForBGR = imageToSearchFor;
        }

        // Perform the template matching
        Mat result = new Mat();
        Imgproc.matchTemplate(imageToSearchIn, imageToSearchForBGR, result, Imgproc.TM_CCOEFF_NORMED, mask);

        // Find all matches above the threshold
        for (int y = 0; y < result.rows(); y++) {
            for (int x = 0; x < result.cols(); x++) {
                double matchValue = result.get(y, x)[0];
                if (matchValue >= threshold && matchValue <= 1.1) {
                    // Avoid matching the same location again
                    Imgproc.floodFill(result, new Mat(), new Point(x, y), new Scalar(0), new Rect(), new Scalar(0), new Scalar(0.1));

                    // Add the match to the list
                    matchRectangles.add(new MatchedRectangle(x, y, imageToSearchForBGR.width(), imageToSearchForBGR.height(), matchValue));
                }
            }
        }
        return matchRectangles;
    }

    public MatchedRectangle returnBestMatchObjectWithMask(Mat imageToSearchIn, Mat imageToFind, Mat mask, double threshold) {
        if (imageToFind.type() != imageToSearchIn.type() || imageToFind.depth() != imageToSearchIn.depth()) {
            logger.devLog("Image types or depths are not compatible for template matching.");
            return null;
        }

        Mat result = new Mat();
        try {
            if (mask != null) {
                Imgproc.matchTemplate(imageToSearchIn, imageToFind, result, Imgproc.TM_CCOEFF_NORMED, mask);
            } else {
                Imgproc.matchTemplate(imageToSearchIn, imageToFind, result, Imgproc.TM_CCOEFF_NORMED);
            }
        } catch (CvException e) {
            logger.devLog("OpenCV Error in matchTemplate: " + e.getMessage());
            return null;
        }

        double maxMatchScore = Double.MIN_VALUE;
        int maxX = -1;
        int maxY = -1;

        // Search for the match with the highest score above the threshold
        for (int y = 0; y < result.rows(); y++) {
            for (int x = 0; x < result.cols(); x++) {
                double matchScore = result.get(y, x)[0];
                if (matchScore >= threshold && matchScore > maxMatchScore) {
                    maxMatchScore = matchScore;
                    maxX = x;
                    maxY = y;
                }
            }
        }

        if (maxX != -1) {
            logger.devLog("Best match score: " + maxMatchScore);
            return new MatchedRectangle(maxX, maxY, imageToFind.width(), imageToFind.height(), maxMatchScore);
        } else {
            return null; // No match found or thread interrupted
        }
    }

    public MatchedRectangle returnFirstMatchObjectWithMask(Mat imageToSearchIn, Mat imageToFind, Mat mask, double threshold) {
        if (imageToFind.type() != imageToSearchIn.type() || imageToFind.depth() != imageToSearchIn.depth()) {
            logger.devLog("Image types or depths are not compatible for template matching.");
            return null;
        }

        Mat result = new Mat();
        try {
            if (mask != null) {
                Imgproc.matchTemplate(imageToSearchIn, imageToFind, result, Imgproc.TM_CCOEFF_NORMED, mask);
            } else {
                Imgproc.matchTemplate(imageToSearchIn, imageToFind, result, Imgproc.TM_CCOEFF_NORMED);
            }
        } catch (CvException e) {
            logger.devLog("OpenCV Error in matchTemplate: " + e.getMessage());
            return null;
        }

        // Search for the first match above the threshold
        for (int y = 0; y < result.rows(); y++) {
            for (int x = 0; x < result.cols(); x++) {
                double matchScore = result.get(y, x)[0];
                if (matchScore >= threshold && matchScore <= 1.1) {
                    logger.devLog("First match score: " + matchScore);

                    return new MatchedRectangle(x, y, imageToFind.width(), imageToFind.height(), matchScore);
                }
            }
        }
        return null; // No match found or thread interrupted
    }
}