package osr.mapping;

import helpers.Logger;
import helpers.Color.ColorFinder;
import helpers.Color.utils.ColorRectanglePair;
import scripts.APIClasses.ClientAPI;
import scripts.APIClasses.ConditionAPI;

import java.awt.*;
import java.util.Arrays;
import java.util.List;

public class GameTabs {
    private final Logger logger;
    private final ClientAPI clientAPI;
    private final ConditionAPI conditionAPI;
    private final ColorFinder colorFinder;

    // Pre-stored rectangles
    private final Rectangle inventoryRect = new Rectangle(803, 225, 32, 30);
    private final Rectangle statsRect = new Rectangle(842, 224, 32, 29);
    private final Rectangle equipRect = new Rectangle(803, 263, 32, 30);
    private final Rectangle emotesRect = new Rectangle(841, 262, 33, 33);
    private final Rectangle prayerRect = new Rectangle(804, 301, 30, 30);
    private final Rectangle musicRect = new Rectangle(844, 301, 30, 30);
    private final Rectangle magicRect = new Rectangle(803, 340, 31, 31);
    private final Rectangle groupRect = new Rectangle(843, 340, 30, 30);
    private final Rectangle combatRect = new Rectangle(804, 380, 30, 30);
    private final Rectangle friendsRect = new Rectangle(843, 380, 31, 32);
    private final Rectangle questsRect = new Rectangle(805, 419, 28, 31);
    private final Rectangle accountRect = new Rectangle(843, 420, 30, 29);
    private final Rectangle settingsRect = new Rectangle(843, 457, 30, 32);
    private final Rectangle logoutRect = new Rectangle(18, 7, 47, 31);
    private final Rectangle brightnessRect = new Rectangle(740, 233, 35, 17);
    private static final Rectangle bankCheckRect1 = new Rectangle(85, 155, 17, 15);
    private static final Rectangle bankCheckRect2 = new Rectangle(569, 153, 15, 17);
    private static final Rectangle bankCheckRect3 = new Rectangle(569, 479, 13, 16);
    private static final Rectangle bankCheckRect4 = new Rectangle(86, 479, 15, 16);

    private final List<Color> selectedTabColor = Arrays.asList(
            Color.decode("#712e24"),
            Color.decode("#7d382c")
    );
    private static final List<Color> bankBorderColors = Arrays.asList(
            Color.decode("#1c1c19"),
            Color.decode("#30302d")
    );

    private final static List<ColorRectanglePair> colorRectPairs = List.of(
            new ColorRectanglePair(bankBorderColors, bankCheckRect1),
            new ColorRectanglePair(bankBorderColors, bankCheckRect2),
            new ColorRectanglePair(bankBorderColors, bankCheckRect3),
            new ColorRectanglePair(bankBorderColors, bankCheckRect4)
    );

    public GameTabs(Logger logger, ClientAPI clientAPI, ConditionAPI conditionAPI, ColorFinder colorFinder) {
        this.logger = logger;
        this.conditionAPI = conditionAPI;
        this.clientAPI = clientAPI;
        this.colorFinder = colorFinder;
    }

    // Methods to check if a certain tab is open, included overloaded classes for tabs with multiple icons
    public boolean isTabOpen(String device, String tab) {
        return isTabOpenHelper(device, tab);
    }

    public boolean isAnyTabOpen(String device) {
        String[] tabs = {
                "Inventory", "Combat", "Stats", "Quests", "Equip",
                "Prayer", "Magic", "Clan", "Friends", "Account",
                "Logout", "Settings", "Emotes", "Music"
        };

        for (String tab : tabs) {
            if (isTabOpen(device, tab)) {
                return true;
            }
        }
        return false;
    }

    public String getOpenTab(String device) {
        String[] tabs = {
                "Inventory", "Combat", "Stats", "Quests", "Equip",
                "Prayer", "Magic", "Clan", "Friends", "Account",
                "Logout", "Settings", "Emotes", "Music"
        };

        for (String tab : tabs) {
            if (isTabOpen(device, tab)) {
                return tab; // Return the name of the open tab
            }
        }
        return null; // Return null if no tab is open
    }

    // Specific tabs/sections
    public void openBrightnessTab(String device) {
        if (!isTabOpenHelper(device, "Settings")) {
            openTab(device, "Settings");
            conditionAPI.sleep(750, 1250);
        }
        clientAPI.tap(brightnessRect);
        conditionAPI.sleep(750, 1250);
    }

