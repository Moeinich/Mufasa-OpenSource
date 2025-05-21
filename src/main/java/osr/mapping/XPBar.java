package osr.mapping;

import helpers.Logger;
import helpers.OCR.ReadXP;
import helpers.services.XPService;

import java.util.concurrent.ConcurrentHashMap;

public class XPBar {
    private final Logger logger;
    private final ReadXP readXP;
    private final XPService xpService;
    private final ConcurrentHashMap<String, Integer> previousXPMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Integer> totalXPGainedMap = new ConcurrentHashMap<>();

    public XPBar(Logger logger, ReadXP readXP, XPService xpService) {
        this.logger = logger;
        this.readXP = readXP;
        this.xpService = xpService;
    }


    public int readXP(String device) {
        try {
            int currentXP = readXP.readXP(device);
    
            // Check for invalid initial XP values (0 or -1) especially for the first read
            if (!isValidInitialXP(currentXP)) {
                return -1;  // Return -1 to indicate invalid initial XP
            }
    
            Integer previousXP = previousXPMap.get(device);
            if (previousXP == null) {
                previousXPMap.put(device, currentXP);
                return currentXP;  // Return current XP as it's the first valid read
            }
    
            if (isValidXP(currentXP) && isHigher(currentXP, previousXP)) {
                int xpGained = calculateXPGained(currentXP, previousXP);
                
                totalXPGainedMap.merge(device, xpGained, Integer::sum);  // Safely update total XP gained
                String formattedTotalXPGained = formatXPWithIndicators(totalXPGainedMap.get(device));
    
                logger.devLog("XP Gained: " + xpGained + " | Total XP Gained: " + formattedTotalXPGained + " | Current XP: " + currentXP);
                xpService.storeXP(device, totalXPGainedMap.get(device));  // Store the XP for the XP service
    
                previousXPMap.put(device, currentXP);  // Safely update previousXP
            }
    
            return previousXP != null ? previousXP : -1;
        } catch (NumberFormatException e) {
            logger.devLog("Invalid XP format encountered: " + e.getMessage());
            return -1;  // Return -1 or another specific error code upon parse failure
        }
    }  

    private boolean isValidInitialXP(int xp) {
        // Check for invalid initial XP values (0 or -1)
        return xp > 0;
    }

    private boolean isValidXP(int xp) {
        // Check for valid XP values (greater than 0)
        return xp > 0;
    }

    private boolean isHigher(int currentXP, int previousXP) {
        return currentXP > previousXP;
    }
    
    private int calculateXPGained(int currentXP, int previousXP) {
        return currentXP - previousXP;
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

    public void removeDeviceData(String device) {
        previousXPMap.remove(device); // Remove the last known XP
        totalXPGainedMap.remove(device); // Remove the total XP gained
    }
}