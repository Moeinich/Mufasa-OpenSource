package osr.mapping;

import helpers.GetGameView;
import helpers.Logger;
import helpers.Color.ColorFinder;
import helpers.Color.utils.ColorRectanglePair;
import helpers.openCV.ImageRecognition;
import helpers.openCV.utils.MatchedRectangle;
import helpers.visualFeedback.FeedbackObservables;
import org.json.JSONObject;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import osr.mapping.utils.GEHelper;
import osr.mapping.utils.ItemProcessor;
import scripts.APIClasses.ClientAPI;
import scripts.APIClasses.ConditionAPI;
import utils.Constants;
import java.util.List;

import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class GrandExchange {

    private final GEHelper geHelper;
    private final Logger logger;
    private final ColorFinder colorFinder;
    private final ClientAPI clientAPI;
    private final ConditionAPI conditionAPI;
    private final GetGameView getGameView;
    private final ImageRecognition imageRecognition;
    private final ItemProcessor itemProcessor;

    public GrandExchange(GEHelper geHelper, Logger logger, ColorFinder colorFinder, ClientAPI clientAPI, ConditionAPI conditionAPI, GetGameView getGameView, ImageRecognition imageRecognition, ItemProcessor itemProcessor) {
        this.geHelper = geHelper;
        this.logger = logger;
        this.colorFinder = colorFinder;
        this.clientAPI = clientAPI;
        this.conditionAPI = conditionAPI;
        this.getGameView = getGameView;
        this.imageRecognition = imageRecognition;
        this.itemProcessor = itemProcessor;
    }

    private static final List<ColorRectanglePair> colorRectPairs = List.of(
            new ColorRectanglePair(GEHelper.GE_OPEN_CHECK_COLOR, GEHelper.GE_TOPLEFT_CHECK_RECT),
            new ColorRectanglePair(GEHelper.GE_OPEN_CHECK_COLOR, GEHelper.GE_BOTTOMRIGHT_CHECK_RECT),
            new ColorRectanglePair(List.of(GEHelper.GE_ORANGE_TEXT_COLOR), GEHelper.GE_TEXTCOLOR_CHECK_RECT) // Single color list
    );

    public boolean isOpen(String device) {
        boolean allMatch = colorFinder.areAllColorsInPairs(device, colorRectPairs, 10);

        if (allMatch) {
            logger.devLog("GE is open");
            return true;
        } else {
            logger.devLog("GE is not open");
            return false;
        }
    }

    public int getProgress(String device, int slot) {
        // Get the progress rectangle for the given slot
        Rectangle progressRect = geHelper.getProgressRect(slot);
        FeedbackObservables.rectangleObservable.setValue(device, progressRect);

        // Check if there are any green or red color points in the rectangle
        boolean hasGreenOrRed = !colorFinder.processColorPointsInRect(device, GEHelper.GE_GREEN_PROGRESS_COLOR, progressRect, 5).isEmpty() ||
                !colorFinder.processColorPointsInRect(device, GEHelper.GE_RED_PROGRESS_COLOR, progressRect, 5).isEmpty();

        if (hasGreenOrRed) {
            logger.devLog("Green or red color points found in the progress rectangle for slot " + slot + ". Progress is 100%.");
            return 100;
        }

        // Get the list of orange color points within the rectangle
        java.util.List<Point> colorPoints = colorFinder.processColorPointsInRect(device, GEHelper.GE_ORANGE_PROGRESS_COLOR, progressRect, 5);

        if (colorPoints.isEmpty()) {
            logger.devLog("No orange color points found in the progress rectangle for slot " + slot + ". Progress is 0%.");
            return 0;
        }

        // Find the most rightward point by iterating over the color points
        int maxX = colorPoints.stream().mapToInt(point -> point.x).max().orElse(progressRect.x);

        // Calculate the progress percentage based on the most rightward point
        int progressWidth = maxX - progressRect.x;
        int progressPercentage = (int) ((double) progressWidth / progressRect.width * 100);

        logger.devLog("Progress for slot " + slot + " " + progressPercentage + "%");

        // Return the progress as a percentage (0-100)
        return Math.min(progressPercentage, 100); // Ensures it doesn't exceed 100%
    }

    public int getCanceled(String device) {
        int cancelledOrders = 0;

        for (int i = 1; i <= 8; i++) {
            boolean cancelled = geHelper.isSlotCancelled(device, i);
            if (cancelled) {
                cancelledOrders++;
            }
        }

        logger.devLog("Total cancelled orders: " + cancelledOrders);
        return cancelledOrders;
    }

    public int getCompleted(String device) {
        int completedOrders = 0;

        for (int i = 1; i <= 8; i++) {
            int progress = getProgress(device, i);
            if (progress == 100) {
                completedOrders++;
            }
        }

        logger.devLog("Total completed orders: " + completedOrders);
        return completedOrders;
    }

    public void collectAllItems(String device) {
        if (hasCollectableItems(device)) {
            logger.debugLog("Collecting all GE items", device);
            clientAPI.tap(GEHelper.GE_COLLECT_ALL_RECT);
            conditionAPI.wait(() -> !hasCollectableItems(device), 100, 40);
        } else {
            logger.debugLog("No GE items to collect.", device);
        }
    }

    public boolean hasCollectableItems(String device) {
        return geHelper.hasCollectableItems(device);
    }

    public int buyItem(String device, String searchString, int itemID, int quantity, int price) {
        int slot = geHelper.getFirstAvailableSlot(device);

        if (slot == -1) {
            logger.debugLog("There are currently no available slots in the Grand Exchange.", device);
            return -1;
        } else {
            // Select buy for the slot that is available
            clientAPI.tap(geHelper.getBuyRect(slot));

            // Wait for the search area to be visible/available
            conditionAPI.wait(() -> geHelper.isSearchAreaVisible(device), 150, 30);

            // Only continue if we have the search area visible
            if (geHelper.isSearchAreaVisible(device)) {
                // Type our item name in the search bar
                for (char c : searchString.toCharArray()) {
                    String keycode;
                    if (c == ' ') {
                        keycode = "KEYCODE_SPACE";
                    } else {
                        keycode = "KEYCODE_" + Character.toUpperCase(c);
                    }
                    clientAPI.sendKeystroke(keycode);
                }
                conditionAPI.sleep(2250);

                // Proceed by finding the item
                Mat croppedItemImage = getCroppedItemImage(itemID);
                Mat searchArea = getGameView.getSubmat(device, GEHelper.GE_SEARCHAREA_RECT);

                MatchedRectangle item = imageRecognition.returnBestMatchObject(croppedItemImage, searchArea, 0.7);

                if (item != null) {
                    // Shift the item rectangle by adding the GE_SEARCHAREA_RECT offsets
                    Rectangle adjustedItemRect = new Rectangle(
                            (int) (item.getX() + GEHelper.GE_SEARCHAREA_RECT.x),
                            (int) (item.getY() + GEHelper.GE_SEARCHAREA_RECT.y),
                            (int) item.getWidth(),
                            (int) item.getHeight()
                    );

                    clientAPI.tap(adjustedItemRect, device);
                    conditionAPI.wait(() -> !geHelper.isSearchAreaVisible(device), 150, 30);
                    conditionAPI.sleep(200);

                    // Open the custom quantity menu
                    clientAPI.tap(GEHelper.QUANTITY_CUSTOM_RECT);
                    conditionAPI.wait(() -> geHelper.isSetQuantityVisible(device), 150, 30);
                    conditionAPI.sleep(200);

                    // Fill in the quantity
                    String numberString = String.valueOf(quantity);

                    for (char digit : numberString.toCharArray()) {
                        String keycode = "KEYCODE_" + digit;
                        clientAPI.sendKeystroke(keycode);
                    }
                    conditionAPI.sleep(150);

                    // Press enter to set the quantity
                    clientAPI.sendKeystroke("KEYCODE_ENTER");
                    conditionAPI.wait(() -> !geHelper.isSetQuantityVisible(device), 150, 30);

                    // Open the custom price menu
                    clientAPI.tap(GEHelper.CUSTOM_PRICE_RECT);
                    conditionAPI.wait(() -> geHelper.isSetQuantityVisible(device), 150, 30);
                    conditionAPI.sleep(200);

                    // Fill in the price
                    String numberString2 = String.valueOf(price);

                    for (char digit : numberString2.toCharArray()) {
                        String keycode = "KEYCODE_" + digit;
                        clientAPI.sendKeystroke(keycode);
                    }
                    conditionAPI.sleep(150);

                    // Press enter to set the price
                    clientAPI.sendKeystroke("KEYCODE_ENTER");
                    conditionAPI.wait(() -> !geHelper.isSetQuantityVisible(device), 150, 30);

                    // Finish by pressing confirm
                    clientAPI.tap(GEHelper.CONFIRM_SALE_RECT);
                    conditionAPI.sleep(150);

                    return slot;
                } else {
                    logger.devLog("Item " + searchString + " not found in the GE search area.");
                    return -1;
                }
            } else {
                return -1;
            }
        }
    }

    public int sellItem(String device, int itemID, int quantity, int price) {
        int slot = geHelper.getFirstAvailableSlot(device);

        if (slot == -1) {
            logger.debugLog("There are currently no available slots in the Grand Exchange.", device);
            return -1;
        } else {
            // Select sell for the slot that is available
            clientAPI.tap(geHelper.getSellRect(slot));
            conditionAPI.sleep(200);

            // Proceed by finding the item
            Mat croppedItemImage = getCroppedItemImage(itemID);
            Mat inventoryArea = getGameView.getSubmat(device, Constants.INVENTORY_RECT);

            MatchedRectangle item = imageRecognition.returnBestMatchObject(croppedItemImage, inventoryArea, 0.7);

            if (item != null) {
                // Shift the item rectangle by adding the GE_SEARCHAREA_RECT offsets
                Rectangle adjustedItemRect = new Rectangle(
                        (int) (item.getX() + Constants.INVENTORY_RECT.x),
                        (int) (item.getY() + Constants.INVENTORY_RECT.y),
                        (int) item.getWidth(),
                        (int) item.getHeight()
                );
                clientAPI.tap(adjustedItemRect, device);
                conditionAPI.sleep(2250);

                // Open the custom quantity menu
                clientAPI.tap(GEHelper.QUANTITY_CUSTOM_RECT);
                conditionAPI.wait(() -> geHelper.isSetQuantityVisible(device), 150, 30);
                conditionAPI.sleep(200);

                // Fill in the quantity
                String numberString = String.valueOf(quantity);

                for (char digit : numberString.toCharArray()) {
                    String keycode = "KEYCODE_" + digit;
                    clientAPI.sendKeystroke(keycode);
                }
                conditionAPI.sleep(150);

                // Press enter to set the quantity
                clientAPI.sendKeystroke("KEYCODE_ENTER");
                conditionAPI.wait(() -> !geHelper.isSetQuantityVisible(device), 150, 30);

                // Open the custom price menu
                clientAPI.tap(GEHelper.CUSTOM_PRICE_RECT);
                conditionAPI.wait(() -> geHelper.isSetQuantityVisible(device), 150, 30);
                conditionAPI.sleep(200);

                // Fill in the price
                String numberString2 = String.valueOf(price);

                for (char digit : numberString2.toCharArray()) {
                    String keycode = "KEYCODE_" + digit;
                    clientAPI.sendKeystroke(keycode);
                }
                conditionAPI.sleep(150);

                // Press enter to set the price
                clientAPI.sendKeystroke("KEYCODE_ENTER");
                conditionAPI.wait(() -> !geHelper.isSetQuantityVisible(device), 150, 30);

                // Finish by pressing confirm
                clientAPI.tap(GEHelper.CONFIRM_SALE_RECT);
                conditionAPI.sleep(150);

                return slot;
            } else {
                logger.devLog("Item " + itemID + " not found in the GE search area.");
                return -1;
            }
        }
    }

    public int getFirstAvailableSlot(String device) {
        return geHelper.getFirstAvailableSlot(device);
    }

    public boolean isSlotAvailable(String device, int slot) {
        return geHelper.isSlotAvailable(device, slot);
    }

    public int slotsAvailable(String device) {
        return geHelper.slotsAvailable(device);
    }

    public boolean has1stItemToCollect(String device) {
        return geHelper.hasSlot1CollectableItems(device);
    }

    public boolean has2ndItemToCollect(String device) {
        return geHelper.hasSlot2CollectableItems(device);
    }

    public int getPrice(int itemID) {
        HttpURLConnection connection = null;
        try {
            // Create the URL with the item ID
            URL url = new URL("https://prices.runescape.wiki/api/v1/osrs/latest?id=" + itemID);
            connection = (HttpURLConnection) url.openConnection();

            // Set up the request
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Safari/537.36");

            // Get the response
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                // Parse the JSON response
                JSONObject jsonResponse = new JSONObject(response.toString());
                JSONObject itemData = jsonResponse.getJSONObject("data").getJSONObject(String.valueOf(itemID));

                // Get the latest high and low prices
                int highPrice = itemData.optInt("high", -1);
                int lowPrice = itemData.optInt("low", -1);

                // Calculate the average price, considering the case where one of the prices might be -1
                int averagePrice;
                if (highPrice != -1 && lowPrice != -1) {
                    averagePrice = (highPrice + lowPrice) / 2;
                } else if (highPrice != -1) {
                    averagePrice = highPrice;
                } else {
                    averagePrice = lowPrice;
                }

                // Return the calculated average price
                return averagePrice;
            } else {
                System.out.println("GET request failed with response code: " + responseCode);
                return -1;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private Mat getCroppedItemImage(int itemID) {
        Mat itemImage = itemProcessor.getItemImage(itemID);
        Rect itemROI = new Rect(0, 10, itemImage.width(), itemImage.height() - 10);
        return new Mat(itemImage, itemROI);
    }
}
