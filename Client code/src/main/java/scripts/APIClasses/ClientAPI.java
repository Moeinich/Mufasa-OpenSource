package scripts.APIClasses;

import helpers.CacheManager;
import helpers.Logger;
import helpers.OCR.DigitReader;
import helpers.adb.ADBHandler;
import helpers.Color.ColorFinder;
import helpers.openCV.utils.MatchedRectangle;
import helpers.services.AFKHandlerService;
import helpers.services.BreakHandlerService;
import helpers.services.SleepHandlerService;
import helpers.visualFeedback.*;
import interfaces.iClient;
import scripts.ScriptInfo;
import utils.Constants;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static osr.mapping.utils.MinimapProjections.MINIMAP_ROTATION_ANGLE;
import static utils.Constants.randomizeCoordinates;

public class ClientAPI implements iClient {
    private final ConditionAPI conditionAPI;
    private final ADBHandler adbHandler;
    private final ColorFinder colorFinder;
    private final Logger logger;
    private final ScriptInfo scriptInfo;
    private final AFKHandlerService afkHandlerService;
    private final BreakHandlerService breakHandlerService;
    private final SleepHandlerService sleepHandlerService;
    private final CacheManager cacheManager;
    private final DigitReader digitReader;

    private final Random random = new Random();

    public ClientAPI(DigitReader digitReader, CacheManager cacheManager, ConditionAPI conditionAPI, ADBHandler adbHandler, ColorFinder colorFinder, Logger logger, ScriptInfo scriptInfo, AFKHandlerService afkHandlerService, BreakHandlerService breakHandlerService, SleepHandlerService sleepHandlerService) {
        this.cacheManager = cacheManager;
        this.conditionAPI = conditionAPI;
        this.adbHandler = adbHandler;
        this.colorFinder = colorFinder;
        this.logger = logger;
        this.scriptInfo = scriptInfo;
        this.afkHandlerService = afkHandlerService;
        this.breakHandlerService = breakHandlerService;
        this.sleepHandlerService = sleepHandlerService;
        this.digitReader = digitReader;
    }

    public void tap(int x, int y) {
        if (x == 0 && y == 0) {
            return;
        }

        int[] randomizedCoordinates = randomizeCoordinates(x, y);
        // Taps x,y coordinate on the device
        logger.devLog("Tapping at: " + randomizedCoordinates[0] + "," + randomizedCoordinates[1]);
        adbHandler.executeADBCommand(
                String.format("shell input tap %d %d", randomizedCoordinates[0], randomizedCoordinates[1]),
                scriptInfo.getCurrentEmulatorId()
        );


        Point tapPoint = new Point(randomizedCoordinates[0], randomizedCoordinates[1]);
        FeedbackObservables.pointObservable.setValue(scriptInfo.getCurrentEmulatorId(), tapPoint);
    }

    public void tap(Color targetColor, Rectangle boundingArea, int tolerance) {
        java.awt.Point upperLeft = new java.awt.Point(boundingArea.x, boundingArea.y);
        java.awt.Point lowerRight = new java.awt.Point(boundingArea.x + boundingArea.width, boundingArea.y + boundingArea.height);

        List<java.awt.Point> points = colorFinder.findColorAtPosition(scriptInfo.getCurrentEmulatorId(), targetColor, upperLeft, lowerRight, tolerance);

        // Check if any points were found
        if (points.isEmpty()) {
            return;
        }

        Point selectedPoint = getWeightedRandomTapFromPoints(points);
        logger.devLog("Tapping at: " + selectedPoint.x + "," + selectedPoint.y);
        tap(selectedPoint.x, selectedPoint.y);
        FeedbackObservables.listPointsAndPointObservable.setValue(scriptInfo.getCurrentEmulatorId(), new ListPointsAndPoint(points, selectedPoint));
    }

    public void tap(java.awt.Point point) {
        // Check if the point is null
        if (point == null) {
            System.err.println("Point provided is null.");
            return;
        }

        logger.devLog("Tapping at: " + point.x + "," + point.y);
        adbHandler.executeADBCommand(String.format("shell input tap %d %d", point.x, point.y), scriptInfo.getCurrentEmulatorId());
        FeedbackObservables.pointObservable.setValue(scriptInfo.getCurrentEmulatorId(), point);
    }

    public void tap(List<Point> points) {
        // Check if the list of points is null or empty
        if (points == null || points.isEmpty()) {
            System.err.println("List of points provided is null or empty.");
            return;
        }

        // Select a random point from the list
        Point randomPoint = points.get(random.nextInt(points.size()));

        // Log the tapping coordinates
        logger.devLog("Tapping at: " + randomPoint.x + "," + randomPoint.y);

        // Execute the ADB command to tap at the random point
        adbHandler.executeADBCommand(String.format("shell input tap %d %d", randomPoint.x, randomPoint.y), scriptInfo.getCurrentEmulatorId());
        FeedbackObservables.pointObservable.setValue(scriptInfo.getCurrentEmulatorId(), randomPoint);
    }

    public void tap(List<Point> points, boolean tapWithinRandom) {
        // Check if the list of points is null or empty
        if (points == null || points.isEmpty()) {
            System.err.println("List of points provided is null or empty.");
            return;
        }

        Point selectedPoint;
        if (tapWithinRandom) {
            selectedPoint = getWeightedRandomTapFromPoints(points);
        } else {
            // Select a random point from the list
            selectedPoint = points.get(random.nextInt(points.size()));
        }

        // Log the tapping coordinates
        logger.devLog("Tapping at: " + selectedPoint.x + "," + selectedPoint.y);

        // Execute the ADB command to tap at the selected point
        adbHandler.executeADBCommand(String.format("shell input tap %d %d", selectedPoint.x, selectedPoint.y), scriptInfo.getCurrentEmulatorId());
        FeedbackObservables.listPointsAndPointObservable.setValue(scriptInfo.getCurrentEmulatorId(), new ListPointsAndPoint(points, selectedPoint));
    }

