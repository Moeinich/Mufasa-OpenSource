package helpers.cacheHandler;

import helpers.adb.ADBHandler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static helpers.cacheHandler.utils.RunescapeCache.getRsCacheLocation;
import static helpers.cacheHandler.utils.RunescapeCache.getRsPreferencesLocation;

public class RSPreferenceUpdater {
    private final ADBHandler adbHandler;
    public static final int DEFAULT_WORLD = 308;

    public RSPreferenceUpdater(ADBHandler adbHandler) {
        this.adbHandler = adbHandler;
    }


    /**
     * Updates the preferences_client.dat file on the device by replacing
     * FpsLimit, Brightness, and DrawDistance values with:
     *   Brightness 95
     *   DrawDistance 25
     *   FpsLimit 15
     *
     * @param deviceId the unique device identifier (e.g., "emulator-5554")
     */
    public void updatePreferencesFile(String deviceId) {
        System.out.println("Writing full preferences file for device: " + deviceId);
        String filePath = getRsPreferencesLocation(deviceId) + "preferences_client.dat";

        String fileContent = String.join("\n", new String[] {
                "AntiAliasingSampleLevel 0",
                "Brightness 55",
                "CachedUserName",
                "DefaultWorldId " + DEFAULT_WORLD,
                "DisplayBuildInfo 0",
                "DisplayFps 15",
                "DrawDistance 25",
                "FpsLimit 15",
                "Fullscreen 0",
                "HapticFeedbackStrength 0",
                "HideUserName 0",
                "IsSfx8Bit 1",
                "LastWorldId 0",
                "MasterVolume 0",
                "MuteTitleScreen 0",
                "PluginSafeMode 0",
                "ScreenshotPath /storage/emulated/0/Android/data/com.jagex.oldscape.android/files",
                "SideBarsWidth 0",
                "TermsAndPrivacy -1",
                "TitleVolume 0",
                "UIQuality 1",
                "WindowHeight 540",
                "WindowMode 2",
                "WindowSavedPosition",
                "WindowTopmost 0",
                "WindowWidth 894"
        }) + "\n";

        boolean success = pushFileToDevice(deviceId, filePath, fileContent.getBytes(StandardCharsets.UTF_8));
        if (!success) {
            System.err.println("Failed to write preferences file to device " + deviceId);
        }
    }

    /**
     * Pulls a file from the device using ADB.
     *
     * @param deviceId   the unique device identifier
     * @param remotePath the full path to the file on the device
     * @return a byte array containing the file data, or null if an error occurred
     */
    private byte[] pullFileFromDevice(String deviceId, String remotePath) {
        String command = "exec-out cat " + remotePath;
        try (InputStream is = adbHandler.runADBCommandAsStream(command, deviceId);
             ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {

            byte[] data = new byte[4096];
            int nRead;
            while ((nRead = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            return buffer.toByteArray();
        } catch (IOException e) {
            System.err.println("Failed to pull file " + remotePath + " for device " + deviceId + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Pushes file data to the device, overwriting the file at the given path.
     * Assumes that adbHandler provides a method for pushing file data.
     *
     * @param deviceId   the unique device identifier
     * @param remotePath the full path to the file on the device
     * @param data       the byte array data to push
     * @return true if the file was pushed successfully; false otherwise
     */
    private boolean pushFileToDevice(String deviceId, String remotePath, byte[] data) {
        try {
            // Assuming ADBHandler has a method pushFileToDevice(remotePath, data, deviceId)
            adbHandler.pushFileToDevice(remotePath, data, deviceId);
            return true;
        } catch (IOException e) {
            System.err.println("Failed to push file " + remotePath + " to device " + deviceId + ": " + e.getMessage());
            return false;
        }
    }
}