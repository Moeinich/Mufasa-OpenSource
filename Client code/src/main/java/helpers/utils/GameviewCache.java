package helpers.utils;

import helpers.Logger;
import helpers.emulator.utils.CircularBuffer;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import osr.utils.ImageUtils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * GameviewCache manages the caching of game view images for different devices.
 * It ensures that native memory held by OpenCV Mats is properly managed to prevent memory leaks.
 */
public class GameviewCache {
    private final ImageUtils imageUtils;
    private final Logger logger;
    private final Mat templateMat;
    private final Map<String, CircularBuffer<BufferedImage>> emulatorBuffers = new ConcurrentHashMap<>();
    private final Map<String, CircularBuffer<Mat>> matCache = new ConcurrentHashMap<>();

    public GameviewCache(ImageUtils imageUtils, Logger logger) {
        this.imageUtils = imageUtils;
        this.logger = logger;

        templateMat = imageUtils.pathToMat("/assets/template.png");
    }

    public void cleanCache(String device) {
        emulatorBuffers.remove(device);
    }

    public void clearCache() {
        emulatorBuffers.clear();
    }

    public BufferedImage getBuffer(String device) {
        return emulatorBuffers
                .computeIfAbsent(device, k -> new CircularBuffer<>(5))
                .getLatest();
    }

    public BufferedImage getSubBuffer(String device, Rectangle subMatRect) {
        BufferedImage image = getBuffer(device);
        return image.getSubimage(subMatRect.x, subMatRect.y, subMatRect.width, subMatRect.height);
    }

    /**
     * Caches the game view image for a specific device.
     *
     * @param device Device identifier.
     * @param image  BufferedImage to cache.
     */
    public void cacheGameview(String device, BufferedImage image) {
        if (device == null || image == null) {
            logger.print("Device or image is null. Cannot cache gameview.");
            return;
        }

        // Cache BufferedImage
        emulatorBuffers.computeIfAbsent(device, k -> new CircularBuffer<>(5)).add(image);

        // Convert to Mat and cache it
        CircularBuffer<Mat> matBuffer = matCache.computeIfAbsent(device, k -> new CircularBuffer<>(5));
        Mat newMat = imageUtils.bufferedImageToMat(image);

        synchronized (matBuffer) {
            Mat oldMat = matBuffer.addAndGetReplaced(newMat); // Add new Mat and get replaced one
            if (oldMat != null) {
                oldMat.release(); // Safely release the replaced Mat
            }
        }
    }

    /**
     * Retrieves the cached game view as a Mat for a specific device.
     *
     * @param device Device identifier.
     * @return Cached Mat, or the templateMat if not available.
     */
    public Mat getGameview(String device) {
        CircularBuffer<Mat> buffer = matCache.get(device);
        if (buffer == null) {
            logger.print("No cached gameview available for device: " + device);
            return templateMat.clone();
        }

        synchronized (buffer) {
            Mat latestMat = buffer.getLatest();
            if (latestMat == null) {
                logger.print("No cached gameview available for device: " + device);
                return templateMat.clone();
            }
            return latestMat.clone(); // Clone the Mat for safe external use
        }
    }

    public Mat getSubMat(String device, Rect subMat) {
        CircularBuffer<Mat> buffer = matCache.get(device);
        if (buffer == null) {
            logger.print("No valid cached gameview available for device: " + device);
            return templateMat.clone();
        }

        Mat matImage;
        synchronized (buffer) {
            matImage = buffer.getLatest();
            if (matImage == null || matImage.empty()) {
                logger.print("No valid cached gameview or cached gameview is empty for device: " + device);
                return templateMat.clone();
            }
            matImage = matImage.clone();
        }

        // Adjust subMat to fit within the Mat's bounds
        int x = Math.max(subMat.x, 0);
        int y = Math.max(subMat.y, 0);
        int width = Math.min(subMat.width, matImage.cols() - x);
        int height = Math.min(subMat.height, matImage.rows() - y);

        if (width <= 0 || height <= 0) {
            logger.print("Adjusted ROI is invalid. Original ROI: " + subMat);
            return new Mat(); // Return an empty Mat if the ROI is invalid
        }

        Rect boundedRect = new Rect(x, y, width, height);
        return matImage.submat(boundedRect);
    }
}