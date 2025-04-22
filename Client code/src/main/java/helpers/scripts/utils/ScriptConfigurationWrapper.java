package helpers.scripts.utils;

import helpers.annotations.ScriptConfiguration;

import java.util.ArrayList;
import java.util.List;

public class ScriptConfigurationWrapper {
    private final List<ScriptConfiguration> standaloneConfigurations;
    private final List<ScriptTabGroup> tabGroups;

    public ScriptConfigurationWrapper() {
        this.standaloneConfigurations = new ArrayList<>();
        this.tabGroups = new ArrayList<>();
    }

    public List<ScriptConfiguration> getStandaloneConfigurations() {
        return standaloneConfigurations;
    }

    public List<ScriptTabGroup> getTabGroups() {
        return tabGroups;
    }

    public static class ScriptTabGroup {
        private final String tabName;
        private final List<ScriptConfiguration> configurations;

        public ScriptTabGroup(String tabName, List<ScriptConfiguration> configurations) {
            this.tabName = tabName;
            this.configurations = configurations;
        }

        public String getTabName() {
            return tabName;
        }

        public List<ScriptConfiguration> getConfigurations() {
            return configurations;
        }
    }
}