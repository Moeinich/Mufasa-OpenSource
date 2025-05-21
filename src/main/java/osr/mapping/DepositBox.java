package osr.mapping;

import helpers.Logger;
import helpers.Color.TemplateMatcher;
import osr.utils.ImageUtils;

import java.awt.*;
import java.awt.image.BufferedImage;

public class DepositBox {
    // imageUtilities
    private final int GRID_START_OFFSET_X = -64; // Right of the close button
    private final int GRID_START_OFFSET_Y = 30; // Increase from 20 to 27 to move down 7 pixels
    private final int ITEM_SLOT_SIZE = 36; // The size of each item slot
    private final int ITEM_PADDING_HEIGHT = 15; // Decrease from 17 to 15 to reduce height padding by 2 pixels
    private final int ITEM_PADDING_WIDTH = 15; // Decrease from 25 to 15 to reduce width padding by 10 pixels
    private final int GRID_COLUMNS = 7; // Number of columns
    private final int GRID_ROWS = 4; // Number of rows
    private final Logger logger;
    private final ImageUtils imageUtils;
    private final TemplateMatcher templateMatcher;
    private final int threshold = 10;
    // Caches
    private Rectangle cachedQuantity1Rect = null;
    private Rectangle cachedQuantity5Rect = null;
    private Rectangle cachedQuantity10Rect = null;
    private Rectangle cachedQuantityXRect = null;
    private Rectangle cachedQuantityAllRect = null;
    private Rectangle cachedDepositInventoryRect = null;
    private Rectangle cachedDepositWornRect = null;
    private Rectangle cachedDepositLootRect = null;
    private Rectangle cachedCloseDepositBoxRect = null;

    public DepositBox(TemplateMatcher templateMatcher, Logger logger, ImageUtils imageUtils) {
        this.logger = logger;
        this.imageUtils = imageUtils;
        this.templateMatcher = templateMatcher;
    }

    public Rectangle findQuantity1(String device) {
        if (cachedQuantity1Rect != null) {
            return cachedQuantity1Rect;
        }

        BufferedImage Qty1ButtonFile = imageUtils.pathToBuffered("/osrsAssets/DepositBox/notSelected/depositqty1.png");
        Rectangle result = templateMatcher.match(device, Qty1ButtonFile, threshold);

        // If unselected is not found, try to find the selected Quantity 1 button
        if (result == null) {
            BufferedImage Qty1SelectedButtonFile = imageUtils.pathToBuffered("/osrsAssets/DepositBox/notSelected/depositqty1selected.png");
            result = templateMatcher.match(device, Qty1SelectedButtonFile, threshold);
        }

        // Cache the result if found
        if (result != null) {
            cachedQuantity1Rect = result;
        } else {
            logger.devLog("Neither the unselected nor the selected Quantity 1 button was found.");
        }

        return result;
    }

    public Rectangle findQuantity5(String device) {
        // If we have a cached location, return it
        if (cachedQuantity5Rect != null) {
            return cachedQuantity5Rect;
        }

        BufferedImage Qty5ButtonFile = imageUtils.pathToBuffered("/osrsAssets/DepositBox/notSelected/depositqty5.png");
        Rectangle result = templateMatcher.match(device, Qty5ButtonFile, threshold);

        // If unselected is not found, try to find the selected Quantity 5 button
        if (result == null) {
            BufferedImage Qty5SelectedButtonFile = imageUtils.pathToBuffered("/osrsAssets/DepositBox/selected/depositqty5selected.png");
            result = templateMatcher.match(device, Qty5SelectedButtonFile, threshold);
        }

        // Cache the result if found
        if (result != null) {
            cachedQuantity5Rect = result;
        } else {
            logger.devLog("Neither the unselected nor the selected Quantity 5 button was found.");
        }

        return result;
    }

    public Rectangle findQuantity10(String device) {
        // If we have a cached location, return it
        if (cachedQuantity10Rect != null) {
            return cachedQuantity10Rect;
        }

        BufferedImage Qty10ButtonFile = imageUtils.pathToBuffered("/osrsAssets/DepositBox/notSelected/depositqty10.png");
        Rectangle result = templateMatcher.match(device, Qty10ButtonFile, threshold);

        if (result == null) {
            BufferedImage Qty10SelectedButtonFile = imageUtils.pathToBuffered("/osrsAssets/DepositBox/selected/depositqty10selected.png");
            result = templateMatcher.match(device, Qty10SelectedButtonFile, threshold);
        }

        // Cache the result if found
        if (result != null) {
            cachedQuantity10Rect = result;
        } else {
            logger.devLog("Neither the unselected nor the selected Quantity 10 button was found.");
        }

        return result;
    }

    public Rectangle findQuantityCustom(String device) {
        // If we have a cached location, return it
        if (cachedQuantityXRect != null) {
            return cachedQuantityXRect;
        }
        BufferedImage QtyXButtonFile = imageUtils.pathToBuffered("/osrsAssets/DepositBox/notSelected/depositqtyx.png");
        Rectangle result = templateMatcher.match(device, QtyXButtonFile, threshold);

        if (result == null) {
            BufferedImage QtyXSelectedButtonFile = imageUtils.pathToBuffered("/osrsAssets/DepositBox/selected/depositqtyxselected.png");
            result = templateMatcher.match(device, QtyXSelectedButtonFile, threshold);
        }

        // Cache the result if found
        if (result != null) {
            cachedQuantityXRect = result;
        } else {
            logger.devLog("Neither the unselected nor the selected Quantity X button was found.");
        }

        return result;
    }

