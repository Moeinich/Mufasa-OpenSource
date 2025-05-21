package helpers.services;

import java.util.concurrent.ConcurrentHashMap;

public class RuntimeService {
    private final long startTime;
    private final ConcurrentHashMap<String, RuntimeService> runtimeServices = new ConcurrentHashMap<>();

    public RuntimeService() {
        this.startTime = System.currentTimeMillis();
    }

    public long getElapsedTime() {
        // Check if startTime is set
        if (startTime == 0) {
            return 0;
        } else {
            // Calculate elapsed time since the service was created
            return System.currentTimeMillis() - startTime;
        }
    }

    // Static method to get or create a RuntimeService instance for a given ID
    public RuntimeService getHandler(String deviceID) {
        return runtimeServices.computeIfAbsent(deviceID, k -> new RuntimeService());
    }

    // Method to specifically retrieve an existing RuntimeService instance for a given ID
    public RuntimeService getExistingHandler(String deviceID) {
        return runtimeServices.get(deviceID); // Returns null if not found
    }

    public synchronized void removeService(String deviceID) {
        runtimeServices.remove(deviceID);
    }
}