    public void tap(Polygon polygon) {
        Rectangle bounds = polygon.getBounds();
        int x, y;

        do {
            x = bounds.x + random.nextInt(bounds.width);
            y = bounds.y + random.nextInt(bounds.height);
        } while (!polygon.contains(x, y));

        // Taps x, y coordinate on the device within the polygon
        logger.devLog("Tapping at: " + x + "," + y);
        adbHandler.executeADBCommand(
                String.format("shell input tap %d %d", x, y),
                scriptInfo.getCurrentEmulatorId()
        );

        // Submit the tap point to the observable
        Point tapPoint = new Point(x, y);
        FeedbackObservables.polygonAndPointObservable.setValue(scriptInfo.getCurrentEmulatorId(), new PolygonAndPoint(polygon, tapPoint));
    }

    public void tapMinimap(int x, int y, String device) {
        if (x == 0 && y == 0) {
            return;
        }

        // If minimap is already facing north, skip rotation calculations
        if (MINIMAP_ROTATION_ANGLE == 0) {
            logger.devLog("Tapping minimap at (north-facing): " + x + "," + y);
            adbHandler.executeADBCommand(String.format("shell input tap %d %d", x, y), device);
            return;
        }

        Point minimapCircle = cacheManager.getMinimapCenter(device);
        int centerX = minimapCircle.x;
        int centerY = minimapCircle.y;

        // Translate coordinates to be relative to the minimap's center
        int translatedX = x - centerX;
        int translatedY = y - centerY;

        // Calculate the inverse rotation angle to map coordinates back to north-facing orientation
        double angleRadians = Math.toRadians(-MINIMAP_ROTATION_ANGLE);

        // Apply rotation transformation
        int rotatedX = (int) (translatedX * Math.cos(angleRadians) - translatedY * Math.sin(angleRadians));
        int rotatedY = (int) (translatedX * Math.sin(angleRadians) + translatedY * Math.cos(angleRadians));

        // Translate back to screen coordinates
        int finalX = rotatedX + centerX;
        int finalY = rotatedY + centerY;

        logger.devLog("Tapping minimap at rotated angle: " + finalX + "," + finalY);
        adbHandler.executeADBCommand(String.format("shell input tap %d %d", finalX, finalY), device);
        FeedbackObservables.pointObservable.setValue(scriptInfo.getCurrentEmulatorId(), new Point(finalX, finalY));
    }

    public void tap(Rectangle rectangle) {
        if (rectangle == null) {
            return;
        }

        // Calculate the weighted tap point within the rectangle
        java.awt.Point tapPoint = calculateTruncatedGaussianPoint(rectangle);

        // Log the tapping coordinates
        logger.devLog("Tapping at: " + tapPoint.x + "," + tapPoint.y);

        // Execute ADB tap command
        adbHandler.executeADBCommand(
                String.format("shell input tap %d %d", tapPoint.x, tapPoint.y),
                scriptInfo.getCurrentEmulatorId()
        );

        // Submit the rectangle and the tap point to the observable
        RectangleAndPoint rectangleAndPoint = new RectangleAndPoint(rectangle, tapPoint);
        FeedbackObservables.rectangleAndPointObservable.setValue(scriptInfo.getCurrentEmulatorId(), rectangleAndPoint);
    }

    public void tap(Rectangle rectangle, String device) {
        Point tap = calculateTruncatedGaussianPoint(rectangle);

        // Log the tapping coordinates
        logger.devLog("Tapping at: " + tap.x + "," + tap.y);

        // Execute the ADB command with the random coordinates
        adbHandler.executeADBCommand(String.format("shell input tap %d %d", tap.x, tap.y), device);

        RectangleAndPoint rectangleAndPoint = new RectangleAndPoint(rectangle, tap);
        FeedbackObservables.rectangleAndPointObservable.setValue(scriptInfo.getCurrentEmulatorId(), rectangleAndPoint);
    }

    public void tap(MatchedRectangle matchedRectangle) {
        Point tap = calculateTruncatedGaussianPoint(matchedRectangle);

        // Check if getCurrentEmulatorId() returns a non-null value
        String emulatorId = scriptInfo.getCurrentEmulatorId();
        if (emulatorId == null) {
            System.err.println("Current emulator ID is null.");
            return;
        }

        // Log the tapping coordinates
        logger.devLog("Tapping at: " + tap.x + "," + tap.y);

        // Execute the ADB command with the Gaussian-based coordinates
        adbHandler.executeADBCommand(String.format("shell input tap %d %d", tap.x, tap.y), emulatorId);

        RectangleAndPoint rectangleAndPoint = new RectangleAndPoint(matchedRectangle, tap);
        FeedbackObservables.rectangleAndPointObservable.setValue(scriptInfo.getCurrentEmulatorId(), rectangleAndPoint);
    }

    public void tap(java.awt.Point point, String device) {
        // Check if the point is null
        if (point == null) {
            System.err.println("Point provided is null.");
            return;
        }

        logger.devLog("Tapping at: " + point.x + "," + point.y);
        adbHandler.executeADBCommand(String.format("shell input tap %d %d", point.x, point.y), device);

        FeedbackObservables.pointObservable.setValue(scriptInfo.getCurrentEmulatorId(), point);
    }

