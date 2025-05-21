package osr.mapping;

import helpers.CacheManager;
import helpers.GetGameView;
import helpers.Logger;
import helpers.OCR.DigitReader;
import helpers.Color.ColorFinder;
import helpers.openCV.ImageRecognition;
import helpers.openCV.utils.MatchedRectangle;
import helpers.utils.ItemPair;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import osr.mapping.utils.ItemProcessor;
import scripts.APIClasses.ClientAPI;
import scripts.ScriptInfo;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.*;

import static utils.Constants.INVENTORY_RECT;

public class Inventory {
    private final Random random = new Random();
    private final static int EMPTY_SLOT_THRESHOLD = 1290;
    private static final Rectangle[] SLOT_BOXES = new Rectangle[]{
            new Rectangle(614, 231, 36, 36), // Slot 1
            new Rectangle(655, 231, 36, 36), // Slot 2
            new Rectangle(696, 231, 36, 36), // Slot 3
            new Rectangle(737, 231, 36, 36), // Slot 4
            new Rectangle(614, 267, 36, 36), // Slot 5
            new Rectangle(655, 267, 36, 36), // Slot 6
            new Rectangle(696, 267, 36, 36), // Slot 7
            new Rectangle(737, 267, 36, 36), // Slot 8
            new Rectangle(614, 303, 36, 36), // Slot 9
            new Rectangle(655, 303, 36, 36), // Slot 10
            new Rectangle(696, 303, 36, 36), // Slot 11
            new Rectangle(737, 303, 36, 36), // Slot 12
            new Rectangle(614, 339, 36, 36), // Slot 13
            new Rectangle(655, 339, 36, 36), // Slot 14
            new Rectangle(696, 339, 36, 36), // Slot 15
            new Rectangle(737, 339, 36, 36), // Slot 16
            new Rectangle(614, 375, 36, 36), // Slot 17
            new Rectangle(655, 375, 36, 36), // Slot 18
            new Rectangle(696, 375, 36, 36), // Slot 19
            new Rectangle(737, 375, 36, 36), // Slot 20
            new Rectangle(614, 411, 36, 36), // Slot 21
            new Rectangle(655, 411, 36, 36), // Slot 22
            new Rectangle(696, 411, 36, 36), // Slot 23
            new Rectangle(737, 411, 36, 36), // Slot 24
            new Rectangle(614, 447, 36, 36), // Slot 25
            new Rectangle(655, 447, 36, 36), // Slot 26
            new Rectangle(696, 447, 36, 36), // Slot 27
            new Rectangle(737, 447, 36, 36)  // Slot 28
    };
    private final CacheManager cacheManager;
    private final Logger logger;
    private final ImageRecognition imageRecognition;
    private final GetGameView getGameView;
    private final GameTabs gameTabs;
    private final ClientAPI clientAPI;
    private final ScriptInfo scriptInfo;
    private final ItemProcessor itemProcessor;
    private final ColorFinder colorFinder;
    private final DigitReader digitReader;
    private final List<Color> stackColors = Arrays.asList(
            Color.decode("#fefe00"),
            Color.decode("#fefefe"),
            Color.decode("#00fe7f")
    );
    private final Random randomPattern = new Random();

    private final static int cropX = INVENTORY_RECT.x;
    private final static int cropY = INVENTORY_RECT.y;

    public Inventory(DigitReader digitReader, ColorFinder colorFinder, ItemProcessor itemProcessor, CacheManager cacheManager, Logger logger, GetGameView getGameView, ImageRecognition imageRecognition, GameTabs gameTabs, ClientAPI clientAPI, ScriptInfo scriptInfo) {
        this.colorFinder = colorFinder;
        this.cacheManager = cacheManager;
        this.logger = logger;
        this.imageRecognition = imageRecognition;
        this.getGameView = getGameView;
        this.gameTabs = gameTabs;
        this.clientAPI = clientAPI;
        this.scriptInfo = scriptInfo;
        this.itemProcessor = itemProcessor;
        this.digitReader = digitReader;
    }

