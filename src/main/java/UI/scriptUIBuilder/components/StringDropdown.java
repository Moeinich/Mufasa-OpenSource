package UI.scriptUIBuilder.components;

import UI.scriptUIBuilder.utils.UIElement;

import java.util.List;
import java.util.Map;

public class StringDropdown implements UIElement {
    private final String label;
    private final List<String> options;
    private final String defaultValue;
    private final String parentLabel;
    private final Map<String, List<String>> dependencyMap;

    public StringDropdown(String label, List<String> options, String defaultValue, String parentLabel, Map<String, List<String>> dependencyMap) {
        this.label = label;
        this.options = options;
        this.defaultValue = defaultValue;
        this.parentLabel = parentLabel;
        this.dependencyMap = dependencyMap;
    }

    // Getters
    public String getLabel() { return label; }
    public List<String> getOptions() { return options; }
    public String getDefaultValue() { return defaultValue; }
    public String getParentLabel() { return parentLabel; }
    public Map<String, List<String>> getDependencyMap() { return dependencyMap; }
}