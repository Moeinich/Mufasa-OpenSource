package osr.mapping;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import helpers.CacheManager;
import helpers.DiscordWebhook;
import helpers.GetGameView;
import helpers.Logger;
import helpers.OCR.DigitReader;
import helpers.OCR.ReadLevels;
import helpers.Color.ColorFinder;
import helpers.Color.TemplateMatcher;
import helpers.Color.utils.ColorRectanglePair;
import helpers.services.BreakHandlerService;
import helpers.utils.*;
import helpers.visualFeedback.FeedbackObservables;
import helpers.visualFeedback.RectangleAndPoint;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import osr.mapping.utils.FairyRings;
import osr.mapping.utils.MinimapProjections;
import osr.mapping.utils.WorldHopperUtils;
import osr.mapping.utils.WorldHopperUtils.WorldInfo;
import osr.utils.ImageUtils;
import osr.walker.utils.TranslatePosition;
import scripts.APIClasses.ClientAPI;
import scripts.APIClasses.ConditionAPI;
import scripts.ScriptAccountManager;
import utils.CredentialsManager;
import utils.SystemUtils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import static utils.Constants.CURRENT_ZOOM_LEVEL;
import static utils.Constants.GameObjectColorMap;

public class Game {
    private final static Rectangle zoom1CheckRect = new Rectangle(657, 336, 4, 4);
    private final static Rectangle zoom2CheckRect = new Rectangle(682, 336, 3, 3);
    private final static Rectangle zoom3CheckRect = new Rectangle(706, 337, 3, 3);
    private final static Rectangle zoom4CheckRect = new Rectangle(729, 336, 3, 4);
    private final static Rectangle zoom5CheckRect = new Rectangle(753, 335, 4, 5);
    private static final Rectangle systemUpdateReadRect = new Rectangle(70, 1, 141, 24);
    private static final List<Color> worldhopColors = List.of(
            Color.decode("#957012"),
            Color.decode("#b18c2e"),
            Color.decode("#ae9a0d"),
            Color.decode("#aeaeae"),
            Color.decode("#ac0c04")
    );
    private static final Color xpDropsEnabledColor = Color.decode("#efe4b0");
    private static final List<Color> systemUpdateTextColors = List.of(
            Color.decode("#ffff00")
    );
    // Rectangles
    private static final Rectangle worldhopCheckRect1 = new Rectangle(76, 3, 99, 15);
    private static final Rectangle worldhopCheckRect2 = new Rectangle(204, 1, 11, 21);
    private static final Rectangle worldhopCheckRect3 = new Rectangle(449, 2, 289, 18);
    private static final Rectangle XP_BUTTON_RECT = new Rectangle(665, 11, 18, 14);
    private final static List<ColorRectanglePair> colorRectPairs = List.of(
            new ColorRectanglePair(worldhopColors, worldhopCheckRect1),
            new ColorRectanglePair(worldhopColors, worldhopCheckRect2),
            new ColorRectanglePair(worldhopColors, worldhopCheckRect3)
    );

    private final BreakHandlerService breakHandlerService;
    private final ColorFinder colorFinder;
    private final MinimapProjections minimapProjections;
    private final TranslatePosition translatePosition;
    private final Objects objects;
    private final CacheManager cacheManager;
    private final WorldHopperUtils worldHopperUtils;
    private final GameTabs gameTabs;
    private final GetGameView getGameView;
    private final Logger logger;
    private final DiscordWebhook discordWebhook;
    private final ClientAPI clientAPI;
    private final Login login;
    private final Logout logout;
    private final FairyRings fairyRings;
    private final ConditionAPI conditionAPI;
    private final ImageUtils imageUtils;
    private final ScriptAccountManager scriptAccountManager;
    private final CredentialsManager credMgr;
    private final Chatbox chatbox;
    private final ReadLevels readLevels;
    private final DigitReader digitReader;
    private final TemplateMatcher templateMatcher;

    private final Rectangle worldListButton = new java.awt.Rectangle(90, 487, 86, 29);
    private final Rectangle arrowRight = new java.awt.Rectangle(782, 261, 37, 23);
    private final Rectangle hotkeysButtonRect = new Rectangle(18, 420, 47, 26);

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
    private final Map<String, BufferedImage> imageCache = new ConcurrentHashMap<>();
    private final Rectangle compassRect = new Rectangle(711, 11, 21, 26);
    private final Rectangle searchRect = new Rectangle(636, 6, 154, 191);
    private final List<Color> whiteLookColor = List.of(
            Color.decode("#ffffff")
    );
    private final Rectangle questSelectionRect = new Rectangle(674, 232, 38, 14);
    private final Rectangle diarySelectionRect = new Rectangle(736, 232, 40, 14);
    private final Rectangle questScrollBarRect = new Rectangle(766, 294, 11, 129);
    private final Rectangle diaryScrollBarRect = new Rectangle(772, 290, 11, 172);
    private final Rectangle musicScrollBarRect = new Rectangle(768, 302, 10, 146);

    public Game(TemplateMatcher templateMatcher, DigitReader digitReader, BreakHandlerService breakHandlerService, ColorFinder colorFinder, MinimapProjections minimapProjections, TranslatePosition translatePosition, Objects objects, CacheManager cacheManager, WorldHopperUtils worldHopperUtils, GameTabs gameTabs, GetGameView getGameView, Logger logger, ClientAPI clientAPI, Login login, Logout logout, FairyRings fairyRings, ConditionAPI conditionAPI, ImageUtils imageUtils, DiscordWebhook discordWebhook, ScriptAccountManager scriptAccountManager, CredentialsManager credMgr, Chatbox chatbox, ReadLevels readLevels, Login login1) {
        this.breakHandlerService = breakHandlerService;
        this.colorFinder = colorFinder;
        this.minimapProjections = minimapProjections;
        this.translatePosition = translatePosition;
        this.objects = objects;
        this.cacheManager = cacheManager;
        this.worldHopperUtils = worldHopperUtils;
        this.gameTabs = gameTabs;
        this.getGameView = getGameView;
        this.logger = logger;
        this.discordWebhook = discordWebhook;
        this.clientAPI = clientAPI;
        this.login = login;
        this.logout = logout;
        this.fairyRings = fairyRings;
        this.conditionAPI = conditionAPI;
        this.imageUtils = imageUtils;
        this.scriptAccountManager = scriptAccountManager;
        this.credMgr = credMgr;
        this.chatbox = chatbox;
        this.readLevels = readLevels;
        this.digitReader = digitReader;
        this.templateMatcher = templateMatcher;
    }

    private Rectangle findBestMatch(String imagePath, String device) {
        BufferedImage imageToSearchFor = imageCache.computeIfAbsent(imagePath, imageUtils::pathToBuffered);
        return templateMatcher.match(device, imageToSearchFor, 15);
    }

