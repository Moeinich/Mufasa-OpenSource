package helpers.cacheHandler;

import helpers.adb.ADBHandler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static helpers.cacheHandler.utils.RunescapeCache.getRsCacheLocation;

public class RSPreferenceUpdater {
    private final ADBHandler adbHandler;

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
        String filePath = getRsCacheLocation(deviceId) + "preferences_client.dat";

        // Pull the current file content from the device
        byte[] fileData = pullFileFromDevice(deviceId, filePath);
        if (fileData == null) {
            System.err.println("Failed to fetch preferences file from device " + deviceId);
            return;
        }

        // Convert bytes to a UTF-8 string
        String fileContent = new String(fileData, StandardCharsets.UTF_8);
        StringBuilder updatedContent = new StringBuilder();

        // Process each line and update target keys
        String[] lines = fileContent.split("\\r?\\n");
        for (String line : lines) {
            if (line.startsWith("FpsLimit")) {
                updatedContent.append("FpsLimit 15\n");
            } else if (line.startsWith("Brightness")) {
                updatedContent.append("Brightness 95\n");
            } else if (line.startsWith("DrawDistance")) {
                updatedContent.append("DrawDistance 25\n");
            } else {
                updatedContent.append(line).append("\n");
            }
        }

        // Push the updated file back to the device (overwrite existing file)
        boolean success = pushFileToDevice(deviceId, filePath, updatedContent.toString().getBytes(StandardCharsets.UTF_8));
        if (!success) {
            System.err.println("Failed to upload updated preferences file to device " + deviceId);
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