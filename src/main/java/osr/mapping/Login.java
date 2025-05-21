package osr.mapping;

import helpers.GetGameView;
import helpers.Logger;
import helpers.OCR.cfOCR;
import helpers.adb.ADBHandler;
import helpers.Color.ColorFinder;
import helpers.Color.TemplateMatcher;
import helpers.Color.utils.ColorRectanglePair;
import helpers.services.BreakHandlerService;
import helpers.visualFeedback.FeedbackObservables;
import osr.mapping.utils.LoginMessages;
import osr.mapping.utils.MinimapProjections;
import osr.utils.ImageUtils;
import scripts.APIClasses.ClientAPI;
import scripts.APIClasses.ConditionAPI;
import utils.Constants;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static osr.mapping.utils.MinimapProjections.MINIMAP_ROTATION_ANGLE;
import static utils.Constants.CURRENT_ZOOM_LEVEL;

public class Login {
    public final static Rectangle TAP_HERE_TO_PLAY_CHECK_RECT = new Rectangle(379, 310, 165, 55);
    private final static Color popOutPanelColor = Color.decode("#d37a28");
    // Pre-defined static rectangles
    private final static Rectangle XP_COUNTER_RECT = new Rectangle(521, 5, 29, 26);
    private final static Rectangle XP_BUTTON_RECT = new Rectangle(665, 11, 18, 14);
    private final static Rectangle HOTKEYS_BUTTON_RECT = new Rectangle(18, 420, 47, 26);
    private final static Rectangle COMPASS_TAP_RECT = new Rectangle(703, 11, 24, 28);
    private final static Rectangle POP_OUT_PANEL_CHECK = new Rectangle(42, 468, 4, 4);
    private final static Rectangle POP_OUT_PANEL_TAP = new Rectangle(20, 461, 44, 30);
    private final static Rectangle ZOOM_CHECK_RECT = new Rectangle(706, 337, 3, 3);
    private final static Rectangle BRIGHTNESS_CHECK_RECT = new Rectangle(705, 298, 3, 4);
    private final static Rectangle SIGNOUT_NO_RECT = new Rectangle(466, 280, 122, 22);
    private final static Rectangle DISCONNECT_OK_RECT = new Rectangle(381, 296, 130, 30);
    private final static Rectangle BUSY_TRY_AGAIN_RECT = new Rectangle(392, 301, 110, 21);
    private final static Rectangle regionToOCR = new Rectangle(282, 184, 332, 95);
    private static final List<Color> blackBorderColor = List.of(
            Color.decode("#0e0e0c"),
            Color.decode("#1c1c19")
    );
    private static final List<java.awt.Color> LoginTextColors = Arrays.asList(java.awt.Color.decode("#ffffff"), java.awt.Color.decode("#ffff00"));
    private static final List<ColorRectanglePair> colorRectPairs = List.of(
            new ColorRectanglePair(blackBorderColor, new Rectangle(83, 155, 21, 16)),
            new ColorRectanglePair(blackBorderColor, new Rectangle(566, 476, 18, 18))
    );
    private static BufferedImage LOGOUT_BUTTON;
    private static BufferedImage WORLD_BUSY_IMAGE;
    private static BufferedImage PLAY_NOW;
    private static BufferedImage TAP_TO_PLAY;
    private static BufferedImage XP_SETUP;
    private final MinimapProjections minimapProjections;
    private final Logger logger;
    private final ConditionAPI conditionAPI;
    private final GetGameView getGameView;
    private final GameTabs gameTabs;
    private final ClientAPI clientAPI;
    private final ADBHandler adbHandler;
    private final ImageUtils imageUtils;
    private final ColorFinder colorFinder;
    private final Chatbox chatbox;
    private final BreakHandlerService breakHandlerService;
    private final TemplateMatcher templateMatcher;
    private final Random random = new Random();
    private final List<Color> totalXPColors = List.of(
            Color.decode("#b33a34"),
            Color.decode("#922329"),
            Color.decode("#508e56"),
            Color.decode("#195c1e"),
            Color.decode("#2e3f9b"),
            Color.decode("#212846"),
            Color.decode("#9c322d"),
            Color.decode("#212846")
    );
    private final List<Color> hotkeyButtonColor = List.of(
            Color.decode("#d37a28")
    );
    private final List<Color> brightnessCheckColors = Arrays.asList(
            Color.decode("#264d98"),
            Color.decode("#3a9239"),
            Color.decode("#093a9b"),
            Color.decode("#87c686"),
            Color.decode("#a4daa3"),
            Color.decode("#a7b9dc")
    );
    private final List<Color> tapHereToPlayColors = Arrays.asList(
            Color.decode("#5D0902"),
            Color.decode("#681B0B"),
            Color.decode("#7F2C1A"),
            Color.decode("#8D3F32"),
            Color.decode("#4E0200")
    );
    private final List<Color> xpDropsEnabledColor = List.of(
            Color.decode("#efe4b0")
    );