    public Rectangle findQuantityAll(String device) {
        // If we have a cached location, return it
        if (cachedQuantityAllRect != null) {
            return cachedQuantityAllRect;
        }

        BufferedImage QtyAllButtonFile = imageUtils.pathToBuffered("/osrsAssets/DepositBox/notSelected/depositall.png");
        Rectangle result = templateMatcher.match(device, QtyAllButtonFile, threshold);

        // If unselected is not found, try to find the selected Quantity All button
        if (result == null) {
            BufferedImage QtyAllSelectedButtonFile = imageUtils.pathToBuffered("/osrsAssets/DepositBox/selected/depositallselected.png");
            result = templateMatcher.match(device, QtyAllSelectedButtonFile, threshold);
        }

        // Cache the result if found
        if (result != null) {
            cachedQuantityAllRect = result;
        } else {
            logger.devLog("Neither the unselected nor the selected Quantity All button was found.");
        }

        return result;
    }

    public Rectangle findDepositInventory(String device) {
        // If we have a cached location and it's still valid, return it
        if (cachedDepositInventoryRect != null) {
            return cachedDepositInventoryRect;
        }
        BufferedImage depositInventoryButtonFile = imageUtils.pathToBuffered("/osrsAssets/DepositBox/depositinvent.png");
        Rectangle result = templateMatcher.match(device, depositInventoryButtonFile, threshold);

        // Cache the result if found
        if (result != null) {
            cachedDepositInventoryRect = result;
        } else {
            logger.devLog("The Deposit Inventory button was not found.");
        }

        return result;
    }

    public Rectangle findDepositWorn(String device) {
        // If we have a cached location and it's still valid, return it
        if (cachedDepositWornRect != null) {
            return cachedDepositWornRect;
        }


        BufferedImage depositWornButtonFile = imageUtils.pathToBuffered("/osrsAssets/DepositBox/depositequip.png");
        Rectangle result = templateMatcher.match(device, depositWornButtonFile, threshold);

        // Cache the result if found
        if (result != null) {
            cachedDepositWornRect = result;
        } else {
            logger.devLog("The Deposit Worn items button was not found.");
        }

        return result;
    }

    public Rectangle findDepositLoot(String device) {
        // If we have a cached location and it's still valid, return it
        if (cachedDepositLootRect != null) {
            return cachedDepositLootRect;
        }


        BufferedImage depositLootButtonFile = imageUtils.pathToBuffered("/osrsAssets/DepositBox/depositlootbag.png");
        Rectangle result = templateMatcher.match(device, depositLootButtonFile, threshold);

        // Cache the result if found
        if (result != null) {
            cachedDepositLootRect = result;
        } else {
            logger.devLog("The Deposit Loot button was not found.");
        }

        return result;
    }

    public Rectangle findCloseDepositBox(String device) {
        // If we have a cached location and it's still valid, return it
        if (cachedCloseDepositBoxRect != null) {
            return cachedCloseDepositBoxRect;
        }
        BufferedImage closeDepositBoxFile = imageUtils.pathToBuffered("/osrsAssets/DepositBox/depositclose.png");
        Rectangle result = templateMatcher.match(device, closeDepositBoxFile, threshold);

        // Cache the result if found
        if (result != null) {
            cachedCloseDepositBoxRect = result;
        } else {
            logger.devLog("The Close Deposit Box button was not found.");
        }

        return result;
    }

    public Rectangle findSetCustomQuantity(String device) {
        BufferedImage setCustomQuantityFile = imageUtils.pathToBuffered("src/main/resources/osrsAssets/DepositBox/depositsetcustomqty.png");
        Rectangle result = templateMatcher.match(device, setCustomQuantityFile, threshold);

        if (result == null) {
            logger.devLog("The Set Custom Quantity button was not found.");
        }

        return result; // Returns the found Rectangle or null if no image is found
    }

    public Rectangle[] buildDepositBoxGrid(String device) {
        Rectangle closeDepositBoxRect = findCloseDepositBox(device);
        if (closeDepositBoxRect == null) {
            logger.devLog("Close Deposit Box button must be visible to build the grid.");
            return null;
        }

        // Calculate the starting point of the grid from the upper right corner of the close button
        Point gridStartPoint = new Point(
                closeDepositBoxRect.x + closeDepositBoxRect.width + GRID_START_OFFSET_X,
                closeDepositBoxRect.y + GRID_START_OFFSET_Y
        );

        Rectangle[] itemSlots = new Rectangle[GRID_COLUMNS * GRID_ROWS];

        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLUMNS; col++) {
                // Create rectangles from right to left by subtracting from the x-coordinate
                int x = gridStartPoint.x - col * (ITEM_SLOT_SIZE + ITEM_PADDING_WIDTH);
                int y = gridStartPoint.y + row * (ITEM_SLOT_SIZE + ITEM_PADDING_HEIGHT);

                // The slot in the itemSlots array should be determined by reversing the column index within each row
                int slotIndex = (row * GRID_COLUMNS) + (GRID_COLUMNS - 1 - col);

                itemSlots[slotIndex] = new Rectangle(x, y, ITEM_SLOT_SIZE, ITEM_SLOT_SIZE);
            }
        }

        return itemSlots;
    }

}
