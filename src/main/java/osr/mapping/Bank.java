package osr.mapping;

import helpers.CacheManager;
import helpers.GetGameView;
import helpers.Logger;
import helpers.OCR.DigitReader;
import helpers.Color.ColorFinder;
import helpers.Color.TemplateMatcher;
import helpers.Color.utils.ColorRectanglePair;
import helpers.openCV.ImageRecognition;
import helpers.utils.Tile;
import helpers.visualFeedback.FeedbackObservables;
import helpers.visualFeedback.RectangleAndPoint;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import osr.mapping.utils.BankPositions;
import osr.mapping.utils.BankAreas;
import osr.mapping.utils.ItemProcessor;
import osr.utils.ImageUtils;
import osr.utils.NamedArea;
import osr.walker.Walker;
import osr.walker.utils.PositionResult;
import scripts.APIClasses.ClientAPI;
import scripts.APIClasses.ConditionAPI;
import scripts.ScriptAccountManager;
import scripts.ScriptInfo;
import utils.CredentialsManager;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

public class Bank {
    private final Rectangle PIN_RECT1 = new Rectangle(115, 264, 58, 57);
    private final Color PIN_COLOR1 = Color.decode("#5b120a");
    private final Rectangle PIN_RECT2 = new Rectangle(303, 408, 59, 60);
    private final Color PIN_COLOR2 = Color.decode("#6a160c");
    private final Color selectedTabColor = new Color(0x48, 0x3e, 0x33); // Color for selected tab

    private final CacheManager cacheManager;
    private final Logger logger;
    private final GetGameView getGameView;
    private final ImageRecognition imageRecognition;
    private final TemplateMatcher templateMatcher;
    private final Walker walker;
    private final ClientAPI clientAPI;
    private final ConditionAPI conditionAPI;
    private final ImageUtils imageUtils;
    private final ScriptInfo scriptInfo;
    private final ScriptAccountManager scriptAccountManager;
    private final CredentialsManager credMgr;
    private final ColorFinder colorFinder;
    private final Player player;
    private final ItemProcessor itemProcessor;
    private final DigitReader digitReader;

    private final int threshold = 10; //OLD OCV WAS 0.95
    private final String bankButtonNonSelectedPath = "/osrsAssets/Bank/notSelected/";
    private final String bankButtonSelectedPath = "/osrsAssets/Bank/selected/";
    Random random = new Random();

    private final List<Color> stackColors = Arrays.asList(
            Color.decode("#fefe00"),
            Color.decode("#fefefe"),
            Color.decode("#00fe7f")
    );

    private static final Rectangle bankCheckRect1 = new Rectangle(85, 155, 17, 15);
    private static final Rectangle bankCheckRect2 = new Rectangle(569, 153, 15, 17);
    private static final Rectangle bankCheckRect3 = new Rectangle(569, 479, 13, 16);
    private static final Rectangle bankCheckRect4 = new Rectangle(86, 479, 15, 16);
    private static final Rectangle bankCheckRect5 = new Rectangle(312, 173, 35, 14);
    private static final Rectangle bankSpacePopupCheckRect1 = new Rectangle(113, 246, 14, 15);
    private static final Rectangle bankSpacePopupCheckRect2 = new Rectangle(545, 250, 11, 11);
    private static final Rectangle bankSpacePopupCheckRect3 = new Rectangle(545, 417, 11, 14);
    private static final Rectangle bankSpacePopupCheckRect4 = new Rectangle(114, 414, 12, 17);
    private static final Rectangle bankSpacePopupCheckRect5 = new Rectangle(221, 385, 55, 20);
    private static final Rect bankItemROIArea = new Rect(119, 234, 440, 221);
    private static final Rectangle bankItemROIAreaRectangle = new Rectangle(119, 234, 440, 221);
    private static final Rectangle bankInterfaceArea = new Rectangle(90, 158, 492, 336);
    private static final List<Color> bankBorderColors = Arrays.asList(
            Color.decode("#1c1c19"),
            Color.decode("#30302d")
    );
    private static final List<Color> bankTextColors = Arrays.asList(
            Color.decode("#ff981f")
    );

    private static final List<ColorRectanglePair> closeBankPairs = List.of(
            new ColorRectanglePair(bankBorderColors, bankCheckRect1),
            new ColorRectanglePair(bankBorderColors, bankCheckRect2),
            new ColorRectanglePair(bankBorderColors, bankCheckRect3),
            new ColorRectanglePair(bankBorderColors, bankCheckRect4),
            new ColorRectanglePair(bankTextColors, bankCheckRect5)
    );
    private static final List<ColorRectanglePair> searchPairs = List.of(
            new ColorRectanglePair(List.of(Color.decode("#2a2415")), new Rectangle(15, 2, 37, 10)),
            new ColorRectanglePair(List.of(Color.decode("#000001")), new Rectangle(83, 43, 371, 43))
    );
    private static final List<ColorRectanglePair> bankSpacePopupPairs = List.of(
            new ColorRectanglePair(bankBorderColors, bankSpacePopupCheckRect1),
            new ColorRectanglePair(bankBorderColors, bankSpacePopupCheckRect2),
            new ColorRectanglePair(bankBorderColors, bankSpacePopupCheckRect3),
            new ColorRectanglePair(bankBorderColors, bankSpacePopupCheckRect4)
    );

