package helpers.OCR;


import helpers.OCR.utils.FontName;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

import static helpers.patterns.Bold12Patterns.bold12Patterns;
import static helpers.patterns.Plain11Patterns.plain11Patterns;
import static helpers.patterns.Plain12Patterns.plain12Patterns;
import static helpers.patterns.Quill8Patterns.quill8Patterns;
import static helpers.patterns.Quill1Patterns.quill1Patterns;
import static helpers.patterns.Quill2Patterns.quill2Patterns;

public class cfOCR {

    public static String findAllPatternsInImage(int tolerance, BufferedImage image, List<Color> colors, FontName font) {

        Map<String, List<Point>> foundLetters = findPatternsInImage(tolerance, image, colors, font);

        // Extract and return the complete string from found letters
        return compileStringFromPatternPoints(foundLetters, font);
    }

    public static String findAllPatternsInImageAnyFont(int tolerance, BufferedImage image, List<Color> colors) {
        // List of fonts to test
        List<FontName> fonts = Arrays.asList(
                FontName.BOLD_12,
                FontName.PLAIN_11,
                FontName.PLAIN_12,
                FontName.QUILL_8,
                FontName.QUILL
        );

        Map<FontName, String> results = new HashMap<>();
        Map<FontName, Long> executionTimes = new HashMap<>();

        // Loop through each font and find the best result
        for (FontName font : fonts) {
            long startTime = System.currentTimeMillis();
            String result = findAllPatternsInImage(tolerance, image, colors, font);
            long elapsedTime = System.currentTimeMillis() - startTime;

            results.put(font, result);
            executionTimes.put(font, elapsedTime);
        }

        // Determine the best font based on the **longest result (most detected characters)**
        FontName bestFont = results.entrySet().stream()
                .max(Comparator.comparingInt(entry -> entry.getValue().length()))
                .map(Map.Entry::getKey)
                .orElse(FontName.BOLD_12); // Default fallback if all fail

        // Return the result from the best matching font
        return results.getOrDefault(bestFont, "");
    }

