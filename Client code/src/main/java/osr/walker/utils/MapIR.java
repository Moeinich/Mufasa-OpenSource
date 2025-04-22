package osr.walker.utils;

import helpers.Logger;
import helpers.ThreadManager;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

public class MapIR {
    private final Logger logger;

    private final ThreadLocal<WalkerCache> threadLocalCache = ThreadLocal.withInitial(WalkerCache::new);
    private final int scale = 4;
    private final ExecutorService threadExecutor = ThreadManager.getInstance().getUnifiedExecutor();

    public MapIR(Logger logger) {
        this.logger = logger;
    }

    /**
     * Returns the map position of our character as LOCAL coordinates (Local to the stitched map)
     * @param miniMap The minimap mat
     * @param worldMap The generated chunk map
     * @return local position on stitched map
     */
    public PositionResult getPosition(Mat miniMap, Mat worldMap) {
        Callable<PositionResult> positionTask = () -> performTemplateMatchForMap(miniMap, worldMap);
        try {
            return threadExecutor.invokeAny(List.of(positionTask));
        } catch (InterruptedException | ExecutionException e) {
            logger.print("Error in position finding task: " + e.getMessage());
            return new PositionResult(new Point(0, 0), 0.0, worldMap.height(), worldMap.width());
        }
    }

    private PositionResult performTemplateMatchForMap(Mat miniMap, Mat worldMap) {
        if (!validateInput(miniMap, worldMap)) {
            return new PositionResult(new Point(0, 0), 0.0, worldMap.height(), worldMap.width());
        }

        WalkerCache cache = threadLocalCache.get();

        // Scale down the maps
        Mat scaledMiniMap = null;
        Mat scaledWorldMap = null;
        Mat result = null;

        try {
            scaledMiniMap = scaleImage(miniMap, scale);
            scaledWorldMap = scaleImage(worldMap, scale);

            // Perform template matching on scaled maps
            result = new Mat();
            Imgproc.matchTemplate(scaledWorldMap, scaledMiniMap, result, Imgproc.TM_CCOEFF_NORMED);

            // Find top 3 matches
            List<Core.MinMaxLocResult> topMatches = findTopMatches(result, 3);

            // Check if any of the top matches have confidence below the threshold
            boolean suspiciousConfidence = topMatches.stream().allMatch(mmr -> mmr.maxVal > 0.995 || mmr.maxVal < 0.40);

            if (suspiciousConfidence) {
                logger.devLog("Low confidence detected. Performing full-scale search.");
                PositionResult fullScaleResult = performFullScaleSearch(miniMap, worldMap);
                if (fullScaleResult != null) {
                    cache.updateCache(topMatches, fullScaleResult.getPosition(), fullScaleResult.getConfidence());
                    return fullScaleResult;
                } else {
                    logger.devLog("Full-scale search failed to find a valid match.");
                    return new PositionResult(new Point(0, 0), 0, worldMap.cols(), worldMap.rows());
                }
            }

            // Check if the result is similar to the last search
            if (cache.isSameAsLastSearch(topMatches)) {
                return new PositionResult(cache.lastFoundPosition, cache.lastConfidence, worldMap.cols(), worldMap.rows());
            }

            // Find the best match in full-size image around top matches
            PositionResult bestMatch = findBestMatchInFullSizeImage(miniMap, worldMap, topMatches);

            if (bestMatch == null) {
                logger.devLog("Failed to get the tile result!");
                return new PositionResult(new Point(0, 0), 0, worldMap.cols(), worldMap.rows());
            }

            // Update the cache with the new results
            cache.updateCache(topMatches, bestMatch.getPosition(), bestMatch.getConfidence());

            return bestMatch;
        } finally {
            // Release the scaled images and result Mat to free native memory
            if (miniMap != null) {
                miniMap.release();
            }
            if (scaledMiniMap != null) {
                scaledMiniMap.release();
            }
            if (scaledWorldMap != null) {
                scaledWorldMap.release();
            }
            if (result != null) {
                result.release();
            }
        }
    }