    private static final int cropX = bankItemROIArea.x;
    private static final int cropY = bankItemROIArea.y;

    public Bank(TemplateMatcher templateMatcher, DigitReader digitReader, ItemProcessor itemProcessor, CacheManager cacheManager, Logger logger, GetGameView getGameView, ImageRecognition imageRecognition, Walker walker, ClientAPI clientAPI, ConditionAPI conditionAPI, ImageUtils imageUtils, ScriptInfo scriptInfo, ScriptAccountManager scriptAccountManager, CredentialsManager credMgr, ColorFinder colorFinder, Player player) {
        this.cacheManager = cacheManager;
        this.logger = logger;
        this.getGameView = getGameView;
        this.imageRecognition = imageRecognition;
        this.walker = walker;
        this.clientAPI = clientAPI;
        this.conditionAPI = conditionAPI;
        this.imageUtils = imageUtils;
        this.scriptInfo = scriptInfo;
        this.scriptAccountManager = scriptAccountManager;
        this.credMgr = credMgr;
        this.colorFinder = colorFinder;
        this.player = player;
        this.itemProcessor = itemProcessor;
        this.digitReader = digitReader;
        this.templateMatcher = templateMatcher;
    }

    public Rectangle findInterfaceBankButton(String device, String unSelectedButton, String selectedButton) {
        BufferedImage unSelected = imageUtils.pathToBuffered(bankButtonNonSelectedPath + unSelectedButton);
        Rectangle result = templateMatcher.match(device, unSelected, bankInterfaceArea, threshold);

        // If the unselected icon is not found, try to find the selected icon
        if (result == null) {
            BufferedImage Selected = imageUtils.pathToBuffered(bankButtonSelectedPath + selectedButton);
            result = templateMatcher.match(device, Selected, bankInterfaceArea, threshold);
        }

        if (result == null) {
            logger.devLog("Neither the unselected nor the selected option was found for: " + unSelectedButton);
            return null; // Return null if neither icon is found
        }

        // Shift the result rectangle by adding the bankInterfaceArea offsets
        Rectangle rect = new Rectangle(result.x + bankInterfaceArea.x, result.y + bankInterfaceArea.y, result.width, result.height);
        FeedbackObservables.rectangleObservable.setValue(device, rect);
        return rect;
    }

    public Rectangle findButton(String device, String ButtonName) {
        // Only cache specific buttons
        boolean shouldCache = ButtonName.equals("banksearchitem.png") ||
                ButtonName.equals("bankdepositinvent.png") ||
                ButtonName.equals("bankdepositequip.png") ||
                ButtonName.equals("1stbankslot.png") ||
                ButtonName.equals("bankclose.png");

        String cacheKey = shouldCache ? ButtonName : "findButton" + "-" + ButtonName + "-" + device;
        Rectangle cachedButtonRect = shouldCache ? cacheManager.getBankButton(cacheKey) : null;

        if (shouldCache && cachedButtonRect != null) {
            FeedbackObservables.rectangleObservable.setValue(device, cachedButtonRect);
            logger.devLog("Using cached location for " + ButtonName + " on " + device);
            return cachedButtonRect;
        }

        // Determine path based on button state
        String buttonPath = ButtonName.endsWith("selected.png")
                ? bankButtonSelectedPath + ButtonName
                : bankButtonNonSelectedPath + ButtonName;

        BufferedImage ButtonNameMat = imageUtils.pathToBuffered(buttonPath);
        Rectangle result = templateMatcher.match(device, ButtonNameMat, bankInterfaceArea, threshold);

        if (result == null) {
            logger.devLog("The following button was not found: " + ButtonName);
            return null; // Return null if the button is not found
        }

        // Shift the result rectangle by adding the offsets from bankInterfaceArea
        Rectangle adjustedResult = new Rectangle(
                result.x + bankInterfaceArea.x,
                result.y + bankInterfaceArea.y,
                result.width,
                result.height
        );

        if (shouldCache) {
            cacheManager.setBankButton(cacheKey, adjustedResult);
            logger.devLog("Cached the bank button location for " + ButtonName);
        }

        FeedbackObservables.rectangleObservable.setValue(device, adjustedResult);
        return adjustedResult;
    }