    public Login(TemplateMatcher templateMatcher, BreakHandlerService breakHandlerService, MinimapProjections minimapProjections, Logger logger, ConditionAPI conditionAPI, GetGameView getGameView, GameTabs gameTabs, ClientAPI clientAPI, ADBHandler adbHandler, ImageUtils imageUtils, ColorFinder colorFinder, Chatbox chatbox) {
        this.minimapProjections = minimapProjections;
        this.logger = logger;
        this.getGameView = getGameView;
        this.gameTabs = gameTabs;
        this.conditionAPI = conditionAPI;
        this.clientAPI = clientAPI;
        this.adbHandler = adbHandler;
        this.imageUtils = imageUtils;
        this.colorFinder = colorFinder;
        this.chatbox = chatbox;
        this.breakHandlerService = breakHandlerService;
        this.templateMatcher = templateMatcher;

        initializeLoginImages();
    }

    public static int levenshteinDistance(String a, String b) {
        if (a == null && b == null) return 0;
        if (a == null) return b.length();
        if (b == null) return a.length();

        a = a.toLowerCase();
        b = b.toLowerCase();
        int[] costs = new int[b.length() + 1];
        for (int j = 0; j < costs.length; j++)
            costs[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            costs[0] = i;
            int nw = i - 1;
            for (int j = 1; j <= b.length(); j++) {
                int cj = Math.min(1 + Math.min(costs[j], costs[j - 1]), a.charAt(i - 1) == b.charAt(j - 1) ? nw : nw + 1);
                nw = costs[j];
                costs[j] = cj;
            }
        }
        return costs[b.length()];
    }

    public void login(String device) {
        logger.devLog("Running login sequence");
        int maxAttempts = 5;
        int attempts = 0;

        if (isLoggedIn(device)) {
            logger.debugLog("We're already logged in!", device);
            return;
        }

        while (!isLoggedIn(device) && attempts < maxAttempts) {
            attempts++;
            Rectangle playNow = findPlayNowOption(device);

            if (playNow != null) {
                tapAndWaitForPlay(device, playNow);
            } else {
                boolean resetAttempts = handleOtherMessages(device);
                if (resetAttempts) {
                    attempts = 0;  // Reset the attempts to 0 if needed
                }
            }
        }
    }

    private void tapAndWaitForPlay(String device, Rectangle playNow) {
        clientAPI.tap(playNow);
        conditionAPI.wait(() -> isTapToPlayVisibleCF(device) || isLoggedIn(device), 100, 500);
        conditionAPI.sleep(150);

        if (isTapToPlayVisibleCF(device)) {
            logger.debugLog("Found tap to play using CF", device);
            clientAPI.tap(TAP_HERE_TO_PLAY_CHECK_RECT, device);
            conditionAPI.wait(() -> isLoggedIn(device), 100, 1000);
        } else if (findTapToPlayOption(device) != null) {
            logger.debugLog("Found tap to play using OCV", device);
            clientAPI.tap(findPlayNowOption(device));
            conditionAPI.wait(() -> isLoggedIn(device), 100, 1000);
        } else {
            logger.debugLog("TapToPlay not found using both CF and OCV.", device);
        }
    }

