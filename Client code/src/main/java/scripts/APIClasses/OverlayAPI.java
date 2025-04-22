package scripts.APIClasses;

import helpers.ThreadManager;
import helpers.visualFeedback.FeedbackObservables;
import interfaces.iOverlay;
import osr.mapping.OverlayFinder;
import osr.utils.OverlayType;
import scripts.ScriptInfo;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

public class OverlayAPI implements iOverlay {
    private final OverlayFinder overlayFinder;
    private final ScriptInfo scriptInfo;
    private final ExecutorService threadManager = ThreadManager.getInstance().getUnifiedExecutor();

    public OverlayAPI(OverlayFinder overlayFinder, ScriptInfo scriptInfo) {
        this.overlayFinder = overlayFinder;
        this.scriptInfo = scriptInfo;
    }

    // Fishing
    public List<Rectangle> findFishingOverlay(Rectangle searchArea) {
        String device = scriptInfo.getCurrentEmulatorId();
        FeedbackObservables.rectangleObservable.setValue(device, searchArea);
        List<Rectangle> overlays = new ArrayList<>();
        threadManager.execute(() -> overlays.addAll(
                overlayFinder.findOverlays(device, OverlayType.FISHING, searchArea, 11, 25))
        );
        return overlays;
    }

    public Rectangle findNearestFishing(Rectangle searchArea) {
        String device = scriptInfo.getCurrentEmulatorId();
        FeedbackObservables.rectangleObservable.setValue(device, searchArea);

        try {
            return threadManager.submit(() -> overlayFinder.findNearestFishingNEW(device, searchArea)).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return null; // Handle or log exception as needed
        }
    }

    public Rectangle findSecondNearestFishing(Rectangle searchArea) {
        String device = scriptInfo.getCurrentEmulatorId();
        FeedbackObservables.rectangleObservable.setValue(device, searchArea);

        try {
            return threadManager.submit(() -> overlayFinder.findSecondNearestFishingNEW(device, searchArea)).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return null; // Handle or log exception as needed
        }
    }

    // Agility
    public List<Rectangle> findAgilityOverlay(Rectangle searchArea) {
        String device = scriptInfo.getCurrentEmulatorId();
        FeedbackObservables.rectangleObservable.setValue(device, searchArea);
        List<Rectangle> overlays = new ArrayList<>();
        threadManager.execute(() -> overlays.addAll(
                overlayFinder.findOverlays(device, OverlayType.AGILITY, searchArea, 10.0, 20))
        );
        return overlays;
    }

    public Rectangle findNearestAgility(Rectangle searchArea) {
        String device = scriptInfo.getCurrentEmulatorId();
        FeedbackObservables.rectangleObservable.setValue(device, searchArea);
        try {
            return threadManager.submit(() ->
                    overlayFinder.findNearestOverlay(device, OverlayType.AGILITY, searchArea, 10.0, 20)
            ).get(); // Blocks until result is available
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return null; // Handle or log exception as needed
        }
    }

    public Rectangle findSecondNearestAgility(Rectangle searchArea) {
        String device = scriptInfo.getCurrentEmulatorId();
        FeedbackObservables.rectangleObservable.setValue(device, searchArea);
        try {
            return threadManager.submit(() ->
                    overlayFinder.findSecondNearestOverlay(device, OverlayType.AGILITY, searchArea, 10.0, 20)
            ).get(); // Blocks until result is available
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return null; // Handle or log exception as needed
        }
    }

    public List<Rectangle> findGroundItems(Rectangle searchArea) {
        String device = scriptInfo.getCurrentEmulatorId();
        FeedbackObservables.rectangleObservable.setValue(device, searchArea);
        List<Rectangle> overlays = new ArrayList<>();
        threadManager.execute(() -> overlays.addAll(
                overlayFinder.findOverlays(device, OverlayType.GROUND_ITEM, searchArea, 10.0, 20))
        );
        return overlays;
    }

    public Rectangle findNearestGroundItem(Rectangle searchArea) {
        FeedbackObservables.rectangleObservable.setValue(scriptInfo.getCurrentEmulatorId(), searchArea);
        return overlayFinder.findNearestOverlay(scriptInfo.getCurrentEmulatorId(), OverlayType.GROUND_ITEM, searchArea, 10.0, 20);
    }

    // To be replaced.
    public List<Polygon> findOverlays(Color color) {
        return overlayFinder.findFishing(color, scriptInfo.getCurrentEmulatorId(), false);
    }

    public Polygon findNearest(Color color) {
        return overlayFinder.findNearestFishing(color, scriptInfo.getCurrentEmulatorId(), false);
    }

    public Polygon findSecondNearest(Color color) {
        return overlayFinder.findSecondNearestFishing(color, scriptInfo.getCurrentEmulatorId(), false);
    }
}