    public void tap(MatchedRectangle matchedRectangle, String emulatorId) {
        Point tap = calculateTruncatedGaussianPoint(matchedRectangle);

        // Check if getCurrentEmulatorId() returns a non-null value
        if (emulatorId == null) {
            System.err.println("Current emulator ID is null.");
            return;
        }

        // Log the tapping coordinates
        logger.devLog("Tapping at: " + tap.x + "," + tap.y);

        // Execute the ADB command with the Gaussian-based coordinates
        adbHandler.executeADBCommand(String.format("shell input tap %d %d", tap.x, tap.y), emulatorId);

        RectangleAndPoint rectangleAndPoint = new RectangleAndPoint(matchedRectangle, tap);
        FeedbackObservables.rectangleAndPointObservable.setValue(scriptInfo.getCurrentEmulatorId(), rectangleAndPoint);
    }

    public void tapWithMenuAction(int x, int y, int rectHeight, int rectWidth, List<Color> colors, String stringToFind) {
        logger.devLog("Tapping at: " + x + "," + y);
        adbHandler.executeADBCommand(String.format("shell input tap %d %d", x, y), scriptInfo.getCurrentEmulatorId());
        int duration = Constants.getRandomDuration();
        conditionAPI.sleep(duration);

        // Define the ROI (Region of Interest) for finding the menu
        int roiX = x - rectWidth / 2;  // Center the menu horizontally based on the long press location
        int roiY = y + 26;  // Menu starts 26 pixels below the long press point
        Rectangle roi = new Rectangle(roiX, roiY, rectWidth, rectHeight);
        FeedbackObservables.rectangleObservable.setValue(scriptInfo.getCurrentEmulatorId(), roi);

        // Find the menu options
        Rectangle menuOption = digitReader.findString(10, roi, colors,null, stringToFind, scriptInfo.getCurrentEmulatorId());

        // Tap the option if found
        if (menuOption != null) {
            logger.devLog("Tapping " + stringToFind + " menu option");
            tap(menuOption);
        }
    }

    public void tapWithMenuAction(int x, int y, int rectHeight, int rectWidth, Map<String, int[][]> letterPatterns, List<Color> colors, String stringToFind) {
        logger.devLog("Tapping at: " + x + "," + y);
        int duration = Constants.getRandomDuration();
        adbHandler.executeADBCommand(String.format("shell input tap %d %d", x, y), scriptInfo.getCurrentEmulatorId());
        conditionAPI.sleep(duration);

        // Define the ROI (Region of Interest) for finding the menu
        int roiX = x - rectWidth / 2;  // Center the menu horizontally based on the long press location
        int roiY = y + 20;  // Menu starts 26 pixels below the long press point
        Rectangle roi = new Rectangle(roiX, roiY, rectWidth, rectHeight);

        // Find the menu options
        Rectangle menuOption = digitReader.findString(10, roi, colors, letterPatterns, stringToFind, scriptInfo.getCurrentEmulatorId());

        // Tap the option if found
        if (menuOption != null) {
            logger.devLog("Tapping " + stringToFind + " menu option");
            tap(menuOption);
        }
    }

    public void tapWithMenuAction(Rectangle clickRect, int rectHeight, int rectWidth, List<Color> colors, String stringToFind) {
        // Calculate our click coordinates
        Point tap = calculateTruncatedGaussianPoint(clickRect);

        // Long press part
        logger.devLog("Tapping at: " + tap.x + "," + tap.y);
        int duration = Constants.getRandomDuration();
        adbHandler.executeADBCommand(String.format("shell input tap %d %d", tap.x, tap.y), scriptInfo.getCurrentEmulatorId());
        conditionAPI.sleep(duration);

        // Define the ROI (Region of Interest) for finding the menu
        int roiX = tap.x - rectWidth / 2;  // Center the menu horizontally based on the long press location
        int roiY = tap.y + 26;  // Menu starts 26 pixels below the long press point
        Rectangle roi = new Rectangle(roiX, roiY, rectWidth, rectHeight);

        // Find the menu options
        Rectangle menuOption = digitReader.findString(10, roi, colors,null, stringToFind, scriptInfo.getCurrentEmulatorId());

        // Tap the option if found
        if (menuOption != null) {
            logger.devLog("Tapping " + stringToFind + " menu option");
            tap(menuOption);
        }
    }

    public void tapWithMenuAction(Rectangle clickRect, int rectHeight, int rectWidth, Map<String, int[][]> letterPatterns, List<Color> colors, String stringToFind) {
        // Calculate our click coordinates
        Point tap = calculateTruncatedGaussianPoint(clickRect);

        // Long press part
        logger.devLog("Tapping at: " + tap.x + "," + tap.y);
        int duration = Constants.getRandomDuration();
        adbHandler.executeADBCommand(String.format("shell input tap %d %d", tap.x, tap.y), scriptInfo.getCurrentEmulatorId());
        conditionAPI.sleep(duration);

        // Define the ROI (Region of Interest) for finding the menu
        int roiX = tap.x - rectWidth / 2;  // Center the menu horizontally based on the long press location
        int roiY = tap.y + 20;  // Menu starts 26 pixels below the long press point
        Rectangle roi = new Rectangle(roiX, roiY, rectWidth, rectHeight);

        // Find the menu options
        Rectangle menuOption = digitReader.findString(10, roi, colors, letterPatterns, stringToFind, scriptInfo.getCurrentEmulatorId());

        // Tap the option if found
        if (menuOption != null) {
            logger.devLog("Tapping " + stringToFind + " menu option");
            tap(menuOption);
        }
    }