    private boolean handleOtherMessages(String device) {
        Rectangle worldBusyRect = templateMatcher.match(device, WORLD_BUSY_IMAGE, 10);
        if (worldBusyRect != null) {
            logger.debugLog("World is busy, tapping try again. ", device);
            clientAPI.tap(BUSY_TRY_AGAIN_RECT);
            conditionAPI.sleep(1500);
        } else {
            return performLoginOCR(device); // Return true if attempts need to be reset
        }

        return false;  // No reset needed
    }

    private boolean performLoginOCR(String device) {
        String closestMessage = readLoginScreen(device);

        if (closestMessage == null) {
            return false;
        }

        switch (findClosestLoginMessage(closestMessage)) {
            case DISCONNECTED:
                logger.debugLog("Disconnected. Dismissing disconnect message.", device);
                clientAPI.tap(DISCONNECT_OK_RECT);
                conditionAPI.sleep(1500);

                Rectangle playNow = findPlayNowOption(device);
                if (playNow != null) {
                    tapAndWaitForPlay(device, playNow);
                }
                break;
            case LOGIN_SERVER_OFFLINE:
                logger.debugLog("Login server is offline. Cannot proceed.", device);
                break;
            case WORLD_FULL:
            case USE_DIFFERENT_WORLD:
                logger.debugLog("World is full; retrying shortly.", device);
                clientAPI.tap(new Rectangle(421, 300, 29, 13));
                conditionAPI.sleep(2500, 5000);
                return true;  // Indicates that the attempts should be reset
            case UPDATE_MSG_1:
            case UPDATE_MSG_2:
            case UPDATE_MSG_3:
            case UPDATE_MSG_4:
                logger.debugLog("Servers are being updated. Sleeping for five minutes before retrying!", device);
                clientAPI.tap(new Rectangle(422, 297, 43, 23));
                conditionAPI.sleep(300000);
                return true;
            case ACCOUNT_DISABLED:
            case ACCOUNT_RULE_BREAKER:
                logger.debugLog("Oh dear, you were caught botting. The account has been (temporarily?) banned!", device);
                logger.debugLog("Cannot proceed!", device);
                clientAPI.tap(new Rectangle(422, 297, 43, 23));
                conditionAPI.sleep(1750, 3000);
                break;
            case SIGN_OUT_CONFIRMATION:
                logger.debugLog("Logout screen detected, cancelling it!", device);
                clientAPI.tap(SIGNOUT_NO_RECT);
                conditionAPI.sleep(1750, 3000);
                break;
            case SIGN_IN_TO_GOOGLE:
                logger.log("No user account logged in. Account login support needed.", device);
                break;
            case LOGIN_LIMIT_EXCEEDED:
                logger.debugLog("Login limit exceeded, sleeping for a minute.", device);
                conditionAPI.sleep(60000);
                clientAPI.tap(new Rectangle(421, 300, 29, 13));
                conditionAPI.sleep(1750, 3000);
                return true;
            case BETA_MSG_1:
            case BETA_MSG_2:
                logger.debugLog("Login to beta world detected", device);
                closeOSRSApp(device);
                conditionAPI.sleep(1500, 2500);
                openOSRSApp(device);
                conditionAPI.sleep(15000, 22500);
            case NONE:
            default:
                logger.devLog("Unrecognized message: " + closestMessage);
                break;
        }
        return false;  // No reset needed for unhandled messages
    }

    public void preSetup(String device, boolean skipZoom) {
        logger.log("Running pre-setup for: " + device, device);
        if (!isOSRSInForeground(device)) {
            logger.log("OSRS is not open, opening it!", device);
            openOSRSApp(device);
        }

        // Login if not yet logged in
        if (!isLoggedIn(device)) {
            logger.log("Logging in..", device);
            login(device);
        }

        if (isLoggedIn(device)) {
            setupDisplay(device, skipZoom);
            checkCompassAndCamera(device);
            closePopOutPanelIfOpen(device);
            setupXPCounter(device);
            closeHotkeysIfOpen(device);
        }
    }

    private void setupDisplay(String device, boolean skipZoom) {
        chatbox.closeChatbox(device);

        gameTabs.openBrightnessTab(device);

        if (!skipZoom) setZoom(device);
        setBrightness(device);
    }

    private void checkCompassAndCamera(String device) {
        if (!isCompassNorth(device)) {
            logger.log("Setting compass angle", device);
            setCompassAngle(device);
        }
        logger.log("Setting camera angle.", device);
        clientAPI.moveCameraUp();
    }

