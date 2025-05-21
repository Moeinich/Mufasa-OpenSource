package helpers.OCR;

import helpers.CacheManager;
import helpers.GetGameView;
import helpers.OCR.utils.DigitLocation;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class DigitReader {
    private final CacheManager cacheManager;
    private final GetGameView getGameView;

    public DigitReader(CacheManager cacheManager, GetGameView getGameView) {
        this.cacheManager = cacheManager;
        this.getGameView = getGameView;
    }

    public List<Map.Entry<Integer, List<Point>>> findAllPlusCoords(int tolerance, BufferedImage image, List<Color> colors, Map<String, int[][]> digitPatterns) {
        List<Map.Entry<Integer, List<Point>>> digitsWithCoords = new ArrayList<>();

        Map<String, List<Point>> foundNumbers = findDigits(tolerance, image, colors, digitPatterns);

        // Extract the digits and their coordinates
        for (Map.Entry<String, List<Point>> entry : foundNumbers.entrySet()) {
            Integer digit = Integer.parseInt(entry.getKey());
            List<Point> coords = entry.getValue();
            digitsWithCoords.add(new AbstractMap.SimpleEntry<>(digit, coords));
        }

        return digitsWithCoords;
    }

    public Rectangle findString(int tolerance, Rectangle ROI, List<Color> colors, Map<String, int[][]> letterPatterns, String targetString, String device) {
        // Set letterPatterns
        if (letterPatterns == null) {
            letterPatterns = cacheManager.getLetterPatterns();
        }

        // Get the ROI image
        BufferedImage image = getGameView.getSubBuffered(device, ROI);

        // Find all letters in the image along with their coordinates
        List<Map.Entry<String, List<Point>>> foundLettersWithCoords = findAllLettersPlusCoords(tolerance, image, colors, letterPatterns);

        // Filter out duplicates and favor the largest letter
        Map<Point, String> filteredLetters = filterOverlappingLetters(foundLettersWithCoords, letterPatterns, targetString);

        // Find the bounding rectangle where the closest match to the target string is located
        Rectangle closestStringRect = getClosestStringBoundingRectangle(filteredLetters, targetString, letterPatterns);

        // If a string bounding box was found, adjust it relative to the full game screen
        if (closestStringRect != null) {
            // Adjust the x and y coordinates by adding the ROI's x and y
            closestStringRect.x += ROI.x;
            closestStringRect.y += ROI.y;
            System.out.println("Final bounding rectangle for '" + targetString + "': " + closestStringRect);
        } else {
            System.out.println("No bounding rectangle found for '" + targetString + "'");
        }

        return closestStringRect;
    }

    public int findAllDigits(int tolerance, BufferedImage image, List<Color> colors, Map<String, int[][]> digitPatterns) {
        Map<String, List<Point>> foundNumbers = findDigits(tolerance, image, colors, digitPatterns);

        // Extract and return the complete number as an integer
        return compileNumberFromPoints(foundNumbers);
    }

    public String findAllLetters(int tolerance, BufferedImage image, List<Color> colors, Map<String, int[][]> letterPatterns) {
        Map<String, List<Point>> foundLetters = findLettersNEW(tolerance, image, colors, letterPatterns);

        // Extract and return the complete string from found letters
        return compileStringFromPoints(foundLetters);
    }

    public List<Map.Entry<String, List<Point>>> findAllLettersPlusCoords(int tolerance, BufferedImage image, List<Color> colors, Map<String, int[][]> letterPatterns) {
        List<Map.Entry<String, List<Point>>> lettersWithCoords = new ArrayList<>();

        Map<String, List<Point>> foundLetters = findLetters(tolerance, image, colors, letterPatterns);

        // Extract the letters and their coordinates
        for (Map.Entry<String, List<Point>> entry : foundLetters.entrySet()) {
            String letter = entry.getKey();
            List<Point> coords = entry.getValue();
            lettersWithCoords.add(new AbstractMap.SimpleEntry<>(letter, coords));
        }

        return lettersWithCoords;
    }

    private Map<String, java.util.List<Point>> findLetters(int tolerance, BufferedImage image, java.util.List<Color> colors, Map<String, int[][]> letterPatterns) {
        Map<String, java.util.List<Point>> foundLetters = new HashMap<>();

        if (image == null || image.getWidth() == 0 || image.getHeight() == 0) {
            System.out.println("Image is empty or not valid.");
            return foundLetters;
        }

        // Iterate through the letter patterns to search within the image
        for (Map.Entry<String, int[][]> entry : letterPatterns.entrySet()) {
            String letter = entry.getKey();
            int[][] pattern = entry.getValue();

            // Search for the letter pattern in the image
            java.util.List<Point> points = findLetterPattern(image, pattern, colors, tolerance);
            if (!points.isEmpty()) {
                foundLetters.put(letter, points);
            }
        }

        return foundLetters;
    }

    private Map<String, java.util.List<Point>> findLettersNEW(int tolerance, BufferedImage image, java.util.List<Color> colors, Map<String, int[][]> letterPatterns) {
        Map<String, java.util.List<Point>> foundLetters = new HashMap<>();

        if (image == null || image.getWidth() == 0 || image.getHeight() == 0) {
            System.out.println("Image is empty or not valid.");
            return foundLetters;
        }

        for (Map.Entry<String, int[][]> entry : letterPatterns.entrySet()) {
            String letter = entry.getKey();
            int[][] pattern = entry.getValue();

            java.util.List<Point> points = findPattern(image, pattern, colors, tolerance);
            if (!points.isEmpty()) {
                foundLetters.put(letter, points);
            }
        }

        return foundLetters;
    }

    private java.util.List<Point> findLetterPattern(BufferedImage image, int[][] pattern, java.util.List<Color> targetColors, int tolerance) {
        java.util.List<Point> points = new ArrayList<>();
        int patternWidth = pattern[0].length;
        int patternHeight = pattern.length;

        for (int x = 0; x <= image.getWidth() - patternWidth; x++) {
            for (int y = 0; y <= image.getHeight() - patternHeight; y++) {
                boolean match = true;

                for (int px = 0; px < patternWidth; px++) {
                    for (int py = 0; py < patternHeight; py++) {
                        int imageX = x + px;
                        int imageY = y + py;
                        Color pixelColor = new Color(image.getRGB(imageX, imageY));

                        if (pattern[py][px] == 1 && !isColorListWithinTolerance(pixelColor, targetColors, tolerance)) {
                            match = false;
                            break;
                        }
                    }
                    if (!match) break;
                }

                if (match) {
                    points.add(new Point(x, y));
                }
            }
        }

        return points;
    }

    private String compileStringFromPoints(Map<String, java.util.List<Point>> foundLetters) {
        // Sort letters based on their positions (e.g., x-coordinates or y-coordinates for reading order).
        java.util.List<Map.Entry<String, Point>> sortedLetters = new ArrayList<>();
        for (Map.Entry<String, List<Point>> entry : foundLetters.entrySet()) {
            for (Point point : entry.getValue()) {
                sortedLetters.add(new AbstractMap.SimpleEntry<>(entry.getKey(), point));
            }
        }

        // Sort by x-coordinate (or y-coordinate for vertical text)
        sortedLetters.sort(Comparator.comparing(entry -> entry.getValue().x));

        // Build the string
        StringBuilder result = new StringBuilder();
        for (Map.Entry<String, Point> entry : sortedLetters) {
            result.append(entry.getKey());
        }

        return result.toString();
    }

    // Modified method to find all numbers in the image, directly using the two colors, using Mat
    private Map<String, List<Point>> findDigits(int tolerance, BufferedImage image, List<Color> colors, Map<String, int[][]> digitPatterns) {
        Map<String, List<Point>> foundNumbers = new HashMap<>();

        if (image == null || image.getWidth() == 0 || image.getHeight() == 0) {
            System.out.println("Image is empty or not valid.");
            return foundNumbers;
        }

        for (Map.Entry<String, int[][]> entry : digitPatterns.entrySet()) {
            String digit = entry.getKey();
            int[][] pattern = entry.getValue();

            List<Point> points = findPattern(image, pattern, colors, tolerance);
            if (!points.isEmpty()) {
                foundNumbers.put(digit, points);
            }
        }

        return foundNumbers;
    }

    // Updated method to use Mat and a list of colors for pattern matching
    private List<Point> findPattern(BufferedImage image, int[][] pattern, List<Color> targetColors, int tolerance) {
        List<Point> points = new ArrayList<>();
        int patternWidth = pattern[0].length;
        int patternHeight = pattern.length;

        for (int x = 0; x <= image.getWidth() - patternWidth; x++) {
            for (int y = 0; y <= image.getHeight() - patternHeight; y++) {
                boolean match = true;

                for (int px = 0; px < patternWidth; px++) {
                    for (int py = 0; py < patternHeight; py++) {
                        int imageX = x + px;
                        int imageY = y + py;
                        Color pixelColor = new Color(image.getRGB(imageX, imageY));

                        if (pattern[py][px] == 1) {
                            // The pixel must match one of the target colors.
                            if (!isColorListWithinTolerance(pixelColor, targetColors, tolerance)) {
                                match = false;
                                break;
                            }
                        } else {
                            // The pixel should NOT match any of the target colors.
                            if (isColorListWithinTolerance(pixelColor, targetColors, tolerance)) {
                                match = false;
                                break;
                            }
                        }
                    }
                    if (!match) break;
                }

                if (match) {
                    points.add(new Point(x, y));
                }
            }
        }

        return points;
    }


    private int compileNumberFromPoints(Map<String, List<Point>> foundNumbers) {
        if (foundNumbers.isEmpty()) {
            return -1;
        }

        StringBuilder number = new StringBuilder();
        List<DigitLocation> digitLocations = new ArrayList<>();

        for (Map.Entry<String, List<Point>> entry : foundNumbers.entrySet()) {
            String digit = entry.getKey();
            for (Point point : entry.getValue()) {
                digitLocations.add(new DigitLocation(digit, point));
            }
        }

        // Sorting based on the x-coordinate, assuming single line of numbers
        digitLocations.sort(Comparator.comparingInt(dl -> dl.point.x));

        for (DigitLocation dl : digitLocations) {
            if (dl.digit.equals("M")) {
                number.append("000000");
            } else if (dl.digit.equals("K")) {
                number.append("000");
            } else {
                number.append(dl.digit);
            }
        }

        try {
            return Integer.parseInt(number.toString());
        } catch (NumberFormatException e) {
            System.out.println("Failed to parse the number: " + number);
            return -1;  // Return -1 if the number format is invalid
        }
    }

    private Map<Point, String> filterOverlappingLetters(List<Map.Entry<String, List<Point>>> foundLettersWithCoords, Map<String, int[][]> letterPatterns, String targetString) {
        Map<Point, String> filteredLetters = new HashMap<>();
        Set<Character> targetLettersSet = targetString.chars().mapToObj(c -> (char) c).collect(Collectors.toSet()); // Get the set of letters in the target string

        for (Map.Entry<String, List<Point>> entry : foundLettersWithCoords) {
            String letter = entry.getKey();
            List<Point> coords = entry.getValue();
            int[][] letterPattern = letterPatterns.get(letter);

            if (letterPattern == null || !targetLettersSet.contains(letter.charAt(0))) {
                continue; // Skip letters that aren't in the target string
            }

            int letterWidth = letterPattern[0].length;
            int letterHeight = letterPattern.length;

            for (Point point : coords) {
                boolean overlaps = false;

                // Check for overlapping existing letters
                for (Map.Entry<Point, String> existingEntry : filteredLetters.entrySet()) {
                    Point existingPoint = existingEntry.getKey();
                    String existingLetter = existingEntry.getValue();

                    int[][] existingPattern = letterPatterns.get(existingLetter);
                    int existingWidth = existingPattern[0].length;
                    int existingHeight = existingPattern.length;

                    // Refine the overlap check logic to allow closer proximity before discarding
                    if (targetLettersSet.contains(existingLetter.charAt(0))) {

                        // Adjust tolerance here; allow letters to be close without being discarded
                        if (Math.abs(point.x - existingPoint.x) < existingWidth * 0.5 && Math.abs(point.y - existingPoint.y) < existingHeight * 0.5) {
                            // If the new letter is larger, replace the old one
                            if (letterWidth * letterHeight > existingWidth * existingHeight) {
                                filteredLetters.remove(existingPoint);  // Remove the smaller letter
                                break;  // No need to check further
                            } else {
                                overlaps = true;  // The existing letter is larger, so discard this one
                                break;
                            }
                        }
                    }
                }

                // Add the letter if it does not overlap with a larger one
                if (!overlaps) {
                    filteredLetters.put(point, letter);
                }
            }
        }

        return filteredLetters;
    }

    private boolean isColorListWithinTolerance(Color color1, List<Color> targetColors, int tolerance) {
        for (Color targetColor : targetColors) {
            if (Math.abs(color1.getRed() - targetColor.getRed()) <= tolerance &&
                    Math.abs(color1.getGreen() - targetColor.getGreen()) <= tolerance &&
                    Math.abs(color1.getBlue() - targetColor.getBlue()) <= tolerance) {
                return true;
            }
        }
        return false;
    }

    private Rectangle getClosestStringBoundingRectangle(Map<Point, String> filteredLetters, String targetString, Map<String, int[][]> letterPatterns) {
        // Sort the letters by their x-coordinate for left-to-right ordering
        List<Map.Entry<Point, String>> sortedLetters = new ArrayList<>(filteredLetters.entrySet());
        sortedLetters.sort(Comparator.comparingInt(entry -> entry.getKey().x));

        List<Point> bestMatchedPoints = new ArrayList<>();
        StringBuilder bestMatchedString = new StringBuilder();
        int bestMatchLength = 0;

        // Iterate over each potential starting point for the first letter
        for (Map.Entry<Point, String> entry : sortedLetters) {
            Point firstPoint = entry.getKey();
            String firstLetter = entry.getValue();

            // If the first letter matches the start of the target string
            if (firstLetter.equals(String.valueOf(targetString.charAt(0)))) {
                List<Point> currentMatchedPoints = new ArrayList<>();
                StringBuilder currentMatchedString = new StringBuilder();

                currentMatchedPoints.add(firstPoint);
                currentMatchedString.append(firstLetter);

                // Track the current Y-coordinate of the first point
                int referenceY = firstPoint.y;

                // Try to find the rest of the string from this starting point
                for (int i = 1; i < targetString.length(); i++) {
                    char currentChar = targetString.charAt(i);

                    // Handle space in the target string
                    if (currentChar == ' ') {
                        currentMatchedString.append(" ");
                        continue;
                    }

                    // Find the next matching letter, ensuring it's within X and Y constraints
                    boolean foundNextChar = false;
                    for (Map.Entry<Point, String> nextEntry : sortedLetters) {
                        Point nextPoint = nextEntry.getKey();
                        String nextLetter = nextEntry.getValue();

                        if (nextLetter.equals(String.valueOf(currentChar))) {
                            if (nextPoint.x > firstPoint.x && Math.abs(nextPoint.y - referenceY) <= 10) {
                                currentMatchedPoints.add(nextPoint);
                                currentMatchedString.append(nextLetter);
                                foundNextChar = true;
                                break;
                            }
                        }
                    }

                    // If we can't find the next letter, stop trying to match this sequence
                    if (!foundNextChar) {
                        break;
                    }
                }

                // Check if the current match is the best match so far
                if (currentMatchedString.length() > bestMatchLength) {
                    bestMatchedPoints = new ArrayList<>(currentMatchedPoints);
                    bestMatchedString = new StringBuilder(currentMatchedString);
                    bestMatchLength = currentMatchedString.length();
                }
            }
        }

        // If we found the best match, calculate its bounding rectangle
        if (!bestMatchedPoints.isEmpty()) {
            System.out.println("Best match found: '" + bestMatchedString + "'");
            return calculateBoundingRectangle(bestMatchedPoints, letterPatterns, bestMatchedString.toString());
        } else {
            System.out.println("No match found for: '" + targetString + "'");
            return null;
        }
    }

    private Rectangle getStringBoundingRectangle(Map<Point, String> filteredLetters, String targetString, Map<String, int[][]> letterPatterns) {
        // Sort the letters by their x-coordinate for left-to-right ordering
        List<Map.Entry<Point, String>> sortedLetters = new ArrayList<>(filteredLetters.entrySet());
        sortedLetters.sort(Comparator.comparingInt(entry -> entry.getKey().x));

        // Track the positions of the matching string
        List<Point> matchedPoints = new ArrayList<>();
        StringBuilder matchedString = new StringBuilder();

        int spaceWidth = 10;  // Adjust based on your font or letter spacing
        Point firstPoint = null;

        for (Map.Entry<Point, String> entry : sortedLetters) {
            String letter = entry.getValue();
            Point point = entry.getKey();

            // If this is the first letter in the match, set the reference Y-coordinate
            if (firstPoint == null && letter.equals(String.valueOf(targetString.charAt(matchedString.length())))) {
                firstPoint = point;
            }

            // Check if the next character in the targetString is a space
            if (matchedString.length() < targetString.length() && targetString.charAt(matchedString.length()) == ' ') {
                matchedString.append(" ");
                continue;  // Skip the space
            }

            // If we find a matching letter, ensure it's within a reasonable Y and X coordinate proximity
            if (matchedString.length() < targetString.length() && letter.equals(String.valueOf(targetString.charAt(matchedString.length())))) {
                // Allow a bit more flexibility in proximity check, reducing the threshold for packed letters
                if (Math.abs(point.y - firstPoint.y) <= 10 && Math.abs(point.x - firstPoint.x) <= spaceWidth) {
                    matchedPoints.add(point);
                    matchedString.append(letter);
                    firstPoint = point;  // Update reference point for the next letter
                }
            }

            // If the full target string is matched, return the bounding rectangle
            if (matchedString.toString().equals(targetString)) {
                return calculateBoundingRectangle(matchedPoints, letterPatterns, targetString);
            }
        }

        // If no match is found, return null
        return null;
    }

    private Rectangle calculateBoundingRectangle(List<Point> matchedPoints, Map<String, int[][]> letterPatterns, String targetString) {
        if (matchedPoints.isEmpty()) {
            System.out.println("No matched points for target string: " + targetString);
            return null;
        }

        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;

        // Iterate through matched points and adjust the bounding box based on the exact letters used
        for (int i = 0; i < matchedPoints.size(); i++) {
            Point point = matchedPoints.get(i);
            String letter = String.valueOf(targetString.charAt(i));
            int[][] pattern = letterPatterns.get(letter);

            if (pattern == null) {
                System.out.println("Pattern not found for letter: " + letter);
                continue;
            }

            int letterWidth = pattern[0].length;
            int letterHeight = pattern.length;

            minX = Math.min(minX, point.x);
            minY = Math.min(minY, point.y);
            maxX = Math.max(maxX, point.x + letterWidth);
            maxY = Math.max(maxY, point.y + letterHeight);
        }

        if (minX == Integer.MAX_VALUE || minY == Integer.MAX_VALUE || maxX == Integer.MIN_VALUE || maxY == Integer.MIN_VALUE) {
            System.out.println("Bounding box calculation failed for string: " + targetString);
            return null;
        }

        return new Rectangle(minX, minY, maxX - minX, maxY - minY);
    }
}
