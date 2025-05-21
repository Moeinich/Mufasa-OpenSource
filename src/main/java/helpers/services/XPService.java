package helpers.services;

import java.util.concurrent.ConcurrentHashMap;

public class XPService {
    private final RuntimeService runtimeService;
    private final ConcurrentHashMap<String, Integer> xpMap = new ConcurrentHashMap<>();

    public XPService(RuntimeService runtimeService) {
        this.runtimeService = runtimeService;
    }

    public String getXPHr(String deviceID) {
        int xpGained = getXP(deviceID);
        long runtimeMilliseconds = runtimeService.getHandler(deviceID).getElapsedTime();

        return calculateXPPerHour(xpGained, runtimeMilliseconds);
    }

    public String getXPLabelFormat(String deviceID) {
        int xpGained = getXP(deviceID);
        long runtimeMilliseconds = runtimeService.getHandler(deviceID).getElapsedTime();
        String xpGainedFormatted = formatXPWithIndicators(xpGained);
        String xpHrFormatted = calculateXPPerHour(xpGained, runtimeMilliseconds);

        return xpGainedFormatted + " (" + xpHrFormatted + "/hr)";
    }

    public void storeXP(String deviceID, int xpAmount) {
        xpMap.put(deviceID, xpAmount);
    }

    public int getXP(String deviceID) {
        return xpMap.getOrDefault(deviceID, 0);
    }

    public boolean hasXPDataForDevice(String device) {
        return xpMap.containsKey(device);
    }

    public synchronized void removeService(String deviceID) {
        xpMap.remove(deviceID);
    }

    private String calculateXPPerHour(int totalXPGained, long totalTimeElapsedMillis) {
        double hoursElapsed = totalTimeElapsedMillis / 3600000.0; // Convert milliseconds to hours
        double xpPerHour = hoursElapsed > 0 ? (totalXPGained / hoursElapsed) : 0;

        if (xpPerHour < 1000) {
            return Math.round(xpPerHour) + ""; // Round off and return as a string
        } else if (xpPerHour < 1000000) {
            return String.format("%.2fk", xpPerHour / 1000);
        } else {
            return String.format("%.2fm", xpPerHour / 1000000);
        }
    }

    private String formatXPWithIndicators(int xp) {
        if (xp < 1000) {
            return xp + ""; // No indicator for values less than 1000
        } else if (xp < 1000000) {
            return String.format("%.2fk", xp / 1000.0);
        } else {
            return String.format("%.2fm", xp / 1000000.0);
        }
    }
}