    private void closePopOutPanelIfOpen(String device) {
        if (!colorFinder.isColorInRect(device, popOutPanelColor, POP_OUT_PANEL_CHECK, 25)) {
            logger.log("The pop out panel is open, closing it", device);
            clientAPI.tap(POP_OUT_PANEL_TAP);
        }
    }

    private void setupXPCounter(String device) {
        logger.devLog("Checking if we need to set up the XP counter");
        if (!isXPEnabled(device)) {
            logger.log("XP drops are not enabled, enabling them!", device);
            clientAPI.tap(XP_BUTTON_RECT);
            conditionAPI.wait(() -> isXPEnabled(device), 100, 50);
            conditionAPI.sleep(1000);
        }
        if (!colorFinder.isAnyColorInRect(device, totalXPColors, XP_COUNTER_RECT, 10)) {
            logger.log("Total XP counter was not found, configuring it.", device);
            configureXPCounter(device);
        } else {
            logger.log("XP Counter is setup correctly.", device);
        }
    }

    private void configureXPCounter(String device) {
        clientAPI.longPress(XP_BUTTON_RECT);
        conditionAPI.wait(() -> imageVisible(device), 200, 25);
        Rectangle setupOption = templateMatcher.match(device, XP_SETUP, 10);

        if (setupOption != null) {
            clientAPI.tap(setupOption);
            conditionAPI.wait(() -> interfacePresent(device), 200, 25);

            if (interfacePresent(device)) {
                setupXPInterface();
            } else {
                logger.devLog("XP interface could not be found. Unable to set XP count settings.");
            }
        } else {
            logger.devLog("Couldn't locate the setup menu entry. Unable to set XP count settings.");
        }
    }

    private void setupXPInterface() {
        clientAPI.tap(new Rectangle(114, 230, 143, 15));
        conditionAPI.sleep(generateRandomDelay(1200, 1750));
        clientAPI.tap(new Rectangle(151, 252, 50, 17));
        conditionAPI.sleep(generateRandomDelay(750, 1000));
        clientAPI.tap(new Rectangle(238, 275, 139, 15));
        conditionAPI.sleep(generateRandomDelay(1200, 1750));
        clientAPI.tap(new Rectangle(268, 299, 63, 14));
        conditionAPI.sleep(generateRandomDelay(750, 1000));
        clientAPI.tap(new Rectangle(554, 172, 14, 13));
        conditionAPI.sleep(generateRandomDelay(400, 800));
    }

    public boolean isXPEnabled(String device) {
        return colorFinder.isAnyColorInRect(device, xpDropsEnabledColor, XP_BUTTON_RECT, 5);
    }

    public void showXPDrops(String device) {
        if (isXPEnabled(device)) {
            logger.debugLog("XP drops are already enabled.", device);
        } else {
            logger.debugLog("XP drops are not enabled, enabling them!", device);
            clientAPI.tap(XP_BUTTON_RECT);
            conditionAPI.wait(() -> isXPEnabled(device), 100, 50);
        }
    }

    public void hideXPDrops(String device) {
        if (!isXPEnabled(device)) {
            logger.debugLog("XP drops are already disabled.", device);
        } else {
            logger.debugLog("Disabling XP drops!", device);
            clientAPI.tap(XP_BUTTON_RECT);
            conditionAPI.wait(() -> !isXPEnabled(device), 100, 50);
        }
    }

    private void closeHotkeysIfOpen(String device) {
        List<Point> points = colorFinder.processColorPointsInRect(device, hotkeyButtonColor, HOTKEYS_BUTTON_RECT, 5);

        if (points.size() > 14) {
            logger.devLog("Closing hotkeys tab");
            clientAPI.tap(HOTKEYS_BUTTON_RECT);
        } else {
            logger.devLog("Hotkeys tab already closed.");
        }
    }

    private boolean interfacePresent(String device) {
        return colorFinder.areAllColorsInPairs(device, colorRectPairs, 10);
    }

    private boolean imageVisible(String device) {
        Rectangle matchedRectangle = templateMatcher.match(device, XP_SETUP, 10);

        return matchedRectangle != null;
    }

