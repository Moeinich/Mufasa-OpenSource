package osr.mapping;

import helpers.GetGameView;
import helpers.Logger;
import helpers.Color.ColorFinder;
import helpers.Color.utils.ColorRectanglePair;
import helpers.utils.Area;
import helpers.utils.Tile;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import osr.mapping.utils.PlayerHelper;
import osr.walker.Walker;
import osr.walker.utils.ChunkCoordinates;
import scripts.APIClasses.ClientAPI;
import scripts.APIClasses.ConditionAPI;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import static utils.Constants.CURRENT_ZOOM_LEVEL;
import static utils.Constants.generateRandomDelay;

public class Player {
    private final PlayerHelper playerHelper;
    private final Logger logger;
    private final GetGameView getGameView;
    private final Walker walker;
    private final ClientAPI clientAPI;
    private final GameTabs gameTabs;
    private final ColorFinder colorFinder;
    private final ConditionAPI conditionAPI;
    private final XPBar xpBar;

    // Static pre-defined colors
    private final List<Color> retaliateOnColors = Arrays.asList(
            Color.decode("#8A211E"),
            Color.decode("#641816"),
            Color.decode("#7C1D1B")
    );

    private static final List<Rect> shiftRects = Arrays.asList(
            new Rect(445, 263, 10, 17),  // SHIFT_RECT_ZOOM_1
            new Rect(438, 259, 13, 21),  // SHIFT_RECT_ZOOM_2
            new Rect(446, 266, 10, 13),  // SHIFT_RECT_ZOOM_3
            new Rect(428, 269, 19, 18),  // SHIFT_RECT_ZOOM_4
            new Rect(450, 280, 24, 27)   // SHIFT_RECT_ZOOM_5
    );
    private static final List<Integer> medianShiftLevels = Arrays.asList(
            1000,
            1000,
            2500,
            2500,
            4800
    );

    // Static pre-defined rectangles
    private final Rectangle autoRetRect = new Rectangle(610, 375, 169, 38);

    private final static Rectangle widgetOpenRect = new Rectangle(14, 0, 14, 11);
    private final static Color widgetOpenColor = Color.decode("#4f4835");
    private final static Rectangle clickContinueRect = new Rectangle(148, 94, 336, 31);
    private final static Color clickContinueColor = Color.decode("#0000ff");
    private final static Rectangle leveledUpTextRect = new Rectangle(99, 21, 414, 35);
    private final static  Color leveledUpColor = Color.decode("#000080");

    private final static List<ColorRectanglePair> colorRectPairs = List.of(
            new ColorRectanglePair(List.of(widgetOpenColor), widgetOpenRect),
            new ColorRectanglePair(List.of(clickContinueColor), clickContinueRect),
            new ColorRectanglePair(List.of(leveledUpColor), leveledUpTextRect)
    );

    public Player(XPBar xpBar, PlayerHelper playerHelper, Logger logger, GetGameView getGameView, Walker walker, ClientAPI clientAPI, GameTabs gameTabs, ColorFinder colorFinder, ConditionAPI conditionAPI) {
        this.xpBar = xpBar;
        this.playerHelper = playerHelper;
        this.logger = logger;
        this.getGameView = getGameView;
        this.walker = walker;
        this.clientAPI = clientAPI;
        this.gameTabs = gameTabs;
        this.colorFinder = colorFinder;
        this.conditionAPI = conditionAPI;
    }

