package UI.scriptUIBuilder.utils;

import java.util.List;
import java.util.Map;

public interface UIElement {
    String getLabel();

    default String getParentLabel() { return null; }
    default Map<String, List<String>> getDependencyMap() { return null; }
}

