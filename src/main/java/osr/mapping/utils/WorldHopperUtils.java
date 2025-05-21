package osr.mapping.utils;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import helpers.GetGameView;
import helpers.Logger;
import helpers.Color.ColorFinder;
import helpers.Color.utils.ColorPointPair;
import helpers.utils.GameObject;
import helpers.utils.Tile;
import javafx.scene.shape.Circle;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import osr.mapping.Minimap;
import osr.utils.MatchedObjects;
import osr.walker.Walker;
import osr.walker.utils.TranslatePosition;
import utils.SystemUtils;

import java.awt.*;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static utils.Constants.ALLOWED_ACTIVITIES;
import static utils.Constants.GameObjectColorMap;


public class WorldHopperUtils {
    // Yellow color: #fef21b
    private static int centerX = -1;
    // constants for the world locations
    private final int START_X = 131;  // X-coordinate of the start point of the first column
    private final int START_Y = 37;   // Y-coordinate of the start point of the first column
    private final int BUTTON_WIDTH = 78;
    private final int BUTTON_HEIGHT = 14;
    private final int VERTICAL_SPACING = 5;
    private final int HORIZONTAL_SPACING = 15;
    private final int COLUMNS_IN_FIRST_IMAGE = 7;
    private final int COLUMN_START_WORLDHOP2 = 4;
    private final Logger logger;
    private final MinimapProjections minimapProjections;
    private final Minimap minimap;
    private final ColorFinder colorFinder;
    private final TranslatePosition translatePosition;
    private final Walker walker;
    private final GetGameView getGameView;
    private final Cache<String, Rectangle> minimapPlayerPositionCache = Caffeine.newBuilder()
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .build();
    private final Color white = Color.WHITE;
    private final Color yellow = Color.decode("#fef100");
    private final Color red = Color.decode("#fe0000");
    private final Color black = Color.BLACK;
    private int x2 = -1;
    private int topMiddleY = -1;
    private int y2 = -1;
    private int blackCheckY = -1;
    private int whiteCheckX = -1;
    private int whiteCheckY1 = -1;
    private int whiteCheckY2 = -1;
    private List<ColorPointPair> topColorChecks;
    private List<ColorPointPair> whiteChecks;
    private List<ColorPointPair> blackChecks;

    public WorldHopperUtils(Logger logger, MinimapProjections minimapProjections, Minimap minimap, ColorFinder colorFinder, TranslatePosition translatePosition, Walker walker, GetGameView getGameView) {
        this.logger = logger;
        this.minimapProjections = minimapProjections;
        this.minimap = minimap;
        this.colorFinder = colorFinder;
        this.translatePosition = translatePosition;
        this.walker = walker;
        this.getGameView = getGameView;

        gatherAndSaveLatestWorldList();
    }

    private Rectangle getCenterRectangle(Circle minimapMat) {
        int centerX = (int) (minimapMat.getCenterX());
        int centerY = (int) (minimapMat.getCenterY());

        // Define the width and height of the region (4 pixels wide, 5 pixels tall)
        int width = 4;
        int height = 5;

        // Calculate the top-left corner of the rectangle
        int x1 = centerX - width / 2;
        int y1 = centerY - height / 2;

        // Create and return a rectangle with these coordinates and size
        return new Rectangle(x1, y1, width, height);
    }

