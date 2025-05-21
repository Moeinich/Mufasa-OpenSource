package helpers.visualFeedback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public class VisualObservable<T> {
    private final Map<String, T> deviceValues = new HashMap<>();
    private final List<BiConsumer<String, T>> listeners = new ArrayList<>();

    // Method to add a listener
    public void addListener(BiConsumer<String, T> listener) {
        listeners.add(listener);
    }

    public void removeListener(BiConsumer<String, T> listener) {
        listeners.remove(listener);
    }

    public void setValue(String device, T value) {
        deviceValues.put(device, value);
        notifyListeners(device, value);
    }

    public T getValue(String device) {
        return deviceValues.get(device);
    }

    public void removeDevice(String device) {
        deviceValues.remove(device);
    }

    // Notify all listeners about a change for a specific device
    private void notifyListeners(String device, T value) {
        for (BiConsumer<String, T> listener : listeners) {
            listener.accept(device, value);
        }
    }
}