    private static List<Point> findAllPatternsInImage(BufferedImage image, int[][] pattern, List<Color> targetColors, int tolerance) {
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
                            if (!isColorListWithinToleranceBuffered(pixelColor, targetColors, tolerance)) {
                                match = false;
                                break;
                            }
                        } else {
                            // The pixel should NOT match any of the target colors.
                            if (isColorListWithinToleranceBuffered(pixelColor, targetColors, tolerance)) {
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

    // Check if the color is within tolerance for any of the target colors
    private static boolean isColorListWithinToleranceBuffered(Color color1, List<Color> targetColors, int tolerance) {
        for (Color targetColor : targetColors) {
            if (Math.abs(color1.getRed() - targetColor.getRed()) <= tolerance &&
                    Math.abs(color1.getGreen() - targetColor.getGreen()) <= tolerance &&
                    Math.abs(color1.getBlue() - targetColor.getBlue()) <= tolerance) {
                return true;
            }
        }
        return false;
    }

    private static String compileStringFromPatternPoints(Map<String, List<Point>> foundLetters, FontName fontName) {
        int rowThreshold = 0;

        switch (fontName) {
            case BOLD_12:
                rowThreshold = 15;
                break;
            case PLAIN_11:
                rowThreshold = 10;
                break;
            case PLAIN_12:
                rowThreshold = 11;
                break;
            case QUILL_8:
                rowThreshold = 9;
                break;
        }

        List<Map.Entry<String, Point>> sortedLetters = new ArrayList<>();

        for (Map.Entry<String, List<Point>> entry : foundLetters.entrySet()) {
            for (Point point : entry.getValue()) {
                sortedLetters.add(new AbstractMap.SimpleEntry<>(entry.getKey(), point));
            }
        }

        // Sort by Y first, then by X within rows
        sortedLetters.sort(Comparator.comparing(entry -> entry.getValue().y * 10000 + entry.getValue().x));

        List<List<Map.Entry<String, Point>>> rows = new ArrayList<>();

        for (Map.Entry<String, Point> entry : sortedLetters) {
            boolean added = false;
            for (List<Map.Entry<String, Point>> row : rows) {
                int rowY = row.get(0).getValue().y;
                int currentY = entry.getValue().y;

                // Ensuring letters from the same row stay together
                if (Math.abs(rowY - currentY) <= rowThreshold) {
                    row.add(entry);
                    added = true;
                    break;
                }
            }

            if (!added) {
                List<Map.Entry<String, Point>> newRow = new ArrayList<>();
                newRow.add(entry);
                rows.add(newRow);
            }
        }

        // **Final Processing: Sort each row left-to-right and construct final text**
        StringBuilder result = new StringBuilder();
        for (List<Map.Entry<String, Point>> row : rows) {
            row.sort(Comparator.comparing(entry -> entry.getValue().x));

            int lastX = -999;
            int lastWidth = 0; // Track width of last character

            for (Map.Entry<String, Point> entry : row) {
                int currentX = entry.getValue().x;
                int letterWidth = getLetterWidth(entry.getKey(), fontName); // Get letter width from patterns

                // Add space if the gap is wider than the expected letter width + buffer
                if (lastX != -999 && (currentX - lastX) > (lastWidth + 2)) {
                    result.append(" ");
                }

                result.append(entry.getKey());
                lastX = currentX;
                lastWidth = letterWidth; // Update lastWidth for next comparison
            }

            result.append("\n"); // Ensure row separation
        }

        return result.toString().trim();
    }

    private static Map<String, List<Point>> findPatternsInImage(int tolerance, BufferedImage image, List<Color> colors, FontName fontName) {
        Map<String, List<Point>> foundLetters = new HashMap<>();

        if (image == null) {
            return foundLetters;
        }

        // Handle Quill font by performing two separate readings and combining results
        if (fontName == FontName.QUILL) {

            // First pass with quill1Patterns
            Map<String, List<Point>> quill1Results = findPatternsInImageInternal(tolerance, image, colors, quill1Patterns);

            // Second pass with quill2Patterns
            Map<String, List<Point>> quill2Results = findPatternsInImageInternal(tolerance, image, colors, quill2Patterns);

            // Merge results from both passes
            for (Map.Entry<String, List<Point>> entry : quill1Results.entrySet()) {
                foundLetters.putIfAbsent(entry.getKey(), new ArrayList<>());
                foundLetters.get(entry.getKey()).addAll(entry.getValue());
            }
            for (Map.Entry<String, List<Point>> entry : quill2Results.entrySet()) {
                foundLetters.putIfAbsent(entry.getKey(), new ArrayList<>());
                foundLetters.get(entry.getKey()).addAll(entry.getValue());
            }

        } else {
            // Normal handling for all other fonts
            foundLetters = findPatternsInImageInternal(tolerance, image, colors, getLetterPatterns(fontName));
        }

        return foundLetters;
    }

    private static Map<String, List<Point>> findPatternsInImageInternal(int tolerance, BufferedImage image, List<Color> colors, Map<String, int[][]> letterPatterns) {
        Map<String, List<Point>> foundLetters = new HashMap<>();
        List<Point> occupiedPoints = new ArrayList<>();

        for (Map.Entry<String, int[][]> entry : letterPatterns.entrySet()) {
            String letter = entry.getKey();
            int[][] pattern = entry.getValue();

            List<Point> points = findAllPatternsInImage(image, pattern, colors, tolerance);
            List<Point> validPoints = new ArrayList<>();

            for (Point point : points) {
                boolean overlaps = false;

                for (Point occupied : occupiedPoints) {
                    if (Math.abs(point.x - occupied.x) < pattern[0].length - 2 &&
                            Math.abs(point.y - occupied.y) < pattern.length - 2) {
                        overlaps = true;
                        break;
                    }
                }

                if (!overlaps || isLetterOrNumber(letter)) {
                    validPoints.add(point);
                    occupiedPoints.add(point);
                }
            }

            if (!validPoints.isEmpty()) {
                foundLetters.put(letter, validPoints);
            }
        }

        return foundLetters;
    }

    /**
     * Gets the width of a letter based on the pattern size.
     */
    private static int getLetterWidth(String letter, FontName fontName) {
        if (fontName == FontName.QUILL) {
            // Check both Quill pattern maps
            if (quill1Patterns.containsKey(letter)) {
                return quill1Patterns.get(letter)[0].length; // Get width from quill1Patterns
            }
            if (quill2Patterns.containsKey(letter)) {
                return quill2Patterns.get(letter)[0].length; // Get width from quill2Patterns
            }
        } else {
            // Normal handling for other fonts
            Map<String, int[][]> letterPatterns = getLetterPatterns(fontName);
            if (letterPatterns.containsKey(letter)) {
                return letterPatterns.get(letter)[0].length; // Get width from pattern array
            }
        }

        return 7; // Default width for unknown letters
    }

    /**
     * Returns the correct pattern map for a given font.
     */
    private static Map<String, int[][]> getLetterPatterns(FontName fontName) {
        switch (fontName) {
            case BOLD_12:
                return bold12Patterns;
            case PLAIN_11:
                return plain11Patterns;
            case PLAIN_12:
                return plain12Patterns;
            case QUILL_8:
                return quill8Patterns;
            default:
                throw new IllegalArgumentException("Unsupported font: " + fontName);
        }
    }

    // Helper function to prioritize letters and numbers over special characters
    private static boolean isLetterOrNumber(String character) {
        return character.matches("[a-zA-Z0-9]");
    }

}