    // method to return a list of profile names
    public List<String> getProfileNames() {
        List<String> profileNames = new ArrayList<>();
        Path dir = Paths.get(SystemUtils.getWorldHopperFolderPath());
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.json")) {
            for (Path entry : stream) {
                String fileName = entry.getFileName().toString();
                if (!fileName.equals("ServerList.json") && !fileName.equals("LatestServerList.json")) {
                    profileNames.add(fileName.replace(".json", ""));
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading profile names: " + e.getMessage());
        }
        return profileNames;
    }

    public List<Server> buildServerCache() {
        List<Server> servers = new ArrayList<>();
        Gson gson = new Gson();
        Type serverListType = new TypeToken<List<Map<String, Object>>>() {
        }.getType();
        String filePath = SystemUtils.getWorldHopperFolderPath() + "/ServerList.json";

        try (FileReader reader = new FileReader(filePath)) {
            List<Map<String, Object>> serverList = gson.fromJson(reader, serverListType);

            // First loop for the first hop page
            addWorldsToCache(serverList, servers, 1, COLUMNS_IN_FIRST_IMAGE);

            // Second loop for the second hop page
            addWorldsToCache(serverList, servers, COLUMN_START_WORLDHOP2, COLUMNS_IN_FIRST_IMAGE);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return servers;
    }

    private void addWorldsToCache(List<Map<String, Object>> serverList, List<Server> servers, int startColumn, int maxColumns) {
        for (Map<String, Object> serverObj : serverList) {
            int world = ((Number) serverObj.get("World")).intValue();
            String indexPosition = (String) serverObj.get("IndexPosition");

            String[] indexParts = indexPosition.split("-");
            int column = Integer.parseInt(indexParts[0]) - startColumn;
            int row = Integer.parseInt(indexParts[1]) - 1;

            if (column < 0 || column >= maxColumns) {
                continue;
            }

            int x = START_X + column * (BUTTON_WIDTH + HORIZONTAL_SPACING);
            int y = START_Y + row * (BUTTON_HEIGHT + VERTICAL_SPACING);
            Rectangle rectangle = new Rectangle(x, y, BUTTON_WIDTH, BUTTON_HEIGHT);

            servers.add(new Server(world, indexPosition, rectangle));
        }
    }

    public WorldInfo getWorldInfo(String worldNumber) {
        List<Server> serverCache = buildServerCache(); // Ensure the cache is built
        for (Server server : serverCache) {
            if (Integer.toString(server.world).equals(worldNumber)) {
                return new WorldInfo(server.rectangle, server.indexPosition);
            }
        }
        return null; // Return null if the world number is not found in the cache
    }

    // White dot hopping things
    public boolean isPlayersAround(String device) {
        if (checkPlayerUnderUs(device)) {
            return true;
        }

        MatchedObjects whiteDotsAmount = getWhiteDots(minimap.getMinimap(device));
        return whiteDotsAmount != null && whiteDotsAmount.getRectangles() != null && !whiteDotsAmount.getRectangles().isEmpty();
    }
    public int countPlayersAround(String device) {
        MatchedObjects whiteDotsAmount = getWhiteDots(minimap.getMinimap(device));
        return whiteDotsAmount.getRectangles().size();
    }

    public boolean isPlayersAround(String device, Tile tileToCheck, int radius) {
        Mat radiusAroundMinimapPoint = null;
        try {
            // Translate the world coordinates to minimap coordinates
            Point minimapPoint = translatePosition.worldToMM(
                    tileToCheck,
                    walker.getPlayerPosition(device).getWorldCoordinates(device).getTile(),
                    device
            );

            // Define the region of interest (ROI) around the minimap point
            int x = minimapPoint.x - radius;
            int y = minimapPoint.y - radius;
            int width = radius * 2;
            int height = radius * 2;

            // Ensure ROI is within bounds of the minimap
            Rect rect = new Rect(
                    Math.max(x, 0),
                    Math.max(y, 0),
                    Math.min(width, 894 - x),
                    Math.min(height, 540 - y)
            );

            // Extract the submat
            radiusAroundMinimapPoint = getGameView.getSubmat(device, rect);

            // Analyze the white dots within the ROI
            MatchedObjects whiteDotsAmount = getWhiteDots(radiusAroundMinimapPoint);

            // Check if there is a player under us
            if (checkPlayerUnderUs(device)) {
                return true;
            }

            // Check if there are more than 1 white dot in the ROI
            return whiteDotsAmount != null &&
                    whiteDotsAmount.getRectangles() != null &&
                    whiteDotsAmount.getRectangles().size() > 1;

        } catch (Exception e) {
            // Handle any exceptions that occur during processing
            e.printStackTrace();
            return false; // Default to false in case of error
        } finally {
            // Ensure the Mat object is released to avoid memory leaks
            if (radiusAroundMinimapPoint != null) {
                radiusAroundMinimapPoint.release();
            }
        }
    }

    public boolean isPlayerAt(String device, Tile checkTile) {
        // Translate the checkTile to minimap coordinates
        Point minimapPoint = translatePosition.worldToMM(
                checkTile,
                walker.getPlayerPosition(device).getWorldCoordinates(device).getTile(),
                device
        );

        if (minimapPoint == null) {
            logger.print("Could not find minimap point for the provided tile!");
            return false;
        }

        // Create a 4x4 rectangle starting from the top-left corner at minimapPoint
        Rectangle rect = new Rectangle(minimapPoint.x, minimapPoint.y, 4, 4);

        // Retrieve the set of int colors for the PLAYER object
        Set<Integer> playerColors = GameObjectColorMap.get(GameObject.PLAYER);
        if (playerColors == null || playerColors.isEmpty()) {
            logger.print("No color data found for GameObject.PLAYER");
            return false;
        }

        return colorFinder.isAnyColorInRect(device, playerColors, rect, 3);
    }

    public boolean checkPlayerUnderUs(String device) {
        if (topColorChecks == null) {
            Rectangle minimapPlayerPosition = minimapCenterPositionCache(device);
            if (minimapPlayerPosition == null) {
                logger.print("Couldnt find minimap position!");
                return false;
            }

            centerX = minimapPlayerPosition.x + minimapPlayerPosition.width / 2;
            x2 = minimapPlayerPosition.x + minimapPlayerPosition.width;
            topMiddleY = minimapPlayerPosition.y;
            y2 = minimapPlayerPosition.y + minimapPlayerPosition.height;
            blackCheckY = y2 - 1;
            whiteCheckX = x2 - 1;
            whiteCheckY1 = y2 - 2;
            whiteCheckY2 = y2 - 3;

            initializeChecks();
        }

        return colorFinder.performPlayerCheck(device, blackChecks, topColorChecks, whiteChecks);
    }

    private MatchedObjects getWhiteDots(Mat checkArea) {
        MatchedObjects matchedObjects;
        if (checkArea != null) {
            matchedObjects = minimapProjections.findAndMarkColorAreas(checkArea, GameObjectColorMap.get(GameObject.PLAYER));
        } else {
            logger.devLog("Could not retrieve the white dots.");
            return null;
        }

        if (matchedObjects != null && matchedObjects.getRectangles() != null) {
            logger.devLog("Found " + matchedObjects.getRectangles().size() + " white dots");
        } else {
            logger.devLog("No white dots found.");
        }

        return matchedObjects;
    }

    public Rectangle minimapCenterPositionCache(String device) {
        // Attempt to retrieve the cached Rectangle
        Rectangle cachedRectangle = minimapPlayerPositionCache.getIfPresent(device);
        if (cachedRectangle != null) {
            return cachedRectangle;
        }

        // Otherwise, calculate the new RegionBox
        Circle minimapMat = minimap.getMinimapPosition(device);
        if (minimapMat == null) {
            return null;
        }

        Rectangle playerDotRegion = getCenterRectangle(minimapMat);

        // Cache the RegionBox with the device ID as the key
        minimapPlayerPositionCache.put(device, playerDotRegion);

        // Return the new RegionBox
        return playerDotRegion;
    }

    public void gatherAndSaveLatestWorldList() {
        String url = "https://oldschool.runescape.com/slu?order=WMLPA";
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .build();

            List<Map<String, Object>> serverList = new ArrayList<>();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 && response.body() != null) {
                Document doc = Jsoup.parse(response.body());
                Elements rows = doc.select("tr.server-list__row");
                if (rows != null) {
                    for (Element row : rows) {
                        if (row != null) {
                            Element link = row.selectFirst("a.server-list__world-link");
                            Element countryCell = row.selectFirst("td.server-list__row-cell--country");
                            Elements activityCells = row.select("td.server-list__row-cell");
                            if (link != null && countryCell != null && activityCells.size() > 4) {
                                String id = link.id();
                                if (id != null && id.split("-").length > 2) {
                                    int worldId = Integer.parseInt(id.split("-")[2]);
                                    String country = countryCell.text().trim();
                                    String activity = activityCells.get(4).text().trim();
                                    String serverType;

                                    if (activity.contains("PvP") || activity.contains("Deadman") || activity.contains("PK") || activity.contains("Beta World")) {
                                        serverType = "PvP";
                                    } else {
                                        Element serverTypeCell = row.selectFirst("td.server-list__row-cell--type");
                                        serverType = serverTypeCell.selectFirst("span.pvp-icon") != null ? "PvP" : serverTypeCell.text().trim();
                                    }

                                    int column = (serverList.size() / 24) + 1;
                                    int position = (serverList.size() % 24) + 1;
                                    String indexPosition = String.format("%02d-%02d", column, position);

                                    Map<String, Object> serverInfo = new HashMap<>();
                                    serverInfo.put("World", worldId);
                                    serverInfo.put("Country", country);
                                    serverInfo.put("Type", serverType);
                                    serverInfo.put("Activity", activity);
                                    serverInfo.put("IndexPosition", indexPosition);
                                    serverList.add(serverInfo);
                                }
                            }
                        }
                    }
                } else {
                    System.out.println("No server list data found on the webpage.");
                }
            } else {
                System.out.println("Failed to retrieve the server list web page. Status code: " + response.statusCode());
            }

            // Save the latest server list to LatestServerList.json
            Path latestServerListPath = Paths.get(SystemUtils.getWorldHopperFolderPath() + "/LatestServerList.json");
            Path serverListPath = Paths.get(SystemUtils.getWorldHopperFolderPath() + "/ServerList.json");
            Gson gson = new GsonBuilder().setPrettyPrinting().create();

            try (FileWriter file = new FileWriter(latestServerListPath.toFile())) {
                gson.toJson(serverList, file);
                System.out.println("LatestServerList.json updated.");
            }

            // Check if ServerList.json exists, if not write to it
            if (!Files.exists(serverListPath)) {
                try (FileWriter file = new FileWriter(serverListPath.toFile())) {
                    gson.toJson(serverList, file);
                    System.out.println("ServerList.json created and updated.");
                }
            }

        } catch (IOException | InterruptedException | NumberFormatException e) {
            System.out.println("Error occurred during server list processing: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public int getRandomWorldToHopTo() {
        try {
            // Load the server list from ServerList.json
            Path serverListPath = Paths.get(SystemUtils.getWorldHopperFolderPath() + "/LatestServerList.json");
            String serverContent = new String(Files.readAllBytes(serverListPath));
            JSONArray serverList = new JSONArray(serverContent);

            // Get the default settings
            JSONObject defaultSettings = createDefaultSettings();
            JSONObject filterSettings = defaultSettings.getJSONObject("Filter settings");

            JSONObject locations = filterSettings.getJSONObject("Locations");
            JSONObject activities = filterSettings.getJSONObject("Activities");
            JSONObject type = filterSettings.getJSONObject("Type");

            // Add a default value for activities not defined in the UI
            String defaultActivity = "Normal/Others";

            // Filter the server list based on the default filter settings
            JSONArray filteredWorldNumbers = new JSONArray();
            for (int i = 0; i < serverList.length(); i++) {
                JSONObject server = serverList.getJSONObject(i);
                String activityKey = server.getString("Activity").strip();

                // Skip if the activity is not allowed
                if (!activityKey.equals("-") && !ALLOWED_ACTIVITIES.contains(activityKey)) {
                    continue;
                }

                boolean locationEnabled = locations.getBoolean(server.getString("Country").strip());
                boolean typeMatch = type.getBoolean(server.getString("Type"));
                boolean activityMatch = activities.has(activityKey) ?
                        activities.getBoolean(activityKey) :
                        activities.getBoolean(defaultActivity);

                if (locationEnabled && typeMatch && activityMatch) {
                    filteredWorldNumbers.put(server.getInt("World"));
                }
            }

            // If there are no filtered worlds, return an indication or throw an exception
            if (filteredWorldNumbers.isEmpty()) {
                throw new IllegalStateException("No worlds available after applying default filters.");
            }

            // Select a random world from the filtered list
            Random random = new Random();
            int randomIndex = random.nextInt(filteredWorldNumbers.length());

            return filteredWorldNumbers.getInt(randomIndex);
        } catch (IOException | JSONException e) {
            System.out.println("Error fetching random world: " + e.getMessage());
            return -1; // Or throw a RuntimeException if you prefer
        }
    }

    private JSONObject createDefaultSettings() {
        JSONObject filterSettings = new JSONObject();
        JSONObject locations = new JSONObject();
        locations.put("United Kingdom", true);
        locations.put("United States", true);
        locations.put("Germany", true);
        locations.put("Australia", false);

        JSONObject activities = new JSONObject();
        activities.put("Blast Furnace", true);
        activities.put("Tempoross", true);
        activities.put("Wintertodt", true);
        activities.put("500 skill total", false);
        activities.put("750 skill total", false);
        activities.put("1250 skill total", false);
        activities.put("1500 skill total", false);
        activities.put("1750 skill total", false);
        activities.put("2000 skill total", false);
        activities.put("2200 skill total", false);
        activities.put("Normal/Others", true);

        JSONObject type = new JSONObject();
        type.put("Free", false);
        type.put("Members", true);
        type.put("PvP", false);

        filterSettings.put("Locations", locations);
        filterSettings.put("Activities", activities);
        filterSettings.put("Type", type);

        JSONObject defaultSettings = new JSONObject();
        defaultSettings.put("Filter settings", filterSettings);
        defaultSettings.put("Filtered worlds", new JSONArray()); // Empty array for filtered worlds
        defaultSettings.put("Hop Timer", 8 + (int) (Math.random() * 5)); // Randomly choose between 8 and 12

        return defaultSettings;
    }

    public Rectangle getWorldRectangle(String worldAsString) {
        int world = Integer.parseInt(worldAsString);
        List<Server> serverCache = buildServerCache(); // or true if it's WorldHop2

        for (Server server : serverCache) {
            if (server.world == world) {
                // Print for debugging purposes
                System.out.println("World: " + server.world);
                System.out.println("IndexPosition: " + server.indexPosition);
                System.out.println("Rectangle: " + server.rectangle);

                return server.rectangle;
            }
        }

        System.out.println("World " + world + " not found in cache.");
        return null;
    }

    private void initializeChecks() {
        if (blackChecks == null) {
            blackChecks = List.of(
                    new ColorPointPair(black, new Point(centerX - 1, blackCheckY), 10),
                    new ColorPointPair(black, new Point(centerX, blackCheckY), 10)
            );
        }

        if (topColorChecks == null) {
            topColorChecks = List.of(
                    new ColorPointPair(yellow, new Point(centerX, topMiddleY), 0),
                    new ColorPointPair(red, new Point(centerX, topMiddleY), 0)
            );
        }

        if (whiteChecks == null) {
            whiteChecks = List.of(
                    new ColorPointPair(white, new Point(whiteCheckX, whiteCheckY1), 0),
                    new ColorPointPair(white, new Point(whiteCheckX, whiteCheckY2), 0)
            );
        }
    }

    static class Server {
        int world;
        String indexPosition;
        Rectangle rectangle;

        public Server(int world, String indexPosition, Rectangle rectangle) {
            this.world = world;
            this.indexPosition = indexPosition;
            this.rectangle = rectangle;
        }
    }

    public static class WorldInfo {
        Rectangle rectangle;
        String indexPosition;

        public WorldInfo(Rectangle rectangle, String indexPosition) {
            this.rectangle = rectangle;
            this.indexPosition = indexPosition;
        }

        // Getters (optional)
        public Rectangle getRectangle() {
            return rectangle;
        }

        public String getIndexPosition() {
            return indexPosition;
        }
    }
}