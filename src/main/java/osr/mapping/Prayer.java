package osr.mapping;

import helpers.GetGameView;
import helpers.Logger;
import helpers.openCV.ImageRecognition;
import helpers.openCV.utils.MatchedRectangle;
import helpers.visualFeedback.FeedbackObservables;
import org.opencv.core.Mat;
import osr.utils.ImageUtils;
import scripts.APIClasses.ClientAPI;
import scripts.APIClasses.ConditionAPI;
import utils.Constants;

import java.awt.*;
import java.util.AbstractMap.SimpleEntry;
import java.util.concurrent.ConcurrentHashMap;

public class Prayer {
    private final Logger logger;
    private final GetGameView getGameView;
    private final ClientAPI clientAPI;
    private final ConditionAPI conditionAPI;
    private final ImageRecognition imageRecognition;
    private final ImageUtils imageUtils;
    private final String inactivePrayersPath = "/osrsAssets/Prayer/Inactive/";
    private final String activePrayersPath = "/osrsAssets/Prayer/Active/";
    // Caches
    private final ConcurrentHashMap<String, MatchedRectangle> prayerCache = new ConcurrentHashMap<>();
    double thresholdPray = 0.98;

    public Prayer(Logger logger, ImageRecognition imageRecognition, GetGameView getGameView, ClientAPI clientAPI, ConditionAPI conditionAPI, ImageUtils imageUtils) {
        this.logger = logger;
        this.imageRecognition = imageRecognition;
        this.getGameView = getGameView;
        this.clientAPI = clientAPI;
        this.conditionAPI = conditionAPI;
        this.imageUtils = imageUtils;
    }

    // Activate method
    public void activatePrayer(String device, String pngName) {
        String cacheKey = "activatePrayer" + "-" + pngName + "-" + device;
        MatchedRectangle cachedButtonRect = prayerCache.get(cacheKey);
        String fullPngName = pngName;

        if (cachedButtonRect != null) {
            logger.devLog("Using cached location for prayer on " + device);
            clientAPI.tap(cachedButtonRect, device);
            conditionAPI.wait(() -> isPrayerActive(device, pngName), 500, 20);
            return;
        }

        // Ensure the pngName ends with ".png"
        if (!fullPngName.endsWith(".png")) {
            fullPngName += ".png";
        }

        Mat inventoryArea = getGameView.getSubmat(device, Constants.INVENTORY_RECT);
        FeedbackObservables.rectangleObservable.setValue(device, Constants.INVENTORY_RECTANGLE);

        try {
            Mat imageToFind = imageUtils.pathToMat(inactivePrayersPath + fullPngName);
            SimpleEntry<Mat, Mat> convertedImageToFind = imageUtils.convertToColorWithAlpha(imageToFind);
            Mat colorImageToFind = convertedImageToFind.getKey();
            Mat mask = convertedImageToFind.getValue(); // Alpha channel

            MatchedRectangle result = imageRecognition.returnBestMatchObjectWithMask(
                    inventoryArea, colorImageToFind, mask, thresholdPray
            );

            if (result != null) {
                logger.devLog("A valid match was found, activating Prayer.");

                // Adjust the rectangle to the full game view coordinates
                Rectangle adjustedRect = new Rectangle(
                        result.x + Constants.INVENTORY_RECT.x,
                        result.y + Constants.INVENTORY_RECT.y,
                        result.width,
                        result.height
                );

                MatchedRectangle adjustedMatchedRect = new MatchedRectangle(adjustedRect.x, adjustedRect.y, adjustedRect.width, adjustedRect.height);
                clientAPI.tap(adjustedMatchedRect, device);

                prayerCache.put(cacheKey, adjustedMatchedRect);
                logger.devLog("Cached the prayer location for " + device);
                conditionAPI.wait(() -> isPrayerActive(device, pngName), 500, 20);
            } else {
                logger.devLog("Prayer icon was not found (" + inactivePrayersPath + fullPngName + "), or the prayer tab is not open.");
            }
        } finally {
            if (inventoryArea != null) inventoryArea.release();
        }
    }

    // isActive method
    public boolean isPrayerActive(String device, String pngName) {
        String fullPngName = pngName;

        // Ensure the pngName ends with ".png"
        if (!fullPngName.endsWith(".png")) {
            fullPngName += ".png";
        }

        // Add "activated_" prefix if not already present
        if (!fullPngName.startsWith("activated_")) {
            fullPngName = "activated_" + fullPngName;
        }

        Mat inventoryArea = getGameView.getSubmat(device, Constants.INVENTORY_RECT);
        FeedbackObservables.rectangleObservable.setValue(device, Constants.INVENTORY_RECTANGLE);

        try {
            Mat imageToFind = imageUtils.pathToMat(activePrayersPath + fullPngName);
            SimpleEntry<Mat, Mat> convertedImageToFind = imageUtils.convertToColorWithAlpha(imageToFind);
            Mat colorImageToFind = convertedImageToFind.getKey();
            Mat mask = convertedImageToFind.getValue(); // Alpha channel

            MatchedRectangle result = imageRecognition.returnBestMatchObjectWithMask(
                    inventoryArea, colorImageToFind, mask, thresholdPray
            );

            if (result != null) {
                logger.devLog("Prayer is active!");
                return true;
            } else {
                logger.devLog("Prayer is not active (" + activePrayersPath + fullPngName + "), or the prayer tab is not open.");
                return false;
            }
        } finally {
            if (inventoryArea != null) inventoryArea.release();
        }
    }
}