    public void setBrightness(String device) {
        if (colorFinder.isAnyColorInRect(device, brightnessCheckColors, BRIGHTNESS_CHECK_RECT, 10)) {
            logger.devLog("Brightness is already at the level 3 setting.");
        } else {
            Point clickPoint = new Point(708, 301);
            clientAPI.tap(clickPoint, device);
            conditionAPI.sleep(300);
            logger.log("Set the brightness to level 3, re-logging for it to take effect.", device);
            logout(device, false);
            login(device);

            // Open Settings tab again, as that's where we left off before this was called.
            if (!gameTabs.isTabOpen(device, "Settings")) {
                gameTabs.openTab(device, "Settings");
                conditionAPI.sleep(250);
            }
        }
    }

    public void setZoom(String device) {
        // Open settings tab if not already open
        if (!gameTabs.isTabOpen(device, "Settings")) {
            gameTabs.openTab(device, "Settings");
            conditionAPI.sleep(250);
        }

        // Colorfind the button to see if we're on level 3
        if (colorFinder.isAnyColorInRect(device, brightnessCheckColors, ZOOM_CHECK_RECT, 10)) {
            logger.devLog("Zoom is already at the level 3 setting.");
        } else {
            CURRENT_ZOOM_LEVEL = 3;
            Point clickPoint = new Point(708, 339);
            clientAPI.tap(clickPoint, device);
            logger.log("Set the zoom to level 3.", device);
            conditionAPI.sleep(500);
        }
    }

    public void setCompassAngle(String device) {
        clientAPI.tap(COMPASS_TAP_RECT);
        conditionAPI.wait(() -> isCompassNorth(device), 250, 30);
    }

    public boolean isOSRSInForeground(String device) {
        // Execute dumpsys command to get a complete output
        String command = "shell dumpsys activity activities";
        List<String> outputLines = adbHandler.executeADBCommandWithOutput(command, device);

        if (outputLines.isEmpty()) {
            logger.devLog("No output from dumpsys command. Check if device is connected and adb permissions are correct.");
            return false;
        }

        for (String line : outputLines) {
            // Check for newer Android versions first
            if (line.contains("visible=true") && line.contains("visibleRequested=true") && line.contains(Constants.OSRS_APP_NAME)) {
                //logger.devLog("OSRS is in the foreground on newer Android version.");
                return true;
            }
            // Check for older Android versions
            if (line.contains("mResumedActivity") && line.contains(Constants.OSRS_APP_NAME)) {
                //logger.devLog("OSRS is in the foreground on older Android version.");
                return true;
            }
        }

        logger.print("OSRS not in the foreground.");
        return false;
    }

    public void openOSRSApp(String device) {
        // Launch the OSRS app if it's not in the foreground
        String launchAppCmd = "shell am start -n " + Constants.OSRS_APP_NAME + "/com.jagex.android.MainActivity";
        adbHandler.executeADBCommand(launchAppCmd, device);
        conditionAPI.wait(() -> findPlayNowOption(device) != null, 2000, 50);
    }

    public Rectangle findPlayNowOption(String device) {
        Rectangle result = templateMatcher.match(device, PLAY_NOW, 10);

        if (result == null) {
            logger.devLog("The PlayNow image was not found");
        }

        return result; // Returns the found Rectangle or null if no image is found
    }

    public boolean onLoginScreen(String device) {
        Rectangle result = templateMatcher.match(device, PLAY_NOW, 10);

        if (result == null) {
            logger.devLog("The PlayNow image was not found");
            return false;
        }

        return true; // Return true if the PlayNow image is found
    }

    public Boolean isLoggedOut(String device) {
        return !isLoggedIn(device);
    }

    public Boolean isLoggedIn(String device) {
        List<Rectangle> areas = Arrays.asList(
                new Rectangle(860, 17, 20, 13),  // Battery area
                new Rectangle(846, 7, 6, 10)     // WiFi area
        );

        List<Color> colors = Arrays.asList(
                Color.decode("#1eff00"),
                Color.decode("#90ff81")
        );

        // Check if any target color is in both rectangles
        return colorFinder.isAnyColorInRects(device, colors, areas, 5);
    }

