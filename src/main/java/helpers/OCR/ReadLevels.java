package helpers.OCR;

import helpers.CacheManager;
import helpers.GetGameView;
import helpers.Logger;
import helpers.utils.Skills;
import helpers.visualFeedback.FeedbackObservables;

import java.awt.*;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ReadLevels {
    private final Logger logger;
    private final GetGameView getGameView;
    private final CacheManager cacheManager;
    private final DigitReader digitReader;
    private final ConcurrentHashMap<String, Rectangle> skillAreas = new ConcurrentHashMap<>();

    public ReadLevels(Logger logger, GetGameView getGameView, CacheManager cacheManager, DigitReader digitReader) {
        this.logger = logger;
        this.getGameView = getGameView;
        this.cacheManager = cacheManager;
        this.digitReader = digitReader;

        // Initialize skillAreas map
        initializeSkillAreas();
    }

    private void initializeSkillAreas() {
        // Map with skill locations
        skillAreas.put("Attack", new Rectangle(624, 227, 38, 30));
        skillAreas.put("Strength", new Rectangle(624, 260, 36, 30));
        skillAreas.put("Defence", new Rectangle(624, 292, 37, 29));
        skillAreas.put("Ranged", new Rectangle(624, 323, 37, 30));
        skillAreas.put("Prayer", new Rectangle(623, 355, 37, 31));
        skillAreas.put("Magic", new Rectangle(623, 389, 39, 30));
        skillAreas.put("Runecrafting", new Rectangle(624, 419, 37, 30));
        skillAreas.put("Construction", new Rectangle(626, 452, 35, 29));
        skillAreas.put("Hitpoints", new Rectangle(690, 227, 36, 30));
        skillAreas.put("Agility", new Rectangle(690, 260, 35, 30));
        skillAreas.put("Herblore", new Rectangle(690, 291, 34, 30));
        skillAreas.put("Thieving", new Rectangle(690, 324, 35, 30));
        skillAreas.put("Crafting", new Rectangle(690, 356, 35, 29));
        skillAreas.put("Fletching", new Rectangle(690, 387, 35, 31));
        skillAreas.put("Slayer", new Rectangle(689, 419, 36, 31));
        skillAreas.put("Hunter", new Rectangle(690, 453, 35, 28));
        skillAreas.put("Mining", new Rectangle(754, 226, 33, 31));
        skillAreas.put("Smithing", new Rectangle(751, 260, 39, 30));
        skillAreas.put("Fishing", new Rectangle(752, 292, 36, 30));
        skillAreas.put("Cooking", new Rectangle(753, 324, 35, 31));
        skillAreas.put("Firemaking", new Rectangle(753, 355, 35, 31));
        skillAreas.put("Woodcutting", new Rectangle(753, 387, 34, 31));
        skillAreas.put("Farming", new Rectangle(753, 420, 34, 30));
        skillAreas.put("Total", new Rectangle(739, 465, 37, 15));
    }

    public Rectangle getSkillRectangle(Skills skill) {
        // Convert the Skills enum value to a properly formatted string
        String skillName = skill.name().toLowerCase();
        skillName = skillName.substring(0, 1).toUpperCase() + skillName.substring(1);

        // Get the Rect associated with the skill name
        Rectangle rect = skillAreas.get(skillName);

        if (rect == null) {
            // Handle the case where the skill is not found (optional logging or exception)
            logger.devLog("Skill rectangle not found for: " + skillName);
            return null;
        }

        // Convert the Rect to java.awt.Rectangle and return
        return new Rectangle(rect.x, rect.y, rect.width, rect.height);
    }

    public String getTotalLevelCF(String device, List<Color> colors) {
        Rectangle adjustedSkillArea = getAdjustedSkillArea("Total");
        if (adjustedSkillArea == null) {
            return null;
        }

        FeedbackObservables.rectangleObservable.setValue(device, new Rectangle(adjustedSkillArea.x, adjustedSkillArea.y, adjustedSkillArea.width, adjustedSkillArea.height));
        List<DetectedDigit> detectedDigits = detectDigits(device, adjustedSkillArea, colors);

        // Combine the digits to form the total level
        StringBuilder totalLevel = new StringBuilder();
        for (DetectedDigit detectedDigit : detectedDigits) {
            totalLevel.append(detectedDigit.digit);
        }

        System.out.println("Detected Total level: " + totalLevel);
        return totalLevel.toString();
    }

    public String getSkillLevelCF(String levelKind, String skillName, String device, List<Color> colors) {
        Rectangle adjustedSkillArea = getAdjustedSkillArea(skillName);
        if (adjustedSkillArea == null) {
            return null;
        }

        FeedbackObservables.rectangleObservable.setValue(device, new Rectangle(adjustedSkillArea.x, adjustedSkillArea.y, adjustedSkillArea.width, adjustedSkillArea.height));
        List<DetectedDigit> detectedDigits = detectDigits(device, adjustedSkillArea, colors);

        // Split the digits into Effective and Real levels based on their y-coordinates
        List<DetectedDigit> effectiveLevelDigits = new ArrayList<>();
        List<DetectedDigit> realLevelDigits = new ArrayList<>();

        int highestY = detectedDigits.isEmpty() ? 0 : detectedDigits.get(0).point.y;
        for (DetectedDigit detectedDigit : detectedDigits) {
            if (detectedDigit.point.y <= highestY) {
                effectiveLevelDigits.add(detectedDigit);
            } else {
                realLevelDigits.add(detectedDigit);
            }
        }

        StringBuilder effectiveLevel = new StringBuilder();
        for (DetectedDigit detectedDigit : effectiveLevelDigits) {
            effectiveLevel.append(detectedDigit.digit);
        }

        StringBuilder realLevel = new StringBuilder();
        for (DetectedDigit detectedDigit : realLevelDigits) {
            realLevel.append(detectedDigit.digit);
        }

        if ("Real".equals(levelKind)) {
            System.out.println("Detected " + skillName + " Real level: " + realLevel);
            return realLevel.toString();
        } else {
            System.out.println("Detected " + skillName + " Effective level: " + effectiveLevel);
            return effectiveLevel.toString();
        }
    }

    private Rectangle getAdjustedSkillArea(String skillName) {
        Rectangle skillArea = skillAreas.get(skillName);
        if (skillArea == null) {
            logger.devLog("Error: Skill '" + skillName + "' not defined or non-existent.");
            return null;
        }

        return skillArea;
    }

    public List<DetectedDigit> detectDigits(String device, Rectangle adjustedSkillArea, List<Color> colors) {
        BufferedImage searchImage = getGameView.getSubBuffered(device, adjustedSkillArea);

        List<Map.Entry<Integer, List<Point>>> digitsWithCoords = digitReader.findAllPlusCoords(0, searchImage, colors, cacheManager.getDigitPatterns());
        logger.devLog("CF results: " + digitsWithCoords);

        List<DetectedDigit> detectedDigits = new ArrayList<>();
        for (Map.Entry<Integer, List<Point>> entry : digitsWithCoords) {
            int digit = entry.getKey();
            for (Point point : entry.getValue()) {
                detectedDigits.add(new DetectedDigit(digit, point));
            }
        }

        detectedDigits.sort(Comparator.comparingInt((DetectedDigit d) -> d.point.y).thenComparingInt(d -> d.point.x));
        return detectedDigits;
    }

    public List<DetectedDigit> detectDigitsWithTolerance(String device, Rectangle adjustedSkillArea, List<Color> colors) {
        BufferedImage searchImage = getGameView.getSubBuffered(device, adjustedSkillArea);

        List<Map.Entry<Integer, List<Point>>> digitsWithCoords = digitReader.findAllPlusCoords(10, searchImage, colors, cacheManager.getDigitPatterns());

        List<DetectedDigit> detectedDigits = new ArrayList<>();
        for (Map.Entry<Integer, List<Point>> entry : digitsWithCoords) {
            int digit = entry.getKey();
            for (Point point : entry.getValue()) {
                detectedDigits.add(new DetectedDigit(digit, point));
            }
        }

        detectedDigits.sort(Comparator.comparingInt((DetectedDigit d) -> d.point.y).thenComparingInt(d -> d.point.x));
        return detectedDigits;
    }

    public static class DetectedDigit {
        public int digit;
        public Point point;

        public DetectedDigit(int digit, Point point) {
            this.digit = digit;
            this.point = point;
        }
    }

}