    public void findAndTapMenuOption(Rectangle searchArea, List<Color> colors, Map<String, int[][]> letterPatterns, String stringToFind) {
        // Find the menu options
        Rectangle menuOption = digitReader.findString(10, searchArea, colors, letterPatterns, stringToFind, scriptInfo.getCurrentEmulatorId());

        // Tap the option if found
        if (menuOption != null) {
            logger.devLog("Tapping " + stringToFind + " menu option");
            tap(menuOption);
        } else {
            logger.devLog("Couldn't find menu option: " + stringToFind);
        }
    }

    public void findAndTapMenuOption(Rectangle searchArea, List<Color> colors, String stringToFind) {
        // Find the menu options
        Rectangle menuOption = digitReader.findString(10, searchArea, colors,null, stringToFind, scriptInfo.getCurrentEmulatorId());

        // Tap the option if found
        if (menuOption != null) {
            logger.devLog("Tapping " + stringToFind + " menu option");
            tap(menuOption);
        } else {
            logger.devLog("Couldn't find menu option: " + stringToFind);
        }
    }

    public void longPress(int x, int y) {
        if (x == 0 && y == 0) {
            return;
        }

        logger.devLog("Longpressing at: " + x + "," + y);
        int duration = Constants.getRandomDuration();
        adbHandler.executeADBCommand(String.format("shell input swipe %d %d %d %d %d", x, y, x, y, duration), scriptInfo.getCurrentEmulatorId());
        FeedbackObservables.pointObservable.setValue(scriptInfo.getCurrentEmulatorId(), new Point(x, y));
        conditionAPI.sleep(duration);
    }

    public void longPress(int x, int y, String device) {
        logger.devLog("Longpressing at: " + x + "," + y);
        int duration = Constants.getRandomDuration();
        adbHandler.executeADBCommand(String.format("shell input swipe %d %d %d %d %d", x, y, x, y, duration), device);
        FeedbackObservables.pointObservable.setValue(device, new Point(x, y));
        conditionAPI.sleep(duration);
    }

    public void longPressWithMenuAction(int x, int y, int rectHeight, int rectWidth, List<Color> colors, String stringToFind) {
        // Long press part
        logger.devLog("Longpressing at: " + x + "," + y);
        int duration = Constants.getRandomDuration();
        adbHandler.executeADBCommand(String.format("shell input swipe %d %d %d %d %d", x, y, x, y, duration), scriptInfo.getCurrentEmulatorId());
        FeedbackObservables.pointObservable.setValue(scriptInfo.getCurrentEmulatorId(), new Point(x, y));
        conditionAPI.sleep(duration);

        // Define the ROI (Region of Interest) for finding the menu
        int roiX = x - rectWidth / 2;  // Center the menu horizontally based on the long press location
        int roiY = y + 26;  // Menu starts 26 pixels below the long press point
        Rectangle roi = new Rectangle(roiX, roiY, rectWidth, rectHeight);
        FeedbackObservables.rectangleObservable.setValue(scriptInfo.getCurrentEmulatorId(), roi);

        // Find the menu options
        Rectangle menuOption = digitReader.findString(10, roi, colors,null, stringToFind, scriptInfo.getCurrentEmulatorId());

        // Tap the option if found
        if (menuOption != null) {
            logger.devLog("Tapping " + stringToFind + " menu option");
            tap(menuOption);
        }
    }

    public void longPressWithMenuAction(int x, int y, int rectHeight, int rectWidth, Map<String, int[][]> letterPatterns, List<Color> colors, String stringToFind) {
        // Long press part
        logger.devLog("Longpressing at: " + x + "," + y);
        int duration = Constants.getRandomDuration();
        adbHandler.executeADBCommand(String.format("shell input swipe %d %d %d %d %d", x, y, x, y, duration), scriptInfo.getCurrentEmulatorId());
        FeedbackObservables.pointObservable.setValue(scriptInfo.getCurrentEmulatorId(), new Point(x, y));
        conditionAPI.sleep(duration);

        // Define the ROI (Region of Interest) for finding the menu
        int roiX = x - rectWidth / 2;  // Center the menu horizontally based on the long press location
        int roiY = y + 20;  // Menu starts 26 pixels below the long press point
        Rectangle roi = new Rectangle(roiX, roiY, rectWidth, rectHeight);
        FeedbackObservables.rectangleObservable.setValue(scriptInfo.getCurrentEmulatorId(), roi);

        // Find the menu options
        Rectangle menuOption = digitReader.findString(10, roi, colors, letterPatterns, stringToFind, scriptInfo.getCurrentEmulatorId());

        // Tap the option if found
        if (menuOption != null) {
            logger.devLog("Tapping " + stringToFind + " menu option");
            tap(menuOption);
        }
    }