    // Methods to open tabs, including overloaded classes for tabs with multiple icons
    public void openTab(String device, String tab) {
        switch (tab) {
            case "Inventory":
                if (!isTabOpenHelper(device, tab)) {
                    clientAPI.tap(inventoryRect);
                    conditionAPI.wait(() -> isTabOpenHelper(device, tab), 100, 30);
                }
                break;
            case "Combat":
                if (!isTabOpenHelper(device, tab)) {
                    clientAPI.tap(combatRect);
                    conditionAPI.wait(() -> isTabOpenHelper(device, tab), 100, 30);
                }
                break;
            case "Stats":
                if (!isTabOpenHelper(device, tab)) {
                    clientAPI.tap(statsRect);
                    conditionAPI.wait(() -> isTabOpenHelper(device, tab), 100, 30);
                }
                break;
            case "Quests":
                if (!isTabOpenHelper(device, tab)) {
                    clientAPI.tap(questsRect);
                    conditionAPI.wait(() -> isTabOpenHelper(device, tab), 100, 30);
                }
                break;
            case "Equip":
                if (!isTabOpenHelper(device, tab)) {
                    clientAPI.tap(equipRect);
                    conditionAPI.wait(() -> isTabOpenHelper(device, tab), 100, 30);
                }
                break;
            case "Prayer":
                if (!isTabOpenHelper(device, tab)) {
                    clientAPI.tap(prayerRect);
                    conditionAPI.wait(() -> isTabOpenHelper(device, tab), 100, 30);
                }
                break;
            case "Magic":
                if (!isTabOpenHelper(device, tab)) {
                    clientAPI.tap(magicRect);
                    conditionAPI.wait(() -> isTabOpenHelper(device, tab), 100, 30);
                }
                break;
            case "Clan":
                if (!isTabOpenHelper(device, tab)) {
                    clientAPI.tap(groupRect);
                    conditionAPI.wait(() -> isTabOpenHelper(device, tab), 100, 30);
                }
                break;
            case "Friends":
                if (!isTabOpenHelper(device, tab)) {
                    clientAPI.tap(friendsRect);
                    conditionAPI.wait(() -> isTabOpenHelper(device, tab), 100, 30);
                }
                break;
            case "Account":
                if (!isTabOpenHelper(device, tab)) {
                    clientAPI.tap(accountRect);
                    conditionAPI.wait(() -> isTabOpenHelper(device, tab), 100, 30);
                }
                break;
            case "Logout":
                if (!isTabOpenHelper(device, tab)) {
                    clientAPI.tap(logoutRect);
                    conditionAPI.wait(() -> isTabOpenHelper(device, tab), 100, 30);
                }
                break;
            case "Settings":
                if (!isTabOpenHelper(device, tab)) {
                    clientAPI.tap(settingsRect);
                    conditionAPI.wait(() -> isTabOpenHelper(device, tab), 100, 30);
                }
                break;
            case "Emotes":
                if (!isTabOpenHelper(device, tab)) {
                    clientAPI.tap(emotesRect);
                    conditionAPI.wait(() -> isTabOpenHelper(device, tab), 100, 30);
                }
                break;
            case "Music":
                if (!isTabOpenHelper(device, tab)) {
                    clientAPI.tap(musicRect);
                    conditionAPI.wait(() -> isTabOpenHelper(device, tab), 100, 30);
                }
                break;
            default:
                logger.debugLog("Unknown tab name: " + tab, device);
                break;
        }
    }

