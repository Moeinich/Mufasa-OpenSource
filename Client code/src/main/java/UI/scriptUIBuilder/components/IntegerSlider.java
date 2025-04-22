package UI.scriptUIBuilder.components;

import UI.scriptUIBuilder.utils.UIElement;

public class IntegerSlider implements UIElement {
    private final String label;
    private final int min;
    private final int max;
    private final int defaultValue;

    public IntegerSlider(String label, int min, int max, int defaultValue) {
        this.label = label;
        this.min = min;
        this.max = max;
        this.defaultValue = defaultValue;
    }

    // Getters
    public String getLabel() { return label; }
    public int getMin() { return min; }
    public int getMax() { return max; }
    public int getDefaultValue() { return defaultValue; }
}