    private PositionResult performFullScaleSearch(Mat miniMap, Mat worldMap) {
        Mat fullResult = new Mat();
        try {
            Imgproc.matchTemplate(worldMap, miniMap, fullResult, Imgproc.TM_CCOEFF_NORMED);
            Core.MinMaxLocResult mmr = Core.minMaxLoc(fullResult);

            if (mmr.maxVal < 0.25) { // Optional: Further validate confidence
                logger.devLog(String.format("Full-scale search confidence too low: %.4f", mmr.maxVal));
                return null;
            }

            Point bestPoint = calculateCenterPoint(mmr.maxLoc, new Rect(0, 0, worldMap.cols(), worldMap.rows()), miniMap.size());
            Point adjustedPoint = adjustPoint(bestPoint, 3);

            logger.devLog(String.format("Full-scale match found at (%.2f, %.2f) with confidence %.4f",
                    adjustedPoint.x, adjustedPoint.y, mmr.maxVal));

            return new PositionResult(adjustedPoint, mmr.maxVal, worldMap.cols(), worldMap.rows());
        } catch (Exception e) {
            logger.print("Exception during full-scale search: " + e.getMessage());
            return null;
        } finally {
            if (fullResult != null) {
                fullResult.release();
            }
        }
    }

    private Mat scaleImage(Mat image, double scale) {
        Mat scaledImage = new Mat();
        Imgproc.resize(image, scaledImage, new Size(image.cols() / scale, image.rows() / scale));
        return scaledImage;
    }

    private PositionResult findBestMatchInFullSizeImage(Mat miniMap, Mat worldMap, List<Core.MinMaxLocResult> topMatches) {
        double bestConfidence = -1;
        Point bestFullSizeCenterPoint = null;

        for (Core.MinMaxLocResult mmr : topMatches) {
            Point fullSizeTopLeft = scalePoint(mmr.maxLoc, scale);

            // Calculate the search region
            Rect searchRegion = calculateSearchRegion(miniMap, worldMap, fullSizeTopLeft);

            Mat subWorldMap = new Mat(worldMap, searchRegion);
            Mat fullResult = new Mat();
            Imgproc.matchTemplate(miniMap, subWorldMap, fullResult, Imgproc.TM_CCOEFF_NORMED);

            Core.MinMaxLocResult fullMmr = Core.minMaxLoc(fullResult);

            if (fullMmr.maxVal > bestConfidence) {
                bestConfidence = fullMmr.maxVal;
                bestFullSizeCenterPoint = calculateCenterPoint(fullMmr.maxLoc, searchRegion, miniMap.size());
            }
        }

        if (bestFullSizeCenterPoint == null) {
            return null;
        }

        Point adjustedPoint = adjustPoint(bestFullSizeCenterPoint, 3);
        return new PositionResult(adjustedPoint, bestConfidence, worldMap.cols(), worldMap.rows());
    }

    private Point scalePoint(Point point, double scale) {
        return new Point(point.x * scale, point.y * scale);
    }

    private Rect calculateSearchRegion(Mat miniMap, Mat worldMap, Point fullSizeTopLeft) {
        int searchRegionWidth = Math.min(miniMap.cols() * 2, worldMap.cols());
        int searchRegionHeight = Math.min(miniMap.rows() * 2, worldMap.rows());

        int searchRegionX = Math.max(0, (int) fullSizeTopLeft.x - miniMap.cols() / 2);
        int searchRegionY = Math.max(0, (int) fullSizeTopLeft.y - miniMap.rows() / 2);

        searchRegionWidth = Math.min(searchRegionWidth, worldMap.cols() - searchRegionX);
        searchRegionHeight = Math.min(searchRegionHeight, worldMap.rows() - searchRegionY);

        return new Rect(searchRegionX, searchRegionY, searchRegionWidth, searchRegionHeight);
    }

    private Point calculateCenterPoint(Point matchLoc, Rect searchRegion, Size miniMapSize) {
        return new Point(matchLoc.x + searchRegion.x + miniMapSize.width / 2, matchLoc.y + searchRegion.y + miniMapSize.height / 2);
    }

    private Point adjustPoint(Point point, int adjustment) {
        return new Point(point.x - adjustment, point.y - adjustment);
    }

    private List<Core.MinMaxLocResult> findTopMatches(Mat result, int topN) {
        List<Core.MinMaxLocResult> topMatches = new ArrayList<>();
        for (int i = 0; i < topN; i++) {
            Core.MinMaxLocResult mmr = Core.minMaxLoc(result);
            topMatches.add(mmr);
            Imgproc.rectangle(result, mmr.maxLoc, new Point(mmr.maxLoc.x + 1, mmr.maxLoc.y + 1), new Scalar(0, 0, 0), -1);
        }
        return topMatches;
    }

    private boolean validateInput(Mat miniMap, Mat worldMap) {
        if (miniMap == null || worldMap == null) {
            logger.print("Inputs for OCV action null, aborting..");
            return false;
        }
        return true;
    }
}
