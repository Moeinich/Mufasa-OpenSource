package UI.scriptUIBuilder;

import UI.scriptUIBuilder.components.Tab;
import UI.scriptUIBuilder.utils.UIConfiguration;

import java.util.ArrayList;
import java.util.List;

public class UIBuilder {
    private final List<Tab> tabs = new ArrayList<>();

    public Tab addTab(String tabName) {
        Tab tab = new Tab(tabName);
        tabs.add(tab);
        return tab;
    }

    public UIConfiguration build() {
        return new UIConfiguration(tabs);
    }
}

