package osr.mapping.utils;

import helpers.GetGameView;
import helpers.Logger;
import helpers.openCV.ImageRecognition;
import helpers.openCV.utils.MatchedRectangle;
import org.opencv.core.Mat;
import osr.utils.ImageUtils;
import scripts.APIClasses.ClientAPI;

import java.awt.*;
import java.util.List;
import java.util.*;

public class FairyRings {
    private final ImageRecognition imageRecognition;
    private final Logger logger;
    private final ClientAPI clientAPI;
    private final ImageUtils imageUtils;

    private final Map<String, String> ring1Map;
    private final Map<String, String> ring2Map;
    private final Map<String, String> ring3Map;

    private final GetGameView getGameView;

    public FairyRings(Logger logger, ImageRecognition imageRecognition, GetGameView getGameView, ClientAPI clientAPI, ImageUtils imageUtils) {
        this.imageRecognition = imageRecognition;
        this.getGameView = getGameView;
        this.logger = logger;
        this.clientAPI = clientAPI;
        this.imageUtils = imageUtils;

        ring1Map = new LinkedHashMap<>();
        ring2Map = new LinkedHashMap<>();
        ring3Map = new LinkedHashMap<>();

        // Populate ring 1 map
        ring1Map.put("A", "/osrsAssets/FairyRings/a.png");
        ring1Map.put("D", "/osrsAssets/FairyRings/d.png");
        ring1Map.put("C", "/osrsAssets/FairyRings/c.png");
        ring1Map.put("B", "/osrsAssets/FairyRings/b.png");

        // Populate ring 2 map
        ring2Map.put("I", "/osrsAssets/FairyRings/i.png");
        ring2Map.put("L", "/osrsAssets/FairyRings/l.png");
        ring2Map.put("K", "/osrsAssets/FairyRings/k.png");
        ring2Map.put("J", "/osrsAssets/FairyRings/j.png");

        // Populate ring 3 map
        ring3Map.put("P", "/osrsAssets/FairyRings/p.png");
        ring3Map.put("S", "/osrsAssets/FairyRings/s.png");
        ring3Map.put("R", "/osrsAssets/FairyRings/r.png");
        ring3Map.put("Q", "/osrsAssets/FairyRings/q.png");
    }

    public void setRingsTo(String device, String letters) {
        logger.log("Setting rings to: " + letters, device);
        if (letters == null || letters.length() != 3) {
            logger.log("Invalid letters input for rings", device);
            return;
        }
        Map<String, LetterInfo> presenceResults = checkLettersInRings(device);

        String[] desiredLetters = letters.split("");
        logger.log("Desired letters: " + Arrays.toString(desiredLetters), device);
        adjustRing(device, desiredLetters[0], ring1Map, presenceResults, "Ring 1");
        adjustRing(device, desiredLetters[1], ring2Map, presenceResults, "Ring 2");
        adjustRing(device, desiredLetters[2], ring3Map, presenceResults, "Ring 3");
    }

    private void adjustRing(String device, String desiredLetter, Map<String, String> ringMap, Map<String, LetterInfo> presenceResults, String ringIdentifier) {
        logger.log("Adjusting " + ringIdentifier + " to " + desiredLetter, device);
        String currentLetter = getCurrentLetter(presenceResults, ringMap);
        logger.log(ringIdentifier + " - Current letter: " + currentLetter, device);
        LetterInfo currentLetterInfo = presenceResults.get(currentLetter);

        if (currentLetterInfo == null || !currentLetterInfo.isPresent()) {
            logger.log("Current letter not found in " + ringIdentifier, device);
            return;
        }

        RingAdjustment adjustment = calculateSteps(currentLetterInfo.getLetter(), desiredLetter, new ArrayList<>(ringMap.keySet()));
        logger.log("Performing Ring Adjustment: " + adjustment.steps + " steps to the " + adjustment.direction, device);
        performRingAdjustment(adjustment.steps, adjustment.direction, currentLetterInfo);
        logger.log("Adjusted " + ringIdentifier + " to " + desiredLetter + " with " + adjustment.steps + " steps to the " + adjustment.direction, device);
    }

    private RingAdjustment calculateSteps(String currentLetter, String desiredLetter, List<String> sequence) {
        int currentIndex = sequence.indexOf(currentLetter);
        int desiredIndex = sequence.indexOf(desiredLetter);
        int steps = desiredIndex - currentIndex;

        String direction;
        int halfSize = sequence.size() / 2;

        if (Math.abs(steps) > halfSize) {
            direction = steps > 0 ? "left" : "right";
            steps = steps > 0 ? sequence.size() - steps : -steps;
        } else {
            direction = steps > 0 ? "right" : "left";
            steps = Math.abs(steps);
        }

        return new RingAdjustment(steps, direction);
    }

