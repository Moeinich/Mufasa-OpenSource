package osr.mapping;

import helpers.CacheManager;
import helpers.GetGameView;
import helpers.Logger;
import helpers.Color.ColorFinder;
import helpers.Color.TemplateMatcher;
import helpers.Color.utils.ColorRectanglePair;
import helpers.openCV.ImageRecognition;
import helpers.openCV.utils.MatchedRectangle;
import helpers.visualFeedback.FeedbackObservables;
import org.jetbrains.annotations.NotNull;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import osr.utils.ImageUtils;
import scripts.APIClasses.ClientAPI;
import scripts.APIClasses.ConditionAPI;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

public class Chatbox {
    private final String basePath = "/osrsAssets/Chatbox/MakeMenu/";
    private final CacheManager cacheManager;
    private final Logger logger;
    private final ImageRecognition imageRecognition;
    private final GetGameView getGameView;
    private final ClientAPI clientAPI;
    private final ConditionAPI conditionAPI;
    private final ImageUtils imageUtils;
    private final ColorFinder colorFinder;
    private final TemplateMatcher templateMatcher;
    int borderThreshold = 10;
    double optionThreshold = 0.99;

    // Rectangles used throughout multiple methods
    public Rectangle allButton = new Rectangle(83, 8, 47, 18);
    public Rectangle gameButton = new Rectangle(148, 8, 47, 17);
    public Rectangle publicButton = new Rectangle(214, 7, 45, 18);
    public Rectangle privateButton = new Rectangle(277, 8, 47, 18);
    public Rectangle channelButton = new Rectangle(345, 7, 45, 19);
    public Rectangle clanButton = new Rectangle(408, 7, 47, 18);
    public Rectangle tradeButton = new Rectangle(473, 8, 47, 17);
    private final Rectangle chatBoxButton = new Rectangle(17, 47, 49, 31);
    private static final Rectangle chatboxBorder1 = new Rectangle(14, 1, 8, 13);
    private static final Rectangle chatboxBorder2 = new Rectangle(521, 0, 9, 11);
    private static final Rectangle chatboxBorder3 = new Rectangle(522, 131, 12, 16);
    private static final Rectangle chatboxBorder4 = new Rectangle(11, 131, 12, 14);
    private static final Rectangle chatboxBorder5 = new Rectangle(259, 137, 28, 6);
    private static final Rectangle chatboxBorder6 = new Rectangle(247, 1, 28, 6);
    private static final Rectangle staticChatboxRectangle = new Rectangle(13, 0, 519, 145);

    // Rects for submats
    // colors
    private final List<Color> chatboxClosedColors = Arrays.asList(
            Color.decode("#733026"),
            Color.decode("#803c2f"),
            Color.decode("#524a3d"),
            Color.decode("#5a5143")
    );
    private static final List<Color> chatboxBorderColors = Arrays.asList(
            Color.decode("#524B3B"),
            Color.decode("#676051")
    );

    // Color rectangle pairs
    private static final List<ColorRectanglePair> makeMenuOpenPairs = List.of(
            new ColorRectanglePair(chatboxBorderColors, chatboxBorder1),
            new ColorRectanglePair(chatboxBorderColors, chatboxBorder2),
            new ColorRectanglePair(chatboxBorderColors, chatboxBorder3),
            new ColorRectanglePair(chatboxBorderColors, chatboxBorder4),
            new ColorRectanglePair(chatboxBorderColors, chatboxBorder5),
            new ColorRectanglePair(chatboxBorderColors, chatboxBorder6)
    );

    public Chatbox(TemplateMatcher templateMatcher, CacheManager cacheManager, Logger logger, GetGameView getGameView, ImageRecognition imageRecognition, ClientAPI clientAPI, ConditionAPI conditionAPI, ImageUtils imageUtils, ColorFinder colorFinder) {
        this.cacheManager = cacheManager;
        this.logger = logger;
        this.imageRecognition = imageRecognition;
        this.getGameView = getGameView;
        this.clientAPI = clientAPI;
        this.conditionAPI = conditionAPI;
        this.imageUtils = imageUtils;
        this.colorFinder = colorFinder;
        this.templateMatcher = templateMatcher;
    }

