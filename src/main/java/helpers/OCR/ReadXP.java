package helpers.OCR;

import helpers.CacheManager;
import helpers.Color.ColorFinder;
import helpers.GetGameView;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

public class ReadXP {
    private final CacheManager cacheManager;
    private final ColorFinder colorFinder;
    private final GetGameView getGameView;
    private final DigitReader digitReader;

    private static final Rectangle xpBox = new Rectangle(551, 5, 87, 24);

    private final List<Color> xpColors = Arrays.asList(
            Color.decode("#FFFFFF"),
            Color.decode("#fefefe")
    );

    public ReadXP(DigitReader digitReader, CacheManager cacheManager, ColorFinder colorFinder, GetGameView getGameView) {
        this.cacheManager = cacheManager;
        this.colorFinder = colorFinder;
        this.getGameView = getGameView;
        this.digitReader = digitReader;
    }

    public int readXP(String device) {
        BufferedImage image = getXPROIMat(xpBox, device);

        int readXpValue = digitReader.findAllDigits(0, image, xpColors, cacheManager.getDigitPatterns());
    
        // Check the current stored value in the cacheManager
        String storedXpString = cacheManager.getDeviceXP(device);
        Integer storedXpValue = (storedXpString != null && !storedXpString.isEmpty()) ? parseXP(storedXpString) : null;
    
        if (storedXpValue != null && readXpValue < storedXpValue) {
            return storedXpValue; // Use stored value if higher
        }
    
        // Update the cacheManager with the new value if it is higher
        cacheManager.setDeviceXP(device, String.valueOf(readXpValue));
        return readXpValue;
    }

    // Helper method to parse XP safely
    private Integer parseXP(String xpString) {
        try {
            return Integer.parseInt(xpString);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private BufferedImage getXPROIMat(Rectangle roi, String device) {
        return getGameView.getSubBuffered(device, roi);
    }
}