    public void tapButton(String device, String ButtonName) {
        String cacheKey = "tapButton" + "-" + ButtonName + "-" + device;
        Rectangle cachedButtonRect = cacheManager.getBankButton(cacheKey);

        if (cachedButtonRect != null) {
            logger.devLog("Using cached location for tapButton on " + device);
            clientAPI.tap(cachedButtonRect, device);
            return;
        }

        String selectedButtonName = ButtonName.replace(".png", "selected.png");
        BufferedImage ButtonNameMat = imageUtils.pathToBuffered(bankButtonNonSelectedPath + ButtonName);

        // Try to find the unselected icon
        Rectangle result = templateMatcher.match(device, ButtonNameMat, bankInterfaceArea, threshold);

        if (result == null) {
            logger.devLog("The following button was not found: " + ButtonName);
        } else {
            // Shift the result rectangle by adding the offsets from bankInterfaceArea
            Rectangle adjustedResult = new Rectangle(
                    result.x + bankInterfaceArea.x,
                    result.y + bankInterfaceArea.y,
                    result.width,
                    result.height
            );

            clientAPI.tap(adjustedResult, device); // Tap on the adjusted rectangle
            cacheManager.setBankButton(cacheKey, adjustedResult); // Cache the adjusted rectangle
            logger.devLog("Cached the bank button location for " + device);

            // Check if ButtonName is one of the specified names
            if (ButtonName.equals("bankclose.png") ||
                    ButtonName.equals("bankdepositequip.png") ||
                    ButtonName.equals("bankdepositinvent.png") ||
                    ButtonName.equals("bankdisabledplaceholders.png") ||
                    ButtonName.equals("bankshowequip.png") ||
                    ButtonName.equals("banksearchitem.png")) {
                Random random = new Random();
                int randomWait = random.nextInt(300) + 200;
                conditionAPI.sleep(randomWait);
            } else {
                conditionAPI.wait(() -> findButton(device, selectedButtonName) != null, 500, 25);
            }
        }
    }

    public Boolean contains(String itemId, double threshold, String device, Color searchColor) {
        if (!isBankOpen(device)) {
            return false;
        }

        Mat itemImage = itemProcessor.getItemImage(itemId);

        // Crop the top 9 pixels off the item image
        Rect itemRoi = new Rect(0, 9, itemImage.cols(), itemImage.rows() - 9);
        Mat croppedItemImage = new Mat(itemImage, itemRoi);

        // Search for the item we want, only inside the bank area
        Rectangle itemRect;
        FeedbackObservables.rectangleObservable.setValue(device, bankItemROIAreaRectangle);
        if (searchColor != null) {
            itemRect = imageRecognition.returnBestMatchWithColor(device, croppedItemImage, getGameView.getSubmat(device, bankItemROIArea), threshold, searchColor, cropX, cropY, 3);
        } else {
            itemRect = imageRecognition.returnBestMatchObject(croppedItemImage, getGameView.getSubmat(device, bankItemROIArea), threshold);
        }

        if (itemRect != null) {
            return true;
        } else {
            logger.devLog("The bank does not contain item " + itemId + ".");
            return false;
        }
    }

    public void withdrawItem(String itemId, boolean cache, double threshold, String device, Color searchColor) {
        String cacheKey = itemId + "-" + threshold + "-" + device;
        Rectangle cachedItemRect = cacheManager.getBankItemPositionCache(cacheKey);

        if (cache) {
            if (cachedItemRect != null) {
                clientAPI.tap(cachedItemRect, device);
                return;
            }
        }

        Mat itemImage = itemProcessor.getItemImage(itemId);

        // Crop the top 9 pixels off the item image
        Rect itemRoi = new Rect(0, 9, itemImage.cols(), itemImage.rows() - 9);
        Mat croppedItemImage = new Mat(itemImage, itemRoi);

        // Search for the item only within the bank area with color matching
        Rectangle itemRect;
        FeedbackObservables.rectangleObservable.setValue(device, bankItemROIAreaRectangle);
        if (searchColor != null) {
            itemRect = imageRecognition.returnBestMatchWithColor(device, croppedItemImage, getGameView.getSubmat(device, bankItemROIArea), threshold, searchColor, cropX, cropY, 3);
        } else {
            itemRect = imageRecognition.returnBestMatchObject(croppedItemImage, getGameView.getSubmat(device, bankItemROIArea), threshold);
        }

        if (itemRect != null) {
            // Shift the itemRect by adding the 9 pixels cropped off the top and the offsets of bankItemROIArea
            Rectangle adjustedItemRect = new Rectangle(
                    itemRect.x + bankItemROIArea.x,
                    itemRect.y + bankItemROIArea.y - 9,
                    itemRect.width - 5,
                    itemRect.height + 9 // Add back the 9 pixels cropped from the top
            );

            logger.print(adjustedItemRect.toString());

            // Update the cache with the adjusted item location
            cacheManager.setBankItemPositionCache(cacheKey, adjustedItemRect);
            clientAPI.tap(adjustedItemRect, device);
        } else {
            logger.devLog("Item not found in the cropped area.");
        }
    }