    @NotNull
    private static List<MatchedRectangle> getMatchedRectangles(List<MatchedRectangle> matches, int adjustedX, int adjustedY) {
        List<MatchedRectangle> translatedMatches = new ArrayList<>();
        for (MatchedRectangle match : matches) {
            int translatedX = adjustedX + (int) match.getX();
            int translatedY = adjustedY + (int) match.getY();
            int fixedWidth = 51;  // Fixed width as per your original method
            int fixedHeight = 72; // Fixed height as per your original method

            // Create a new MatchedRectangle with translated coordinates and fixed size
            MatchedRectangle fixedSizeMatch = new MatchedRectangle(translatedX, translatedY, fixedWidth, fixedHeight);
            translatedMatches.add(fixedSizeMatch);
        }
        return translatedMatches;
    }

    // General methods
    public Rectangle findChatboxMenu(String device) {
        if (colorFinder.areAllColorsInPairs(device, makeMenuOpenPairs, 5)) { // Use CF to check if we have it at a static location
            return staticChatboxRectangle;
        } else {
            BufferedImage chatboxBorder = imageUtils.pathToBuffered("/osrsAssets/Chatbox/MakeMenu/border.png");
            Rectangle borderMatch = templateMatcher.match(device, chatboxBorder, borderThreshold);

            if (borderMatch != null) {
                logger.print(borderMatch.x + "," + borderMatch.y + "," + borderMatch.width + "," + borderMatch.height);
                return new Rectangle(borderMatch.x, borderMatch.y, borderMatch.width, borderMatch.height);
            } else {
                logger.devLog("Failed to find chatbox border.");
                return null;
            }
        }
    }

    public boolean makeMenuVisible(String device) {
        return colorFinder.areAllColorsInPairs(device, makeMenuOpenPairs, 5);
    }

    public void closeChatbox(String device) {
        // Check if the chatbox is even opened at all, if not close it.
        if (isChatboxOpened(device)) {
            clientAPI.tap(chatBoxButton);
            conditionAPI.wait(() -> !isChatboxOpened(device), 200, 30);
        }
    }

    public void openChatboxHelper(String device, Rectangle chatButton) {
        // Check if the chatbox is even opened at all, if not open it first.
        if (!isChatboxOpened(device)) {
            clientAPI.tap(chatBoxButton);
            conditionAPI.wait(() -> isChatboxOpened(device), 200, 30);
        }

        // Check if the tab chosen is open or not
        if (!colorFinder.isColorInRect(device, Color.decode("#8B8371"), chatButton, 10)) {
            clientAPI.tap(chatButton);
            conditionAPI.wait(() -> colorFinder.isColorInRect(device, Color.decode("#8B8371"), chatButton, 10), 150, 20);
        }
    }

    public boolean isChatboxOpened(String device) {
        return !colorFinder.isAnyColorInRect(device, chatboxClosedColors, chatBoxButton, 5);
    }

    public boolean isChatboxOpenHelper(String device, Rectangle chatButton) {
        // Check if the chatbox is even opened at all, if not open it first.
        if (!isChatboxOpened(device)) {
            clientAPI.tap(chatBoxButton);
            conditionAPI.wait(() -> isChatboxOpened(device), 200, 30);
        }
        FeedbackObservables.rectangleObservable.setValue(device, chatButton);
        return colorFinder.isColorInRect(device, Color.decode("#8B8371"), chatButton, 10);
    }

