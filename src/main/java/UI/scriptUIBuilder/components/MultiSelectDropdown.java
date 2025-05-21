package UI.scriptUIBuilder.components;

import UI.scriptUIBuilder.utils.UIElement;

import java.util.List;
import java.util.Map;

public class MultiSelectDropdown implements UIElement {
    private final String label;
    private final List<String> options;
    private final List<String> defaultSelections;
    private final Map<String, List<String>> dependencyMap;

    public MultiSelectDropdown(String label, List<String> options, List<String> defaultSelections, Map<String, List<String>> dependencyMap) {
        this.label = label;
        this.options = options;
        this.defaultSelections = defaultSelections;
        this.dependencyMap = dependencyMap;
    }

    // Getters
    public String getLabel() { return label; }
    public List<String> getOptions() { return options; }
    public List<String> getDefaultSelections() { return defaultSelections; }
    public Map<String, List<String>> getDependencyMap() { return dependencyMap; }
}