    public Rectangle findTapToPlayOption(String device) {
        Rectangle result = templateMatcher.match(device, TAP_TO_PLAY, 10);

        if (result == null) {
            logger.devLog("The taptoPlay image was not found");
            return null;
        }

        return result;
    }

    public boolean isTapToPlayVisibleCF(String device) {
        return colorFinder.isAnyColorInRect(device, tapHereToPlayColors, TAP_HERE_TO_PLAY_CHECK_RECT, 10);
    }

    public boolean isCompassNorth(String device) {
        minimapProjections.determineCompassAngle(device);
        return MINIMAP_ROTATION_ANGLE == 0;
    }

    public void logout(String device, boolean killApp) {
        logger.devLog("Running logout sequence");

        gameTabs.openTab(device, "Logout");
        conditionAPI.sleep(500);

        Rectangle logoutOption = findLogoutOption(device);

        if (logoutOption != null) {
            clientAPI.tap(logoutOption);
            conditionAPI.wait(() -> isLoggedOut(device), 500, 50);
        } else {
            logger.devLog("Logout option not found, exiting method.");
        }

        if (isLoggedOut(device)) {
            logger.devLog("Logged out successfully.");

            if (killApp) {
                if (breakHandlerService.getExistingHandler(device).shouldCloseAppOnBreak()) {
                    closeOSRSApp(device);
                }
            }
        } else {
            logger.devLog("Not sure what happened during logout? Please check.");
        }
    }

    public void closeOSRSApp(String device) {
        String closeAppCmd = "shell am force-stop " + Constants.OSRS_APP_NAME;
        adbHandler.executeADBCommand(closeAppCmd, device);
    }

    public String readLoginScreen(String emulatorId) {
        try {
            FeedbackObservables.rectangleObservable.setValue(emulatorId, regionToOCR);

            // Getting only the part of the image within the rectangle
            BufferedImage partOfGameView = getGameView.getSubBuffered(emulatorId, regionToOCR);

            // Performing OCR on the specified region
            String ocrResults = cfOCR.findAllPatternsInImageAnyFont(0, partOfGameView, LoginTextColors);
            logger.devLog("Login screen OCR result: \n" + ocrResults);

            // Find the closest matching login message
            LoginMessages closestMatch = findClosestLoginMessage(ocrResults);
            logger.devLog("Closest login message: " + closestMatch.getMessage());
            return closestMatch.getMessage();
        } catch (Exception e) {
            logger.devLog("Error during OCR: " + e.getMessage());
            return null;
        }
    }

    public LoginMessages findClosestLoginMessage(String ocrResult) {
        LoginMessages closestMatch = LoginMessages.NONE;
        int closestDistance = Integer.MAX_VALUE;

        for (LoginMessages message : LoginMessages.values()) {
            int distance = levenshteinDistance(ocrResult, message.getMessage());
            if (distance < closestDistance) {
                closestDistance = distance;
                closestMatch = message;
            }
        }

        return closestMatch;
    }

    public Rectangle findLogoutOption(String device) {
        Rectangle result = templateMatcher.match(device, LOGOUT_BUTTON, 10);

        if (result == null) {
            logger.devLog("The logout button was not found");
        }

        return result;
    }

    public int generateRandomDelay(int lowerBound, int upperBound) {
        // Swap if lowerBound is greater than upperBound
        if (lowerBound > upperBound) {
            int temp = lowerBound;
            lowerBound = upperBound;
            upperBound = temp;
        }
        return lowerBound + random.nextInt(upperBound - lowerBound + 1);
    }

    private void initializeLoginImages() {
        LOGOUT_BUTTON = imageUtils.pathToBuffered("/osrsAssets/Loginout/logoutbutton.png");
        WORLD_BUSY_IMAGE = imageUtils.pathToBuffered("/osrsAssets/Loginout/worldbusy.png");
        PLAY_NOW = imageUtils.pathToBuffered("/osrsAssets/Loginout/playnow.png");
        TAP_TO_PLAY = imageUtils.pathToBuffered("/osrsAssets/Loginout/taptoplay.png");
        XP_SETUP = imageUtils.pathToBuffered("/osrsAssets/XP/setup.png");
    }
}