    public void makeOption(int optionNumber, String device) {
        String cacheKey = "makeOption" + "-" + optionNumber + "-" + device;
        MatchedRectangle cachedRect = cacheManager.getMakeOption(cacheKey);

        if (cachedRect != null) {
            clientAPI.tap(cachedRect, device);
            conditionAPI.wait(() -> findChatboxMenu(device) == null, 500, 25);
            return;
        }

        Rectangle chatboxMenuRect = cacheManager.getChatboxMenuRect(device);
        if (chatboxMenuRect == null) {
            chatboxMenuRect = findChatboxMenu(device);
            if (chatboxMenuRect != null) {
                cacheManager.setChatboxMenuRect(device, chatboxMenuRect);
            } else {
                logger.devLog("Failed to locate chatbox menu.");
                return;
            }
        }

        // Adjusting the rectangle
        int adjustedX = chatboxMenuRect.x + 5;
        int adjustedY = chatboxMenuRect.y + 45;
        int adjustedWidth = chatboxMenuRect.width - 10;
        int adjustedHeight = chatboxMenuRect.height - 50;

        Mat imageToSearchIn = null;
        Mat subImage = null;
        Mat leftItemCorner = null;
        Mat leftItemCornerBGR = new Mat();
        Mat mask = new Mat();

        try {
            // Creating a subimage (region of interest)
            imageToSearchIn = getGameView.getMat(device);
            Rect adjustedRect = new Rect(adjustedX, adjustedY, adjustedWidth, adjustedHeight);
            subImage = new Mat(imageToSearchIn, adjustedRect);

            // Loading the image for the left item corner
            leftItemCorner = imageUtils.pathToMat(basePath + "makemenuleftitemcorner.png");

            // Prepare BGR and alpha channels for template matching
            if (leftItemCorner.channels() == 4) {
                List<Mat> channels = new ArrayList<>();
                Core.split(leftItemCorner, channels);
                Core.merge(channels.subList(0, 3), leftItemCornerBGR);
                mask = channels.get(3);
            } else {
                leftItemCornerBGR = leftItemCorner;
            }

            // Perform template matching using the mask
            List<MatchedRectangle> matches;
            if (!mask.empty()) {
                matches = imageRecognition.performTemplateMatchForGameObjectsWithMask(leftItemCornerBGR, subImage, 0.80);
            } else {
                matches = imageRecognition.performTemplateMatchForGameObjects(leftItemCornerBGR, subImage, 0.80);
            }

            // Debugging: Print details of each found match
            for (int i = 0; i < matches.size(); i++) {
                MatchedRectangle match = matches.get(i);
                logger.devLog("Match " + (i + 1) + ": (x: " + match.getX() + ", y: " + match.getY() + ", width: " + match.getWidth() + ", height: " + match.getHeight() + ")");
            }

            // Check if the desired option is within the range of found matches
            if (optionNumber > 0 && optionNumber <= matches.size()) {
                MatchedRectangle match = matches.get(optionNumber - 1);

                // Translate the coordinates from subimage to full game view
                int translatedX = adjustedX + (int) match.getX();
                int translatedY = adjustedY + (int) match.getY();
                int fixedWidth = 51;
                int fixedHeight = 60;

                // Create a new MatchedRectangle with translated coordinates and fixed size
                MatchedRectangle fixedSizeMatch = new MatchedRectangle(translatedX, translatedY, fixedWidth, fixedHeight);

                // Use the fixed size MatchedRectangle for the tap action
                clientAPI.tap(fixedSizeMatch, device);
                cacheManager.setMakeOption(cacheKey, fixedSizeMatch);
                conditionAPI.wait(() -> findChatboxMenu(device) == null, 500, 25);
            } else {
                logger.devLog("Option " + optionNumber + " is not available. Total options found: " + matches.size());
            }
        } catch (Exception e) {
            logger.devLog("Error in makeOption: " + e.getMessage());
        } finally {
            // Ensure all Mat objects are released
            if (imageToSearchIn != null) imageToSearchIn.release();
            if (subImage != null) subImage.release();
            if (leftItemCorner != null) leftItemCorner.release();
            if (leftItemCornerBGR != leftItemCorner) leftItemCornerBGR.release();
            if (!mask.empty()) mask.release();
        }
    }