    public int getItemStack(String device, int itemID, Color searchColor) {
        // Create a cache key for the item stack based on itemID and device
        String cacheKey = itemID + "-" + device;
        Rectangle cachedItemRect = cacheManager.getBankItemStackPositionCache(cacheKey);

        if (cachedItemRect != null) {
            logger.devLog("Using cached item stack location for itemID: " + itemID + " on device: " + device);
            BufferedImage cachedRoiImage = getGameView.getSubBuffered(device, cachedItemRect);
            return digitReader.findAllDigits(0, cachedRoiImage, stackColors, cacheManager.getDigitPatterns());
        }

        Mat itemImage = itemProcessor.getItemImage(itemID);

        // Crop the top 9 pixels off the item image
        Rect itemRoi = new Rect(0, 9, itemImage.cols(), itemImage.rows() - 9);
        Mat croppedItemImage = new Mat(itemImage, itemRoi);

        // Search for the item in the bank area with or without color matching
        Rectangle itemRect;
        FeedbackObservables.rectangleObservable.setValue(device, bankItemROIAreaRectangle);
        if (searchColor != null) {
            logger.print("Matching with color");
            itemRect = imageRecognition.returnBestMatchWithColor(
                    device, croppedItemImage, getGameView.getSubmat(device, bankItemROIArea), 0.7, searchColor, cropX, cropY, 3
            );
        } else {
            itemRect = imageRecognition.returnBestMatchObject(
                    croppedItemImage, getGameView.getSubmat(device, bankItemROIArea), 0.7
            );
        }

        if (itemRect != null) {
            FeedbackObservables.rectangleObservable.setValue(device, itemRect);
            Rectangle adjustedItemRect = new Rectangle(
                    itemRect.x + bankItemROIArea.x - 3,
                    itemRect.y + bankItemROIArea.y - 15,
                    itemRect.width + 3,
                    itemRect.height
            );

            BufferedImage adjustedRoiImage = getGameView.getSubBuffered(device, adjustedItemRect);

            // Cache the adjusted rectangle for future lookups
            cacheManager.setBankItemStackPositionCache(cacheKey, adjustedItemRect);

            return digitReader.findAllDigits(0, adjustedRoiImage, stackColors, cacheManager.getDigitPatterns());
        } else {
            logger.devLog("Item not found in the cropped area for itemID: " + itemID + " on device: " + device);
        }
        return 0;
    }

    public void openTab(int tabNr, String device) {
        String cacheKey = "openTab" + "-" + tabNr + "-" + device;
        Rectangle cachedButtonRect = cacheManager.getBankButton(cacheKey);

        if (cachedButtonRect != null) {
            clientAPI.tap(cachedButtonRect, device);
            conditionAPI.wait(() -> isTabSelected(device, tabNr), 100, 20);
            return;
        }

        Rectangle firstTab = findButton(device, "1stbankslot.png");

        if (firstTab != null && tabNr >= 0 && tabNr <= 9) {
            // Calculate the position of the requested tab based on the first tab
            Rectangle targetTab = calculateTabPosition(firstTab, tabNr);

            // Add your logic to interact with the targetTab
            clientAPI.tap(targetTab, device);
            cacheManager.setBankButton(cacheKey, targetTab);
            conditionAPI.wait(() -> isTabSelected(device, tabNr), 100, 20);
        } else {
            logger.devLog("Invalid bank tab number, only 0-9 is valid, or the bank tabs could not be located.");
        }
    }
    public boolean isTabSelected(String device, int tab) {
        Rectangle firstTab = findButton(device, "1stbankslot.png");
        if (firstTab == null) {
            logger.devLog("Could not locate the bank tabs.");
            return false;
        }

        int tolerance = 10; // Tolerance for color matching

        for (int tabNr = 0; tabNr <= 9; tabNr++) {
            Rectangle tabRectangle = calculateTabPosition(firstTab, tabNr);
            java.awt.Point checkPoint = new java.awt.Point(tabRectangle.x, tabRectangle.y); // Top left corner of the tab
            FeedbackObservables.rectangleAndPointObservable.setValue(device, new RectangleAndPoint(tabRectangle, checkPoint));

            if (colorFinder.isPixelColor(device, checkPoint, selectedTabColor, tolerance)) {
                if (tabNr == tab) {
                    return true;
                }
            }
        }

        return false;
    }

    public String getCurrentTab(String device) {
        Rectangle firstTab = findButton(device, "1stbankslot.png");
        if (firstTab == null) {
            logger.devLog("Could not locate the bank tabs.");
            return "-1";
        }
        int tolerance = 10; // Tolerance for color matching

        for (int tabNr = 0; tabNr <= 9; tabNr++) {
            Rectangle tabRectangle = calculateTabPosition(firstTab, tabNr);
            java.awt.Point checkPoint = new java.awt.Point(tabRectangle.x, tabRectangle.y); // Top left corner of the tab
            FeedbackObservables.rectangleAndPointObservable.setValue(device, new RectangleAndPoint(tabRectangle, checkPoint));

            if (colorFinder.isPixelColor(device, checkPoint, selectedTabColor, tolerance)) {
                return String.valueOf(tabNr);
            }
        }

        logger.devLog("No tab is currently selected.");
        return "-1";
    }

