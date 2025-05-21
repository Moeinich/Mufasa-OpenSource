package osr.mapping;

import helpers.CacheManager;
import helpers.GetGameView;
import helpers.Logger;
import helpers.Color.ColorFinder;
import helpers.openCV.ImageRecognition;
import helpers.openCV.utils.MatchedRectangle;
import helpers.utils.Spells;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import osr.mapping.utils.MagicSpellList;
import osr.utils.ImageUtils;
import scripts.APIClasses.ClientAPI;

import java.awt.*;
import java.util.AbstractMap.SimpleEntry;

public class Magic {
    private final CacheManager cacheManager;
    private final Logger logger;
    private final GetGameView getGameView;
    private final ClientAPI clientAPI;
    private final ImageRecognition imageRecognition;
    private final ImageUtils imageUtils;
    private final ColorFinder colorFinder;

    private final String SpellbookPath = "/osrsAssets/Spellbook/";
    double thresholdSpell = 0.95;
    private final Rectangle spellbookROI = new Rectangle(596, 221, 197, 246);

    public Magic(CacheManager cacheManager, Logger logger, ImageRecognition imageRecognition, GetGameView getGameView, ClientAPI clientAPI, ImageUtils imageUtils, ColorFinder colorFinder) {
        this.cacheManager = cacheManager;
        this.logger = logger;
        this.imageRecognition = imageRecognition;
        this.getGameView = getGameView;
        this.clientAPI = clientAPI;
        this.imageUtils = imageUtils;
        this.colorFinder = colorFinder;
    }

    public void tapSpell(String device, String spellName) {
        String cacheKey = "tapSpell" + "-" + spellName + "-" + device;
        MatchedRectangle cachedSpellRect = cacheManager.getSpell(cacheKey);

        if (cachedSpellRect != null) {
            logger.devLog("Using cached location for tapSpell on " + device);
            clientAPI.tap(cachedSpellRect, device);
            return;
        }

        // Get the subfolder based on the spell name from MagicSpellList
        String subfolder = MagicSpellList.getSubfolder(spellName);
        // Create the full path to the spell image
        String fullPngName = SpellbookPath + subfolder + spellName;
        String scaledPngName = fullPngName.replace(".png", "_scaled.png");

        Mat imageToFind = null;
        Mat scaledImageToFind = null;
        Mat imageToFindIn = null;
        Mat imageToFindInRoi = null;
        Mat colorImageToFind = null;
        Mat mask = null;
        Mat scaledColorImageToFind = null;
        Mat scaledMask = null;

        try {
            // Load images
            imageToFind = imageUtils.pathToMat(fullPngName);
            scaledImageToFind = imageUtils.pathToMat(scaledPngName);
            imageToFindIn = getGameView.getMat(device);

            // Extract the ROI from the game view image
            imageToFindInRoi = new Mat(imageToFindIn, new Rect(spellbookROI.x, spellbookROI.y, spellbookROI.width, spellbookROI.height));

            // Convert images to color and mask (with alpha)
            SimpleEntry<Mat, Mat> convertedImageToFind = imageUtils.convertToColorWithAlpha(imageToFind);
            SimpleEntry<Mat, Mat> convertedScaledImageToFind = imageUtils.convertToColorWithAlpha(scaledImageToFind);

            colorImageToFind = convertedImageToFind.getKey();
            mask = convertedImageToFind.getValue();

            scaledColorImageToFind = convertedScaledImageToFind.getKey();
            scaledMask = convertedScaledImageToFind.getValue();

            logger.devLog("Attempting to find spell at regular scale.");
            // Try finding the spell at regular scale
            MatchedRectangle spell = imageRecognition.returnBestMatchObjectWithMask(imageToFindInRoi, colorImageToFind, mask, thresholdSpell);

            if (spell == null) {
                logger.devLog("Spell icon was not found at regular scale. Trying pre-scaled version.");
                spell = imageRecognition.returnBestMatchObjectWithMask(imageToFindInRoi, scaledColorImageToFind, scaledMask, thresholdSpell);

                if (spell != null) {
                    logger.devLog("A valid match was found for " + spellName + " using pre-scaled image.");
                } else {
                    logger.devLog("Spell icon was not found using pre-scaled image. Spell tab might not be open.");
                }
            } else {
                logger.devLog("A valid match was found for " + spellName + " at regular scale.");
            }

            if (spell != null) {
                // Adjust the coordinates of the matched rectangle to the full image
                MatchedRectangle adjustedSpell = new MatchedRectangle(
                        spell.x + spellbookROI.x,
                        spell.y + spellbookROI.y,
                        spell.width,
                        spell.height,
                        spell.getMatchValue()
                );
                clientAPI.tap(adjustedSpell, device);
                cacheManager.setSpell(cacheKey, adjustedSpell);
                logger.devLog("Cached the " + spellName + " spell location for " + device);
            } else {
                logger.devLog("Failed to find and tap the spell: " + spellName + " on device: " + device);
            }
        } catch (Exception e) {
            logger.devLog("An error occurred while processing spell: " + spellName + " - " + e.getMessage());
        } finally {
            // Release Mat objects
            if (imageToFind != null) imageToFind.release();
            if (scaledImageToFind != null) scaledImageToFind.release();
            if (imageToFindIn != null) imageToFindIn.release();
            if (imageToFindInRoi != null) imageToFindInRoi.release();
            if (colorImageToFind != null) colorImageToFind.release();
            if (mask != null) mask.release();
            if (scaledColorImageToFind != null) scaledColorImageToFind.release();
            if (scaledMask != null) scaledMask.release();
        }
    }