    public void longPressWithMenuAction(Rectangle clickRect, int rectHeight, int rectWidth, List<Color> colors, String stringToFind) {
        // Calculate our click coordinates
        Point tap = calculateTruncatedGaussianPoint(clickRect);

        // Long press part
        logger.devLog("Longpressing at: " + tap.x + "," + tap.y);
        int duration = Constants.getRandomDuration();
        adbHandler.executeADBCommand(String.format("shell input swipe %d %d %d %d %d", tap.x, tap.y, tap.x, tap.y, duration), scriptInfo.getCurrentEmulatorId());
        FeedbackObservables.pointObservable.setValue(scriptInfo.getCurrentEmulatorId(), tap);
        conditionAPI.sleep(duration);

        // Define the ROI (Region of Interest) for finding the menu
        int roiX = tap.x - rectWidth / 2;  // Center the menu horizontally based on the long press location
        int roiY = tap.y + 26;  // Menu starts 26 pixels below the long press point
        Rectangle roi = new Rectangle(roiX, roiY, rectWidth, rectHeight);
        FeedbackObservables.rectangleObservable.setValue(scriptInfo.getCurrentEmulatorId(), roi);

        // Find the menu options
        Rectangle menuOption = digitReader.findString(10, roi, colors,null, stringToFind, scriptInfo.getCurrentEmulatorId());

        // Tap the option if found
        if (menuOption != null) {
            logger.devLog("Tapping " + stringToFind + " menu option");
            tap(menuOption);
        }
    }

    public void longPressWithMenuAction(Rectangle clickRect, Rectangle searchArea, List<Color> colors, String stringToFind) {
        // Calculate our click coordinates
        Point tap = calculateTruncatedGaussianPoint(clickRect);

        // Long press part
        logger.devLog("Longpressing at: " + tap.x + "," + tap.y);
        int duration = Constants.getRandomDuration();
        adbHandler.executeADBCommand(String.format("shell input swipe %d %d %d %d %d", tap.x, tap.y, tap.x, tap.y, duration), scriptInfo.getCurrentEmulatorId());
        FeedbackObservables.pointObservable.setValue(scriptInfo.getCurrentEmulatorId(), tap);
        conditionAPI.sleep(duration);

        // Find the menu options
        Rectangle menuOption = digitReader.findString(10, searchArea, colors,null, stringToFind, scriptInfo.getCurrentEmulatorId());

        // Tap the option if found
        if (menuOption != null) {
            logger.devLog("Tapping " + stringToFind + " menu option");
            tap(menuOption);
        }
    }

    public void longPressWithMenuAction(Rectangle clickRect, int rectHeight, int rectWidth, Map<String, int[][]> letterPatterns, List<Color> colors, String stringToFind) {
        // Calculate our click coordinates
        Point tap = calculateTruncatedGaussianPoint(clickRect);

        // Long press part
        logger.devLog("Longpressing at: " + tap.x + "," + tap.y);
        int duration = Constants.getRandomDuration();
        adbHandler.executeADBCommand(String.format("shell input swipe %d %d %d %d %d", tap.x, tap.y, tap.x, tap.y, duration), scriptInfo.getCurrentEmulatorId());
        FeedbackObservables.pointObservable.setValue(scriptInfo.getCurrentEmulatorId(), tap);
        conditionAPI.sleep(duration);

        // Define the ROI (Region of Interest) for finding the menu
        int roiX = tap.x - rectWidth / 2;  // Center the menu horizontally based on the long press location
        int roiY = tap.y + 20;  // Menu starts 26 pixels below the long press point
        Rectangle roi = new Rectangle(roiX, roiY, rectWidth, rectHeight);
        FeedbackObservables.rectangleObservable.setValue(scriptInfo.getCurrentEmulatorId(), roi);

        // Find the menu options
        Rectangle menuOption = digitReader.findString(10, roi, colors, letterPatterns, stringToFind, scriptInfo.getCurrentEmulatorId());

        // Tap the option if found
        if (menuOption != null) {
            logger.devLog("Tapping " + stringToFind + " menu option");
            tap(menuOption);
        }
    }

    public void longPress(List<Point> points) {
        // Check if the list of points is null or empty
        if (points == null || points.isEmpty()) {
            System.err.println("List of points provided is null or empty.");
            return;
        }

        Point selectedPoint = getWeightedRandomTapFromPoints(points);

        // Log the tapping coordinates
        logger.devLog("long pressing at: " + selectedPoint.x + "," + selectedPoint.y);

        // Execute the ADB command to tap at the selected point
        longPress(selectedPoint.x, selectedPoint.y);
    }

    public void longPress(Color targetColor, Rectangle boundingArea, int tolerance) {
        Point upperLeft = new Point(boundingArea.x, boundingArea.y);
        Point lowerRight = new Point(boundingArea.x + boundingArea.width, boundingArea.y + boundingArea.height);

        List<Point> points = colorFinder.findColorAtPosition(scriptInfo.getCurrentEmulatorId(), targetColor, upperLeft, lowerRight, tolerance);
        if (points.isEmpty()) {
            return;
        }

        // Favor points closer to the center using a simple weighted random choice
        Point selectedPoint = getWeightedRandomTapFromPoints(points);

        // Long press the selected point
        logger.devLog("Longpressing at: " + selectedPoint.x + "," + selectedPoint.y);
        longPress(selectedPoint.x, selectedPoint.y);
    }

    public void longPress(Rectangle rectangle) {
        if (rectangle == null) {
            return;
        }

        Point tap = calculateTruncatedGaussianPoint(rectangle);

        // Check if getCurrentEmulatorId() returns a non-null value
        String emulatorId = scriptInfo.getCurrentEmulatorId();
        if (emulatorId == null) {
            System.err.println("Current emulator ID is null.");
            return;
        }
        logger.devLog("Longpressing at: " + tap.x + "," + tap.y);
        int duration = Constants.getRandomDuration();
        adbHandler.executeADBCommand(String.format("shell input swipe %d %d %d %d %d", tap.x, tap.y, tap.x, tap.y, duration), emulatorId);
        FeedbackObservables.pointObservable.setValue(scriptInfo.getCurrentEmulatorId(), tap);
        conditionAPI.sleep(duration);
    }

