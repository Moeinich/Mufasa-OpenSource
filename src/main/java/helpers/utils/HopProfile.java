package helpers.utils;

import java.util.List;
import java.util.Map;

public class HopProfile {
    public List<Integer> filteredWorlds;
    public double hopTimer;
    FilterSettings filterSettings;

    static class FilterSettings {
        Map<String, Boolean> Activities;
        Map<String, Boolean> Type;
        Map<String, Boolean> Locations;
    }
}