    public void castSpell(Spells spell, String device) {
        // Normalize the spell name
        String spellName = spell.toString().toLowerCase().replace("_", "") + ".png";

        String cacheKey = "castSpell" + "-" + spellName + "-" + device;
        MatchedRectangle cachedSpellRect = cacheManager.getSpell(cacheKey);

        if (cachedSpellRect != null) {
            if (isCastable(spell, device)) {
                logger.devLog("Using cached location for castSpell on " + device);
                clientAPI.tap(cachedSpellRect, device);
            } else {
                logger.devLog("Spell " + spell + " is not cast able currently.");
            }
            return;
        }

        // Get the subfolder based on the spell name from MagicSpellList
        String subfolder = MagicSpellList.getSubfolder(spellName);
        // Create the full path to the spell image
        String fullPngName = SpellbookPath + subfolder + spellName;
        String scaledPngName = fullPngName.replace(".png", "_scaled.png");

        Mat imageToFind = null;
        Mat scaledImageToFind = null;
        Mat imageToFindIn = null;
        Mat imageToFindInRoi = null;
        Mat colorImageToFind = null;
        Mat mask = null;
        Mat scaledColorImageToFind = null;
        Mat scaledMask = null;

        try {
            // Load images
            imageToFind = imageUtils.pathToMat(fullPngName);
            scaledImageToFind = imageUtils.pathToMat(scaledPngName);
            imageToFindIn = getGameView.getMat(device);

            // Extract the ROI from the game view image
            imageToFindInRoi = new Mat(imageToFindIn, new Rect(spellbookROI.x, spellbookROI.y, spellbookROI.width, spellbookROI.height));

            // Convert images to color and mask (with alpha)
            SimpleEntry<Mat, Mat> convertedImageToFind = imageUtils.convertToColorWithAlpha(imageToFind);
            SimpleEntry<Mat, Mat> convertedScaledImageToFind = imageUtils.convertToColorWithAlpha(scaledImageToFind);

            colorImageToFind = convertedImageToFind.getKey();
            mask = convertedImageToFind.getValue();

            scaledColorImageToFind = convertedScaledImageToFind.getKey();
            scaledMask = convertedScaledImageToFind.getValue();

            logger.devLog("Attempting to find spell at regular scale.");
            // Try finding the spell at regular scale
            MatchedRectangle spellRect = imageRecognition.returnBestMatchObjectWithMask(imageToFindInRoi, colorImageToFind, mask, thresholdSpell);

            if (spellRect == null) {
                logger.devLog("Spell icon was not found at regular scale. Trying pre-scaled version.");
                spellRect = imageRecognition.returnBestMatchObjectWithMask(imageToFindInRoi, scaledColorImageToFind, scaledMask, thresholdSpell);

                if (spellRect != null) {
                    logger.devLog("A valid match was found for " + spellName + " using pre-scaled image.");
                } else {
                    logger.devLog("Spell icon was not found using pre-scaled image. Spell tab might not be open.");
                }
            } else {
                logger.devLog("A valid match was found for " + spellName + " at regular scale.");
            }

            if (spellRect != null) {
                // Adjust the coordinates of the matched rectangle to the full image
                MatchedRectangle adjustedSpell = new MatchedRectangle(
                        spellRect.x + spellbookROI.x,
                        spellRect.y + spellbookROI.y,
                        spellRect.width,
                        spellRect.height,
                        spellRect.getMatchValue()
                );
                clientAPI.tap(adjustedSpell, device);
                cacheManager.setSpell(cacheKey, adjustedSpell);
                logger.devLog("Cached the " + spellName + " spell location for " + device);
            } else {
                logger.devLog("Failed to find and tap the spell: " + spellName + " on device: " + device);
            }
        } catch (Exception e) {
            logger.devLog("An error occurred while processing spell: " + spellName + " - " + e.getMessage());
        } finally {
            // Release Mat objects
            if (imageToFind != null) imageToFind.release();
            if (scaledImageToFind != null) scaledImageToFind.release();
            if (imageToFindIn != null) imageToFindIn.release();
            if (imageToFindInRoi != null) imageToFindInRoi.release();
            if (colorImageToFind != null) colorImageToFind.release();
            if (mask != null) mask.release();
            if (scaledColorImageToFind != null) scaledColorImageToFind.release();
            if (scaledMask != null) scaledMask.release();
        }
    }