    public void longPress(Rectangle rectangle, String device) {
        Point tap = calculateTruncatedGaussianPoint(rectangle);
        logger.devLog("Longpressing at: " + tap.x + "," + tap.y);
        int duration = Constants.getRandomDuration();
        adbHandler.executeADBCommand(String.format("shell input swipe %d %d %d %d %d", tap.x, tap.y, tap.x, tap.y, duration), device);
        FeedbackObservables.pointObservable.setValue(device, tap);
        conditionAPI.sleep(duration
        );
    }

    public void sendKeystroke(String keystroke) {
        // Normalize keystroke to uppercase
        keystroke = keystroke.toUpperCase();

        // Check if the keystroke is a digit, letter, "ENTER", "SPACE", "(" or ")"
        if (keystroke.matches("[0-9A-Z]") || keystroke.equals("ENTER") || keystroke.equals("SPACE")) {
            keystroke = "KEYCODE_" + keystroke;
        } else if (keystroke.equals("(")) {
            keystroke = "KEYCODE_NUMPAD_LEFT_PAREN";
        } else if (keystroke.equals(")")) {
            keystroke = "KEYCODE_NUMPAD_RIGHT_PAREN";
        }

        logger.devLog("Sending keystroke: " + keystroke);
        adbHandler.executeADBCommand(String.format("shell input keyevent %s", keystroke), scriptInfo.getCurrentEmulatorId());
    }