    // Default reset threshold is 4000ms as per PlayerAPI
    private int lastXP = -1;
    private long lastCheckTime = 0;
    /**
     * Checks if the XP gained has changed since the last check.
     *
     * @param device the device to read the XP from
     * @param resetThreshold the maximum allowed duration before resetting the check
     * @return true if XP has changed, false if XP is 0 or hasn't changed
     */
    public boolean xpGained(String device, int resetThreshold) {
        long currentTime = System.currentTimeMillis();

        // Reset if too much time has passed since the last check
        if (currentTime - lastCheckTime > resetThreshold) {
            lastXP = -1;  // Reset XP tracking to an invalid value
        }

        // Get the current XP
        int currentXP = xpBar.readXP(device);

        // If XP is 0 or -1, return false as it may indicate an invalid read
        if (currentXP == 0 || currentXP == -1) {
            return false;
        }

        // If this is not the first call and XP has changed, return true
        if (lastXP != -1 && lastXP < currentXP) {
            lastXP = currentXP;  // Update last XP to current XP
            lastCheckTime = currentTime;  // Update the last check time
            return true;  // XP has changed
        }

        // Update last XP and last check time for the first valid read or no change
        lastXP = currentXP;
        lastCheckTime = currentTime;

        // Return false if XP hasn't changed
        return false;
    }

    public boolean leveledUp(String device) {
        return colorFinder.areAllColorsInPairs(device, colorRectPairs, 10);
    }

    public boolean within(String device, Area areaToSearchIn) {
        ChunkCoordinates currentLocation = walker.getPlayerPosition(device).getWorldCoordinates(device);

        if (currentLocation != null) {
            // Check plane first, if not equal per definition, it cannot be within that area.
            if (currentLocation.z != areaToSearchIn.getTopTile().z) {
                return false;
            }
            java.awt.Point topPoint = new Point(areaToSearchIn.getTopTile().x(), areaToSearchIn.getTopTile().y());
            java.awt.Point bottomPoint = new Point(areaToSearchIn.getBottomTile().x(), areaToSearchIn.getBottomTile().y());
            java.awt.Rectangle targetRectangle = createRectangleFromPoints(topPoint, bottomPoint);

            return targetRectangle.contains(currentLocation.x, currentLocation.y);
        } else {
            logger.devLog("No current location found for device " + device);
            return false;
        }
    }

    public boolean atTile(String device, Tile tileToCheck) {
        ChunkCoordinates currentLocation = walker.getPlayerPosition(device).getWorldCoordinates(device);

        if (currentLocation != null) {
            // Check if the Z index (plane) matches
            if (currentLocation.z != tileToCheck.z()) {
                return false;
            }

            // Allow a 3-pixel tolerance for X and Y coordinates
            return Math.abs(currentLocation.x - tileToCheck.x()) <= 3
                    && Math.abs(currentLocation.y - tileToCheck.y()) <= 3;
        } else {
            // Return false if there is no current location
            return false;
        }
    }

    public boolean atPosition(String device, Tile positionToCheck) {
        ChunkCoordinates currentLocation = walker.getPlayerPosition(device).getWorldCoordinates(device);

        if (currentLocation != null) {
            // Check plane first, if not equal per definition, it cannot be within that area.
            if (currentLocation.z != positionToCheck.z) {
                return false;
            }
            // Check if the current location is within 3 pixels of the position to check
            return Math.abs(currentLocation.x - positionToCheck.x) <= 3
                    && Math.abs(currentLocation.y - positionToCheck.y) <= 3;
        } else {
            // Return false if there is no current location
            return false;
        }
    }

    public boolean tileEquals(Tile originTile, Tile tileToCheck) {
        // Check if the Z index matches between the origin tile and the tile to check
        if (originTile.z() != tileToCheck.z()) {
            return false;
        }

        int originTileX = originTile.x();
        int originTileY = originTile.y();
        int tileToCheckX = tileToCheck.x();
        int tileToCheckY = tileToCheck.y();

        // Allow a 3-pixel tolerance for X and Y coordinates
        return Math.abs(originTileX - tileToCheckX) <= 3 && Math.abs(originTileY - tileToCheckY) <= 3;
    }

