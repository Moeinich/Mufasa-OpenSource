package UI.scriptUIBuilder.components;

import UI.scriptUIBuilder.utils.UIElement;

public class BooleanCheckbox implements UIElement {
    private final String label;
    private final boolean defaultValue;

    public BooleanCheckbox(String label, boolean defaultValue) {
        this.label = label;
        this.defaultValue = defaultValue;
    }

    // Getters
    public String getLabel() { return label; }
    public boolean getDefaultValue() { return defaultValue; }
}
