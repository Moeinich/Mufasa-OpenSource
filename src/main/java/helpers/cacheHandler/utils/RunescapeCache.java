package helpers.cacheHandler.utils;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import static utils.Constants.IS_WINDOWS_USER;

public class RunescapeCache {
    private static final Cache<String, String> RS_CACHE_LOCATION_CACHE = Caffeine.newBuilder().build();

    /**
     * Retrieves the RS cache location for the given device.
     * It checks three possible locations on the connected device via ADB.
     * If one of them exists, it is cached and returned. If none exist,
     * a default path is returned based on the IS_WINDOWS_USER flag.
     *
     * @param deviceIdentifier A unique identifier for the device.
     * @return The RS cache location string.
     */
    public static String getRsCacheLocation(String deviceIdentifier) {
        return RS_CACHE_LOCATION_CACHE.get(deviceIdentifier, key -> {
            String[] possiblePaths = new String[] {
                    "/sdcard/Android/data/com.jagex.oldscape.android/files/Old School Runescape/data/",
                    "/0/android/data/com.jagex.oldscape.android/files/Old School Runescape/data/",
                    "storage/self/primary/android/data/com.jagex.oldscape.android/files/Old School Runescape/data/"
            };

            for (String path : possiblePaths) {
                if (checkPathExists(path)) {
                    return path;
                }
            }

            // If none of the paths exist, fall back to a default.
            return IS_WINDOWS_USER
                    ? "/storage/emulated/0/android/data/com.jagex.oldscape.android/files/Old School Runescape/data/"
                    : "storage/self/primary/android/data/com.jagex.oldscape.android/files/Old School Runescape/";
        });
    }

    /**
     * Uses ADB to check if a directory exists on the connected device.
     *
     * @param path The directory path to check.
     * @return true if the directory exists, false otherwise.
     */
    private static boolean checkPathExists(String path) {
        try {
            // The command echoes "exists" if the directory is present.
            String command = "if [ -d \"" + path + "\" ]; then echo exists; fi";
            ProcessBuilder pb = new ProcessBuilder("adb", "shell", command);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String output = reader.readLine();
            process.waitFor();

            return output != null && output.trim().equals("exists");
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return false;
    }
}