    private Mat getInventoryImage(String device) {
        return getGameView.getSubmat(device, INVENTORY_RECT);
    }

    public boolean isInventoryFull(String device) {
        return checkInventorySlots(device, false);
    }

    public int getNumberOfUsedInventorySlots(String device) {
        return countInventorySlots(device, true);
    }

    public int getNumberOfEmptyInventorySlots(String device) {
        return 28 - getNumberOfUsedInventorySlots(device);
    }

    private boolean checkInventorySlots(String device, boolean countUsed) {
        Mat inventoryImage = getGameView.getMat(device);
        if (inventoryImage == null) {
            return false;
        }
        Mat grayImage;
        Mat mask;

        try {
            grayImage = new Mat();
            Imgproc.cvtColor(inventoryImage, grayImage, Imgproc.COLOR_BGR2GRAY);

            mask = new Mat();
            Core.compare(grayImage, new Scalar(0), mask, Core.CMP_GT);

            for (int i = SLOT_BOXES.length - 1; i >= 0; i--) {
                Rectangle slot = SLOT_BOXES[i];
                Rect slotRect = new Rect(slot.x, slot.y, slot.width, slot.height);
                Mat slotMat = new Mat(mask, slotRect);

                try {
                    int slotNonZeroCount = Core.countNonZero(slotMat);
                    if ((countUsed && slotNonZeroCount < EMPTY_SLOT_THRESHOLD) || (!countUsed && slotNonZeroCount >= EMPTY_SLOT_THRESHOLD)) {
                        return false;
                    }
                } finally {
                    slotMat.release();
                }
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean slotContains(int slot, int itemID, double threshold, String device) {
        if (slot < 1 || slot > SLOT_BOXES.length) {
            logger.devLog("Invalid slot number: " + slot);
            return false; // Invalid slot number
        }

        Mat slotImage = null;
        try {
            // Adjust slot to 0-based index
            int adjustedSlot = slot - 1;
            Rectangle slotBox = SLOT_BOXES[adjustedSlot];
            Rect CVRect = new Rect(slotBox.x, (slotBox.y - 3), (slotBox.width + 5), (slotBox.height + 5));

            // Get the sub-image for the slot
            slotImage = getGameView.getSubmat(device, CVRect);
            if (slotImage == null || slotImage.empty()) {
                logger.devLog("Inventory image could not be retrieved.");
                return false;
            }

            // Get the image of the item
            Mat itemImage = itemProcessor.getItemImage(itemID);
            if (itemImage == null || itemImage.empty()) {
                logger.devLog("Item image not found for ID: " + itemID);
                return false;
            }

            // Perform template matching to see if the item is present in the slot
            MatchedRectangle match = imageRecognition.returnBestMatchObject(itemImage, slotImage, threshold);
            if (match != null) {
                logger.devLog("Item with ID " + itemID + " found in slot " + slot);
                return true;
            } else {
                logger.devLog("Item with ID " + itemID + " not found in slot " + slot);
                return false;
            }
        } catch (Exception e) {
            logger.devLog("Error in slotContains for slot " + slot + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            // Release the slotImage to free memory
            if (slotImage != null) {
                slotImage.release();
            }
        }
    }

    private int countInventorySlots(String device, boolean countUsed) {
        int usedSlots = 0;
        if (gameTabs.isTabOpen(device, "Inventory")) {
            Mat gameView;
            Mat inventoryImage;
            Mat grayImage;

            try {
                gameView = getGameView.getMat(device);

                Rect rectCrop = new Rect(INVENTORY_RECT.x - 10, INVENTORY_RECT.y - 10, INVENTORY_RECT.width + 15, INVENTORY_RECT.height + 15);
                inventoryImage = new Mat(gameView, rectCrop);
                grayImage = new Mat();
                Imgproc.cvtColor(inventoryImage, grayImage, Imgproc.COLOR_BGR2GRAY);

                int offsetX = INVENTORY_RECT.x - 10;
                int offsetY = INVENTORY_RECT.y - 10;

                for (Rectangle slot : SLOT_BOXES) {
                    int translatedX = slot.x - offsetX;
                    int translatedY = slot.y - offsetY;
                    Rect slotRect = new Rect(translatedX, translatedY, slot.width, slot.height);
                    Mat slotMat = new Mat(grayImage, slotRect);

                    try {
                        int slotNonZeroCount = Core.countNonZero(slotMat);
                        if (slotNonZeroCount < EMPTY_SLOT_THRESHOLD) {
                            if (countUsed) {
                                usedSlots++;
                            }
                        } else {
                            if (!countUsed) {
                                usedSlots++;
                            }
                        }
                    } finally {
                        slotMat.release();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return usedSlots;
    }

    public Rectangle[] getInventorySlotBoxes() {
        if (!cacheManager.getSlotBox().isEmpty()) {
            return cacheManager.getSlotBox().values().toArray(new Rectangle[0]);
        }

        final int columns = 4;
        final int rows = 7;
        Rectangle[] slotBoxes = new Rectangle[columns * rows];
        int startX = INVENTORY_RECT.x + 21;  // Adjust left corner
        int startY = INVENTORY_RECT.y + 11;  // Adjust top corner
        int totalWidth = INVENTORY_RECT.width;
        int cellWidth = (totalWidth - 38) / columns;  // Adjust width for padding

        int slotWidth = 36;
        int slotHeight = 36;

        logger.devLog("Creating new slot boxes relative to inventory area:");
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < columns; col++) {
                int x = startX + col * cellWidth;  // Retain horizontal padding
                int y = startY + row * slotHeight; // Remove vertical padding
                int index = row * columns + col;
                Rectangle slotBox = new Rectangle(x, y, slotWidth, slotHeight);
                slotBoxes[index] = slotBox;
                cacheManager.setSlotBox(index, slotBox);
                logger.devLog("Slot " + (index + 1) + ": " + slotBox);
            }
        }

        return slotBoxes;
    }

    public Rectangle itemPosition(String device, String itemID, double threshold) {
        return findItemPosition(device, itemProcessor.getItemImage(itemID), threshold);
    }

    public Rectangle lastItemPosition(String device, String itemID, double threshold) {
        return findLastItemPosition(device, itemProcessor.getItemImage(itemID), threshold);
    }

    public Integer itemSlotPosition(String device, int[] itemIDs, double threshold) {
        for (int itemID : itemIDs) {
            Rectangle position = findItemPosition(device, itemProcessor.getItemImage(itemID), threshold);
            if (position != null) {
                for (int i = 0; i < SLOT_BOXES.length; i++) {
                    Rectangle slot = SLOT_BOXES[i];
                    if (slot.intersects(position)) {  // Check if the slot intersects with the found position
                        return i + 1;  // Return the slot number (1-based index)
                    }
                }
            }
        }
        return 0; // Return 0 if no item is found in any slot
    }

    private Rectangle findItemPosition(String device, Mat itemImage, double threshold) {
        logger.devLog("Item Image - Type: " + itemImage.type() + ", Size: " + itemImage.size() + ", Channels: " + itemImage.channels());

        Mat croppedItemImage = getCroppedItemImage(itemImage);
        Mat inventoryImage = getInventoryImage(device);
        Mat itemImageBGR = extractBGRImage(croppedItemImage);
        Mat mask = extractMask(croppedItemImage);

        MatchedRectangle match = imageRecognition.returnBestMatchObjectWithMask(inventoryImage, itemImageBGR, mask, threshold);
        if (match != null) {
            Rectangle adjustedMatch = adjustAndTranslateMatch(match);
            for (Rectangle slot : SLOT_BOXES) {
                if (slot.intersects(adjustedMatch)) {
                    logger.devLog("Item first found in inventory slot at position: " + slot);
                    return slot;
                }
            }

            logger.devLog("Adjusted match found outside of inventory slots.");
        }

        return null;
    }

    private Rectangle findLastItemPosition(String device, Mat itemImage, double threshold) {
        logger.devLog("Item Image - Type: " + itemImage.type() + ", Size: " + itemImage.size() + ", Channels: " + itemImage.channels());

        Mat croppedItemImage = getCroppedItemImage(itemImage);
        Mat inventoryImage = getInventoryImage(device);
        List<MatchedRectangle> matches = imageRecognition.performTemplateMatchForGameObjectsWithMask(croppedItemImage, inventoryImage, threshold);

        Rectangle lastSlot = null;
        int highestSlotNumber = 0;

        for (MatchedRectangle match : matches) {
            Rectangle adjustedMatch = adjustAndTranslateMatch(match);
            for (int i = 0; i < SLOT_BOXES.length; i++) {
                Rectangle slot = SLOT_BOXES[i];
                if (slot.intersects(adjustedMatch)) {
                    if (i + 1 > highestSlotNumber) {
                        highestSlotNumber = i + 1;
                        lastSlot = slot;
                    }
                }
            }
        }

        if (lastSlot != null) {
            logger.devLog("Item last found in inventory slot at position: " + lastSlot);
            return lastSlot;
        } else {
            logger.devLog("No valid matches found within inventory slots.");
        }

        logger.devLog("Item not found in any slot.");
        return null;
    }

    public boolean contains(String device, int[] itemIDs, double threshold) {
        return containsItems(device, itemIDs, threshold, null, null);
    }

    public boolean contains(String device, int itemID, double threshold) {
        return contains(device, new int[]{itemID}, threshold, null, null);
    }

    public boolean contains(String device, int itemID, double threshold, Color color) {
        return contains(device, new int[]{itemID}, threshold, color, null);
    }

    public boolean contains(String device, int[] itemIDs, double threshold, Color targetColor) {
        return containsItems(device, itemIDs, threshold, targetColor, null);
    }

    public boolean contains(String device, int itemID, double threshold, Color color, Color exclusionColor) {
        return contains(device, new int[]{itemID}, threshold, color, exclusionColor);
    }

    public boolean contains(String device, int[] itemIDs, double threshold, Color targetColor, Color exclusionColor) {
        return containsItems(device, itemIDs, threshold, targetColor, exclusionColor);
    }

    // Consolidated method to handle both types of itemID arrays
    private boolean containsItems(String device, int[] itemIDs, double threshold, Color targetColor, Color exclusionColor) {
        logger.devLog("Checking inventory for items. Device ID: " + device + ", Threshold: " + threshold);
        Mat inventoryArea = getGameView.getSubmat(device, INVENTORY_RECT);

        try {
            for (int itemID : itemIDs) {
                logger.devLog("Processing item with ID: " + itemID);

                Mat itemImage = itemProcessor.getItemImage(itemID);
                if (itemImage.empty()) {
                    logger.devLog("Item image not found or is empty for ID: " + itemID);
                    continue;
                }

                Mat croppedItemImage = getCroppedItemImage(itemImage);

                MatchedRectangle match;
                if (targetColor != null) {
                    match = imageRecognition.returnBestMatchWithColor(
                            device,
                            croppedItemImage,
                            inventoryArea,
                            threshold,
                            targetColor,
                            cropX,
                            cropY,
                            3
                    );
                } else {
                    match = imageRecognition.returnBestMatchObject(croppedItemImage, inventoryArea, threshold);
                }

                if (match != null) {
                    if (exclusionColor != null) {
                        if (colorFinder.isColorInRect(device, exclusionColor, match, 5)) {
                            logger.devLog("Match for " + itemID + " excluded cause of exclusion color");
                            return false;
                        } else {
                            logger.devLog("Item " + itemID + " found in the inventory after exclusion check");
                            return true;
                        }
                    } else {
                        logger.devLog("Item " + itemID + " found in the inventory.");
                        return true;
                    }
                } else {
                    logger.devLog("No match found for item ID: " + itemID);
                }
            }
        } finally {
            if (inventoryArea != null) inventoryArea.release();
        }

        return false;
    }

    public int count(String deviceID, int itemID, double threshold, Color color) {
        Mat itemImage = itemProcessor.getItemImage(itemID);
        Mat croppedItemImage = getCroppedItemImage(itemImage);
        Mat inventoryImage = getInventoryImage(deviceID);

        List<MatchedRectangle> matches = findMatches(deviceID, color, croppedItemImage, inventoryImage, threshold, 28);

        return matches.size();
    }

    public void tapItem(Integer slotID, String itemId, boolean useCache, double threshold, String device) {
        if (slotID != null) {
            tapSlot(slotID);
            return;
        }

        String cacheKey = itemId + "-" + threshold + "-" + device;
        Rectangle cachedItemRect = cacheManager.getItemLocation(cacheKey);

        if (cachedItemRect != null && useCache) {
            logger.devLog("Using cached location for tapItem on " + device);
            clientAPI.tap(cachedItemRect, device);
            return;
        }

        Mat itemImage = itemProcessor.getItemImage(itemId);
        performTapAction(device, itemImage, threshold, cacheKey, useCache, itemId);
    }

    public void tapItem(int itemId, double threshold, Color targetColor, String device) {
        Mat itemImage = itemProcessor.getItemImage(itemId);
        performTapActionWithColor(device, itemImage, threshold, targetColor);
    }

    public void tapAllItems(int itemId, double threshold, String device) {
        Mat croppedItemImage = getCroppedItemImage(itemProcessor.getItemImage(itemId));
        Mat inventoryImage = getInventoryImage(device);

        if (inventoryImage != null) {
            List<MatchedRectangle> matches = imageRecognition.performTemplateMatchForGameObjectsWithMask(croppedItemImage, inventoryImage, threshold);
            processMatches(matches, device);
        }
    }

    public void tapAllItems(List<ItemPair> itemPairs, double threshold, String device) {
        Mat inventoryImage = getInventoryImage(device);

        if (inventoryImage != null) {
            for (ItemPair itemPair : itemPairs) {
                Mat itemImage = getCroppedItemImage(itemProcessor.getItemImage(itemPair.getItemID()));

                List<MatchedRectangle> matches = imageRecognition.returnBestMatchesWithColor(
                        device, itemImage, inventoryImage, threshold, itemPair.getCheckColor(), cropX, cropY, 3
                );

                processMatches(matches, device);
            }
        }
    }

    public void dropInventItems(List<Integer> exclusionList, String device) {
        List<Integer> slots = getRandomPattern();

        for (Integer slot : slots) {
            if (slot < 1 || slot > SLOT_BOXES.length || exclusionList.contains(slot)) {
                logger.devLog("Skipping slot " + slot + " (either excluded or invalid).");
                continue;  // Skip excluded or invalid slots
            }

            Rectangle slotBox = SLOT_BOXES[slot - 1];  // Use 0-based index for SLOT_BOXES
            int centerX = slotBox.x + slotBox.width / 2;
            int centerY = slotBox.y + slotBox.height / 2;
            Rectangle tapArea = new Rectangle(centerX - 13, centerY - 13, 26, 26);  // Center tap area within the slot

            clientAPI.tap(tapArea, device);
            logger.devLog("Tapped inventory slot " + slot + " at center (" + centerX + ", " + centerY + ").");

            try {
                int sleepTime = 25 + randomPattern.nextInt(126);
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        logger.devLog("Completed tapping inventory slots, excluding specified slots.");
    }

    public void dropInventItemsBySlots(List<Integer> slotsToDrop, String device) {
        for (Integer slot : slotsToDrop) {
            if (slot < 1 || slot > SLOT_BOXES.length) {
                logger.devLog("Invalid slot number: " + slot);
                continue;  // Skip invalid slots
            }

            Rectangle slotBox = SLOT_BOXES[slot - 1];  // Use 0-based index for SLOT_BOXES
            int centerX = slotBox.x + slotBox.width / 2;
            int centerY = slotBox.y + slotBox.height / 2;
            Rectangle tapArea = new Rectangle(centerX - 13, centerY - 13, 26, 26);  // Center tap area within the slot

            clientAPI.tap(tapArea, device);
            logger.devLog("Tapped inventory slot " + slot + " at center (" + centerX + ", " + centerY + ").");

            try {
                int sleepTime = 25 + randomPattern.nextInt(126);
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private List<Integer> getRandomPattern() {
        int inventorySize = 28;
        List<Integer> pattern = new ArrayList<>(Collections.nCopies(inventorySize, 0));
        for (int i = 0; i < inventorySize; i++) {
            pattern.set(i, i + 1);
        }

        switch (randomPattern.nextInt(7)) {
            case 0:
                logger.devLog("Using drop pattern 0");
                break;
            case 1:
                logger.devLog("Using drop pattern 1");
                pattern.sort((a, b) -> {
                    int colCompare = Integer.compare((a - 1) % 4, (b - 1) % 4);
                    return colCompare == 0 ? Integer.compare((b - 1) / 4, (a - 1) / 4) : colCompare;
                });
                break;
            case 2:
                logger.devLog("Using drop pattern 2");
                pattern.sort((a, b) -> Integer.compare((b - 1) / 4, (a - 1) / 4));
                break;
            case 3:
                logger.devLog("Using drop pattern 3");
                Collections.reverse(pattern);
                break;
            case 4:
                logger.devLog("Using drop pattern 4");
                pattern.sort((a, b) -> {
                    int rowCompare = Integer.compare((a - 1) / 4, (b - 1) / 4);
                    return rowCompare == 0 ? Integer.compare((b - 1) % 4, (a - 1) % 4) : rowCompare;
                });
                break;
            case 5:
                logger.devLog("Using drop pattern 5");
                pattern.sort((a, b) -> {
                    int colCompare = Integer.compare((b - 1) % 4, (a - 1) % 4);
                    return colCompare == 0 ? Integer.compare((b - 1) / 4, (a - 1) / 4) : colCompare;
                });
                break;
            case 6:
                logger.devLog("Using drop pattern 6");
                Collections.shuffle(pattern);
                break;
        }
        return pattern;
    }

    public void eat(String itemId, double threshold, String device, Color searchColor) {
        Mat croppedItemImage = getCroppedItemImage(itemProcessor.getItemImage(itemId));
        Mat inventoryArea = getInventoryImage(device);

        MatchedRectangle item = findMatch(device, searchColor, croppedItemImage, inventoryArea, threshold);
        if (item != null) {
            Rectangle adjustedMatch = adjustAndTranslateMatch(item);
            logger.devLog("Item " + itemId + " found at adjusted coordinates: (" + adjustedMatch.x + ", " + adjustedMatch.y + ")");

            clientAPI.tap(adjustedMatch, device);
        } else {
            logger.devLog("Item " + itemId + " not found in the inventory.");
        }
    }

    public Point getItemCenterPoint(String itemId, double threshold, String device) {
        Mat croppedItemImage = getCroppedItemImage(itemProcessor.getItemImage(itemId));
        Mat inventoryArea = getInventoryImage(device);

        MatchedRectangle item = imageRecognition.returnBestMatchObject(croppedItemImage, inventoryArea, threshold);

        if (item != null) {
            Rectangle adjustedMatch = adjustAndTranslateMatch(item);
            int centerX = adjustedMatch.x + adjustedMatch.width / 2;
            int centerY = adjustedMatch.y + adjustedMatch.height / 2;
            return new Point(centerX, centerY);
        } else {
            logger.devLog("Item " + itemId + " not found in the inventory.");
            return null;
        }
    }

    public Rectangle findItem(int itemId, double threshold, Color searchColor, String device) {
        Mat croppedItemImage = getCroppedItemImage(itemProcessor.getItemImage(itemId));
        Mat inventoryArea = getInventoryImage(device);

        MatchedRectangle item = findMatch(device, searchColor, croppedItemImage, inventoryArea, threshold);
        if (item != null) {
            Rectangle adjustedMatch = adjustAndTranslateMatch(item);
            logger.devLog("Item " + itemId + " found at adjusted coordinates: (" + adjustedMatch.x + ", " + adjustedMatch.y + ")");
            return adjustedMatch;
        } else {
            logger.devLog("Item " + itemId + " not found in the inventory.");
            return null;
        }
    }

    public Integer getItemStack(String device, Integer itemID) {
        Mat gameScreen = getGameView.getMat(device);
        Mat croppedItemImage = getCroppedItemImage(itemProcessor.getItemImage(itemID));
        Mat inventoryArea = getInventoryImage(device);

        if (gameScreen == null || gameScreen.empty()) {
            logger.devLog("The gameScreen Mat is null or empty.");
            return null;
        }

        MatchedRectangle result = imageRecognition.returnBestMatchObject(croppedItemImage, inventoryArea, 0.68);

        if (result != null) {
            Rectangle adjustedMatch = adjustAndTranslateMatch(result);

            // Calculate adjusted ROI within game screen bounds
            int newX = Math.max(adjustedMatch.x - 3, 0);
            int newY = Math.max(adjustedMatch.y - 18, 0);
            int newWidth = Math.min(adjustedMatch.width + 3, gameScreen.width() - newX);
            int newHeight = Math.min(adjustedMatch.height + 18, gameScreen.height() - newY);

            Rectangle adjustedRect = new Rectangle(newX, newY, newWidth, newHeight);
            BufferedImage adjustedRoiImage = getGameView.getSubBuffered(device, adjustedRect);

            return digitReader.findAllDigits(0, adjustedRoiImage, stackColors, cacheManager.getDigitPatterns());
        } else {
            logger.devLog("No match found.");
        }

        return 0;
    }

    // Helper method for tapping on slot items
    private void tapSlot(int slotID) {
        int index = slotID - 1;
        if (index >= 0 && index < SLOT_BOXES.length) {
            Rectangle slotBox = SLOT_BOXES[index];
            clientAPI.tap(slotBox, scriptInfo.getCurrentEmulatorId());
        } else {
            System.err.println("Slot ID is out of valid range.");
        }
    }

    // Helper method for cropping the top 10 pixels from the item image
    private Mat getCroppedItemImage(Mat itemImage) {
        Rect itemROI = new Rect(0, 10, itemImage.width(), itemImage.height() - 10);
        return new Mat(itemImage, itemROI);
    }

    // Helper method for adjusting the rectangle position based on cropping and offsets
    private void adjustMatchedRectangle(MatchedRectangle item) {
        item.y -= 10;  // Shift up by 10 pixels
        item.height += 10;  // Add back the 10 pixels to the height
        item.x += INVENTORY_RECT.x + 7;
        item.y += INVENTORY_RECT.y + 7;
        item.width -= 14;
        item.height -= 14;
    }

    // Helper method to handle the tap action with cache
    private void performTapAction(String device, Mat itemImage, double threshold, String cacheKey, boolean useCache, String itemId) {
        Mat croppedItemImage = getCroppedItemImage(itemImage);
        Mat inventoryArea = getGameView.getSubmat(device, INVENTORY_RECT);

        MatchedRectangle item = imageRecognition.returnBestMatchObject(croppedItemImage, inventoryArea, threshold);

        if (item != null) {
            adjustMatchedRectangle(item);

            if (useCache) {
                cacheManager.setItemLocation(cacheKey, item);
                logger.devLog("Cached the inventory item " + itemId + " location for " + device);
            }

            clientAPI.tap(item, device);
        } else {
            logger.devLog("Item " + itemId + " not found in the inventory.");
        }
    }

    // Helper method to handle the tap action with color matching
    private void performTapActionWithColor(String device, Mat itemImage, double threshold, Color targetColor) {
        Mat croppedItemImage = getCroppedItemImage(itemImage);
        Mat inventoryArea = getGameView.getSubmat(device, INVENTORY_RECT);

        MatchedRectangle item = imageRecognition.returnBestMatchWithColor(device, croppedItemImage, inventoryArea, threshold, targetColor, cropX, cropY, 3);

        if (item != null) {
            adjustMatchedRectangle(item);
            clientAPI.tap(item, device);
        } else {
            logger.devLog("Item " + itemImage + " not found in the inventory.");
        }
    }

    // Helper method to adjust match rectangles and log their positions
    private void adjustAndLogMatches(List<MatchedRectangle> matches) {
        matches.forEach(match -> {
            match.y -= 10;  // Shift up by 10 pixels to account for cropping
            match.height += 10;  // Add back the 10 pixels to the height
            logger.devLog("Match found at (" + match.x + ", " + match.y + ") with score: " + match.getMatchValue());
        });
        logger.devLog("Matches found: " + matches.size());
    }


    // Helper to extract BGR image and handle alpha channels
    private Mat extractBGRImage(Mat croppedItemImage) {
        Mat itemImageBGR = new Mat();
        if (croppedItemImage.channels() == 4) {
            List<Mat> channels = new ArrayList<>();
            Core.split(croppedItemImage, channels);
            Core.merge(channels.subList(0, 3), itemImageBGR);
        } else {
            itemImageBGR = croppedItemImage;
        }
        return itemImageBGR;
    }

    // Helper to extract the alpha mask if present
    private Mat extractMask(Mat croppedItemImage) {
        Mat mask = new Mat();
        if (croppedItemImage.channels() == 4) {
            List<Mat> channels = new ArrayList<>();
            Core.split(croppedItemImage, channels);
            mask = channels.get(3); // alpha channel
        }
        return mask;
    }

    // Helper method to adjust and translate match coordinates to game view
    private Rectangle adjustAndTranslateMatch(MatchedRectangle match) {
        match.y -= 10;  // Adjust match to account for cropped top 10 pixels
        match.height += 10;
        int matchXInGameView = match.x + INVENTORY_RECT.x;
        int matchYInGameView = match.y + INVENTORY_RECT.y;
        return new Rectangle(matchXInGameView, matchYInGameView, match.width, match.height);
    }

    private void processMatches(List<MatchedRectangle> matches, String device) {
        for (MatchedRectangle match : matches) {
            if (scriptInfo.getCancellationToken(device).isCancellationRequested()) {
                break;
            }

            // Translate and adjust match coordinates to full game view
            Rectangle adjustedMatch = adjustAndTranslateMatch(match);
            int shrinkSize = 7;
            Rectangle tapArea = new Rectangle(
                    adjustedMatch.x + shrinkSize,
                    adjustedMatch.y + shrinkSize,
                    adjustedMatch.width - 2 * shrinkSize,
                    adjustedMatch.height - 2 * shrinkSize
            );

            clientAPI.tap(tapArea, device);
            logger.devLog("Tapped item at (" + adjustedMatch.x + ", " + adjustedMatch.y + ") in " + matches.size() + " locations.");

            try {
                int sleepTime = 25 + random.nextInt(126);
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public MatchedRectangle findMatch(String device, Color searchColor, Mat croppedItemImage, Mat inventoryArea, double threshold) {
        if (searchColor != null) {
            return imageRecognition.returnBestMatchWithColor(device, croppedItemImage, inventoryArea, threshold, searchColor, cropX, cropY, 3);
        } else {
            return imageRecognition.returnBestMatchObject(croppedItemImage, inventoryArea, threshold);
        }
    }

    public List<MatchedRectangle> findMatches(String device, Color searchColor, Mat croppedItemImage, Mat inventoryArea, double threshold, int maxMatchesCheck) {
        if (searchColor != null) {
            return imageRecognition.returnBestMatchesWithColor(device, croppedItemImage, inventoryArea, threshold, searchColor, cropX, cropY, maxMatchesCheck);
        } else {
            return imageRecognition.performTemplateMatchForGameObjectsWithMask(croppedItemImage, inventoryArea, threshold);
        }
    }
}