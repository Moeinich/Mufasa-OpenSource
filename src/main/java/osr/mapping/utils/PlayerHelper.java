package osr.mapping.utils;

import helpers.OCR.ReadLevels;
import helpers.Color.ColorFinder;
import scripts.APIClasses.ClientAPI;

import java.awt.*;
import java.util.List;

public class PlayerHelper {
    private final ClientAPI clientAPI;
    private final ReadLevels readLevels;
    private final ColorFinder colorFinder;

    // Rectangles
    private static final Rectangle runBoxRect = new Rectangle(697, 121, 18, 14);
    private static final Rectangle HPValueRectangle = new Rectangle(657, 48, 27, 18);
    private static final Rectangle prayValueRectangle = new Rectangle(654, 82, 27, 20);
    private static final Rectangle runValueRectangle = new Rectangle(662, 118, 29, 21);
    private static final Rectangle specValueRectangle = new Rectangle(689, 150, 30, 20);
    private static final Rectangle specTapRectangle = new Rectangle(725, 153, 14, 15);

    // Colors for boxes
    private final List<Color> orbColors = List.of(
            Color.decode("#00FF00"),
            Color.decode("#0CFF00"),
            Color.decode("#17FF00"),
            Color.decode("#22ff00"),
            Color.decode("#2DFF00"),
            Color.decode("#38FF00"),
            Color.decode("#43FF00"),
            Color.decode("#4EFF00"),
            Color.decode("#59FF00"),
            Color.decode("#64FF00"),
            Color.decode("#6FFF00"),
            Color.decode("#7AFF00"),
            Color.decode("#86FF00"),
            Color.decode("#91FF00"),
            Color.decode("#9CFF00"),
            Color.decode("#A7FF00"),
            Color.decode("#B2FF00"),
            Color.decode("#BDFF00"),
            Color.decode("#C8FF00"),
            Color.decode("#DEFF00"),
            Color.decode("#E9FF00"),
            Color.decode("#F4FF00"),
            Color.decode("#FFFF00"),
            Color.decode("#FFF300"),
            Color.decode("#FFE800"),
            Color.decode("#FFDD00"),
            Color.decode("#FFD200"),
            Color.decode("#FFC700"),
            Color.decode("#FFBC00"),
            Color.decode("#FFB100"),
            Color.decode("#FFA600"),
            Color.decode("#FF9B00"),
            Color.decode("#FF9000"),
            Color.decode("#FF8500"),
            Color.decode("#FF7900"),
            Color.decode("#FF6E00"),
            Color.decode("#FF6300"),
            Color.decode("#FF5800"),
            Color.decode("#FF4D00"),
            Color.decode("#FF4200"),
            Color.decode("#FF3700"),
            Color.decode("#FF2C00"),
            Color.decode("#FF2100"),
            Color.decode("#FF1600"),
            Color.decode("#FF0B00"),
            Color.decode("#FF0000")
    );
    private final List<Color> runColors = List.of(
            Color.decode("#8d7200"),
            Color.decode("#a08101"),
            Color.decode("#c29e01"),
            Color.decode("#b29101")
    );

    public PlayerHelper(ClientAPI clientAPI, ReadLevels readLevels, ColorFinder colorFinder) {
        this.clientAPI = clientAPI;
        this.readLevels = readLevels;
        this.colorFinder = colorFinder;
    }

    public boolean isRunEnabled(String device) {
        return colorFinder.isAnyColorInRect(device, runColors, runBoxRect, 5);
    }

    public void toggleRun(String device) {
        clientAPI.tap(runBoxRect, device);
    }

    public int readHP(String device) {
        List<ReadLevels.DetectedDigit> detectedDigits = readLevels.detectDigitsWithTolerance(device, HPValueRectangle, orbColors);

        if (detectedDigits.isEmpty()) {
            return 99; // Return 99 if no digits are found (early exit)
        }

        int hitpoints = 0;
        for (ReadLevels.DetectedDigit detectedDigit : detectedDigits) {
            hitpoints = hitpoints * 10 + detectedDigit.digit;
        }

        return hitpoints;
    }

    public int readPray(String device) {
        List<ReadLevels.DetectedDigit> detectedDigits = readLevels.detectDigitsWithTolerance(device, prayValueRectangle, orbColors);

        if (detectedDigits.isEmpty()) {
            return 99; // Return 99 if no digits are found (early exit)
        }

        int prayerPoints = 0;
        for (ReadLevels.DetectedDigit detectedDigit : detectedDigits) {
            prayerPoints = prayerPoints * 10 + detectedDigit.digit;
        }

        return prayerPoints;
    }

    public int readRun(String device) {
        List<ReadLevels.DetectedDigit> detectedDigits = readLevels.detectDigitsWithTolerance(device, runValueRectangle, orbColors);

        if (detectedDigits.isEmpty()) {
            return 0; // Return 99 if no digits are found (early exit)
        }

        int runEnergy = 0;
        for (ReadLevels.DetectedDigit detectedDigit : detectedDigits) {
            runEnergy = runEnergy * 10 + detectedDigit.digit;
        }

        return runEnergy;
    }

    public int readSpec(String device) {
        List<ReadLevels.DetectedDigit> detectedDigits = readLevels.detectDigitsWithTolerance(device, specValueRectangle, orbColors);

        if (detectedDigits.isEmpty()) {
            return 100; // Return 100 if no digits are found (early exit)
        }

        int specEnergy = 0;
        for (ReadLevels.DetectedDigit detectedDigit : detectedDigits) {
            specEnergy = specEnergy * 10 + detectedDigit.digit;
        }

        return specEnergy;
    }

    public void useSpec(String device) {
        clientAPI.tap(specTapRectangle, device);
    }
}