    public void setCustomQuantity(String device, int quantity) {
        // Set a different quantity by default randomly
        int choice = random.nextInt(4);
        switch (choice) {
            case 0:
                tapButton(device, "bankqty1.png");
                break;
            case 1:
                tapButton(device, "bankqty5.png");
                break;
            case 2:
                tapButton(device, "bankqty10.png");
                break;
            case 3:
                tapButton(device, "bankqtyall.png");
                break;
        }
        conditionAPI.sleep(generateDelay(750, 1000));

        // Set custom quantity
        Rectangle customQtyBtn = findInterfaceBankButton(device, "bankqtyx.png", "bankqtyxselected.png");
        if (customQtyBtn != null) {
            logger.debugLog("Setting quantity to x" + quantity, device);

            // Pick a random point within the Rectangle
            int randomX = customQtyBtn.x + (int) (Math.random() * customQtyBtn.width);
            int randomY = customQtyBtn.y + (int) (Math.random() * customQtyBtn.height);

            // Perform a longPress at the random point
            clientAPI.longPress(randomX, randomY);
            conditionAPI.sleep(generateDelay(350, 600));

            // Randomize the X offset (-25 to +25) and Y offset (25 to 30 pixels below the longPress)
            int offsetX = -25 + (int) (Math.random() * 51); // Random value between -25 and +25
            int offsetY = 25 + (int) (Math.random() * 6);  // Random value between 25 and 30

            // Calculate the tap point with the randomized offsets
            Point tapPoint = new Point(randomX + offsetX, randomY + offsetY);
            clientAPI.tap(tapPoint);

            conditionAPI.sleep(generateDelay(700, 1200));

            // Convert the integer quantity to a string for keystroke simulation
            String quantityStr = Integer.toString(quantity);

            // Type the quantity digit by digit
            for (char c : quantityStr.toCharArray()) {
                String keycode;
                if (c == ' ') {
                    keycode = "space";
                } else {
                    keycode = String.valueOf(c);
                }
                clientAPI.sendKeystroke(keycode);
                conditionAPI.sleep(generateDelay(20, 40));
            }
            clientAPI.sendKeystroke("enter");

            conditionAPI.wait(() -> findButton(device, "bankqtyxselected.png") != null, 200, 12);
        } else {
            logger.debugLog("Could not locate the custom quantity button.", device);
        }
    }

    public boolean isBankPinNeeded(String device) {
        // Perform the first check
        if (!colorFinder.isColorInRect(device, PIN_COLOR1, PIN_RECT1, 10)) {
            return false; // Early return if the first condition is not met
        }

        // Perform the second check only if the first passes
        return colorFinder.isColorInRect(device, PIN_COLOR2, PIN_RECT2, 10);
    }

    public boolean isBankSpacePopupPresent(String device) {
        // Check if the popup is present
        if (colorFinder.areAllColorsInPairs(device, bankSpacePopupPairs, 5)) {
            // If we think it is present, check for the red color of the 'now' button to confirm
            return colorFinder.isColorInRect(device, Color.decode("#ff0000"), bankSpacePopupCheckRect5, 10);
        } else {
            return false;
        }
    }

    public void enterBankPin(String device) {
        String pin = retrieveBankPin();

        // Check if the provided pin is exactly 4 digits
        if (pin != null && pin.matches("\\d{4}")) {
            // Iterate through each digit in the pin
            for (int i = 0; i < pin.length(); i++) {
                int digit = Character.getNumericValue(pin.charAt(i));

                // Refresh and retrieve the updated bank pin tiles and their digits
                List<Map.Entry<Integer, Rectangle>> digitTileAssociations = findBankPinNumbers(device);

                // Map digits to their associated tiles for quick access
                Map<Integer, Rectangle> digitToTileMap = new HashMap<>();
                for (Map.Entry<Integer, Rectangle> entry : digitTileAssociations) {
                    digitToTileMap.put(entry.getKey(), entry.getValue());
                }

                // Find and tap the tile for the current digit
                Rectangle tileToTap = digitToTileMap.get(digit);
                if (tileToTap != null) {
                    clientAPI.tap(tileToTap, device);

                    // Log the tapped digit and coordinates for debugging
                    System.out.println("Tapped digit: " + digit + " at (x: " + tileToTap.x + ", y: " + tileToTap.y + ")");

                    // Add a randomized delay after tapping to simulate natural input
                    int randomWait = random.nextInt(500) + 750;
                    conditionAPI.sleep(randomWait);
                } else {
                    // Log a warning if a digit doesn't have a corresponding tile
                    System.err.println("Could not find a tile for digit: " + digit);
                    return; // Exit if unable to find the required digit
                }
            }
        } else {
            // Log the case where the pin is not valid
            logger.log("No valid bank pin was entered. Please add it to the Account Manager.", device);
        }
    }