    public boolean isXPEnabled(String device) {
        FeedbackObservables.rectangleObservable.setValue(device, XP_BUTTON_RECT);
        return colorFinder.isAnyColorInRect(device, (List<Color>) xpDropsEnabledColor, XP_BUTTON_RECT, 5);
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

    public boolean isSystemUpdate(String device) {
        FeedbackObservables.rectangleObservable.setValue(device, systemUpdateReadRect);
        return digitReader.findString(5, systemUpdateReadRect, systemUpdateTextColors, cacheManager.getLetterPatterns(), "System update", device) != null;
    }

    public boolean isGameObjectAt(String device,
                                  GameObject type,
                                  org.opencv.core.Point playerPosition,
                                  org.opencv.core.Point worldPosition) {
        // Convert some "world" position to the minimap position
        org.opencv.core.Point pointToCheck = translatePosition.worldToMM(worldPosition, playerPosition, device);

        // Grab the image (likely a screenshot or minimap)
        BufferedImage gameView = getGameView.getBuffered(device);
        if (gameView == null) {
            return false; // Or throw an exception/log error
        }

        // Define how many pixels around (x, y) to check
        int radiusToCheck = 4;

        // Ensure we stay within the image bounds
        int minX = Math.max((int) pointToCheck.x - radiusToCheck, 0);
        int maxX = Math.min((int) pointToCheck.x + radiusToCheck, gameView.getWidth() - 1);
        int minY = Math.max((int) pointToCheck.y - radiusToCheck, 0);
        int maxY = Math.min((int) pointToCheck.y + radiusToCheck, gameView.getHeight() - 1);

        // Retrieve the int-based color set for the target GameObject
        Set<Integer> targetColors = GameObjectColorMap.get(type);
        if (targetColors == null || targetColors.isEmpty()) {
            // No known colors for this GameObject
            return false;
        }

        // Scan pixels in a small area around (x, y)
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                // getRGB(...) returns ARGB, so we mask off alpha: 0xFFFFFF & color
                int color = 0xFFFFFF & gameView.getRGB(x, y);

                // Check if the color is in the set
                if (targetColors.contains(color)) {
                    FeedbackObservables.pointObservable.setValue(device, new Point(x, y));
                    return true; // Found a match
                }
            }
        }

