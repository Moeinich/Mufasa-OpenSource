package UI.scriptUIBuilder.utils;

import UI.scriptUIBuilder.components.Tab;

import java.util.List;

public class UIConfiguration {
    private final List<Tab> tabs;

    public UIConfiguration(List<Tab> tabs) {
        this.tabs = tabs;
    }

    public List<Tab> getTabs() {
        return tabs;
    }
}