    // TEMPORARY
    public List<MatchedRectangle> visualizeOptions(String device) {
        // Your existing logic to find the chatbox menu and adjust the rectangle
        Rectangle chatboxMenuRect = cacheManager.getChatboxMenuRect(device);
        if (chatboxMenuRect == null) {
            chatboxMenuRect = findChatboxMenu(device);
            if (chatboxMenuRect != null) {
                cacheManager.setChatboxMenuRect(device, chatboxMenuRect);
            } else {
                logger.devLog("Failed to locate chatbox menu.");
            }
        }

        // Adjusting the rectangle
        int adjustedX = chatboxMenuRect.x + 5; // 5 pixels from the left
        int adjustedY = chatboxMenuRect.y + 45; // 45 pixels from the top
        int adjustedWidth = chatboxMenuRect.width - 10; // 5 pixels from both left and right
        int adjustedHeight = chatboxMenuRect.height - 50; // 45 from top, 5 from bottom

        // Creating a subimage (region of interest)
        Mat imageToSearchIn = getGameView.getMat(device);
        Rect adjustedRect = new Rect(adjustedX, adjustedY, adjustedWidth, adjustedHeight);
        Mat subImage = new Mat(imageToSearchIn, adjustedRect);

        // Loading the image for the left item corner
        Mat leftItemCorner = imageUtils.pathToMat(basePath + "makemenuleftitemcorner.png");

        // Prepare BGR and alpha channels for template matching
        Mat leftItemCornerBGR = new Mat();
        Mat mask = new Mat();
        if (leftItemCorner.channels() == 4) {
            List<Mat> channels = new ArrayList<>();
            Core.split(leftItemCorner, channels);
            Core.merge(channels.subList(0, 3), leftItemCornerBGR);
            mask = channels.get(3);
        } else {
            leftItemCornerBGR = leftItemCorner;
        }

        // Perform template matching using the mask
        List<MatchedRectangle> matches;
        if (!mask.empty()) {
            matches = imageRecognition.performTemplateMatchForGameObjectsWithMask(leftItemCornerBGR, subImage, 0.80);
        } else {
            matches = imageRecognition.performTemplateMatchForGameObjects(leftItemCornerBGR, subImage, 0.80);
        }

        // Debugging: Print details of each found match
        for (MatchedRectangle match : matches) {
            logger.devLog("Match: (x: " + match.getX() + ", y: " + match.getY() + ", width: " + match.getWidth() + ", height: " + match.getHeight() + ")");
        }

        return getMatchedRectangles(matches, adjustedX, adjustedY);
    }

    // Select methods
    public void selectMake1(String device) {
        selectOption("make1.png", device);
    }

    public void selectMake10(String device) {
        selectOption("make10.png", device);
    }

    public void selectMake5(String device) {
        selectOption("make5.png", device);
    }

    public void selectMakeAll(String device) {
        selectOption("makeall.png", device);
    }

    public void selectMakeX(String device) {
        selectOption("makex.png", device);
    }

    // isSelected methods
    public boolean isSelectedMake1(String device) {
        return isSelectedOption("make1selected.png", device);
    }

    public boolean isSelectedMake10(String device) {
        return isSelectedOption("make10selected.png", device);
    }

    public boolean isSelectedMake5(String device) {
        return isSelectedOption("make5selected.png", device);
    }

    public boolean isSelectedMakeAll(String device) {
        return isSelectedOption("makeallselected.png", device);
    }

    // Utilities
    private void selectOption(String filename, String device) {
        String cacheKey = "selectOption" + "-" + filename + "-" + device;
        MatchedRectangle cachedButtonRect = cacheManager.getChatboxButton(cacheKey);

        if (cachedButtonRect != null) {
            clientAPI.tap(cachedButtonRect, device);
            return;
        }

        Mat buttonFile = imageUtils.pathToMat(basePath + filename);
        Mat sourceImage = getGameView.getMat(device);

        MatchedRectangle buttonResult = imageRecognition.returnBestMatchObject(buttonFile, sourceImage, optionThreshold);

        if (buttonResult != null) {
            clientAPI.tap(buttonResult, device);
            cacheManager.setChatboxButton(cacheKey, buttonResult);
        } else {
            logger.devLog(filename + " button not found.");
        }
    }