    public void restartApp() {
        adbHandler.executeADBCommand(String.format("shell am force-stop %s", Constants.OSRS_APP_NAME), scriptInfo.getCurrentEmulatorId());
        try {
            // Wait for 5 seconds before starting the app again
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        adbHandler.executeADBCommand(String.format("shell monkey -p %s -c android.intent.category.LAUNCHER 1", Constants.OSRS_APP_NAME), scriptInfo.getCurrentEmulatorId());
    }

    public void drag(int startX, int startY, int endX, int endY, int duration) {
        // Randomize start x, start y, and duration
        int randomizedStartX = startX + random.nextInt(11) - 5; // Randomize startX by ±5 pixels
        int randomizedStartY = startY + random.nextInt(11) - 5; // Randomize startY by ±5 pixels
        int randomizedDuration = (int) (duration * (0.9 + random.nextDouble() * 0.2)); // Randomize duration by ±10%

        // Randomize end x and end y by ±5 pixels
        int randomizedEndX = endX + random.nextInt(11) - 5;
        int randomizedEndY = endY + random.nextInt(11) - 5;

        logger.devLog("Moving the camera up with randomized start and end positions and duration.");
        adbHandler.executeADBCommand(String.format(
                "shell input swipe %d %d %d %d %d",
                randomizedStartX, randomizedStartY, randomizedEndX, randomizedEndY, randomizedDuration
        ), scriptInfo.getCurrentEmulatorId());

        // Submit the start and end points to the observable
        String emulatorId = scriptInfo.getCurrentEmulatorId();
        PointAndPoint dragData = new PointAndPoint(
                new Point(randomizedStartX, randomizedStartY),
                new Point(randomizedEndX, randomizedEndY)
        );
        FeedbackObservables.pointAndPointObservable.setValue(emulatorId, dragData);
    }

    public void drag(Rectangle startRect, Rectangle endRect, int duration) {
        // Pick random start x and y within the start rectangle
        int randomStartX = startRect.x + random.nextInt(startRect.width);
        int randomStartY = startRect.y + random.nextInt(startRect.height);

        // Pick random end x and y within the end rectangle
        int randomEndX = endRect.x + random.nextInt(endRect.width);
        int randomEndY = endRect.y + random.nextInt(endRect.height);

        // Randomize duration by ±10%
        int randomizedDuration = (int) (duration * (0.9 + random.nextDouble() * 0.2));

        logger.devLog("Moving the camera up from start rectangle to end rectangle with randomized duration.");
        adbHandler.executeADBCommand(String.format("shell input swipe %d %d %d %d %d", randomStartX, randomStartY, randomEndX, randomEndY, randomizedDuration), scriptInfo.getCurrentEmulatorId());
        FeedbackObservables.pointAndPointObservable.setValue(scriptInfo.getCurrentEmulatorId(), new PointAndPoint(new Point(randomStartX, randomStartY), new Point(randomEndX, randomEndY)));
    }

    public void drag(java.awt.Point startPoint, java.awt.Point endPoint, int duration) {
        // Randomize start point's x and y by ±5 pixels
        int randomizedStartX = startPoint.x + random.nextInt(11) - 5;
        int randomizedStartY = startPoint.y + random.nextInt(11) - 5;

        // Randomize duration by ±10%
        int randomizedDuration = (int) (duration * (0.9 + random.nextDouble() * 0.2));

        // Randomize end point's x and y by ±5 pixels
        int randomizedEndX = endPoint.x + random.nextInt(11) - 5;
        int randomizedEndY = endPoint.y + random.nextInt(11) - 5;

        logger.devLog("Moving the camera up from start to end point with randomized positions and duration.");
        adbHandler.executeADBCommand(String.format("shell input swipe %d %d %d %d %d", randomizedStartX, randomizedStartY, randomizedEndX, randomizedEndY, randomizedDuration), scriptInfo.getCurrentEmulatorId());
        FeedbackObservables.pointAndPointObservable.setValue(scriptInfo.getCurrentEmulatorId(), new PointAndPoint(new Point(randomizedStartX, randomizedStartY), new Point(randomizedEndX, randomizedEndY)));
    }

    public void moveCameraUp() {
        // Generate random x and y coordinates within the specified ranges
        int randomX = random.nextInt(461) + 103; // Generates a random number between 103 and 563
        int randomY = random.nextInt(216) + 160; // Generates a random number between 160 and 375
        int randomisationY = random.nextInt(30);
        int endY = randomY + 150 + randomisationY;

        // Generate a random duration between 1200 and 2000 ms
        int randomDuration = random.nextInt(801) + 1200;

        logger.devLog("Moving the camera up.");
        adbHandler.executeADBCommand(String.format("shell input swipe %d %d %d %d %d", randomX, randomY, randomX, endY, randomDuration), scriptInfo.getCurrentEmulatorId());
        FeedbackObservables.pointAndPointObservable.setValue(scriptInfo.getCurrentEmulatorId(), new PointAndPoint(new Point(randomX, randomY), new Point(randomX, endY)));
    }

    public boolean disableBreakHandler() {
        // Get the existing handler for a specific emulator
        BreakHandlerService existingHandler = breakHandlerService.getExistingHandler(scriptInfo.getCurrentEmulatorId());

        if (existingHandler != null && existingHandler.isEnabled()) {
            existingHandler.disable();
            return true;
        } else {
            return false;
        }
    }

    public boolean disableSleepHandler() {
        // Get the existing handler for a specific emulator
        SleepHandlerService existingHandler = sleepHandlerService.getExistingHandler(scriptInfo.getCurrentEmulatorId());

        if (existingHandler != null && existingHandler.isEnabled()) {
            existingHandler.disable();
            return true;
        } else {
            return false;
        }
    }

    public boolean enableBreakHandler() {
        // Get the existing handler for a specific emulator
        BreakHandlerService existingHandler = breakHandlerService.getExistingHandler(scriptInfo.getCurrentEmulatorId());

        if (existingHandler != null && !existingHandler.isEnabled()) {
            existingHandler.enable();
            return true;
        } else {
            return false;
        }
    }

    public boolean enableSleepHandler() {
        // Get the existing handler for a specific emulator
        SleepHandlerService existingHandler = sleepHandlerService.getExistingHandler(scriptInfo.getCurrentEmulatorId());

        if (existingHandler != null && !existingHandler.isEnabled()) {
            existingHandler.enable();
            return true;
        } else {
            return false;
        }
    }

    public void postponeBreaksAndSleeps() {
        breakHandlerService.getExistingHandler(scriptInfo.getCurrentEmulatorId()).postponeBreak();
        sleepHandlerService.getExistingHandler(scriptInfo.getCurrentEmulatorId()).postponeSleeps();
    }

    public void resumeBreaksAndSleeps() {
        breakHandlerService.getExistingHandler(scriptInfo.getCurrentEmulatorId()).allowBreaks();
        sleepHandlerService.getExistingHandler(scriptInfo.getCurrentEmulatorId()).allowSleeps();
    }

    public void postponeBreaks() {
        breakHandlerService.getExistingHandler(scriptInfo.getCurrentEmulatorId()).postponeBreak();
    }

    public void resumeBreaks() {
        breakHandlerService.getExistingHandler(scriptInfo.getCurrentEmulatorId()).allowBreaks();
    }

    public void startBreak(Long timeMilis) {
        breakHandlerService.forceBreak(scriptInfo.getCurrentEmulatorId(), timeMilis);
    }

    public boolean isTimeForBreak() {
        BreakHandlerService service = breakHandlerService.getExistingHandler(scriptInfo.getCurrentEmulatorId());
        if (service != null) {
            return service.isTimeForBreak() || service.postponedButTimeForBreak();
        } else {
            return false;
        }
    }

    public void postponeSleeps() {
        sleepHandlerService.getExistingHandler(scriptInfo.getCurrentEmulatorId()).postponeSleeps();
    }

    public void resumeSleeps() {
        sleepHandlerService.getExistingHandler(scriptInfo.getCurrentEmulatorId()).allowSleeps();
    }

    public boolean isTimeForSleep() {
        SleepHandlerService service = sleepHandlerService.getExistingHandler(scriptInfo.getCurrentEmulatorId());
        if (service != null) {
            return service.isTimeForSleep() || service.postponedButTimeForBreak();
        } else {
            return false;
        }
    }

    public boolean disableAFKHandler() {
        AFKHandlerService existingHandler = afkHandlerService.getExistingHandler(scriptInfo.getCurrentEmulatorId());

        if (existingHandler != null && existingHandler.isEnabled()) {
            existingHandler.disable();
            return true;
        } else {
            return false;
        }
    }

    public boolean enableAFKHandler() {
        // Get the existing handler for a specific emulator
        AFKHandlerService existingHandler = afkHandlerService.getExistingHandler(scriptInfo.getCurrentEmulatorId());

        if (existingHandler != null && !existingHandler.isEnabled()) {
            existingHandler.enable();
            return true;
        } else {
            return false;
        }
    }

    public boolean isColorInRect(Color targetColor, Rectangle regionToSearch, int tolerance) {
        return colorFinder.isColorInRect(scriptInfo.getCurrentEmulatorId(), targetColor, regionToSearch, tolerance);
    }

    public boolean isAnyColorInRect(List<Color> targetColor, Rectangle regionToSearch, int tolerance) {
        return colorFinder.isAnyColorInRect(scriptInfo.getCurrentEmulatorId(), targetColor, regionToSearch, tolerance);
    }

    public boolean isColorAtPoint(Color targetColor, Point pointToSearch, int tolerance) {
        return colorFinder.isColorAtPoint(scriptInfo.getCurrentEmulatorId(), targetColor, pointToSearch, tolerance);
    }

    public List<Rectangle> getObjectsFromColor(List<Color> colors, int tolerance) {
        return colorFinder.processColorClusters(scriptInfo.getCurrentEmulatorId(), colors, tolerance);
    }

    public List<Rectangle> getObjectsFromColor(List<Color> colors, int tolerance, List<Color> exclusionColors, int exclusionTolerance) {
        return colorFinder.processColorClusters(scriptInfo.getCurrentEmulatorId(), colors, tolerance, exclusionColors, exclusionTolerance);
    }

    public List<Rectangle> getObjectsFromColorsInRect(List<Color> colors, Rectangle searchRect, int tolerance) {
        return colorFinder.processColorClustersInRect(scriptInfo.getCurrentEmulatorId(), colors, tolerance, searchRect, 10.0, 20);
    }

    public List<Rectangle> getObjectsFromColorsInRect(List<Color> colors, Rectangle searchRect, int tolerance, List<Color> exclusionColors, int exclusionTolerance) {
        return colorFinder.processColorClustersInRect(scriptInfo.getCurrentEmulatorId(), colors, tolerance, searchRect, exclusionColors, exclusionTolerance, 10.0, 20);
    }

    public List<Point> getPointsFromColors(List<Color> colors, int tolerance) {
        return colorFinder.processColorPoints(scriptInfo.getCurrentEmulatorId(), colors, tolerance);
    }

    public List<Point> getPointsFromColors(List<Color> colors, int tolerance, List<Color> exclusionColors, int exclusionTolerance) {
        return colorFinder.processColorPoints(scriptInfo.getCurrentEmulatorId(), colors, tolerance, exclusionColors, exclusionTolerance);
    }

    public List<Point> getPointsFromColorsInRect(List<Color> colors, Rectangle searchRect, int tolerance) {
        return colorFinder.processColorPointsInRect(scriptInfo.getCurrentEmulatorId(), colors, searchRect, tolerance);
    }

    public List<Point> getPointsFromColorsInRect(List<Color> colors, Rectangle searchRect, int tolerance, List<Color> exclusionColors, int exclusionTolerance) {
        return colorFinder.processColorPointsInRect(scriptInfo.getCurrentEmulatorId(), colors, searchRect, tolerance, exclusionColors, exclusionTolerance);
    }

    public Point getWeightedRandomTapFromPoints(List<Point> points) {
        // Calculate the bounding rectangle of all points
        Rectangle bounds = calculateBounds(points);

        // Weight points to give some areas more probability (e.g., around center or certain areas)
        List<Point> weightedPoints = applyPointWeights(points, bounds);

        // Return a random point based on the weighted distribution
        return weightedPoints.get(random.nextInt(weightedPoints.size()));
    }

    private List<Point> applyPointWeights(List<Point> points, Rectangle bounds) {
        List<Point> weightedPoints = new ArrayList<>();

        int centerX = bounds.x + bounds.width / 2;
        int centerY = bounds.y + bounds.height / 2;

        // Apply weight based on proximity to the center
        for (Point p : points) {
            double distance = Math.sqrt(Math.pow(p.x - centerX, 2) + Math.pow(p.y - centerY, 2));

            // Example: Add more weight to points closer to the center
            int weight = Math.max(1, (int) (100 - distance / 10)); // Adjust the factor to fine-tune
            for (int i = 0; i < weight; i++) {
                weightedPoints.add(p);
            }
        }

        return weightedPoints;
    }

    private boolean isWithinBounds(Rectangle rect, int x, int y) {
        return x >= rect.x && x < rect.x + rect.width
                && y >= rect.y && y < rect.y + rect.height;
    }

    private Point calculateTruncatedGaussianPoint(Rectangle rect) {
        int centerX = rect.x + rect.width / 2;
        int centerY = rect.y + rect.height / 2;

        double stdDevX = rect.width / 4.0;
        double stdDevY = rect.height / 4.0;

        int randomX;
        int randomY;

        while (true) {
            double candidateX = centerX + random.nextGaussian() * stdDevX;
            double candidateY = centerY + random.nextGaussian() * stdDevY;

            if (!isWithinBounds(rect, (int) candidateX, (int) candidateY)) {
                candidateX = Math.max(rect.x, Math.min(candidateX, rect.x + rect.width));
                candidateY = Math.max(rect.y, Math.min(candidateY, rect.y + rect.height));
            }

            candidateX += random.nextInt(5) - 2; // +/- 2 pixels
            candidateY += random.nextInt(5) - 2; // +/- 2 pixels

            randomX = (int) Math.round(candidateX);
            randomY = (int) Math.round(candidateY);

            if (isWithinBounds(rect, randomX, randomY)) {
                break;
            }
        }

        return new Point(randomX, randomY);
    }

    private Rectangle calculateBounds(List<Point> points) {
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;

        for (Point p : points) {
            if (p.x < minX) minX = p.x;
            if (p.x > maxX) maxX = p.x;
            if (p.y < minY) minY = p.y;
            if (p.y > maxY) maxY = p.y;
        }

        return new Rectangle(minX, minY, maxX - minX, maxY - minY);
    }
}