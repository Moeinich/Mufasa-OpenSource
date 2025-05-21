package osr.mapping;

import helpers.CacheManager;
import helpers.GetGameView;
import helpers.Logger;
import helpers.OCR.DigitReader;
import helpers.OCR.cfOCR;
import helpers.OCR.utils.FontName;
import helpers.visualFeedback.FeedbackObservables;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

public class GameOCR {
    private final GetGameView getGameView;
    private final Logger logger;
    private final DigitReader digitReader;
    private final CacheManager cacheManager;

    List<java.awt.Color> ChatTextColors = Arrays.asList(java.awt.Color.decode("#ffffff"), java.awt.Color.decode("#9090ff"));

    public GameOCR(GetGameView getGameView, Logger logger, DigitReader digitReader, CacheManager cacheManager) {
        this.getGameView = getGameView;
        this.logger = logger;
        this.digitReader = digitReader;
        this.cacheManager = cacheManager;
    }

    // Performs OCR with a specific font
    public String readText(String emulatorId, Rectangle regionToOCR, FontName font, List<Color> colors) {
        try {
            BufferedImage bufferedImage = getGameView.getSubBuffered(emulatorId, regionToOCR);
            if (bufferedImage == null) {
                return logAndReturn("Failed to load game view for emulator: " + emulatorId, "No result found as gameview is null");
            }

            switch (font) {
                case NONE:
                    return logAndReturn("Font is NONE, skipping OCR.", "No results as font is NONE");
                case ANY:
                    return performOcrForMultipleFonts(emulatorId, regionToOCR, colors);
                default:
                    return performOcr(bufferedImage, font, colors);
            }
        } catch (Exception e) {
            return logAndReturn("Error during OCR: " + e.getMessage(), "Error during OCR");
        }
    }

    private String performOcr(BufferedImage bufferedImage, FontName font, List<Color> colors) {
        logger.devLog("Processing with font: " + font);
        return cfOCR.findAllPatternsInImage(0, bufferedImage, colors, font);
    }

    // Logs a message and returns a result string
    private String logAndReturn(String logMessage, String returnValue) {
        logger.devLog(logMessage);
        return returnValue;
    }

    // Handles OCR processing for multiple fonts (when font == ANY)
    private String performOcrForMultipleFonts(String emulatorId, Rectangle regionToOCR, List<Color> colors) {
        logger.devLog("Performing OCR with all major fonts.");

        List<FontName> fontsToTest = Arrays.asList(
                FontName.BOLD_12, FontName.PLAIN_11, FontName.PLAIN_12, FontName.QUILL, FontName.QUILL_8
        );

        Map<FontName, String> results = new HashMap<>();

        for (FontName testFont : fontsToTest) {
            long startTime = System.currentTimeMillis();
            String result = readText(emulatorId, regionToOCR, testFont, colors);
            long elapsedTime = System.currentTimeMillis() - startTime;

            results.put(testFont, result);
            logger.devLog(String.format("%s took %dms, result: %s", testFont, elapsedTime, result.isEmpty() ? "No match found" : result));
        }

        FontName bestFont = results.entrySet().stream()
                .max(Comparator.comparingInt(entry -> entry.getValue().length()))
                .map(Map.Entry::getKey)
                .orElse(FontName.NONE);

        logger.devLog("Best matching font: " + bestFont);
        return results.getOrDefault(bestFont, "");
    }

    // Other OCR Methods
    public boolean isTextVisible(Rectangle searchArea, List<Color> colors, Map<String, int[][]> letterPatterns, String stringToFind, String device) {
        FeedbackObservables.rectangleObservable.setValue(device, searchArea);
        // Find the menu options
        Rectangle foundString = digitReader.findString(10, searchArea, colors, letterPatterns, stringToFind, device);

        // Tap the option if found
        return foundString != null;
    }

    public String readChatboxArea(String emulatorId, Rectangle regionToOCR) {
        try {
            BufferedImage bufferedImage = getGameView.getSubBuffered(emulatorId, regionToOCR);
            if (bufferedImage == null) {
                logger.devLog("Failed to load the game view image for emulator: " + emulatorId);
                return null;
            }
            return cfOCR.findAllPatternsInImage(0, bufferedImage, ChatTextColors, FontName.PLAIN_12);
        } catch (Exception e) {
            logger.devLog("Error during OCR: " + e.getMessage());
            return null;
        }
    }

    public int readDigitsInArea(Rectangle areaToOCR, List<Color> colorsToScan, String emulatorId) {

        FeedbackObservables.rectangleObservable.setValue(emulatorId, areaToOCR);

        // Get our partial Mat to read from
        BufferedImage image = getGameView.getSubBuffered(emulatorId, areaToOCR);

        // Find digits in the Mat
        int digitsFound = digitReader.findAllDigits(5, image, colorsToScan, cacheManager.getDigitPatterns());

        // Check if any digits were found
        if (digitsFound == 0) {
            return -1;
        }

        return digitsFound;
    }
}
