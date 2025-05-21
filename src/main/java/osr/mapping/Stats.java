package osr.mapping;

import helpers.Logger;
import helpers.OCR.ReadLevels;
import helpers.utils.Skills;

import java.awt.*;
import java.util.List;

public class Stats {
    private final Logger logger;
    private final ReadLevels readLevels;
    private final List<Color> stackColors = List.of(
            Color.decode("#ffff00")
    );

    public Stats(Logger logger, ReadLevels ReadLevels) {
        this.logger = logger;
        this.readLevels = ReadLevels;
    }

    // Public methods
    public Integer getRealLevelCF(Skills skill, String device) {
        return getLevel("Real", capitalizeFirstLetter(skill.name()), device);
    }
    
    public Integer getEffectiveLevelCF(Skills skill, String device) {
        return getLevel("Effective", capitalizeFirstLetter(skill.name()), device);
    }

    // Private helper method
    private Integer getLevel(String type, String skill, String device) {
        String level = readLevels.getSkillLevelCF(type, skill, device, stackColors);

        // Check if the level is not empty, if it is, return 1
        if (level == null || level.isEmpty()) {
            return 1;
        }

        // Return the integer after verifying it is numeric and non-empty
        return isNumeric(level) ? Integer.parseInt(level) : 1;
    }

    public Integer getTotalLevelCF(String device) {
        String level = readLevels.getTotalLevelCF(device, stackColors);
        logger.devLog("Results of getSkillLevel CF action: " + level);

        // Split the result to get a single line of the digits
        String[] levels = level.split("\n");
        String totalLevel = levels[0].trim();
        // Return the integer
        return Integer.parseInt(totalLevel);
    }

    // Helper method to determine if a string is numeric
    private boolean isNumeric(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private String capitalizeFirstLetter(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return input.substring(0, 1).toUpperCase() + input.substring(1).toLowerCase();
    }
}