    Rectangle checkRect = new Rectangle(634, 470, 35, 13);

    public boolean isInfoEnabled(String device) {
        return colorFinder.isRedTinted(device, checkRect, 3.50);
    }

    public boolean isCastable(Spells spell, String device) {
        // Normalize the spell name
        String spellName = spell.toString().toLowerCase().replace("_", "") + ".png";

        String cacheKey = "castSpell" + "-" + spellName + "-" + device;
        MatchedRectangle cachedSpellRect = cacheManager.getSpell(cacheKey);

        if (cachedSpellRect != null) {
            return !colorFinder.isBlackTinted(device, cachedSpellRect, 70);
        }

        // Get the subfolder based on the spell name from MagicSpellList
        String subfolder = MagicSpellList.getSubfolder(spellName);
        String fullPngName = SpellbookPath + subfolder + spellName;
        String scaledPngName = fullPngName.replace(".png", "_scaled.png");

        Mat imageToFind = null;
        Mat scaledImageToFind = null;
        Mat imageToFindIn = null;
        Mat imageToFindInRoi = null;
        Mat colorImageToFind = null;
        Mat mask = null;
        Mat scaledColorImageToFind = null;
        Mat scaledMask = null;

        try {
            // Load images
            imageToFind = imageUtils.pathToMat(fullPngName);
            scaledImageToFind = imageUtils.pathToMat(scaledPngName);
            imageToFindIn = getGameView.getMat(device);

            // Extract the ROI from the game view image
            imageToFindInRoi = new Mat(imageToFindIn, new Rect(spellbookROI.x, spellbookROI.y, spellbookROI.width, spellbookROI.height));

            // Convert images to color and mask (with alpha)
            SimpleEntry<Mat, Mat> convertedImageToFind = imageUtils.convertToColorWithAlpha(imageToFind);
            SimpleEntry<Mat, Mat> convertedScaledImageToFind = imageUtils.convertToColorWithAlpha(scaledImageToFind);

            colorImageToFind = convertedImageToFind.getKey();
            mask = convertedImageToFind.getValue();

            scaledColorImageToFind = convertedScaledImageToFind.getKey();
            scaledMask = convertedScaledImageToFind.getValue();

            logger.devLog("Attempting to find spell at regular scale.");
            // Try finding the spell at regular scale
            MatchedRectangle spellRect = imageRecognition.returnBestMatchObjectWithMask(imageToFindInRoi, colorImageToFind, mask, thresholdSpell);

            if (spellRect == null) {
                logger.devLog("Spell icon was not found at regular scale. Trying pre-scaled version.");
                spellRect = imageRecognition.returnBestMatchObjectWithMask(imageToFindInRoi, scaledColorImageToFind, scaledMask, thresholdSpell);

                if (spellRect != null) {
                    logger.devLog("A valid match was found for " + spellName + " using pre-scaled image.");
                } else {
                    logger.devLog("Spell icon was not found using pre-scaled image. Spell tab might not be open.");
                }
            } else {
                logger.devLog("A valid match was found for " + spellName + " at regular scale.");
            }

            if (spellRect != null) {
                // Adjust the coordinates of the matched rectangle to the full image
                MatchedRectangle adjustedSpell = new MatchedRectangle(
                        spellRect.x + spellbookROI.x,
                        spellRect.y + spellbookROI.y,
                        spellRect.width,
                        spellRect.height,
                        spellRect.getMatchValue()
                );

                cacheManager.setSpell(cacheKey, adjustedSpell);
                logger.devLog("Cached the result for spell: " + spellName + " on " + device);
                return true; // Spell found and cast able
            }
        } catch (Exception e) {
            logger.devLog("An error occurred while checking spell: " + spellName + " - " + e.getMessage());
        } finally {
            // Release Mat objects
            if (imageToFind != null) imageToFind.release();
            if (scaledImageToFind != null) scaledImageToFind.release();
            if (imageToFindIn != null) imageToFindIn.release();
            if (imageToFindInRoi != null) imageToFindInRoi.release();
            if (colorImageToFind != null) colorImageToFind.release();
            if (mask != null) mask.release();
            if (scaledColorImageToFind != null) scaledColorImageToFind.release();
            if (scaledMask != null) scaledMask.release();
        }

        return false; // Spell not found
    }
}
