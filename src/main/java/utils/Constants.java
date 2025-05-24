package utils;

import com.google.common.io.Resources;
import helpers.utils.GameObject;
import helpers.utils.Tile;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import org.opencv.core.Rect;

import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;


public class Constants {
    public static final String CLIENT_VERSION = "1.0.5";
    public static final String OS_WIN = "win";
    public static final String OS_MAC = "mac";
    public static final String PLATFORM_TOOLS = "platform-tools";
    public static final String OSRS_APP_NAME = "com.jagex.oldscape.android";
    public static final Rect INVENTORY_RECT = new Rect(593, 220, 202, 273);
    public static final Rectangle INVENTORY_RECTANGLE = new Rectangle(593, 220, 202, 273);
    public static final boolean IS_WINDOWS_USER = System.getProperty("os.name").toLowerCase().contains("win");
    // re-usable images
    public static final Image PLAY_ICON = new Image(Objects.requireNonNull(Resources.class.getResourceAsStream("/assets/icons/play.png")));
    public static final Image PAUSE_ICON = new Image(Objects.requireNonNull(Resources.class.getResourceAsStream("/assets/icons/pause.png")));
    public static final Image STOP_ICON = new Image(Objects.requireNonNull(Resources.class.getResourceAsStream("/assets/icons/stop.png")));
    public static final Image BUG_ICON = new Image(Objects.requireNonNull(Resources.class.getResourceAsStream("/assets/icons/bug_white.png")));
    public static final Image BUG_ICON_WHITE = new Image(Objects.requireNonNull(Resources.class.getResourceAsStream("/assets/icons/bug_grey.png")));
    public static final Image MUFASA_LOGO = new Image(Objects.requireNonNull(Resources.class.getResourceAsStream("/assets/mufasa-transparent.png")));
    public static final Image MUFASA_LOGO_ANIMATED = new Image(Objects.requireNonNull(Resources.class.getResourceAsStream("/assets/mufasa-logo.gif")));
    public static final Image BANK_IMAGE = new Image(Objects.requireNonNull(Resources.class.getResourceAsStream("/osrsAssets/banktab.png")));
    // static files we use a lot of places
    public static final String STYLESHEET = Objects.requireNonNull(Resources.class.getResource("/styles.css")).toExternalForm();
    private static final Random random = new Random();
    public static int CURRENT_ZOOM_LEVEL = 4;
    public static HashMap<GameObject, Set<Integer>> GameObjectColorMap = new HashMap<>();

    // World hop allowed activities
    public static final Set<String> ALLOWED_ACTIVITIES = Set.of(
            "Agility Training",
            "Barbarian Assault",
            "Blast Furnace",
            "Bounty Hunter World",
            "Brimhaven Agility",
            "Brimhaven Agility Arena",
            "Burthorpe Games Room",
            "Castle Wars - Free",
            "Castle Wars 1",
            "Castle Wars 2",
            "Clan Recruitment",
            "Clan Wars - Free-for-all",
            "Falador Party Room",
            "Fishing Trawler",
            "Forestry",
            "Group PvM",
            "Group Skilling",
            "Guardians of the Rift",
            "House Party, Gilded Altar",
            "LMS Casual",
            "LMS Competitive",
            "Mort'ton temple, Rat Pits",
            "Nex FFA",
            "Nightmare of Ashihama",
            "Ourania Altar",
            "Pest Control",
            "Pyramid Plunder",
            "Role-playing",
            "Royal Titans",
            "Soul Wars",
            "Sulliuscep cutting",
            "Tempoross",
            "Theatre of Blood",
            "ToA FFA",
            "Tombs of Amascut",
            "Trade - Free",
            "Trade - Members",
            "Trouble Brewing",
            "TzHaar Fight Pit",
            "Varlamore PvM",
            "Volcanic Mine",
            "Wintertodt",
            "Zalcano",
            "Zeah Runecrafting"
    );

    // other things
    static {
        GameObjectColorMap.put(GameObject.PLAYER, convertHexColorsToInt(Set.of(
                "#fefefe", "#f6f6f6", "#f2f2f2", "#f9f9f9", "#dcdcdc", "#fcfcfc",
                "#e2e2e2", "#d2d2d2", "#c5c5c5", "#ececec"
        )));
        GameObjectColorMap.put(GameObject.NPC, convertHexColorsToInt(Set.of(
                "#fef100", "#f6e900", "#d2c700", "#eadf00", "#e9dc00",
                "#fef000", "#f3e600", "#efe300", "#e6d900",
                "#f0e400", "#eee100", "#faed00", "#f8eb00", "#dace00", "#e4d900",
                "#ebde00", "#d1c51c", "#f7ea00", "#feed00", "#fff000", "#f7e700",
                "#eada00", "#fff400", "#f7e600", "#ecdf00", "#e2d600", "#c5ba00"
        )));
        GameObjectColorMap.put(GameObject.ITEM, convertHexColorsToInt(Set.of(
                "#fe0000", "#ef0704", "#fb0000", "#e50000", "#fc0000", "#db0000",
                "#e30000", "#ec0000", "#c50000", "#e20000", "#d20000"
        )));
    }

    private static Set<Integer> convertHexColorsToInt(Set<String> hexColors) {
        Set<Integer> intColors = new HashSet<>();
        for (String hex : hexColors) {
            intColors.add(Integer.parseInt(hex.substring(1), 16)); // Remove '#' and parse as integer
        }
        return intColors;
    }

    // Helper method to apply small randomization to methods
    public static int[] randomizeCoordinates(int x, int y) {
        int offsetX = random.nextInt(5) - 2; // Random offset between -2 and 2
        int offsetY = random.nextInt(5) - 2; // Random offset between -2 and 2

        int newX = x + offsetX;
        int newY = y + offsetY;

        return new int[]{newX, newY};
    }

    // Random long press duration
    public static int getRandomDuration() {
        // Generates a random duration between 500 and 1000
        return 500 + random.nextInt(501);
    }

    public static String loadDataFromFile(String fileName) {
        try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName);
             BufferedReader reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(inputStream), StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining(System.lineSeparator()));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static int generateRandomDelay(int lowerBound, int upperBound) {
        if (lowerBound > upperBound) {
            int temp = lowerBound;
            lowerBound = upperBound;
            upperBound = temp;
        }
        return lowerBound + random.nextInt(upperBound - lowerBound + 1);
    }

    public static void showDialog(String title, String message, Alert.AlertType alertType) {
        Platform.runLater(() -> {
            Alert alert = new Alert(alertType);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);

            // Apply the stylesheet to the alert's dialog pane
            alert.getDialogPane().getStylesheets().add(STYLESHEET);

            alert.showAndWait();
        });
    }

    public static org.opencv.core.Point[] convertTileArrayToOpenCVPointArray(Tile[] path) {
        org.opencv.core.Point[] opencvPath = new org.opencv.core.Point[path.length];

        for (int i = 0; i < path.length; i++) {
            if (path[i] == null) {
                System.out.println("Null Tile found at index: " + i);
                throw new NullPointerException("Tile at index " + i + " is null.");
            }

            int x = path[i].x(); // Extract x from Tile
            int y = path[i].y(); // Extract y from Tile

            // z is not needed here since OpenCV uses 2D Points
            opencvPath[i] = new org.opencv.core.Point(x, y);
        }

        return opencvPath;
    }

    public static String getPublicIp() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(new URL("http://checkip.amazonaws.com").openStream()))) {
            return in.readLine();
        } catch (Exception e) {
            return "Unable to fetch public IP";
        }
    }
}