    // Methods to close tabs, including overloaded classes for tabs with multiple icons
    public void closeTab(String device, String tab) {
        switch (tab) {
            case "Inventory":
                if (isTabOpenHelper(device, tab)) {
                    clientAPI.tap(inventoryRect);
                    conditionAPI.wait(() -> !isTabOpenHelper(device, tab), 100, 30);
                }
                break;
            case "Combat":
                if (isTabOpenHelper(device, tab)) {
                    clientAPI.tap(combatRect);
                    conditionAPI.wait(() -> !isTabOpenHelper(device, tab), 100, 30);
                }
                break;
            case "Stats":
                if (isTabOpenHelper(device, tab)) {
                    clientAPI.tap(statsRect);
                    conditionAPI.wait(() -> !isTabOpenHelper(device, tab), 100, 30);
                }
                break;
            case "Quests":
                if (isTabOpenHelper(device, tab)) {
                    clientAPI.tap(questsRect);
                    conditionAPI.wait(() -> !isTabOpenHelper(device, tab), 100, 30);
                }
                break;
            case "Equip":
                if (isTabOpenHelper(device, tab)) {
                    clientAPI.tap(equipRect);
                    conditionAPI.wait(() -> !isTabOpenHelper(device, tab), 100, 30);
                }
                break;
            case "Prayer":
                if (isTabOpenHelper(device, tab)) {
                    clientAPI.tap(prayerRect);
                    conditionAPI.wait(() -> !isTabOpenHelper(device, tab), 100, 30);
                }
                break;
            case "Magic":
                if (isTabOpenHelper(device, tab)) {
                    clientAPI.tap(magicRect);
                    conditionAPI.wait(() -> !isTabOpenHelper(device, tab), 100, 30);
                }
                break;
            case "Clan":
                if (isTabOpenHelper(device, tab)) {
                    clientAPI.tap(groupRect);
                    conditionAPI.wait(() -> !isTabOpenHelper(device, tab), 100, 30);
                }
                break;
            case "Friends":
                if (isTabOpenHelper(device, tab)) {
                    clientAPI.tap(friendsRect);
                    conditionAPI.wait(() -> !isTabOpenHelper(device, tab), 100, 30);
                }
                break;
            case "Account":
                if (isTabOpenHelper(device, tab)) {
                    clientAPI.tap(accountRect);
                    conditionAPI.wait(() -> !isTabOpenHelper(device, tab), 100, 30);
                }
                break;
            case "Logout":
                if (isTabOpenHelper(device, tab)) {
                    clientAPI.tap(logoutRect);
                    conditionAPI.wait(() -> !isTabOpenHelper(device, tab), 100, 30);
                }
                break;
            case "Settings":
                if (isTabOpenHelper(device, tab)) {
                    clientAPI.tap(settingsRect);
                    conditionAPI.wait(() -> !isTabOpenHelper(device, tab), 100, 30);
                }
                break;
            case "Emotes":
                if (isTabOpenHelper(device, tab)) {
                    clientAPI.tap(emotesRect);
                    conditionAPI.wait(() -> !isTabOpenHelper(device, tab), 100, 30);
                }
                break;
            case "Music":
                if (isTabOpenHelper(device, tab)) {
                    clientAPI.tap(musicRect);
                    conditionAPI.wait(() -> !isTabOpenHelper(device, tab), 100, 30);
                }
                break;
            default:
                logger.debugLog("Unknown tab name: " + tab, device);
                break;
        }
    }

    // Helpers
    private boolean isTabOpenHelper(String device, String tab) {
        switch (tab) {
            case "Inventory":
                if (!colorFinder.isAnyColorInRect(device, selectedTabColor, inventoryRect, 5)) {
                    return colorFinder.areAllColorsInPairs(device, colorRectPairs, 5);
                } else {
                    return true;
                }
            case "Combat":
                return colorFinder.isAnyColorInRect(device, selectedTabColor, combatRect, 5);
            case "Stats":
                return colorFinder.isAnyColorInRect(device, selectedTabColor, statsRect, 5);
            case "Quests":
                return colorFinder.isAnyColorInRect(device, selectedTabColor, questsRect, 5);
            case "Equip":
                return colorFinder.isAnyColorInRect(device, selectedTabColor, equipRect, 5);
            case "Prayer":
                return colorFinder.isAnyColorInRect(device, selectedTabColor, prayerRect, 5);
            case "Magic":
                return colorFinder.isAnyColorInRect(device, selectedTabColor, magicRect, 5);
            case "Clan":
                return colorFinder.isAnyColorInRect(device, selectedTabColor, groupRect, 5);
            case "Friends":
                return colorFinder.isAnyColorInRect(device, selectedTabColor, friendsRect, 5);
            case "Account":
                return colorFinder.isAnyColorInRect(device, selectedTabColor, accountRect, 5);
            case "Logout":
                return colorFinder.isAnyColorInRect(device, selectedTabColor, logoutRect, 5);
            case "Settings":
                return colorFinder.isAnyColorInRect(device, selectedTabColor, settingsRect, 5);
            case "Emotes":
                return colorFinder.isAnyColorInRect(device, selectedTabColor, emotesRect, 5);
            case "Music":
                return colorFinder.isAnyColorInRect(device, selectedTabColor, musicRect, 5);
            default:
                logger.debugLog("Unknown tab name: " + tab, device);
                return false;
        }
    }

    public Rectangle findTabHelper(String device, String tab) {
        switch (tab) {
            case "Inventory":
                return inventoryRect;
            case "Combat":
                return combatRect;
            case "Stats":
                return statsRect;
                case "Quests":
                return questsRect;
                case "Equip":
                return equipRect;
                case "Prayer":
                return prayerRect;
                case "Magic":
                return magicRect;
                case "Clan":
                return groupRect;
                case "Friends":
                return friendsRect;
                case "Account":
                return accountRect;
                case "Logout":
                return logoutRect;
                case "Settings":
                return settingsRect;
                case "Emotes":
                return emotesRect;
                case "Music":
                return musicRect;
            default:
                logger.debugLog("Unknown tab name: " + tab, device);
                return new Rectangle(0, 0, 0, 0);
        }
    }
}
