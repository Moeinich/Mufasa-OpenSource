package osr.mapping;

import helpers.CacheManager;
import helpers.GetGameView;
import helpers.Logger;
import helpers.OCR.DigitReader;
import helpers.Color.ColorFinder;
import helpers.Color.TemplateMatcher;
import helpers.Color.utils.ColorRectanglePair;
import helpers.openCV.ImageRecognition;
import helpers.openCV.utils.MatchedRectangle;
import helpers.utils.SmithItems;
import helpers.visualFeedback.FeedbackObservables;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import osr.mapping.utils.ItemProcessor;
import osr.utils.ImageUtils;
import scripts.APIClasses.ClientAPI;
import scripts.APIClasses.ConditionAPI;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Interfaces {
    // imageUtilities
    private final int SmithingIntThreshold = 10;
    //private final double SmithingIntThreshold = 0.95;
    private final double SmithItemThreshold = 0.70;
    private final CacheManager cacheManager;
    private final Logger logger;
    private final ImageRecognition imageRecognition;
    private final GetGameView getGameView;
    private final ClientAPI clientAPI;
    private final ConditionAPI conditionAPI;
    private final ImageUtils imageUtils;
    private final ItemProcessor itemProcessor;
    private final ColorFinder colorFinder;
    private final DigitReader digitReader;
    private final TemplateMatcher templateMatcher;

    private final List<Color> stackColors = Arrays.asList(
            Color.decode("#fefe00"),
            Color.decode("#fefefe"),
            Color.decode("#00fe7f")
    );

    private static final Rectangle craftIntBorder1_1 = new Rectangle(86, 172, 12, 13);
    private static final Rectangle craftIntBorder1_2 = new Rectangle(573, 171, 11, 17);
    private static final Rectangle craftIntBorder1_3 = new Rectangle(571, 463, 14, 16);
    private static final Rectangle craftIntBorder1_4 = new Rectangle(87, 461, 12, 18);
    private static final Rectangle craftIntBorder2_1 = new Rectangle(79, 161, 13, 13);
    private static final Rectangle craftIntBorder2_2 = new Rectangle(581, 160, 11, 12);
    private static final Rectangle craftIntBorder2_3 = new Rectangle(578, 476, 12, 12);
    private static final Rectangle craftIntBorder2_4 = new Rectangle(79, 475, 11, 15);
    private static final Rectangle smithIntBorder1 = new Rectangle(82, 162, 11, 13);
    private static final Rectangle smithIntBorder2 = new Rectangle(577, 161, 12, 15);
    private static final Rectangle smithIntBorder3 = new Rectangle(577, 475, 10, 13);
    private static final Rectangle smithIntBorder4 = new Rectangle(83, 473, 10, 13);


    private static final Rect craftInterfaceRect = new Rect(74, 150, 518, 341);
    private static final Rectangle craftInterfaceRectangle = new Rectangle(74, 150, 518, 341);
    private static final Rect smithInterfaceRect = new Rect(81, 161, 508, 329);
    private static final Rectangle smithInterfaceRectangle = new Rectangle(81, 161, 508, 329);

    private static final List<Color> craftAndSmithMenuBorderColors = Arrays.asList(
            Color.decode("#1C1C19"),
            Color.decode("#343431"),
            Color.decode("#3F403C"),
            Color.decode("#252522"),
            Color.decode("#11110E")
    );

    // Color rectangle pairs
    private static final List<ColorRectanglePair> craftGOLDMenuOpenPairs = List.of(
            new ColorRectanglePair(craftAndSmithMenuBorderColors, craftIntBorder1_1),
            new ColorRectanglePair(craftAndSmithMenuBorderColors, craftIntBorder1_2),
            new ColorRectanglePair(craftAndSmithMenuBorderColors, craftIntBorder1_3),
            new ColorRectanglePair(craftAndSmithMenuBorderColors, craftIntBorder1_4)
    );
    private static final List<ColorRectanglePair> craftSILVERMenuOpenPairs = List.of(
            new ColorRectanglePair(craftAndSmithMenuBorderColors, craftIntBorder2_1),
            new ColorRectanglePair(craftAndSmithMenuBorderColors, craftIntBorder2_2),
            new ColorRectanglePair(craftAndSmithMenuBorderColors, craftIntBorder2_3),
            new ColorRectanglePair(craftAndSmithMenuBorderColors, craftIntBorder2_4)
    );
    private static final List<ColorRectanglePair> smithMenuOpenPairs = List.of(
            new ColorRectanglePair(craftAndSmithMenuBorderColors, smithIntBorder1),
            new ColorRectanglePair(craftAndSmithMenuBorderColors, smithIntBorder2),
            new ColorRectanglePair(craftAndSmithMenuBorderColors, smithIntBorder3),
            new ColorRectanglePair(craftAndSmithMenuBorderColors, smithIntBorder4)
    );

    public Interfaces(TemplateMatcher templateMatcher, DigitReader digitReader, ItemProcessor itemProcessor, CacheManager cacheManager, Logger logger, GetGameView getGameView, ImageRecognition imageRecognition, ClientAPI clientAPI, ConditionAPI conditionAPI, ImageUtils imageUtils, ColorFinder colorFinder) {
        this.cacheManager = cacheManager;
        this.logger = logger;
        this.imageRecognition = imageRecognition;
        this.getGameView = getGameView;
        this.clientAPI = clientAPI;
        this.conditionAPI = conditionAPI;
        this.imageUtils = imageUtils;
        this.itemProcessor = itemProcessor;
        this.colorFinder = colorFinder;
        this.digitReader = digitReader;
        this.templateMatcher = templateMatcher;
    }

    // SMITHING SECTION
    public boolean smithingIsOpen(String device) {
        return colorFinder.areAllColorsInPairs(device, smithMenuOpenPairs, 5);
    }

    public void closeSmithingInterface(String device) {
        String cacheKey = "closeSmithingInterface" + "-" + device;
        MatchedRectangle cachedButtonRect = cacheManager.getInterface(cacheKey);

        if (cachedButtonRect != null) {
            logger.devLog("Using cached location for closeSmithingInterface on " + device);
            clientAPI.tap(cachedButtonRect, device);
            conditionAPI.wait(() -> !smithingIsOpen(device), 100, 40);
            return;
        }

        BufferedImage CloseIntButtonFile = imageUtils.pathToBuffered("/osrsAssets/Interfaces/Smithing/Anvil/closeinterface.png");
        // Try to find the Close Interface button
        Rectangle CloseIntResult = templateMatcher.match(device, CloseIntButtonFile, SmithingIntThreshold);

        if (CloseIntResult != null) {
            logger.devLog("Close Smithing interface found, closing...");
            MatchedRectangle tapRect = new MatchedRectangle(CloseIntResult.x, CloseIntResult.y, CloseIntResult.width, CloseIntResult.height);
            clientAPI.tap(tapRect, device);
            cacheManager.setInterface(cacheKey, tapRect);
            logger.devLog("Cached the close smithing interface button location for " + device);
            conditionAPI.wait(() -> !smithingIsOpen(device), 100, 40);
        } else {
            logger.devLog("Close Interface button not found.");
        }
    }

    public void smithItem(SmithItems itemName, String device) {
        String cacheKey = "smithItem" + "-" + itemName.getItemName() + "-" + device;
        MatchedRectangle cachedButtonRect = cacheManager.getSmithing(cacheKey);

        if (cachedButtonRect != null) {
            logger.devLog("Using cached location for smithItem on " + device);
            clientAPI.tap(cachedButtonRect, device);
            conditionAPI.wait(() -> !smithingIsOpen(device), 100, 40);
            return;
        }

        FeedbackObservables.rectangleObservable.setValue(device, smithInterfaceRectangle);
        Mat ImageToSearchIn = getGameView.getSubmat(device, smithInterfaceRect);
        Mat itemImage = itemProcessor.getItemImage(itemName.getItemID());

        // Try to find the item we want to smith
        logger.devLog("Searching for item on the game interface...");
        MatchedRectangle itemToSmith = imageRecognition.returnBestMatchObject(itemImage, ImageToSearchIn, SmithItemThreshold);

        if (itemToSmith != null) {
            MatchedRectangle tapRect = new MatchedRectangle(itemToSmith.x + smithInterfaceRect.x, itemToSmith.y + smithInterfaceRect.y, itemToSmith.width, itemToSmith.height);
            clientAPI.tap(tapRect);
            cacheManager.setSmithing(cacheKey, tapRect);
            logger.devLog("Cached the smithItem location for " + itemName.getItemName() + " on " + device);
            conditionAPI.wait(() -> !smithingIsOpen(device), 100, 40);
        } else {
            logger.devLog("Failed: Item not found on game interface for ID: " + itemName.getItemID());
        }
    }

    public void smithItem(int itemId, String device) {
        String cacheKey = "smithItem" + "-" + itemId + "-" + device;
        MatchedRectangle cachedButtonRect = cacheManager.getSmithing(cacheKey);

        if (cachedButtonRect != null) {
            logger.devLog("Using cached location for smithItem on " + device);
            clientAPI.tap(cachedButtonRect, device);
            conditionAPI.wait(() -> !smithingIsOpen(device), 100, 40);
            return;
        }

        FeedbackObservables.rectangleObservable.setValue(device, smithInterfaceRectangle);
        Mat ImageToSearchIn = getGameView.getSubmat(device, smithInterfaceRect);
        Mat itemImage = itemProcessor.getItemImage(itemId);

        // Try to find the item we want to smith
        logger.devLog("Searching for item on the game interface...");
        MatchedRectangle itemToSmith = imageRecognition.returnBestMatchObject(itemImage, ImageToSearchIn, SmithItemThreshold);

        if (itemToSmith != null) {
            MatchedRectangle tapRect = new MatchedRectangle(itemToSmith.x + smithInterfaceRect.x, itemToSmith.y + smithInterfaceRect.y, itemToSmith.width, itemToSmith.height);
            clientAPI.tap(tapRect);
            cacheManager.setSmithing(cacheKey, tapRect);
            logger.devLog("Cached the smithItem location for " + device);
            conditionAPI.wait(() -> !smithingIsOpen(device), 100, 40);
        } else {
            logger.devLog("Failed: Item not found on game interface for ID: " + itemId);
        }
    }

    // CRAFTING METHODS
    public boolean craftJewelleryIsOpen(String device) {
        if (colorFinder.areAllColorsInPairs(device, craftGOLDMenuOpenPairs, 5)) { // Check for the gold jewelry crafting interface
            return true;
        } else
            return colorFinder.areAllColorsInPairs(device, craftSILVERMenuOpenPairs, 5); // If gold not found, try and find silver instead.
    }

    public void closeCraftJewelleryInterface(String device) {
        String cacheKey = "closeCraftJewelleryInterface" + "-" + device;
        MatchedRectangle cachedButtonRect = cacheManager.getInterface(cacheKey);

        if (cachedButtonRect != null) {
            logger.devLog("Using cached location for selectMakeX on " + device);
            clientAPI.tap(cachedButtonRect, device);
            return;
        }
        BufferedImage CloseIntButtonFile = imageUtils.pathToBuffered("/osrsAssets/Interfaces/Crafting/Furnace/closeinterface.png");
        // Try to find the Close Interface button
        Rectangle CloseIntResult = templateMatcher.match(device, CloseIntButtonFile, 10);

        if (CloseIntResult != null) {
            logger.devLog("Close crafting interface found, closing...");
            MatchedRectangle tapRect = new MatchedRectangle(CloseIntResult.x, CloseIntResult.y, CloseIntResult.width, CloseIntResult.height);
            clientAPI.tap(tapRect, device);
            cacheManager.setInterface(cacheKey, tapRect);
            logger.devLog("Cached the close interface button location for " + device);
        } else {
            logger.devLog("Close Interface button not found.");
        }
    }

    public void craftJewellery(int itemId, String device) {
        String cacheKey = "craftJewellery" + "-" + itemId + "-" + device;
        MatchedRectangle cachedButtonRect = cacheManager.getCraftingCache(cacheKey);

        if (cachedButtonRect != null) {
            logger.devLog("Using cached location for craftJewellery on " + device);
            clientAPI.tap(cachedButtonRect, device);
            return;
        }

        FeedbackObservables.rectangleObservable.setValue(device, craftInterfaceRectangle);
        Mat ImageToSearchIn = getGameView.getSubmat(device, craftInterfaceRect);
        Mat itemImage = itemProcessor.getItemImage(itemId);

        // Try to find the item we want to craft
        logger.devLog("Searching for item on the craft interface...");
        MatchedRectangle itemToCraft = imageRecognition.returnBestMatchObject(itemImage, ImageToSearchIn, 0.7);

        if (itemToCraft != null) {
            MatchedRectangle tapRect = new MatchedRectangle(itemToCraft.x + craftInterfaceRect.x, itemToCraft.y + craftInterfaceRect.y, itemToCraft.width, itemToCraft.height);
            clientAPI.tap(tapRect);
            cacheManager.setCraftingCache(cacheKey, tapRect);
            logger.devLog("Cached the craftJewellery location for " + device);
        } else {
            logger.devLog("Failed: Item not found on game interface for ID: " + itemId);
        }
    }

    public Integer readStackSize(Rectangle roi, String device) {
        FeedbackObservables.rectangleObservable.setValue(device, roi);
        BufferedImage gameScreen = getGameView.getSubBuffered(device, roi);

        if (gameScreen == null) {
            logger.devLog("The gameScreen Mat is null or empty.");
            return null;
        }

        // Use colorFinder to detect and return the stack size
        return digitReader.findAllDigits(0, gameScreen, stackColors, cacheManager.getDigitPatterns());
    }

    public Integer readCustomStackSize(Rectangle roi, List<Color> textColors, Map<String, int[][]> digitPatterns, String device) {
        FeedbackObservables.rectangleObservable.setValue(device, roi);
        BufferedImage gameScreen = getGameView.getSubBuffered(device, roi);

        if (gameScreen == null) {
            logger.devLog("The gameScreen Mat is null or empty.");
            return null;
        }

        // Use colorFinder to detect and return the stack size using the provided digitPatterns
        return digitReader.findAllDigits(0, gameScreen, textColors, digitPatterns);
    }

    public int readCustomDigitsInArea(Rectangle roi, List<Color> textColors, Map<String, int[][]> digitPatterns, String device) {
        FeedbackObservables.rectangleObservable.setValue(device, roi);
        BufferedImage gameScreen = getGameView.getSubBuffered(device, roi);

        if (gameScreen == null) {
            logger.devLog("The gameScreen Mat is null or empty.");
            return -1;
        }

        // Use colorFinder to detect and return the stack size using the provided digitPatterns
        return digitReader.findAllDigits(0, gameScreen, textColors, digitPatterns);
    }

    public String readCustomLettersInArea(Rectangle roi, List<Color> textColors, Map<String, int[][]> letterPatterns, String device) {
        FeedbackObservables.rectangleObservable.setValue(device, roi);
        BufferedImage gameScreen = getGameView.getSubBuffered(device, roi);

        if (gameScreen == null) {
            logger.devLog("The gameScreen Mat is null or empty.");
            return "";
        }

        // Use colorFinder to detect and return the string using the provided letterPatterns
        return digitReader.findAllLetters(0, gameScreen, textColors, letterPatterns);
    }

    public boolean isSelectedMake(String device, String buttonType) {
        BufferedImage buttonFile = imageUtils.pathToBuffered("/osrsAssets/Interfaces/General/make" + buttonType + ".png");
        Rectangle buttonResult = templateMatcher.match(device, buttonFile, SmithingIntThreshold);

        if (buttonResult != null) {
            return false; // Button found but not selected
        }

        BufferedImage selectedButtonFile = imageUtils.pathToBuffered("/osrsAssets/Interfaces/General/make" + buttonType + "selected.png");
        Rectangle selectedResult = templateMatcher.match(device, selectedButtonFile, SmithingIntThreshold);

        return selectedResult != null; // Return true if button is selected
    }

    public void selectMake(String device, String buttonType) {
        String cacheKey = "selectMake" + buttonType + "-" + device;
        MatchedRectangle cachedButtonRect = cacheManager.getInterface(cacheKey);

        if (cachedButtonRect != null) {
            logger.devLog("Using cached location for selectMake" + buttonType + " on " + device);
            clientAPI.tap(cachedButtonRect, device);
            if (!buttonType.equals("X")) {
                conditionAPI.wait(() -> isSelectedMake(device, buttonType), 500, 25);
            } else {
                conditionAPI.sleep(500);
            }
            return;
        }

        BufferedImage buttonFile = imageUtils.pathToBuffered("/osrsAssets/Interfaces/General/make" + buttonType + ".png");
        Rectangle result = templateMatcher.match(device, buttonFile, SmithingIntThreshold);

        if (result != null) {
            logger.devLog("Make " + buttonType + " button found, selecting...");
            MatchedRectangle tapRect = new MatchedRectangle(result.x + craftInterfaceRect.x, result.y + craftInterfaceRect.y, result.width, result.height);
            clientAPI.tap(tapRect, device);
            cacheManager.setInterface(cacheKey, tapRect);
            logger.devLog("Cached the make" + buttonType + " interface button location for " + device);

            if (!buttonType.equals("X")) {
                conditionAPI.wait(() -> isSelectedMake(device, buttonType), 500, 25);
            } else {
                conditionAPI.sleep(500);
            }
        } else {
            logger.devLog("Make " + buttonType + " button not found.");
        }
    }
}