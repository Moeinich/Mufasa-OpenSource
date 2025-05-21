package helpers.visualFeedback;

import UI.components.EmulatorView;
import javafx.application.Platform;
import javafx.scene.paint.Color;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.HashMap;
import java.util.Map;

import static helpers.visualFeedback.FeedbackObservables.*;

public class FeedbackListenerManager {
    private final List<Runnable> activeListeners = new ArrayList<>();
    private final FeedbackDrawHandler feedbackDrawer;
    private final Map<VisualObservable<?>, BiConsumer<String, ?>> observableHandlers = new HashMap<>();

    public FeedbackListenerManager(FeedbackDrawHandler feedbackDrawer) {
        this.feedbackDrawer = feedbackDrawer;
        registerHandlers();
    }

    private void registerHandlers() {
        observableHandlers.put(pointObservable, (BiConsumer<String, Point>) (device, point) -> feedbackDrawer.drawPoint(point));
        observableHandlers.put(rectangleObservable, (BiConsumer<String, Rectangle>) (device, rectangle) -> feedbackDrawer.drawRectangle(rectangle, Color.PURPLE));
        observableHandlers.put(polygonObservable, (BiConsumer<String, Polygon>) (device, polygon) -> feedbackDrawer.drawPolygon(polygon));
        observableHandlers.put(rectangleAndPointObservable, (BiConsumer<String, RectangleAndPoint>) (device, data) -> feedbackDrawer.drawRectangleAndPoint(data));
        observableHandlers.put(polygonAndPointObservable, (BiConsumer<String, PolygonAndPoint>) (device, data) -> feedbackDrawer.drawPolygonAndPoint(data));
        observableHandlers.put(rectangleAndRectangleObservable, (BiConsumer<String, RectangleAndRectangle>) (device, data) -> feedbackDrawer.drawRectangleAndRectangle(data));
        observableHandlers.put(listPointsAndPointObservable, (BiConsumer<String, ListPointsAndPoint>) (device, data) -> feedbackDrawer.drawListPointsAndPoint(data));
        observableHandlers.put(pointAndPointObservable, (BiConsumer<String, PointAndPoint>) (device, data) -> feedbackDrawer.drawPointAndPoint(data));
    }

    public void addListenersForCurrentEmulator() {
        String currentSelectedEmulator = EmulatorView.getSelectedEmulator();

        if (currentSelectedEmulator == null) return;

        // Iterate through the registry and add listeners dynamically
        for (Map.Entry<VisualObservable<?>, BiConsumer<String, ?>> entry : observableHandlers.entrySet()) {
            addListenerCasted(entry.getKey(), entry.getValue(), currentSelectedEmulator);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> void addListenerCasted(VisualObservable<?> observable, BiConsumer<String, ?> onUpdate, String currentSelectedEmulator) {
        VisualObservable<T> typedObservable = (VisualObservable<T>) observable;
        BiConsumer<String, T> typedOnUpdate = (BiConsumer<String, T>) onUpdate;
        addListener(typedObservable, typedOnUpdate, currentSelectedEmulator);
    }

    private <T> void addListener(VisualObservable<T> observable, BiConsumer<String, T> onUpdate, String currentSelectedEmulator) {
        BiConsumer<String, T> listener = (device, data) -> Platform.runLater(() -> {
            if (currentSelectedEmulator.equals(device)) {
                feedbackDrawer.clearCanvas();
                onUpdate.accept(device, data);
            }
        });
        observable.addListener(listener);

        // Store a cleanup function for removing the listener
        activeListeners.add(() -> observable.removeListener(listener));
    }

    public void clearAllListeners() {
        activeListeners.forEach(Runnable::run);
        activeListeners.clear();
    }
}