    public void enterBankPin(String device, String pin) {
        // Check if the provided pin is exactly 4 digits
        if (pin != null && pin.matches("\\d{4}")) {
            // Iterate through each digit in the pin
            for (int i = 0; i < pin.length(); i++) {
                int digit = Character.getNumericValue(pin.charAt(i));

                // Refresh and retrieve the updated bank pin tiles and their digits
                List<Map.Entry<Integer, Rectangle>> digitTileAssociations = findBankPinNumbers(device);

                // Map digits to their associated tiles for quick access
                Map<Integer, Rectangle> digitToTileMap = new HashMap<>();
                for (Map.Entry<Integer, Rectangle> entry : digitTileAssociations) {
                    digitToTileMap.put(entry.getKey(), entry.getValue());
                }

                // Find and tap the tile for the current digit
                Rectangle tileToTap = digitToTileMap.get(digit);
                if (tileToTap != null) {
                    clientAPI.tap(tileToTap, device);

                    // Log the tapped digit and coordinates for debugging
                    System.out.println("Tapped digit: " + digit + " at (x: " + tileToTap.x + ", y: " + tileToTap.y + ")");

                    // Add a randomized delay after tapping to simulate natural input
                    int randomWait = random.nextInt(500) + 750;
                    conditionAPI.sleep(randomWait);
                } else {
                    // Log a warning if a digit doesn't have a corresponding tile
                    System.err.println("Could not find a tile for digit: " + digit);
                    return; // Exit if unable to find the required digit
                }
            }
        } else {
            // Log the case where the pin is not valid
            logger.log("No valid bank pin was provided. Please provide a valid 4-digit pin.", device);
        }
    }

    public List<Map.Entry<Integer, Rectangle>> findBankPinNumbers(String device) {
        // Define the target colors for bank pin visualization
        List<Color> pinColor = List.of(
                Color.decode("#ff7f00")
        );

        int tolerance = 10; // Tolerance for color matching
        Rectangle searchArea = new Rectangle(6, 150, 883, 384); // ROI for the bank pin area

        // Locate all bank pin tiles using color detection
        List<Rectangle> tiles = visualizeBankPin(device);

        // Extract game screen and filter ROI for digit recognition
        BufferedImage searchROI = getGameView.getSubBuffered(device, new Rectangle(searchArea.x, searchArea.y, searchArea.width, searchArea.height));

        // Find all digits within the search area
        List<Map.Entry<Integer, List<Point>>> digitsWithCoords = digitReader.findAllPlusCoords(tolerance, searchROI, pinColor, cacheManager.getBankpinDigitPatterns());

        // Associate each digit with the correct tile
        List<Map.Entry<Integer, Rectangle>> digitTileAssociations = new ArrayList<>();

        for (Map.Entry<Integer, List<Point>> digitEntry : digitsWithCoords) {
            int digit = digitEntry.getKey();
            for (Point point : digitEntry.getValue()) {
                // Convert point relative to the search area
                int absX = point.x + searchArea.x;
                int absY = point.y + searchArea.y;

                // Find the corresponding tile for the digit based on the point location
                for (Rectangle tile : tiles) {
                    if (tile.contains(absX, absY)) {
                        digitTileAssociations.add(new AbstractMap.SimpleEntry<>(digit, tile));
                        break;
                    }
                }
            }
        }

        return digitTileAssociations;
    }

    public List<Rectangle> visualizeBankPin(String device) {
        // The specific HEX colors for the bank pin visualization
        List<Color> targetColors = Arrays.asList(
                Color.decode("#63140B"), Color.decode("#FF7F00"), Color.decode("#72170D"),
                Color.decode("#541009"), Color.decode("#450C08"), Color.decode("#3D0500"),
                Color.decode("#AB837F")
        );

        int tolerance = 10; // Tolerance for color matching
        double coverageThreshold = 0.8; // Minimum 80% coverage with the target colors
        int tileSize = 64; // Each tile is 64x64 pixels
        Rectangle searchArea = new Rectangle(6, 150, 883, 384); // Defined ROI

        // Use findFixedTiles to locate tiles with target colors
        return colorFinder.findBankPinTiles(device, targetColors, searchArea, tileSize, tolerance, coverageThreshold);
    }

    public Rectangle findBankTab(int tab, String device) {
        Rectangle firstBankSlot = findButton(device, "1stbankslot.png");
        Rectangle result = new Rectangle(); // Default placeholder result

        int slotWidth = 38; // Width of each bank slot
        int slotHeight = 35; // Height of each bank slot
        double padding = 1.6; // Padding between slots

        if (firstBankSlot != null) {
            int firstSlotX = (int) firstBankSlot.getX();
            int firstSlotY = (int) firstBankSlot.getY();

            // Convert the slot position to a double for more precise calculations
            double slotX = firstSlotX;

            for (int i = 1; i < tab; i++) {
                slotX += slotWidth + padding;
            }

            // Cast back to int for the Rectangle constructor since it does not take double values
            result = new Rectangle((int) slotX, firstSlotY, slotWidth, slotHeight);
            FeedbackObservables.rectangleObservable.setValue(device, result);
        } else {
            logger.devLog("The bank slot was not found.");
        }
        return result;
    }

