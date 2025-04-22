package helpers.cacheHandler;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import helpers.adb.ADBHandler;
import helpers.cacheHandler.utils.RSCacheData;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import static helpers.cacheHandler.utils.RunescapeCache.getRsCacheLocation;

public class RSCacheHandler {
    private final ADBHandler adbHandler;
    private final Cache<String, RSCacheData> deviceCache;

    public RSCacheHandler(ADBHandler adbHandler) {
        this.adbHandler = adbHandler;
        this.deviceCache = Caffeine.newBuilder()
                .expireAfterAccess(30, TimeUnit.MINUTES)
                .build();
    }

    /**
     * Retrieves the RSCacheData for the given device from the cache.
     *
     * @param deviceId the unique device identifier (e.g., "emulator-5554")
     * @return the RSCacheData containing all cache files, or null if not present
     */
    public RSCacheData getCacheForDevice(String deviceId) {
        return deviceCache.getIfPresent(deviceId);
    }

    /**
     * Updates the complete RSCacheData for the device by fetching all individual files
     * and then storing the assembled RSCacheData in the device cache.
     *
     * @param deviceId the unique device identifier
     */
    public void updateCacheForDevice(String deviceId) {
        RSCacheData data = new RSCacheData();
        try {
            // Main cache file (.dat2)
            data.setMainCacheDat2(fetchFile(deviceId, getRsCacheLocation(deviceId) + ".dat2"));

            // Individual index files:
            data.setSkeleton(fetchFile(deviceId, getRsCacheLocation(deviceId) + ".idx0"));
            data.setSkin(fetchFile(deviceId, getRsCacheLocation(deviceId) + ".idx1"));
            data.setConfig(fetchFile(deviceId, getRsCacheLocation(deviceId) + ".idx2"));
            data.setInterfaceData(fetchFile(deviceId, getRsCacheLocation(deviceId) + ".idx3"));
            data.setLandscape(fetchFile(deviceId, getRsCacheLocation(deviceId) + ".idx5"));
            data.setModels(fetchFile(deviceId, getRsCacheLocation(deviceId) + ".idx7"));
            data.setSprites(fetchFile(deviceId, getRsCacheLocation(deviceId) + ".idx8"));
            data.setTexture(fetchFile(deviceId, getRsCacheLocation(deviceId) + ".idx9"));
            data.setClientScripts(fetchFile(deviceId, getRsCacheLocation(deviceId) + ".idx12"));
            data.setFonts(fetchFile(deviceId, getRsCacheLocation(deviceId) + ".idx13"));
            // Put the assembled data into the cache
            deviceCache.put(deviceId, data);
        } catch (Exception e) {
            System.err.println("Failed to update RS cache data for device " + deviceId + ": " + e.getMessage());
        }
    }

    // Individual update methods for each cache file:

    public void updateMainCacheDat2(String deviceId) {
        RSCacheData data = getOrCreateRSCacheData(deviceId);
        data.setMainCacheDat2(fetchFile(deviceId, getRsCacheLocation(deviceId) + ".dat2"));
        deviceCache.put(deviceId, data);
    }

    public void updateSkeleton(String deviceId) {
        RSCacheData data = getOrCreateRSCacheData(deviceId);
        data.setSkeleton(fetchFile(deviceId, getRsCacheLocation(deviceId) + ".idx0"));
        deviceCache.put(deviceId, data);
    }

    public void updateSkin(String deviceId) {
        RSCacheData data = getOrCreateRSCacheData(deviceId);
        data.setSkin(fetchFile(deviceId, getRsCacheLocation(deviceId) + ".idx1"));
        deviceCache.put(deviceId, data);
    }

    public void updateConfig(String deviceId) {
        RSCacheData data = getOrCreateRSCacheData(deviceId);
        data.setConfig(fetchFile(deviceId, getRsCacheLocation(deviceId) + ".idx2"));
        deviceCache.put(deviceId, data);
    }

    public void updateInterfaceData(String deviceId) {
        RSCacheData data = getOrCreateRSCacheData(deviceId);
        data.setInterfaceData(fetchFile(deviceId, getRsCacheLocation(deviceId) + ".idx3"));
        deviceCache.put(deviceId, data);
    }

    public void updateLandscape(String deviceId) {
        RSCacheData data = getOrCreateRSCacheData(deviceId);
        data.setLandscape(fetchFile(deviceId, getRsCacheLocation(deviceId) + ".idx5"));
        deviceCache.put(deviceId, data);
    }

    public void updateModels(String deviceId) {
        RSCacheData data = getOrCreateRSCacheData(deviceId);
        data.setModels(fetchFile(deviceId, getRsCacheLocation(deviceId) + ".idx7"));
        deviceCache.put(deviceId, data);
    }

    public void updateSprites(String deviceId) {
        RSCacheData data = getOrCreateRSCacheData(deviceId);
        data.setSprites(fetchFile(deviceId, getRsCacheLocation(deviceId) + ".idx8"));
        deviceCache.put(deviceId, data);
    }

    public void updateTexture(String deviceId) {
        RSCacheData data = getOrCreateRSCacheData(deviceId);
        data.setTexture(fetchFile(deviceId, getRsCacheLocation(deviceId) + ".idx9"));
        deviceCache.put(deviceId, data);
    }

    public void updateClientScripts(String deviceId) {
        RSCacheData data = getOrCreateRSCacheData(deviceId);
        data.setClientScripts(fetchFile(deviceId, getRsCacheLocation(deviceId) + ".idx12"));
        deviceCache.put(deviceId, data);
    }

    public void updateFonts(String deviceId) {
        RSCacheData data = getOrCreateRSCacheData(deviceId);
        data.setFonts(fetchFile(deviceId, getRsCacheLocation(deviceId) + ".idx13"));
        deviceCache.put(deviceId, data);
    }

    /**
     * Helper method to retrieve existing RSCacheData from cache or create a new instance if not present.
     *
     * @param deviceId the unique device identifier
     * @return RSCacheData instance
     */
    private RSCacheData getOrCreateRSCacheData(String deviceId) {
        RSCacheData data = deviceCache.getIfPresent(deviceId);
        if (data == null) {
            data = new RSCacheData();
        }
        return data;
    }

    /**
     * A helper method to fetch a file from the device using the provided remote path.
     *
     * @param deviceId   the unique device identifier
     * @param remotePath the full path to the file on the device
     * @return a byte array containing the file data, or null if an error occurred
     */
    private byte[] fetchFile(String deviceId, String remotePath) {
        try {
            return pullFileFromDevice(deviceId, remotePath);
        } catch (IOException e) {
            System.err.println("Failed to fetch file " + remotePath + " for device " + deviceId + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Uses ADB to pull a file from the device by streaming its contents.
     * Executes the command "exec-out cat <remotePath>".
     *
     * @param deviceId   the unique device identifier
     * @param remotePath the full path to the file on the device
     * @return a byte array containing the file data
     * @throws IOException if an I/O error occurs during retrieval
     */
    private byte[] pullFileFromDevice(String deviceId, String remotePath) throws IOException {
        String command = "exec-out cat " + remotePath;
        try (InputStream is = adbHandler.runADBCommandAsStream(command, deviceId);
             ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {

            byte[] data = new byte[4096];
            int nRead;
            while ((nRead = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            return buffer.toByteArray();
        }
    }
}