        return false; // No matching color found in that region
    }

    public boolean isHotkeymenuOpen(String device) {
        // Check if hotkeys are visible
        FeedbackObservables.rectangleObservable.setValue(device, hotkeysButtonRect);
        List<Point> points = colorFinder.processColorPointsInRect(device, hotkeyButtonColor, hotkeysButtonRect, 5);

        if (points.size() > 14) {
            logger.devLog("Hotkeys tab is open");
            return true;
        } else {
            logger.devLog("Hotkeys tab is not open");
            return false;
        }
    }

    public void openHotkeymenu(String device) {
        // Check if hotkeys are visible
        FeedbackObservables.rectangleObservable.setValue(device, hotkeysButtonRect);
        List<Point> points = colorFinder.processColorPointsInRect(device, hotkeyButtonColor, hotkeysButtonRect, 5);

        if (points.size() > 14) {
            logger.devLog("Hotkeys tab is already open");
        } else {
            logger.devLog("Hotkeys tab is not open, opening!");
            clientAPI.tap(hotkeysButtonRect);
            conditionAPI.wait(() -> isHotkeymenuOpen(device), 200, 30);
        }
    }

    public void closeHotkeymenu(String device) {
        // Check if hotkeys are visible
        FeedbackObservables.rectangleObservable.setValue(device, hotkeysButtonRect);
        List<Point> points = colorFinder.processColorPointsInRect(device, hotkeyButtonColor, hotkeysButtonRect, 5);

        if (points.size() > 14) {
            logger.devLog("Hotkeys tab is open, closing!");
            clientAPI.tap(hotkeysButtonRect);
            conditionAPI.wait(() -> isHotkeymenuOpen(device), 200, 30);
        } else {
            logger.devLog("Hotkeys tab is not open.");
        }
    }

    public boolean isActionEnabled(String device, String imagePath) {
        // Open hotkey menu if not already open
        if (!isHotkeymenuOpen(device)) {
            openHotkeymenu(device);
        }

        Rectangle result = findBestMatch(imagePath, device);

        if (result != null) {
            logger.log("We found the tap to drop BUTTON", device);
            // We found the rectangle, check if it's active
            FeedbackObservables.rectangleObservable.setValue(device, result);
            return true; // Since we are looking for the enabled button.
        } else {
            return false;
        }
    }

    public boolean isTapToDropEnabled(String device) {
        boolean result = isActionEnabled(device, "/osrsAssets/menuButtons/actionButton/taptodropbutton.png");

        if (result) {
            logger.devLog("Tap to drop option is active!");
            return true;
        } else {
            logger.devLog("Tap to drop option is not active!");
            return false;
        }
    }

    public boolean isSingleTapEnabled(String device) {
        boolean result = isActionEnabled(device, "/osrsAssets/menuButtons/actionButton/singletapbutton.png");

        if (result) {
            logger.devLog("Single tap option is active!");
            return true;
        } else {
            logger.devLog("Couldn't locate the single tap option, returning false by default.");
            return false;
        }
    }

    public void enableTapToDrop(String device) {
        // Check if tap to drop is on, otherwise enable
        if (!isTapToDropEnabled(device)) {
            Rectangle result = findBestMatch("/osrsAssets/menuButtons/actionButton/taptodropbutton_disabled.png", device);
            if (result != null) {
                clientAPI.tap(result);
                conditionAPI.wait(() -> isTapToDropEnabled(device), 200, 30);
                closeHotkeymenu(device);
            } else {
                logger.debugLog("Couldn't locate the tap to drop option, make sure it is on your hotkey menu!", device);
            }
        }
    }

    public void enableSingleTap(String device) {
        // Check if single tap is on, otherwise enable
        if (!isSingleTapEnabled(device)) {
            Rectangle result = findBestMatch("/osrsAssets/menuButtons/actionButton/singletapbutton_disabled.png", device);
            if (result != null) {
                clientAPI.tap(result);
                conditionAPI.wait(() -> isSingleTapEnabled(device), 200, 30);
                closeHotkeymenu(device);
            } else {
                logger.debugLog("Couldn't locate the single tap option, make sure it is on your hotkey menu!", device);
            }
        }
    }

    public boolean isPlayersAround(String device) {
        return worldHopperUtils.isPlayersAround(device);
    }
    public int countPlayersAround(String device) {
        return worldHopperUtils.countPlayersAround(device);
    }

    public boolean isPlayersUnderUs(String device) {
        return worldHopperUtils.checkPlayerUnderUs(device);
    }

    public boolean isPlayerAt(String device, Tile tileToCheck) {
        return worldHopperUtils.isPlayerAt(device, tileToCheck);
    }

    public boolean isPlayersAround(String device, Tile tileToCheck, int radius) {
        return worldHopperUtils.isPlayersAround(device, tileToCheck, radius);
    }

    public void setZoom(String device, String level) {
        // Declare clickPoint and zoomCheckRect outside the switch block
        Point clickPoint;
        Rectangle zoomCheckRect;

        // Set clickPoint and zoomCheckRect based on the specified level
        switch (level) {
            case "1":
                clickPoint = new Point(659, 340);
                zoomCheckRect = zoom1CheckRect;
                FeedbackObservables.rectangleAndPointObservable.setValue(device, new RectangleAndPoint(zoom1CheckRect, clickPoint));
                break;
            case "2":
                clickPoint = new Point(684, 339);
                zoomCheckRect = zoom2CheckRect;
                FeedbackObservables.rectangleAndPointObservable.setValue(device, new RectangleAndPoint(zoom2CheckRect, clickPoint));
                break;
            case "3":
                clickPoint = new Point(708, 339);
                zoomCheckRect = zoom3CheckRect;
                FeedbackObservables.rectangleAndPointObservable.setValue(device, new RectangleAndPoint(zoom3CheckRect, clickPoint));
                break;
            case "4":
                clickPoint = new Point(732, 339);
                zoomCheckRect = zoom4CheckRect;
                FeedbackObservables.rectangleAndPointObservable.setValue(device, new RectangleAndPoint(zoom4CheckRect, clickPoint));
                break;
            case "5":
                clickPoint = new Point(756, 339);
                zoomCheckRect = zoom5CheckRect;
                FeedbackObservables.rectangleAndPointObservable.setValue(device, new RectangleAndPoint(zoom5CheckRect, clickPoint));
                break;
            default:
                logger.debugLog("Invalid level supplied, only 1-5 is supported (as string)", device);
                return; // Exit the method for an invalid level
        }

        boolean zoomSet = false;
        int attempts = 0;

        // Try setting the zoom up to 3 times if it isn't successful on the first attempt
        while (!zoomSet && attempts < 3) {
            // Check if the zoom/brightness tab is open within the settings tab, if not open it.
            if (!gameTabs.isTabOpen(device, "Settings")) {
                gameTabs.openTab(device, "Settings");
                conditionAPI.sleep(500, 1000);
            }

            gameTabs.openBrightnessTab(device);
            conditionAPI.sleep(750, 1250);

            // Check if the zoom level is already set
            if (zoomCheckRect != null && colorFinder.isAnyColorInRect(device, brightnessCheckColors, zoomCheckRect, 10)) {
                logger.devLog("Zoom is already at the level " + level + " setting.");
                zoomSet = true;
                // Set CURRENT_ZOOM_LEVEL only when zoom is confirmed as set
                CURRENT_ZOOM_LEVEL = Integer.parseInt(level);
            } else {
                if (clickPoint != null) {
                    clientAPI.tap(clickPoint, device);
                    logger.log("Set the zoom to level " + level + ".", device);

                    // Give some time to apply the zoom setting before re-checking
                    conditionAPI.sleep(750);
                } else {
                    // Handle case where clickPoint is not set (invalid level)
                    logger.log("Invalid zoom level: " + level, device);
                    break;
                }
            }
            attempts++;
        }

        if (!zoomSet) {
            logger.log("Failed to set the zoom to level " + level + " after two attempts.", device);
        }
    }

    public void setFairyRing(String device, String Letters) {
        fairyRings.setRingsTo(device, Letters);
    }

    // SECTION FOR WORLD HOPPING
    public synchronized List<Integer> loadProfile(String profileName) {
        boolean isWednesday = LocalDateTime.now().getDayOfWeek() == DayOfWeek.WEDNESDAY;
        long currentTimeMillis = System.currentTimeMillis();

        Long lastUpdateTime = cacheManager.getSLLastUpdateTime();
        if (lastUpdateTime == null) {
            lastUpdateTime = 0L;
            logger.print("No last update time found, initializing to 0.");
        }

        // Determine update frequency based on the day of the week
        long updateInterval = isWednesday ? 15 * 60 * 1000 : 60 * 60 * 1000;  // 15 minutes on Wednesdays, 1 hour otherwise

        boolean shouldUpdate = (currentTimeMillis - lastUpdateTime) > updateInterval;

        if (shouldUpdate) {
            logger.print("Update interval has passed. Proceeding with server list update and profile cache update.");

            Path serverListPath = Paths.get(SystemUtils.getWorldHopperFolderPath() + "/ServerList.json");
            Path latestServerListPath = Paths.get(SystemUtils.getWorldHopperFolderPath() + "/LatestServerList.json");

            try {
                // Gather and save the latest world list only if the timing criteria is met
                worldHopperUtils.gatherAndSaveLatestWorldList();

                // Update the last update time
                cacheManager.setSLLastUpdateTime(currentTimeMillis);

                // Read both ServerList.json and LatestServerList.json
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                Type type = new TypeToken<List<Map<String, Object>>>() {
                }.getType();

                List<Map<String, Object>> latestList = null;
                List<Map<String, Object>> existingList = null;

                if (Files.exists(latestServerListPath)) {
                    try (FileReader reader = new FileReader(latestServerListPath.toFile())) {
                        latestList = gson.fromJson(reader, type);
                    }
                }

                if (Files.exists(serverListPath)) {
                    try (FileReader reader = new FileReader(serverListPath.toFile())) {
                        existingList = gson.fromJson(reader, type);
                    }
                }

                // Check if the new list is different from the existing one
                if (!latestList.equals(existingList)) {
                    logger.print("Server list has changed, updating ServerList.json and profile caches.");

                    // Write the latest list to ServerList.json
                    try (FileWriter file = new FileWriter(serverListPath.toFile())) {
                        gson.toJson(latestList, file);
                        logger.print("ServerList.json updated.");
                    }

                    // Update all profiles - ensure this doesn't re-trigger loadProfile for the same profile
                    updateAllProfilesCache();
                } else {
                    logger.print("No changes in the server list. No update needed.");
                }
            } catch (IOException e) {
                logger.print("Error occurred during server list update: " + e.getMessage());
                e.printStackTrace();
            }
        }

        // Load the profile
        HopProfile profile = cacheManager.getHopProfile(profileName);
        if (profile != null) {
            return profile.filteredWorlds;
        }

        Path profilePath = Paths.get(SystemUtils.getWorldHopperFolderPath() + "/" + profileName + ".json");
        List<Integer> worldList = new ArrayList<>();

        try {
            logger.print("Loading profile from file: " + profilePath);
            String content = new String(Files.readAllBytes(profilePath));
            JSONObject jsonObject = new JSONObject(content);
            JSONArray filteredWorldsArray = jsonObject.getJSONArray("Filtered worlds");

            for (int i = 0; i < filteredWorldsArray.length(); i++) {
                worldList.add(filteredWorldsArray.getInt(i));
            }

            profile = new HopProfile();
            profile.filteredWorlds = worldList;
            profile.hopTimer = jsonObject.getDouble("Hop Timer");
            cacheManager.setHopProfile(profileName, profile);

            logger.print("Profile " + profileName + " loaded and cached.");
        } catch (IOException e) {
            logger.print("Error loading profile " + profileName + ": " + e.getMessage());
            e.printStackTrace();
        } catch (JSONException e) {
            logger.print("Error parsing profile JSON for " + profileName + ": " + e.getMessage());
            e.printStackTrace();
        }

        return worldList;
    }

    // Helper method to update all profiles in cache
    private void updateAllProfilesCache() {
        logger.print("Updating all profiles in cache with the new server list.");
        List<String> profileNames = worldHopperUtils.getProfileNames();
        for (String profileName : profileNames) {
            logger.print("Updating profile cache for: " + profileName);
            cacheManager.removeHopProfile(profileName);  // Clear cached profile before reloading
            loadProfile(profileName);  // Reload profiles to ensure they use the latest server list
        }
        logger.print("All profiles have been updated with the latest server list.");
    }

    public Integer getRandomWorld(String profileName) {
        List<Integer> worldList = loadProfile(profileName);
        if (worldList == null || worldList.isEmpty()) {
            System.out.println("World list is empty or not found for profile: " + profileName);
            return null;
        }
        int randomIndex = ThreadLocalRandom.current().nextInt(worldList.size());
        return worldList.get(randomIndex);
    }

    public List<Integer> getWorldHopList(String profileName) {
        // Load the profile and get the world list
        List<Integer> worldList = loadProfile(profileName);

        // Check if the world list is null or empty
        if (worldList == null || worldList.isEmpty()) {
            System.out.println("World list is empty or not found for profile: " + profileName);
            return new ArrayList<>(); // Return an empty list instead of null
        }

        return worldList; // Return the full world list
    }

    public void hopIfPlayersAround(String profileName, String emulatorId) {
        boolean playersAround = isPlayersAround(emulatorId);

        if (playersAround) {
            performHop(profileName, emulatorId);
        }
    }

    public void hopWithOptionalWDH(String profileName, String emulatorId, boolean useWDH) {
        HopTimeInfo hopTimeInfo = cacheManager.getHopTimeInfo(emulatorId);

        if (hopTimeInfo == null || hopTimeInfo.lastHopTime == 0) {
            loadProfile(profileName);
            initializeHopTimes(profileName, emulatorId);
            return;
        }

        if (hopTimeInfo.isHopsPostponed) {
            return;
        }

        double currentTime = System.currentTimeMillis() / 1000.0;
        boolean playersAround = isPlayersAround(emulatorId);
        boolean wdhTriggered = (useWDH && playersAround);
        boolean scheduledHop = (currentTime >= hopTimeInfo.nextHopTime);

        if (wdhTriggered || scheduledHop) {
            loadProfile(profileName);

            if (wdhTriggered) {
                logger.log("Hopping due to player detection (WDH).", emulatorId);
            } else {
                logger.log("Hopping due to reaching the scheduled hop time.", emulatorId);
            }

            performHop(profileName, emulatorId);
        }
    }

    public void hopDuringBreak(String emulatorId) {
        // Retrieve the hop profile name for the device
        String profileName = cacheManager.getHopProfilePerDevice(emulatorId);

        if (profileName == null) {
            // No stored profileName, so return and do nothing
            return;
        }

        // Retrieve the hop time information
        HopTimeInfo hopTimeInfo = cacheManager.getHopTimeInfo(emulatorId);
        double currentTime = System.currentTimeMillis() / 1000.0; // Current time in seconds

        // Check if there is a hop time stored; if not, exit as we don't need to do anything
        if (hopTimeInfo == null || hopTimeInfo.lastHopTime == 0) {
            // Don't do anything as hops have not been initialized, so probably no hopping enabled in the script
            return;
        }

        // If the current time is later than the next hop time, proceed to switching worlds
        if (currentTime >= hopTimeInfo.nextHopTime) {
            loadProfile(profileName); // Only load the profile when it's time to switch worlds
            logger.log("Switching worlds due to reaching the scheduled hop time during a break.", emulatorId);

            BreakHandlerService breakService = breakHandlerService.getHandlerForEmulator(emulatorId);

            if (!breakService.shouldCloseAppOnBreak()) {
                switchWorld(profileName, emulatorId);
            } else {
                updateHopTimes(profileName, emulatorId); //if we are closing app on break, just update the hop time and skip this hop
            }
        }
    }

    public void instantHop(String profileName, String emulatorId) {
        // Load the profile to ensure it's in the cache
        loadProfile(profileName);

        // Log the action for clarity and debugging
        logger.debugLog("Hopping to a new world.", emulatorId);

        // Directly call the method to perform the hop without checking conditions
        performHop(profileName, emulatorId);
    }

    public void switchWorldNoProfile(String emulatorId) {

        int world = worldHopperUtils.getRandomWorldToHopTo();
        // Log the action for clarity and debugging
        logger.debugLog("Switching to world " + world + ".", emulatorId);

        // Directly call the method to perform the hop without checking conditions
        performSwitchNoProfile(world, emulatorId);
    }

    public void switchWorld(String profileName, String emulatorId) {
        // Load the profile to ensure it's in the cache
        loadProfile(profileName);

        // Log the action for clarity and debugging
        logger.debugLog("Switching to a new world.", emulatorId);

        // Directly call the method to perform the hop without checking conditions
        performSwitch(profileName, emulatorId);
    }

    public boolean isWorldListOpen(String device) {
        return colorFinder.areAllColorsInPairs(device, colorRectPairs, 5);
    }

    private void updateHopTimes(String profileName, String emulatorId) {
        HopProfile profile = cacheManager.getHopProfile(profileName);
        if (profile != null) {
            double hopTimerMinutes = profile.hopTimer; // hop timer is in minutes
            double hopTimerSeconds = hopTimerMinutes * 60; // convert minutes to seconds
            double randomFactor = ThreadLocalRandom.current().nextDouble(0.35, 1.65); // Randomize by 65% up or down
            double adjustedHopTimerSeconds = hopTimerSeconds * randomFactor;

            long currentTime = (long) (System.currentTimeMillis() / 1000.0); // Current time in seconds
            long nextHopTime = (long) (currentTime + adjustedHopTimerSeconds); // calculate next hop time in seconds

            // Correctly handle the creation of a new HopTimeInfo object if the cache doesn't have an entry
            // Retrieve HopTimeInfo from the cache manager
            HopTimeInfo hopTimeInfo = cacheManager.getHopTimeInfo(emulatorId);

            // Check if hopTimeInfo is null and create a new instance if necessary
            if (hopTimeInfo == null) {
                hopTimeInfo = new HopTimeInfo(currentTime, nextHopTime);
            }

            hopTimeInfo.lastHopTime = currentTime;
            hopTimeInfo.nextHopTime = nextHopTime;

            System.out.println(hopTimeInfo);

            cacheManager.setHopTime(emulatorId, hopTimeInfo); // Update the cache
        } else {
            logger.devLog("Profile not found: " + profileName);
        }
    }

    private void initializeHopTimes(String profileName, String emulatorId) {
        HopProfile profile = cacheManager.getHopProfile(profileName);

        if (cacheManager.getHopProfilePerDevice(emulatorId) == null) {
            cacheManager.setHopProfilePerDevice(emulatorId, profileName);
        }

        if (profile != null) {
            double hopTimerMinutes = profile.hopTimer;
            double hopTimerSeconds = hopTimerMinutes * 60;
            double randomFactor = ThreadLocalRandom.current().nextDouble(0.35, 1.65);
            double adjustedHopTimerSeconds = hopTimerSeconds * randomFactor;

            long currentTime = (long) (System.currentTimeMillis() / 1000.0);
            long nextHopTime = (long) (currentTime + adjustedHopTimerSeconds);

            HopTimeInfo hopTimeInfo = new HopTimeInfo(currentTime, nextHopTime);
            cacheManager.setHopTime(emulatorId, hopTimeInfo);
        } else {
            logger.devLog("Profile not found: " + profileName);
        }
    }

    public boolean isTimeToHop(String emulatorId, boolean checkPostponed) {
        HopTimeInfo hopTimeInfo = cacheManager.getHopTimeInfo(emulatorId);
        long currentTime = System.currentTimeMillis() / 1000L; // Current time in seconds

        if (hopTimeInfo == null) {
            logger.devLog("HopTimeInfo not found for emulatorId: " + emulatorId);
            return false;
        }

        // Early check for postponed hops
        if (checkPostponed && hopTimeInfo.isHopsPostponed) {
            return false;
        }

        // Check if the current time is greater than or equal to the next hop time
        if (currentTime >= hopTimeInfo.nextHopTime) {
            logger.devLog("It is time to hop. Current time: " + currentTime
                    + ", Next hop time: " + hopTimeInfo.nextHopTime);
            return true;
        } else {
            logger.devLog("It is not time to hop yet. Current time: " + currentTime
                    + ", Next hop time: " + hopTimeInfo.nextHopTime);
            return false;
        }
    }

    public void postponeHops(String device, boolean state) {
        HopTimeInfo hopTimeInfo = cacheManager.getHopTimeInfo(device);
        hopTimeInfo.isHopsPostponed = state;
    }

    public void performAntiBan(String device) {
        long currentTime = System.currentTimeMillis() / 1000L; // Current time in seconds

        Long nextAntiBanTime = cacheManager.getAntiBanTime(device);

        // If no next AntiBan time exists, set a new one and skip AntiBan this time
        if (nextAntiBanTime == null) {
            nextAntiBanTime = generateNextAntiBanTime(currentTime);
            cacheManager.setAntiBanTime(device, nextAntiBanTime);

            long secondsToNextAntiBan = nextAntiBanTime - currentTime;
            String timeToNextAntiBan = String.format("%02d:%02d:%02d",
                    secondsToNextAntiBan / 3600, // Hours
                    (secondsToNextAntiBan % 3600) / 60, // Minutes
                    secondsToNextAntiBan % 60 // Seconds
            );

            logger.debugLog("No AntiBan time found for device: " + device + ". Setting next AntiBan time to: " + timeToNextAntiBan, device);
            return;
        }

        // Calculate the time remaining until the next AntiBan
        long secondsToNextAntiBan = nextAntiBanTime - currentTime;
        String timeToNextAntiBan = String.format("%02d:%02d:%02d",
                secondsToNextAntiBan / 3600,
                (secondsToNextAntiBan % 3600) / 60,
                secondsToNextAntiBan % 60
        );

        // Check if it is time to perform AntiBan
        if (currentTime >= nextAntiBanTime) {
            logger.debugLog("Performing AntiBan for device: " + device, device);

            performRandomAntiBanAction(device);

            nextAntiBanTime = generateNextAntiBanTime(currentTime);
            cacheManager.setAntiBanTime(device, nextAntiBanTime);

            secondsToNextAntiBan = nextAntiBanTime - currentTime;
            timeToNextAntiBan = String.format("%02d:%02d:%02d",
                    secondsToNextAntiBan / 3600,
                    (secondsToNextAntiBan % 3600) / 60,
                    secondsToNextAntiBan % 60
            );

            logger.debugLog("Next AntiBan time set to: " + timeToNextAntiBan + " for device: " + device, device);
        }

        // Handle additional anti-ban if extended AFK option is enabled
        if (cacheManager.isOptionalExtendedAFKEnabled(device)) {
            Long nextAdditionalAntiBanTime = cacheManager.getAdditionalAntiBanTime(device);

            if (nextAdditionalAntiBanTime == null) {
                long nextInterval = 600 + (long) (Math.random() * (1500 - 600)); // 600 to 1500 seconds (10-25 minutes)
                nextAdditionalAntiBanTime = currentTime + nextInterval;
                cacheManager.setAdditionalAntiBanTime(device, nextAdditionalAntiBanTime);

                String additionalTimeToNextAntiBan = String.format("%02d:%02d:%02d",
                        nextInterval / 3600,
                        (nextInterval % 3600) / 60,
                        nextInterval % 60
                );

                logger.debugLog("Initial additional AntiBan time set to: " + additionalTimeToNextAntiBan + " for device: " + device, device);
                return;
            }

            if (currentTime >= nextAdditionalAntiBanTime) {
                logger.debugLog("Performing additional AntiBan for device: " + device, device);

                logger.log("[AntiBan]: Do extended AFK", device);
                doExtendedAFK(device);

                long nextInterval = 600 + (long) (Math.random() * (1500 - 600)); // 600 to 1500 seconds (10-25 minutes)
                nextAdditionalAntiBanTime = currentTime + nextInterval;
                cacheManager.setAdditionalAntiBanTime(device, nextAdditionalAntiBanTime);

                String additionalTimeToNextAntiBan = String.format("%02d:%02d:%02d",
                        nextInterval / 3600,
                        (nextInterval % 3600) / 60,
                        nextInterval % 60
                );

                logger.debugLog("Next additional AntiBan time set to: " + additionalTimeToNextAntiBan + " for device: " + device, device);
            }
        }
    }

    private void performRandomAntiBanAction(String device) {
        Integer lastAction = cacheManager.getLastAntiBanAction(device);
        int randomAction;

        do {
            randomAction = (int) (Math.random() * 7); // Random number between 0 and 6 for default actions
        } while (lastAction != null && randomAction == lastAction);

        cacheManager.setLastAntiBanAction(device, randomAction);

        switch (randomAction) {
            case 0:
                logger.log("[AntiBan]: Check stats", device);
                doHoverStatAntiBan(device);
                break;
            case 1:
                logger.log("[AntiBan]: Open random tab and return", device);
                doOpenRandomTab(device);
                break;
            case 2:
                if (cacheManager.isOptionalGearEquipEnabled(device)) {
                    logger.log("[AntiBan]: Unequip and equip random item", device);
                    doUnequipAndEquip(device);
                } else {
                    performRandomAntiBanAction(device); // Retry with a default action
                }
                break;
            case 3:
                logger.log("[AntiBan]: Change chat tab", device);
                doChangeChatTab(device);
                break;
            case 4:
                logger.log("[AntiBan]: Write random character", device);
                doWriteCharacter(device);
                break;
            case 5:
                logger.log("[AntiBan]: Rotate screen", device);
                doRotateScreen(device);
                break;
            case 6:
                if (cacheManager.isOptionalExtendedAFKEnabled(device)) {
                    logger.log("[AntiBan]: Do extended AFK", device);
                    doExtendedAFK(device);
                } else {
                    performRandomAntiBanAction(device); // Retry with a default action
                }
                break;
            case 7:
                logger.log("[AntiBan]: Scroll music", device);
                doScrollMusic(device);
                break;
            case 8:
                logger.log("[AntiBan]: Scroll quest", device);
                doScrollQuests(device);
                break;
            case 9:
                logger.log("[AntiBan]: Scroll achievement diaries", device);
                doScrollAchievements(device);
                break;
            default:
                logger.log("[AntiBan]: No action to take.", device);
                break;
        }
    }

    public void enableOptionalAntiBan(String device, AntiBan antiBanOption) {
        if (antiBanOption == AntiBan.EXTENDED_AFK) {
            logger.devLog("Enable additional antiban option: " + antiBanOption);
            cacheManager.setOptionalExtendedAFKEnabled(device, true);
        } else if (antiBanOption == AntiBan.UNEQUIP_AND_EQUIP_ITEM) {
            logger.devLog("Enable additional antiban option: " + antiBanOption);
            cacheManager.setOptionalGearEquipEnabled(device, true);
        }
    }

    public void addOptionalAntiBanSkill(String device, Skills skill) {
        logger.devLog("Adding optional antiban skill to list: " + skill);
        cacheManager.addOptionalAntiBanSkill(device, skill);
    }

    private long generateNextAntiBanTime(long currentTime) {
        // Random minutes between 10 and 90 (inclusive)
        long randomMinutes = 10 + (long) (Math.random() * 81); // 10 to 90 minutes
        // Random seconds (0 to 59)
        long randomSeconds = (long) (Math.random() * 60); // 0 to 59 seconds
        // Convert to total seconds
        long randomInterval = randomMinutes * 60 + randomSeconds;
        // Add the interval to the current time
        return currentTime + randomInterval;
    }

    private void doHoverStatAntiBan(String device) {
        logger.devLog("Antiban check stat");
        // Cache the currently open tab state
        String openTab = gameTabs.getOpenTab(device);
        logger.devLog("Current open tab is: " + openTab);

        // Open the Stats tab if it's not already open
        if (!"Stats".equals(openTab)) {
            gameTabs.openTab(device, "Stats");
            conditionAPI.sleep(250, 600);
        }

        // Check if a skill list is present in the cache
        boolean isSkillListPresent = cacheManager.isOptionalSkillListPresent(device);
        Skills selectedSkill;

        if (isSkillListPresent) {
            // Retrieve the cached skill list
            java.util.List<Skills> cachedSkills = cacheManager.getOptionalSkillsList(device);

            if (cachedSkills != null && !cachedSkills.isEmpty()) {
                // Decide whether to use the cached skills (75% chance) or a random skill (25% chance)
                if (Math.random() < 0.75) {
                    // Pick a random skill from the cached list
                    selectedSkill = cachedSkills.get((int) (Math.random() * cachedSkills.size()));
                } else {
                    // Pick a random skill from the entire Skills enum
                    Skills[] allSkills = Skills.values();
                    selectedSkill = allSkills[(int) (Math.random() * allSkills.length)];
                }
            } else {
                // Fallback: No cached skills available, pick a random skill from the Skills enum
                Skills[] allSkills = Skills.values();
                selectedSkill = allSkills[(int) (Math.random() * allSkills.length)];
            }
        } else {
            // No skill list is set up, pick a random skill from the Skills enum
            Skills[] allSkills = Skills.values();
            selectedSkill = allSkills[(int) (Math.random() * allSkills.length)];
        }

        Rectangle skillRectangle = readLevels.getSkillRectangle(selectedSkill);
        clientAPI.tap(skillRectangle, device);
        conditionAPI.sleep(1100, 5000);
        clientAPI.tap(skillRectangle, device);
        conditionAPI.sleep(400, 900);

        // Return to previous tab or close the tab we opened.
        if (openTab == null) {
            // No tab was initially open, close the Stats tab
            gameTabs.closeTab(device, "Stats");
        } else if (!"Stats".equals(openTab)) {
            // Switch back to the originally open tab
            gameTabs.openTab(device, openTab);
            conditionAPI.sleep(250, 600); // Small delay for switching back
        }
    }

    private void doOpenRandomTab(String device) {
        logger.devLog("Antiban game tab");
        // Determine the currently open tab
        String openTab = gameTabs.getOpenTab(device);
        logger.devLog("Current open tab is: " + openTab);

        // List of all tabs
        String[] tabs = {
                "Inventory", "Combat", "Stats", "Quests", "Equip",
                "Prayer", "Magic", "Clan", "Friends", "Account",
                "Logout", "Settings", "Emotes", "Music"
        };

        // Choose a random tab
        String randomTab = tabs[(int) (Math.random() * tabs.length)];
        logger.devLog("Random tab: " + randomTab);

        gameTabs.openTab(device, randomTab);
        conditionAPI.sleep(500, 5000);
        if (openTab == null) {
            gameTabs.closeTab(device, randomTab);
        } else {
            gameTabs.openTab(device, openTab);
        }
    }

    private void doScrollMusic(String device) {
        logger.devLog("Antiban scroll music");
        // Determine the currently open tab
        String openTab = gameTabs.getOpenTab(device);
        logger.devLog("Current open tab is: " + openTab);

        gameTabs.openTab(device, "Music");
        conditionAPI.sleep(500, 1250);

        int randomization = (750 + (int) (Math.random() * (2000 - 750)));
        clientAPI.drag(musicScrollBarRect, musicScrollBarRect, randomization);

        if (openTab == null) {
            gameTabs.closeTab(device, "Quests");
        } else {
            gameTabs.openTab(device, openTab);
        }
    }

    private void doScrollQuests(String device) {
        logger.devLog("Antiban scroll quests");
        // Determine the currently open tab
        String openTab = gameTabs.getOpenTab(device);
        logger.devLog("Current open tab is: " + openTab);

        //gameTabs.openTab(device, "Music");
        gameTabs.openTab(device, "Quests");
        conditionAPI.sleep(500, 1250);

        clientAPI.tap(questSelectionRect, device);
        conditionAPI.sleep(800, 1300);

        int randomization = (750 + (int) (Math.random() * (2000 - 750)));
        clientAPI.drag(questScrollBarRect, questScrollBarRect, randomization);

        if (openTab == null) {
            gameTabs.closeTab(device, "Quests");
        } else {
            gameTabs.openTab(device, openTab);
        }
    }

    private void doScrollAchievements(String device) {
        logger.devLog("Antiban scroll achievements");
        // Determine the currently open tab
        String openTab = gameTabs.getOpenTab(device);
        logger.devLog("Current open tab is: " + openTab);

        gameTabs.openTab(device, "Quests");
        conditionAPI.sleep(500, 1250);

        clientAPI.tap(diarySelectionRect, device);
        conditionAPI.sleep(800, 1300);

        if ((int) (Math.random() * 4) == 0) {
            int randomization = 750 + (int) (Math.random() * (2000 - 750));
            clientAPI.drag(diaryScrollBarRect, diaryScrollBarRect, randomization);
            logger.devLog("Performed scroll with duration: " + randomization + "ms");
        } else {
            logger.devLog("Skipping scroll action this time.");
        }

        clientAPI.tap(questSelectionRect, device);
        conditionAPI.sleep(800, 1300);

        if (openTab == null) {
            gameTabs.closeTab(device, "Quests");
        } else {
            gameTabs.openTab(device, openTab);
        }
    }


    private void doUnequipAndEquip(String device) {
        // Implementation for unequipping and equipping random items
    }

    private void doChangeChatTab(String device) {
        logger.devLog("Antiban changing chat tabs");
        // Check if the chatbox is open
        boolean chatboxOpen = chatbox.isChatboxOpened(device);

        // List of chat tabs
        String[] chatTabs = {
                "All", "Game", "Public", "Private",
                "Channel", "Clan", "Trade"
        };

        if (chatboxOpen) {
            // If chatbox is open, determine the currently active tab
            String currentTab = chatbox.getActiveChatTab(device);

            if (currentTab != null) {
                // Choose a random tab that is not the current tab
                String randomTab;
                do {
                    randomTab = chatTabs[(int) (Math.random() * chatTabs.length)];
                } while (randomTab.equals(currentTab));

                // Switch to the random tab
                chatbox.switchToChatTab(device, randomTab);

                // Switch back to the original tab
                chatbox.switchToChatTab(device, currentTab);
            }
        } else {
            logger.devLog("Open chatbox");
            // If chatbox is not open, open it
            chatbox.openChatboxHelper(device, chatbox.allButton);
            conditionAPI.sleep(300, 750);

            // Choose a random tab
            String randomTab = chatTabs[(int) (Math.random() * chatTabs.length)];

            // Switch to the random tab
            chatbox.switchToChatTab(device, randomTab);
            conditionAPI.sleep(1000, 5000);

            // Close the chatbox
            chatbox.closeChatbox(device);
        }
    }

    private void doWriteCharacter(String device) {
        // Generate a random character: either a letter (a-z) or a number (0-9)
        char randomChar;
        if (Math.random() < 0.5) {
            // Generate a random letter (a-z)
            randomChar = (char) ('a' + (int) (Math.random() * 26));
        } else {
            // Generate a random number (0-9)
            randomChar = (char) ('0' + (int) (Math.random() * 10));
        }

        // Send the generated character as a keystroke
        logger.devLog("Antiban send keystroke: " + randomChar);
        clientAPI.sendKeystroke(String.valueOf(randomChar));
    }

    private void doRotateScreen(String device) {
        logger.devLog("Antiban rotating screen");
        // Define the rectangle dimensions
        Rectangle screenRect = new Rectangle(109, 181, 417, 299);

        // Generate random start and end points within the rectangle
        int startX = screenRect.x + (int) (Math.random() * screenRect.width);
        int startY = screenRect.y + (int) (Math.random() * screenRect.height);
        int endX = screenRect.x + (int) (Math.random() * screenRect.width);
        int endY = screenRect.y + (int) (Math.random() * screenRect.height);

        // Generate a random duration between 500ms and 3500ms
        int durationMS = 500 + (int) (Math.random() * 3001);

        // Create start and end points
        Point startPoint = new Point(startX, startY);
        Point endPoint = new Point(endX, endY);

        // Perform the drag
        clientAPI.drag(startPoint, endPoint, durationMS);
        conditionAPI.sleep(1000, 1400);

        // Now reset back to default view
        login.setCompassAngle(device);
        conditionAPI.wait(() -> login.isCompassNorth(device), 100, 20);
        conditionAPI.sleep(500, 750);
        clientAPI.moveCameraUp();

        // Check if camera is north again
        if (!login.isCompassNorth(device)) {
            logger.debugLog("Camera is not north again... resetting it!", device);
            login.setCompassAngle(device);
            conditionAPI.wait(() -> login.isCompassNorth(device), 100, 20);
            conditionAPI.sleep(500, 750);
            clientAPI.moveCameraUp();
        }
    }

    private void doExtendedAFK(String device) {
        // Generate random sleep times inline (5 to 45 seconds in milliseconds)
        int mintime = 5000 + (int) (Math.random() * 40001); // 5000 to 45000 ms
        int maxtime = mintime + (int) (Math.random() * (45000 - mintime)); // Ensures maxtime > mintime

        logger.debugLog("[AntiBan]: AFK for " + mintime / 1000 + " to " + maxtime / 1000 + " seconds.", device);

        // Simulate AFK behavior
        conditionAPI.sleep(mintime, maxtime);
    }

    public Rectangle getActionButtonLocation(String device) {
        // Get all paths for the images we might need
        String[] imagePaths = {
                "/osrsAssets/menuButtons/actionButton/disabledactionbutton.png",
                "/osrsAssets/menuButtons/actionButton/actiontaptodropdisabled.png",
                "/osrsAssets/menuButtons/actionButton/actionsingletapdisabled.png"
        };

        for (String imagePath : imagePaths) {
            Rectangle result = findBestMatch(imagePath, device);
            if (result != null) {
                logger.devLog("Found the ActionButton location using image: " + imagePath);
                return shrinkRectangle(result, 7);
            }
        }

        return null;
    }

    public void disableTapToDrop(String device) {
        // Check if tap to drop is on, disable if so
        if (isTapToDropEnabled(device)) {
            Rectangle result = findBestMatch("/osrsAssets/menuButtons/actionButton/taptodropbutton.png", device);
            if (result != null) {
                clientAPI.tap(result);
                conditionAPI.wait(() -> isTapToDropEnabled(device), 200, 30);
                closeHotkeymenu(device);
            } else {
                logger.debugLog("Couldn't locate the tap to drop option, make sure it is on your hotkey menu!", device);
            }
        }
    }

    public void disableSingleTap(String device) {
        // Check if single tap is on, otherwise enable
        if (isSingleTapEnabled(device)) {
            Rectangle result = findBestMatch("/osrsAssets/menuButtons/actionButton/singletapbutton.png", device);
            if (result != null) {
                clientAPI.tap(result);
                conditionAPI.wait(() -> !isSingleTapEnabled(device), 200, 30);
                closeHotkeymenu(device);
            } else {
                logger.debugLog("Couldn't locate the single tap option, make sure it is on your hotkey menu!", device);
            }
        }
    }

    public Rectangle findOption(String device, String option) {
        Rectangle foundRect;
        switch (option.toLowerCase()) {  // Use toLowerCase() to make the switch case-insensitive
            case "bank":
                foundRect = objects.getNearest(device, "/osrsAssets/Game/bank.png");
                break;
            case "collect":
                foundRect = objects.getNearest(device, "/osrsAssets/Game/collect.png");
                break;
            case "talk-to":
                foundRect = objects.getNearest(device, "/osrsAssets/Game/talkto.png");
                break;
            case "pickpocket":
                foundRect = objects.getNearest(device, "/osrsAssets/Game/pickpocket.png");
                break;
            case "buy-plank":
                foundRect = objects.getNearest(device, "/osrsAssets/Game/buy-plank.png");
                break;
            case "bloom":
                foundRect = objects.getNearest(device, "/osrsAssets/Game/bloom.png");
                break;
            case "cast-bloom":
                foundRect = objects.getNearest(device, "/osrsAssets/Game/cast-bloom.png");
                break;
            default:
                logger.devLog("Could not locate the " + option + " option on the game screen.");
                return null;
        }
        if (foundRect != null) {
            FeedbackObservables.rectangleObservable.setValue(device, foundRect);
        }
        return foundRect;//empty rect
    }

    private int generateRandomSleepDuration(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    private Rectangle shrinkRectangle(Rectangle rect, int shrinkBy) {
        int newX = rect.x + shrinkBy;
        int newY = rect.y + shrinkBy;
        int newWidth = rect.width - 2 * shrinkBy;
        int newHeight = rect.height - 2 * shrinkBy;

        if (newWidth < 0 || newHeight < 0) {
            logger.devLog("Shrinking the rectangle by " + shrinkBy + " pixels makes it invalid.");
            return null;
        }

        return new Rectangle(newX, newY, newWidth, newHeight);
    }

    private void performWorldSwitch(int world, String emulatorId, boolean isProfileSwitch) {
        String webhookURL = credMgr.getWebhookURL(scriptAccountManager.getSelectedAccount(emulatorId));
        WorldInfo worldInfo = worldHopperUtils.getWorldInfo(String.valueOf(world));

        if (worldInfo != null) {
            Rectangle rectangle = worldInfo.getRectangle();
            String indexPosition = worldInfo.getIndexPosition();
            logger.debugLog("Switching to world: " + world + ".", emulatorId);

            if (webhookURL != null) {
                discordWebhook.sendHopMessage(emulatorId, world);
            }

            clientAPI.tap(worldListButton, emulatorId);
            conditionAPI.wait(() -> isWorldListOpen(emulatorId), 250, 25);

            // Try to tap the world and wait for the login screen
            tapWorld(rectangle, indexPosition, emulatorId);
            conditionAPI.wait(() -> login.onLoginScreen(emulatorId), 250, 25);
            boolean success = login.onLoginScreen(emulatorId);

            if (!success) {
                logger.debugLog("First attempt to switch to world " + world + " failed. Retrying once.", emulatorId);

                // Retry tapping the world and waiting for the login screen
                tapWorld(rectangle, indexPosition, emulatorId);
                conditionAPI.wait(() -> login.onLoginScreen(emulatorId), 250, 25);
                success = login.onLoginScreen(emulatorId);

                if (!success) {
                    logger.debugLog("Hopping to world " + world + " failed after retry. Please create a support thread in Discord.", emulatorId);
                    logger.debugLog("Tapping cancel", emulatorId);
                    clientAPI.tap(796, 10);
                    return;
                }
            }

            if (isProfileSwitch) {
                login.login(emulatorId);
                handleTabStatesAfterHop(emulatorId);
            }
        } else {
            logger.devLog("World " + world + " not found in server cache.");
        }
    }

    private void tapWorld(Rectangle rectangle, String indexPosition, String emulatorId) {
        if (indexPosition.matches("0[1-7].*")) {
            clientAPI.tap(rectangle, emulatorId);
            conditionAPI.sleep(generateRandomSleepDuration(200, 400));
            clientAPI.tap(rectangle, emulatorId);
        } else {
            for (int i = 0; i < 3; i++) {
                clientAPI.tap(arrowRight, emulatorId);
                conditionAPI.sleep(generateRandomSleepDuration(200, 400));
            }
            clientAPI.tap(rectangle, emulatorId);
            conditionAPI.sleep(generateRandomSleepDuration(200, 400));
            clientAPI.tap(rectangle, emulatorId);
        }
    }

    private void handleTabStatesAfterHop(String emulatorId) {
        TabState tabState = cacheManager.getTabState(emulatorId);
        if (tabState != null) {
            logger.devLog("Tabstate cache: gametab state: " + tabState.isInventoryTabOpen() + " magic state: " + tabState.isMagicTabOpen());
            if (tabState.isInventoryTabOpen()) {
                gameTabs.openTab(emulatorId, "Inventory");
                logger.devLog("Opening the inventory tab as the tab was open before hopping worlds!");
            } else if (tabState.isMagicTabOpen()) {
                gameTabs.openTab(emulatorId, "Magic");
                logger.devLog("Opening the magic tab as the tab was open before hopping worlds!");
            }
            cacheManager.removeTabState(emulatorId);
        }
    }

    private void handleTabStatesAntiBan(String emulatorId) {
        TabState tabState = cacheManager.getTabState(emulatorId);
        if (tabState != null) {
            logger.devLog("Tabstate cache: gametab state: " + tabState.isInventoryTabOpen() + " magic state: " + tabState.isMagicTabOpen());
            if (tabState.isInventoryTabOpen()) {
                gameTabs.openTab(emulatorId, "Inventory");
                logger.devLog("Opening the inventory tab as the tab was open before anti-ban actions!");
            } else if (tabState.isMagicTabOpen()) {
                gameTabs.openTab(emulatorId, "Magic");
                logger.devLog("Opening the magic tab as the tab was open before anti-ban actions!");
            }
            cacheManager.removeTabState(emulatorId);
        }
    }

    private void performSwitchNoProfile(int world, String emulatorId) {
        performWorldSwitch(world, emulatorId, false);
    }

    private void performSwitch(String profileName, String emulatorId) {
        Integer worldNumber = getRandomWorld(profileName.replace(".json", ""));
        if (worldNumber != null) {
            performWorldSwitch(worldNumber, emulatorId, false);
            updateHopTimes(profileName, emulatorId);
        } else {
            logger.devLog("No world found for profile: " + profileName);
        }
    }

    private void performHop(String profileName, String emulatorId) {
        Integer worldNumber = getRandomWorld(profileName.replace(".json", ""));
        if (worldNumber != null) {
            cacheManager.setTabState(emulatorId, new TabState(gameTabs.isTabOpen(emulatorId, "Inventory"), gameTabs.isTabOpen(emulatorId, "Magic")));
            logout.logout(emulatorId, true);
            performWorldSwitch(worldNumber, emulatorId, true);
            updateHopTimes(profileName, emulatorId);
        } else {
            logger.devLog("No world found for profile: " + profileName);
        }
    }

    public void performSwitchSpecifiedWorld(String profileName, Integer world, String emulatorId) {
        performWorldSwitch(world, emulatorId, false);
        updateHopTimes(profileName, emulatorId);
    }

    public void performHopSpecifiedWorld(String profileName, Integer world, String emulatorId) {
        cacheManager.setTabState(emulatorId, new TabState(gameTabs.isTabOpen(emulatorId, "Inventory"), gameTabs.isTabOpen(emulatorId, "Magic")));
        logout.logout(emulatorId, true);
        performWorldSwitch(world, emulatorId, true);
        updateHopTimes(profileName, emulatorId);
    }

    public void setCompassAngle(CompassAngle compassAngle, String device) {
        String action;
        switch (compassAngle) {
            case NORTH:
                action = "Look North";
                break;
            case EAST:
                action = "Look East";
                break;
            case SOUTH:
                action = "Look South";
                break;
            case WEST:
                action = "Look West";
                break;
            default:
                logger.devLog("Unknown compass angle.");
                return;  // Exit if the angle is unknown
        }

        // Perform action if the compass angle is valid
        clientAPI.longPressWithMenuAction(compassRect, searchRect, whiteLookColor, action);
        minimapProjections.determineCompassAngle(device);
    }
}