package osr.mapping;

import helpers.CacheManager;
import helpers.Logger;
import helpers.scripts.CancellationToken;
import helpers.utils.GameObject;
import javafx.scene.image.Image;
import javafx.scene.shape.Circle;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.photo.Photo;
import osr.mapping.utils.MinimapProjections;
import osr.utils.ImageUtils;
import osr.utils.MatchedObjects;
import scripts.ScriptInfo;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class Minimap {
    private final CacheManager cacheManager;
    private final Logger logger;
    private final MinimapProjections minimapProjections;

    private final ScriptInfo scriptInfo;
    private final ImageUtils imageUtils;

    public Minimap(CacheManager cacheManager, Logger logger, MinimapProjections minimapProjections, ScriptInfo scriptInfo, ImageUtils imageUtils) {
        this.cacheManager = cacheManager;
        this.logger = logger;
        this.minimapProjections = minimapProjections;
        this.scriptInfo = scriptInfo;
        this.imageUtils = imageUtils;
    }

    public Circle getMinimapPosition(String device) {
        Circle cachedCircle = cacheManager.getMinimapPosition(device);
        if (cachedCircle != null) {
            return cachedCircle;
        } else {
            Circle newCircle = minimapProjections.getMinimapPosition(device);
            if (newCircle != null) {
                cacheManager.setMinimapPosition(device, newCircle);
                return newCircle;
            }
            return null;
        }
    }

    public BufferedImage getMinimap(String device, Boolean bool) {
        return minimapProjections.getMinimapImage(device);
    }

    public List<Rectangle> findDots(BufferedImage minimap, GameObject... gameObject) {
        return minimapProjections.findDots(minimap, gameObject);
    }

    public BufferedImage inpaintRectangles(BufferedImage minimap, List<Rectangle> rectangles) {
        return minimapProjections.inpaintRectangles(minimap, rectangles);
    }

    public Point getMinimapCenter(String device) {
        // Check if the center is already cached
        Point cachedCenter = cacheManager.getMinimapCenter(device);
        if (cachedCenter != null) {
            return cachedCenter;
        }

        // Get the minimap position circle using the existing method
        Circle minimapCircle = getMinimapPosition(device);

        // Calculate the center of the minimap circle
        double centerX = minimapCircle.getCenterX();
        double centerY = minimapCircle.getCenterY();

        // Cache the center coordinates for future use
        Point centerPoint = new Point((int) centerX, (int) centerY);
        cacheManager.setMinimapCenter(device, centerPoint);

        return centerPoint;
    }

    // Public method returning Mat
    public Mat getCleanMinimapMat(String device, int regionSize, boolean saveImage, boolean cropCircle) {
        return processMinimapImage(device, regionSize, saveImage, cropCircle, true);
    }

    public Mat getCleanMinimapMat(String device, boolean saveImage, boolean cropCircle) {
        return processMinimapImage(device, 92, saveImage, cropCircle, true);
    }

    public Image getCleanMinimap(String device, int regionSize, boolean saveImage, boolean cropCircle, boolean ignoreCancellationToken) {
        // Process the minimap image and convert it to an Image object for the GUI
        Mat minimapMat = processMinimapImage(device, regionSize, saveImage, cropCircle, ignoreCancellationToken);
        return imageUtils.matToImage(minimapMat);
    }

    public Image getCleanMinimap(String device, boolean saveImage, boolean cropCircle, boolean ignoreCancellationToken) {
        return getCleanMinimap(device, 92, saveImage, cropCircle, ignoreCancellationToken);
    }

    private Mat processMinimapImage(String device, int regionSize, boolean saveImage, boolean cropCircle, boolean ignoreCancellationToken) {
        Mat minimapImage = getMinimap(device);
        if (minimapImage == null) {
            return null;
        }

        if (cropCircle) {
            Mat cropped = cropCircleRegion(minimapImage, regionSize);
            minimapImage.release();
            minimapImage = cropped;
        }

        List<MatchedObjects> matches = minimapProjections.getMinimapMatches(
                minimapImage,
                GameObject.PLAYER,
                GameObject.NPC,
                GameObject.ITEM
        );

        Mat mask = null;
        Mat inpaintedImage;

        try {
            mask = buildMinimapMask(device, minimapImage, matches, ignoreCancellationToken);
            if (mask == null) {
                return minimapImage;
            }

            inpaintedImage = new Mat();
            Photo.inpaint(minimapImage, mask, inpaintedImage, 5, Photo.INPAINT_TELEA);

            if (saveImage) {
                saveImageToFile(inpaintedImage);
            }
            return inpaintedImage;
        } finally {
            if (mask != null) {
                mask.release();
            }
        }
    }

    /**
     * Helper: Crops the minimap image down to a rectangle with specified regionSize (Default 92).
     */
    private Mat cropCircleRegion(Mat minimapImage, int regionSize) {
        int centerRow = minimapImage.rows() / 2;
        int centerCol = minimapImage.cols() / 2;

        // Centered rectangle
        Rect centerRect = new Rect(
                Math.max(centerCol - regionSize / 2, 0),
                Math.max(centerRow - regionSize / 2, 0),
                regionSize,
                regionSize
        );

        centerRect.width = Math.min(centerRect.width, minimapImage.cols() - centerRect.x);
        centerRect.height = Math.min(centerRect.height, minimapImage.rows() - centerRect.y);
        return new Mat(minimapImage, centerRect);
    }

    /**
     * Helper: Builds a mask by marking matched objects, plus black pixels underneath each match.
     */
    private Mat buildMinimapMask(String device, Mat minimapImage,
                                 List<MatchedObjects> matches,
                                 boolean ignoreCancellationToken) {
        // Create an empty mask
        Mat mask = Mat.zeros(minimapImage.size(), CvType.CV_8UC1);

        for (MatchedObjects matchedObject : matches) {
            // Allow user cancellation between matched objects
            if (!ignoreCancellationToken && isCancelled(device)) {
                return mask;
            }

            for (Rectangle rectangle : matchedObject.getRectangles()) {
                fillMaskRegion(mask, rectangle);
                fillMaskForBlackPixelsUnderneath(minimapImage, mask, rectangle);
            }
        }
        return mask;
    }

    /**
     * Fills the corresponding region of the mask with 255.
     */
    private void fillMaskRegion(Mat mask, Rectangle rect) {
        Rect opencvRect = new Rect(rect.x, rect.y, rect.width, rect.height);
        Mat regionMask = mask.submat(opencvRect);
        regionMask.setTo(new Scalar(255));
    }

    /**
     * Checks for black pixels directly underneath the matched rectangle
     * and marks them in the mask as well.
     */
    private void fillMaskForBlackPixelsUnderneath(Mat minimapImage, Mat mask, Rectangle rect) {
        Rect areaUnderneath = new Rect(
                rect.x,
                rect.y + rect.height,
                rect.width,
                1
        );

        if (areaUnderneath.y + areaUnderneath.height > minimapImage.rows()) {
            return;
        }

        Mat regionUnderneath = minimapImage.submat(areaUnderneath);
        Mat grayRegionUnderneath = new Mat();
        Imgproc.cvtColor(regionUnderneath, grayRegionUnderneath, Imgproc.COLOR_BGRA2GRAY);

        for (int x = 0; x < grayRegionUnderneath.cols(); x++) {
            double[] pixel = grayRegionUnderneath.get(0, x);
            if (pixel != null && pixel[0] == 0) {
                mask.put(areaUnderneath.y, areaUnderneath.x + x, 255);
            }
        }
    }

    /**
     * Checks the script's cancellation token.
     * Returns true if process is cancelled.
     */
    private boolean isCancelled(String device) {
        CancellationToken cancellationToken = scriptInfo.getCancellationToken(device);
        return (cancellationToken != null && cancellationToken.isCancellationRequested());
    }

    public Mat getMinimap(String device) {
        return minimapProjections.getMinimapMat(device, getMinimapPosition(device));
    }

    public boolean isPointOn(Point point, String device) {
        Circle minimapCircle = getMinimapPosition(device);
        if (minimapCircle != null) {
            double dx = point.getX() - minimapCircle.getCenterX();
            double dy = point.getY() - minimapCircle.getCenterY();
            double distanceSquared = dx * dx + dy * dy;
            double adjustedRadius = minimapCircle.getRadius() - 3;  // Subtract 3 pixel from the radius
            double radiusSquared = adjustedRadius * adjustedRadius;
            return distanceSquared <= radiusSquared;
        }
        return false;
    }

    private void saveImageToFile(Mat image) {
        // Get the current date and time
        LocalDateTime now = LocalDateTime.now();
        // Format the date and time in a file-friendly format
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        String dateTime = now.format(formatter);

        String homeDirectory = System.getProperty("user.home");
        // Include the formatted date and time in the filename
        String path = homeDirectory + File.separator + "Downloads" + File.separator + "cleanedMinimap_" + dateTime + ".png";
        boolean result = Imgcodecs.imwrite(path, image);
        if (result) {
            logger.devLog("Image saved to " + path);
        } else {
            logger.devLog("Failed to save image to " + path);
        }
    }
}