    private java.awt.Rectangle createRectangleFromPoints(java.awt.Point topPoint, java.awt.Point bottomPoint) {
        int x = Math.min(topPoint.x, bottomPoint.x);
        int y = Math.min(topPoint.y, bottomPoint.y);
        int width = Math.abs(bottomPoint.x - topPoint.x) + 1;
        int height = Math.abs(bottomPoint.y - topPoint.y) + 1;

        return new java.awt.Rectangle(x, y, width, height);
    }
    public int getHP(String device) {
        return playerHelper.readHP(device);
    }
    public int getPray(String device) {
        return playerHelper.readPray(device);
    }
    public int getRun(String device) {
        return playerHelper.readRun(device);
    }

    public int getSpec(String device) {
        return playerHelper.readSpec(device);
    }

    public void useSpec(String device) {
        playerHelper.useSpec(device);
    }

    public boolean isRunEnabled(String device) {
        return playerHelper.isRunEnabled(device);
    }

    public void toggleRun(String device) {
        playerHelper.toggleRun(device);
    }

    public void enableAutoRetaliate(String device) {
        gameTabs.openTab(device, "Combat");

        if (!colorFinder.isAnyColorInRect(device, retaliateOnColors, autoRetRect, 5)) {
            clientAPI.tap(autoRetRect);
            conditionAPI.wait(() -> colorFinder.isAnyColorInRect(device, retaliateOnColors, autoRetRect, 5), 200, 20);
        }
    }

    public void disableAutoRetaliate(String device) {
        gameTabs.openTab(device, "Combat");

        if (colorFinder.isAnyColorInRect(device, retaliateOnColors, autoRetRect, 5)) {
            clientAPI.tap(autoRetRect);
            conditionAPI.wait(() -> !colorFinder.isAnyColorInRect(device, retaliateOnColors, autoRetRect, 5), 200, 20);
        }
    }

    public boolean isAutoRetaliateOn(String device) {
        gameTabs.openTab(device, "Combat");

        if (colorFinder.isAnyColorInRect(device, retaliateOnColors, autoRetRect, 5)) {
            logger.devLog("Auto retaliate is currently enabled.");
            return true;
        } else {
            logger.devLog("Auto retaliate is currently disabled.");
            return false;
        }
    }

    private final ThreadLocal<Map<String, Integer>> threadLocalCheckTimes = ThreadLocal.withInitial(HashMap::new);

    public boolean checkMovement(int checkTimes, boolean expectMovement, String device) {
        // Initialize or reset the check counter for the device
        Map<String, Integer> deviceCheckTimes = threadLocalCheckTimes.get();
        deviceCheckTimes.putIfAbsent(device, 0);

        Tile lastPosition = walker.getPlayerPosition(device)
                .getWorldCoordinates(device)
                .getTile();
        boolean runEnabled = isRunEnabled(device);

        // We will do at most `checkTimes` iterations
        for (int i = 0; i < checkTimes; i++) {
            Tile currentPosition = walker.getPlayerPosition(device)
                    .getWorldCoordinates(device)
                    .getTile();
            logger.print(String.format(
                    "Check %d: Checking for movement from %s to %s.",
                    i + 1, lastPosition, currentPosition
            ));

            // Check if position has changed
            if (currentPosition.equals(lastPosition)) {
                // Position hasn't changed => increment counter
                int consecutiveNoMove = deviceCheckTimes.get(device) + 1;
                deviceCheckTimes.put(device, consecutiveNoMove);

                // If we EXPECT NO MOVEMENT, check whether we've hit 2 consecutive checks
                if (!expectMovement && consecutiveNoMove >= 2) {
                    logger.print(
                            "Player has not moved for 2 consecutive checks. " +
                                    "Early exit from check."
                    );
                    deviceCheckTimes.put(device, 0); // Reset the counter
                    return true;
                }
            } else {
                // Position changed => reset the no-move counter
                deviceCheckTimes.put(device, 0);

                // If we EXPECT MOVEMENT, we can exit as soon as we see a move
                if (expectMovement) {
                    logger.print("Player has moved as expected. Early exit from check.");
                    return true;
                }
                // Update lastPosition to the new tile since the player has moved
                lastPosition = currentPosition;
            }

            // Delay to allow for movement updates
            conditionAPI.sleep(
                    generateRandomDelay(
                            runEnabled ? 100 : 200,
                            runEnabled ? 200 : 300
                    )
            );
        }

        // If we exit the loop, reset the counter and return false
        deviceCheckTimes.put(device, 0);
        logger.print(
                "Movement check completed. Final status did not meet expectations."
        );
        return false;
    }