    private void performRingAdjustment(int steps, String direction, LetterInfo letterInfo) {
        logger.devLog("Performing ring adjustment for " + letterInfo.getLetter() + ": " + steps + " steps " + direction);
        MatchedRectangle matchedRectangle = letterInfo.getMatchedRectangle();

        if (matchedRectangle == null) {
            logger.devLog("No matched rectangle found for letter adjustment.");
            return;
        }

        int centerX = matchedRectangle.x + matchedRectangle.width / 2;
        int centerY = matchedRectangle.y + matchedRectangle.height / 2;

        logger.devLog("Center coordinates for tap: (" + centerX + ", " + centerY + ")");

        int tapAreaWidth = 20;
        int tapAreaHeight = 10;

        int tapX = centerX - tapAreaWidth / 2;
        int tapY = centerY - tapAreaHeight / 2;

        if ("left".equals(direction)) {
            tapX -= 20;
        } else if ("right".equals(direction)) {
            tapX += 20;
        }
        Rectangle tapRectangle = new Rectangle(tapX, tapY, tapAreaWidth, tapAreaHeight);
        logger.devLog("Tap Rectangle: " + tapRectangle);

        int numberOfClicks = Math.abs(steps);
        Random random = new Random();

        // Removed the while loop. Now the loop will iterate based on the number of clicks required.
        for (int i = 0; i < numberOfClicks; i++) {
            clientAPI.tap(tapRectangle); // Make sure to add device ID if necessary, e.g., tap(tapRectangle, device);
            long randomDelayMilliseconds = 500 + random.nextInt(1000);
            logger.devLog("Tapped. Waiting for " + randomDelayMilliseconds + "ms before next tap.");

            // Wait for the random delay
            try {
                Thread.sleep(randomDelayMilliseconds);
            } catch (InterruptedException e) {
                logger.devLog("Interrupted while waiting after tap. Exiting adjustment.");
                Thread.currentThread().interrupt(); // Properly handle interruption
                break; // Exit the loop if the thread is interrupted
            }
        }
    }

    private String getCurrentLetter(Map<String, LetterInfo> presenceResults, Map<String, String> ringMap) {
        return ringMap.keySet().stream()
                .filter(letter -> {
                    LetterInfo letterInfo = presenceResults.get(letter);
                    return letterInfo != null && letterInfo.isPresent();
                })
                .findFirst()
                .orElse(null);
    }

    private Map<String, LetterInfo> checkLettersInRings(String device) {
        Map<String, LetterInfo> presenceResults = new HashMap<>();

        // Check each ring map
        checkRingMap(device, ring1Map, presenceResults);
        checkRingMap(device, ring2Map, presenceResults);
        checkRingMap(device, ring3Map, presenceResults);

        return presenceResults;
    }

    private void checkRingMap(String device, Map<String, String> ringMap, Map<String, LetterInfo> results) {
        Mat gameview = getGameView.getMat(device);
        for (String letter : ringMap.keySet()) {
            String imagePath = ringMap.get(letter);
            MatchedRectangle matchedRectangle = imageRecognition.returnBestMatchObject(imageUtils.pathToMat(imagePath), gameview, 0.50);

            // Assuming returnBestMatchObject checks if the image at imagePath is present on the device
            boolean isPresent = matchedRectangle != null;

            LetterInfo letterInfo = new LetterInfo(letter, isPresent, matchedRectangle);
            results.put(letter, letterInfo);
        }
    }

    static class RingAdjustment {
        final int steps;
        final String direction;

        RingAdjustment(int steps, String direction) {
            this.steps = steps;
            this.direction = direction;
        }
    }

    public static class LetterInfo {
        private final String letter;
        private final boolean isPresent;
        private final MatchedRectangle matchedRectangle;

        public LetterInfo(String letter, boolean isPresent, MatchedRectangle matchedRectangle) {
            this.letter = letter;
            this.isPresent = isPresent;
            this.matchedRectangle = matchedRectangle;
        }

        public String getLetter() {
            return letter;
        }

        public boolean isPresent() {
            return isPresent;
        }

        public MatchedRectangle getMatchedRectangle() {
            return matchedRectangle;
        }
    }
}