    private boolean isSelectedOption(String selectedFilename, String device) {
        Mat selectedButtonFile = imageUtils.pathToMat(basePath + selectedFilename);
        Mat sourceImage = getGameView.getMat(device);

        MatchedRectangle selectedButtonResult = imageRecognition.returnBestMatchObject(selectedButtonFile, sourceImage, optionThreshold);
        return selectedButtonResult != null; // Return true if selected, false otherwise
    }

    public int findMaxOptionAmount(Rect chatboxMenuRect, String device) {
        // Adjusting the subimage rectangle
        int adjustedX = chatboxMenuRect.x + 5;
        int adjustedY = chatboxMenuRect.y + 45;
        int adjustedWidth = chatboxMenuRect.width - 10;
        int adjustedHeight = chatboxMenuRect.height - 50;

        Mat imageToSearchIn = null;
        Mat subImage = null;
        Mat leftItemCorner = null;
        Mat leftItemCornerBGR = new Mat();
        Mat mask = new Mat();

        try {
            // Creating the subimage (region of interest)
            imageToSearchIn = getGameView.getMat(device);
            Rect adjustedRect = new Rect(adjustedX, adjustedY, adjustedWidth, adjustedHeight);
            subImage = new Mat(imageToSearchIn, adjustedRect);

            // Load the left item corner image
            leftItemCorner = imageUtils.pathToMat(basePath + "makemenuleftitemcorner.png");

            // Prepare BGR and alpha channels for template matching
            if (leftItemCorner.channels() == 4) {
                List<Mat> channels = new ArrayList<>();
                Core.split(leftItemCorner, channels);
                Core.merge(channels.subList(0, 3), leftItemCornerBGR);
                mask = channels.get(3);
            } else {
                leftItemCornerBGR = leftItemCorner;
            }

            // Perform template matching using the mask
            List<MatchedRectangle> matches;
            if (!mask.empty()) {
                matches = imageRecognition.performTemplateMatchForGameObjectsWithMask(leftItemCornerBGR, subImage, 0.80);
            } else {
                matches = imageRecognition.performTemplateMatchForGameObjects(leftItemCornerBGR, subImage, 0.80);
            }

            return matches.size();
        } catch (Exception e) {
            logger.devLog("Error in findMaxOptionAmount: " + e.getMessage());
            return 0;
        } finally {
            // Ensure all Mat objects are released
            if (imageToSearchIn != null) imageToSearchIn.release();
            if (subImage != null) subImage.release();
            if (leftItemCorner != null) leftItemCorner.release();
            if (leftItemCornerBGR != null && leftItemCornerBGR != leftItemCorner) leftItemCornerBGR.release();
            if (!mask.empty()) mask.release();
        }
    }

    // Helper method to determine the currently active chat tab
    public String getActiveChatTab(String device) {
        if (isChatboxOpenHelper(device, allButton)) return "All";
        if (isChatboxOpenHelper(device, gameButton)) return "Game";
        if (isChatboxOpenHelper(device, publicButton)) return "Public";
        if (isChatboxOpenHelper(device, privateButton)) return "Private";
        if (isChatboxOpenHelper(device, channelButton)) return "Channel";
        if (isChatboxOpenHelper(device, clanButton)) return "Clan";
        if (isChatboxOpenHelper(device, tradeButton)) return "Trade";
        return null; // No tab is active
    }

    // Helper method to switch to a specific chat tab
    public void switchToChatTab(String device, String tabName) {
        logger.devLog("Switching chat tab to: " + tabName);
        switch (tabName) {
            case "All":
                openChatboxHelper(device, allButton);
                break;
            case "Game":
                openChatboxHelper(device, gameButton);
                break;
            case "Public":
                openChatboxHelper(device, publicButton);
                break;
            case "Private":
                openChatboxHelper(device, privateButton);
                break;
            case "Channel":
                openChatboxHelper(device, channelButton);
                break;
            case "Clan":
                openChatboxHelper(device, clanButton);
                break;
            case "Trade":
                openChatboxHelper(device, tradeButton);
                break;
        }
    }

}