    public Rectangle[] bankItemGrid(String device) {
        // Get the first bank slot rectangle to use as a reference
        Rectangle firstBankSlot = findButton(device, "1stbankslot.png");
        if (firstBankSlot == null) {
            logger.devLog("Could not find the bank slot.");
            return null; // Return null if the first slot isn't found
        }

        // Define the grid parameters
        final int itemWidth = 39; // Width of each item slot (adjusted width)
        final int itemHeight = 36; // Height of each item slot
        final int reducedItemHeight = itemHeight - 2; // Reduced height for rows 2-6
        final int paddingHorizontal = 9; // Adjusted horizontal padding
        final int paddingVertical = 4; // Vertical padding
        final int columns = 8; // Number of columns
        final int rows = 6; // Number of rows

        // Create an array to hold all the Rectangles representing the item slots
        Rectangle[] itemGrid = new Rectangle[rows * columns];

        // The starting X coordinate is 7 pixels to the right of the first bank slot
        int startX = (int) firstBankSlot.getX() + 5 + 2;

        // The starting Y coordinate is 3 pixels below the bottom edge of the first bank slot
        int startY = (int) firstBankSlot.getY() + (int) firstBankSlot.getHeight() + 3;

        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                int x = startX + column * (itemWidth + paddingHorizontal);
                int y = startY + row * (reducedItemHeight + paddingVertical);

                // Adjust the height for rows 2 to 6
                int height = (row == 0) ? itemHeight : reducedItemHeight;

                // Special case for the 6th row height
                if (row == 5) {
                    height = (int) (reducedItemHeight * 0.75);
                }

                itemGrid[row * columns + column] = new Rectangle(x, y, itemWidth, height);
            }
        }

        return itemGrid;
    }

    public void openBank(String bankName, String device) {
        Rectangle bankObject = BankAreas.getRectangleByName(bankName);
        if (bankObject != null) {
            clientAPI.tap(bankObject, device);
            conditionAPI.wait(() -> isBankOpen(device) || isBankPinNeeded(device), 500, 25);
        }

        if (isBankPinNeeded(device)) {
            enterBankPin(device);
            conditionAPI.wait(() -> isBankOpen(device), 500, 25);
        }

        if (isBankSpacePopupPresent(device)) {
            logger.debugLog("Bank space pop-up detected, clicking it away.", device);
            clientAPI.tap(bankSpacePopupCheckRect5, device);
            conditionAPI.wait(() -> !isBankSpacePopupPresent(device), 100, 50);
            conditionAPI.sleep(500);
        }
    }

    public void closeBank(String device) {
        String cacheKey = "closeBank" + "-" + device;
        Rectangle cachedItemRect = cacheManager.getBankButton(cacheKey);

        if (cachedItemRect != null) {
            clientAPI.tap(cachedItemRect, device);
            conditionAPI.wait(() -> !isBankOpen(device), 200, 6);
            return;
        }

        Rectangle closebutton = findButton(device, "bankclose.png");
        if (closebutton != null) {
            clientAPI.tap(closebutton);
            cacheManager.setBankButton(cacheKey, closebutton);
            conditionAPI.wait(() -> !isBankOpen(device), 200, 6);
        }
    }

    public boolean isBankOpen(String device) {
        if (isBankPinNeeded(device)) {
            enterBankPin(device);
            conditionAPI.wait(() -> !isBankPinNeeded(device), 200, 30);
        }

        if (isBankSpacePopupPresent(device)) {
            logger.debugLog("Bank space pop-up detected, clicking it away.", device);
            clientAPI.tap(bankSpacePopupCheckRect5, device);
            conditionAPI.wait(() -> isBankSpacePopupPresent(device), 100, 50);
        }

        // Use the areAllColorsInRects method to check if all pairs match
        return colorFinder.areAllColorsInPairs(device, closeBankPairs, 5);
    }


    // Dynamic banking part
    public String findDynamicBankRegion(String device, double threshold) {
        // Check if the bank location is already cached
        String bankLoc = cacheManager.getBankLoc(device);
        if (bankLoc != null) {
            logger.devLog("Using cached bank location for device: " + device);
            return bankLoc;
        }

        // Get the player's current position
        PositionResult playerPosition = walker.getBankPosition(device);

        if (playerPosition != null && playerPosition.getConfidence() >= threshold) {
            logger.devLog("Player position found with confidence: " + playerPosition.getConfidence());

            // Get the player's position in world coordinates
            Point playerPoint = playerPosition.getWorldCoordinates("bankDevice").getPoint();
            logger.print("player pos: " + playerPoint.x + "," + playerPoint.y);

            // Check if the player's point is within any defined bank area
            for (NamedArea bankArea : BankAreas.getBankAreas()) {
                // If the player's point is within the specified bank area, return the bank name
                if (playerPoint.x >= bankArea.topTile.x() && playerPoint.x <= bankArea.bottomTile.x() &&
                        playerPoint.y >= bankArea.topTile.y() && playerPoint.y <= bankArea.bottomTile.y()) {
                    logger.devLog("Player is within the bank area: " + bankArea.getName());
                    cacheManager.setBankLoc(device, bankArea.getName());
                    return bankArea.getName();
                }
            }

            logger.devLog("Player is not in any known bank area.");
        } else {
            logger.devLog("Player position not found or below confidence threshold.");
        }

        return null;
    }

    public String setupDynamicBank(String device) {
        String bankloc = findDynamicBankRegion(device, 0.3);
        logger.devLog("We are in bank: " + bankloc + ".");
        NamedArea namedArea = getNamedAreaForBankLocation(bankloc);
        if (namedArea == null) {
            logger.devLog("Unknown banking location: " + bankloc);
            return null;
        }
        PositionResult positionResult = walker.getBankPosition(device);
        boolean reached = reachBankWithRetries(bankloc, positionResult, device);
        if (reached) {
            logger.devLog("Successfully reached the bank.");
        }
        return bankloc;
    }

    public String findDynamicBank(String device) {
        // Finding the bank we are in dynamically and store the location
        logger.devLog("Attempting to find which bank we are in.");
        String bankloc = findDynamicBankRegion(device, 0.3);

        if (bankloc != null) {
            logger.devLog("We are in bank: " + bankloc + ".");
            return bankloc;
        } else {
            logger.devLog("Unknown banking location: " + bankloc);
            return null;
        }
    }

    public void stepToBank(String device, String bankloc) {
        if (bankloc == null) {
            bankloc = findDynamicBank(device);
        }

        NamedArea namedArea = getNamedAreaForBankLocation(bankloc);
        if (namedArea == null) {
            logger.devLog("Unknown banking location: " + bankloc);
            return;
        }
        PositionResult positionResult = walker.getBankPosition(device);
        reachBankWithRetries(bankloc, positionResult, device);
    }

    public boolean isSearchOpen(String device) {
        return colorFinder.areAllColorsInPairs(device, searchPairs, 5);
    }


    private Rectangle calculateTabPosition(Rectangle firstTab, int tabNr) {
        int tabWidth = 35; // Width of each tab
        int tabHeight = 28; // Height of each tab

        // The initial x-offset from tab 0 to tab 1 is different
        int initialOffset = 38; // Offset from tab 0 to tab 1
        int subsequentOffset = 40; // Offset from one tab to the next, starting from tab 1

        // Calculate the x position of the target tab
        int x;
        if (tabNr == 0) {
            x = firstTab.x; // Tab 0 is the first tab
        } else {
            // For tab 1, use the initial offset, and for subsequent tabs, add the regular offset
            x = firstTab.x + initialOffset + (tabNr - 1) * subsequentOffset;
        }

        // y position is 1 pixel lower than the first tab
        int y = firstTab.y + 1;

        return new Rectangle(x, y, tabWidth, tabHeight);
    }

    private String retrieveBankPin() {
        return credMgr.getBankPin(scriptAccountManager.getSelectedAccount(scriptInfo.getCurrentEmulatorId()));
    }

    // Helper method to fetch and convert the RegionBox for a given bank location
    private NamedArea getNamedAreaForBankLocation(String bankloc) {
        NamedArea dynamicBankArea = BankAreas.getAreaByName(bankloc);
        if (dynamicBankArea != null) {
            return new NamedArea(
                    dynamicBankArea.getName(),
                    new Tile(dynamicBankArea.topTile.x, dynamicBankArea.topTile.y, dynamicBankArea.topTile.z),
                    new Tile(dynamicBankArea.bottomTile.x, dynamicBankArea.bottomTile.y, dynamicBankArea.bottomTile.z)
            );
        }
        return null;
    }

    // Helper method to attempt reaching the bank with retries
    private boolean reachBankWithRetries(String bankloc, PositionResult playerPoint, String device) {
        Tile currentPosition = playerPoint.getWorldCoordinates("bankDevice").getTile();

        if (BankPositions.isCoordinatesAtObject(bankloc, currentPosition)) {
            logger.devLog("We're already at a bank object!");
            return true; // Already at the bank
        }

        for (int i = 0; i < 3; i++) {
            Tile randomPosition = BankPositions.getRandomPosition(bankloc);
            boolean hasReached = attemptToReachBank(randomPosition, device);
            if (hasReached) {
                return true;
            }
            logger.devLog("Attempt to reach bank failed, retrying...");
        }
        logger.devLog("Failed to reach bank after three attempts.");
        return false;
    }

    // Helper method to attempt to step to the bank tile
    private boolean attemptToReachBank(Tile destination, String device) {
        logger.devLog("Moving character to bank booth/chest at: " + destination.toString() + ".");
        walker.stepToPointBank(device, destination);
        return conditionAPI.waitWithReturn(() -> player.tileEquals(walker.getBankPosition(device).getTile("bankDevice"), destination), 300, 25);
    }

    private int generateDelay(int lowerEnd, int higherEnd) {
        if (lowerEnd > higherEnd) {
            int temp = lowerEnd;
            lowerEnd = higherEnd;
            higherEnd = temp;
        }
        return random.nextInt(higherEnd - lowerEnd + 1) + lowerEnd;
    }
}
