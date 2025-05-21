package UI.scriptUIBuilder.components;

import UI.scriptUIBuilder.utils.UIElement;

public class Description implements UIElement {
    private final String label;
    private final String text;

    public Description(String label, String text) {
        this.label = label;
        this.text = text;
    }

    // Getters
    public String getLabel() {
        return label;
    }

    public String getText() {
        return text;
    }
}

