package UI.scriptUIBuilder.components;

import UI.scriptUIBuilder.utils.UIElement;

import java.util.ArrayList;
import java.util.List;

public class Tab {
    private final String name;
    private final List<UIElement> elements;

    public Tab(String name) {
        this.name = name;
        this.elements = new ArrayList<>();
    }

    public Tab addElement(UIElement element) {
        elements.add(element);
        return this;
    }

    public String getName() { return name; }
    public List<UIElement> getElements() { return elements; }
}

