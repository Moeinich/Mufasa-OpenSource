package helpers.utils;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import java.util.concurrent.ConcurrentHashMap;

public class IsScriptRunning {
    private final ConcurrentHashMap<String, BooleanProperty> isScriptRunningMap = new ConcurrentHashMap<>();

    public BooleanProperty isScriptRunningProperty(String emulatorID) {
        return isScriptRunningMap.computeIfAbsent(emulatorID, k -> new SimpleBooleanProperty(this, "isScriptRunning", false));
    }
}