    private Mat lastFrame;  // Store the last frame to calculate pixel shifts
    private long lastFrameTime = 0;  // Store the timestamp of the last frame capture
    private final LinkedList<Double> pixelShiftCache = new LinkedList<>();  // Cache for recent pixel shifts
    private final int cacheSize = 5;  // Number of results to keep in the cache
    private final long FRAME_EXPIRATION_TIME_MS = 5000;  // Frame expiration time in milliseconds (5 seconds)

    // Method to calculate the median of the cached shifts
    private double calculateMedian(LinkedList<Double> cache) {
        // Filter out zero values to avoid skewing the median
        java.util.List<Double> filteredCache = cache.stream().filter(val -> val > 0).collect(Collectors.toList());

        // If all values are zero, return 0 (indicating no movement)
        if (filteredCache.isEmpty()) {
            return 0;
        }

        // Sort the non-zero values and calculate the median
        Collections.sort(filteredCache);
        int middle = filteredCache.size() / 2;
        if (filteredCache.size() % 2 == 0) {
            return (filteredCache.get(middle - 1) + filteredCache.get(middle)) / 2.0;
        } else {
            return filteredCache.get(middle);
        }
    }

    // Method to get the current median pixel shift
    public double currentPixelShift(String device) {
        if (CURRENT_ZOOM_LEVEL < 1 || CURRENT_ZOOM_LEVEL > shiftRects.size()) {
            logger.print("Invalid zoom level: " + CURRENT_ZOOM_LEVEL);
            return 0.0;
        }

        long currentTime = System.currentTimeMillis();

        // Select the correct Rect based on currentZoomLevel
        Rect selectedRect = shiftRects.get(CURRENT_ZOOM_LEVEL - 1);  // Subtract 1 to match index

        // Get the current frame for comparison using the selected Rect
        Mat currentPlayerRegion = getGameView.getSubmat(device, selectedRect);

        if (lastFrame == null || (currentTime - lastFrameTime) > FRAME_EXPIRATION_TIME_MS) {
            pixelShiftCache.clear();
            lastFrame = currentPlayerRegion.clone();
            lastFrameTime = currentTime;
            return 0.0;
        }

        Mat diff = new Mat();
        Core.absdiff(currentPlayerRegion, lastFrame, diff);
        Scalar sumDiff = Core.sumElems(diff);
        double currentShift = sumDiff.val[0];

        if (pixelShiftCache.size() >= cacheSize) {
            pixelShiftCache.poll();
        }
        pixelShiftCache.add(currentShift);

        lastFrame = currentPlayerRegion.clone();
        lastFrameTime = currentTime;

        return calculateMedian(pixelShiftCache);
    }

    // Method to check if the player is idle based on the threshold
    public boolean isIdle(String device) {
        // Get the current median pixel shift
        double medianShift = currentPixelShift(device);

        // Log the current shift value
        logger.devLog("current shift: " + medianShift);

        // Return true if the median shift is below the threshold (indicating idleness)
        if (medianShift == 0) { // Return false if we get the 0 results, to make sure we dont stop some action cause we cleared cache.
            return false;
        }

        Integer shiftLevel = medianShiftLevels.get(CURRENT_ZOOM_LEVEL - 1);
        return medianShift < shiftLevel; //as far as my testing, the max idle shifting is about 1180.
    }
}
