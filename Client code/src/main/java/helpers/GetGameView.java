package helpers;

import helpers.utils.GameviewCache;
import org.opencv.core.Mat;
import org.opencv.core.Rect;

import java.awt.*;
import java.awt.image.BufferedImage;

public class GetGameView {
    private final GameviewCache gameviewCache;
    private final CacheManager cacheManager;

    public GetGameView(GameviewCache gameviewCache, CacheManager cacheManager) {
        this.gameviewCache = gameviewCache;
        this.cacheManager = cacheManager;
    }

    public BufferedImage getBuffered(String device) {
        return gameviewCache.getBuffer(device);
    }

    public BufferedImage getSubBuffered(String device, Rectangle subMatRect) {
        return gameviewCache.getSubBuffer(device, subMatRect);
    }

    public Mat getMat(String device) {
        return gameviewCache.getGameview(device);
    }

    public Mat getSubmat(String device, Rect subMatRect) {
        return gameviewCache.getSubMat(device, subMatRect);
    }

    public Point getGameviewCenter(String device) {
        // Check if the center is already cached
        Point cachedCenter = cacheManager.getGameviewCenter(device);
        if (cachedCenter != null) {
            return cachedCenter;
        }

        // Get the gameview Mat using the existing method
        Mat gameviewMat = getMat(device);

        // Calculate the center of the gameview Mat
        int centerX = gameviewMat.cols() / 2;
        int centerY = gameviewMat.rows() / 2;

        // Cache the center coordinates for future use
        Point centerPoint = new Point(centerX, centerY);
        cacheManager.setGameviewCenter(device, centerPoint);

        return centerPoint;
    }
}