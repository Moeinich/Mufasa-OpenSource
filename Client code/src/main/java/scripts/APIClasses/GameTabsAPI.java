package scripts.APIClasses;

import helpers.utils.UITabs;
import interfaces.iGameTabs;
import osr.mapping.GameTabs;
import scripts.ScriptInfo;

import java.awt.*;

public class GameTabsAPI implements iGameTabs {
    private final GameTabs gameTabs;
    private final ScriptInfo scriptInfo;

    public GameTabsAPI(GameTabs gameTabs, ScriptInfo scriptInfo) {
        this.gameTabs = gameTabs;
        this.scriptInfo = scriptInfo;
    }

    // isTabOpen part
    public boolean isInventoryTabOpen() {
        return gameTabs.isTabOpen(scriptInfo.getCurrentEmulatorId(), "Inventory");
    }

    public boolean isCombatTabOpen() {
        return gameTabs.isTabOpen(scriptInfo.getCurrentEmulatorId(), "Combat");
    }

    public boolean isStatsTabOpen() {
        return gameTabs.isTabOpen(scriptInfo.getCurrentEmulatorId(), "Stats");
    }

    public boolean isQuestsTabOpen() {
        return gameTabs.isTabOpen(scriptInfo.getCurrentEmulatorId(), "Quests");
    }

    public boolean isEquipTabOpen() {
        return gameTabs.isTabOpen(scriptInfo.getCurrentEmulatorId(), "Equip");
    }

    public boolean isPrayerTabOpen() {
        return gameTabs.isTabOpen(scriptInfo.getCurrentEmulatorId(), "Prayer");
    }

    public boolean isMagicTabOpen() {
        return gameTabs.isTabOpen(scriptInfo.getCurrentEmulatorId(), "Magic");
    }

    public boolean isClanTabOpen() {
        return gameTabs.isTabOpen(scriptInfo.getCurrentEmulatorId(), "Clan");
    }

    public boolean isFriendsTabOpen() {
        return gameTabs.isTabOpen(scriptInfo.getCurrentEmulatorId(), "Friends");
    }

    public boolean isAccountTabOpen() {
        return gameTabs.isTabOpen(scriptInfo.getCurrentEmulatorId(), "Account");
    }

    public boolean isLogoutTabOpen() {
        return gameTabs.isTabOpen(scriptInfo.getCurrentEmulatorId(), "Logout");
    }

    public boolean isSettingsTabOpen() {
        return gameTabs.isTabOpen(scriptInfo.getCurrentEmulatorId(), "Settings");
    }

    public boolean isEmotesTabOpen() {
        return gameTabs.isTabOpen(scriptInfo.getCurrentEmulatorId(), "Emotes");
    }

    public boolean isMusicTabOpen() {
        return gameTabs.isTabOpen(scriptInfo.getCurrentEmulatorId(), "Music");
    }

    // openTab part
    public void openInventoryTab() {
        gameTabs.openTab(scriptInfo.getCurrentEmulatorId(), "Inventory");
    }

    public void openCombatTab() {
        gameTabs.openTab(scriptInfo.getCurrentEmulatorId(), "Combat");
    }

    public void openStatsTab() {
        gameTabs.openTab(scriptInfo.getCurrentEmulatorId(), "Stats");
    }

    public void openQuestsTab() {
        gameTabs.openTab(scriptInfo.getCurrentEmulatorId(), "Quests");
    }

    public void openEquipTab() {
        gameTabs.openTab(scriptInfo.getCurrentEmulatorId(), "Equip");
    }

    public void openPrayerTab() {
        gameTabs.openTab(scriptInfo.getCurrentEmulatorId(), "Prayer");
    }

    public void openMagicTab() {
        gameTabs.openTab(scriptInfo.getCurrentEmulatorId(), "Magic");
    }

    public void openClanTab() {
        gameTabs.openTab(scriptInfo.getCurrentEmulatorId(), "Clan");
    }

    public void openFriendsTab() {
        gameTabs.openTab(scriptInfo.getCurrentEmulatorId(), "Friends");
    }

    public void openAccountTab() {
        gameTabs.openTab(scriptInfo.getCurrentEmulatorId(), "Account");
    }

    public void openLogoutTab() {
        gameTabs.openTab(scriptInfo.getCurrentEmulatorId(), "Logout");
    }

    public void openSettingsTab() {
        gameTabs.openTab(scriptInfo.getCurrentEmulatorId(), "Settings");
    }

    public void openEmotesTab() {
        gameTabs.openTab(scriptInfo.getCurrentEmulatorId(), "Emotes");
    }

    public void openMusicTab() {
        gameTabs.openTab(scriptInfo.getCurrentEmulatorId(), "Music");
    }

    // closeTab part
    public void closeInventoryTab() {
        gameTabs.closeTab(scriptInfo.getCurrentEmulatorId(), "Inventory");
    }

    public void closeCombatTab() {
        gameTabs.closeTab(scriptInfo.getCurrentEmulatorId(), "Combat");
    }

    public void closeStatsTab() {
        gameTabs.closeTab(scriptInfo.getCurrentEmulatorId(), "Stats");
    }

    public void closeQuestsTab() {
        gameTabs.closeTab(scriptInfo.getCurrentEmulatorId(), "Quests");
    }

    public void closeEquipTab() {
        gameTabs.closeTab(scriptInfo.getCurrentEmulatorId(), "Equip");
    }

    public void closePrayerTab() {
        gameTabs.closeTab(scriptInfo.getCurrentEmulatorId(), "Prayer");
    }

    public void closeMagicTab() {
        gameTabs.closeTab(scriptInfo.getCurrentEmulatorId(), "Magic");
    }

    public void closeClanTab() {
        gameTabs.closeTab(scriptInfo.getCurrentEmulatorId(), "Clan");
    }

    public void closeFriendsTab() {
        gameTabs.closeTab(scriptInfo.getCurrentEmulatorId(), "Friends");
    }

    public void closeAccountTab() {
        gameTabs.closeTab(scriptInfo.getCurrentEmulatorId(), "Account");
    }

    public void closeLogoutTab() {
        gameTabs.closeTab(scriptInfo.getCurrentEmulatorId(), "Logout");
    }

    public void closeSettingsTab() {
        gameTabs.closeTab(scriptInfo.getCurrentEmulatorId(), "Settings");
    }

    public void closeEmotesTab() {
        gameTabs.closeTab(scriptInfo.getCurrentEmulatorId(), "Emotes");
    }

    public void closeMusicTab() {
        gameTabs.closeTab(scriptInfo.getCurrentEmulatorId(), "Music");
    }


    // findTab part
    public Rectangle findInventoryTab() {
        return gameTabs.findTabHelper(scriptInfo.getCurrentEmulatorId(), "Inventory");
    }

    public Rectangle findCombatTab() {
        return gameTabs.findTabHelper(scriptInfo.getCurrentEmulatorId(), "Combat");
    }

    public Rectangle findStatsTab() {
        return gameTabs.findTabHelper(scriptInfo.getCurrentEmulatorId(), "Stats");
    }

    public Rectangle findQuestsTab() {
        return gameTabs.findTabHelper(scriptInfo.getCurrentEmulatorId(), "Quests");
    }

    public Rectangle findEquipTab() {
        return gameTabs.findTabHelper(scriptInfo.getCurrentEmulatorId(), "Equip");
    }

    public Rectangle findPrayerTab() {
        return gameTabs.findTabHelper(scriptInfo.getCurrentEmulatorId(), "Prayer");
    }

    public Rectangle findMagicTab() {
        return gameTabs.findTabHelper(scriptInfo.getCurrentEmulatorId(), "Magic");
    }

    public Rectangle findClanTab() {
        return gameTabs.findTabHelper(scriptInfo.getCurrentEmulatorId(), "Clan");
    }

    public Rectangle findFriendsTab() {
        return gameTabs.findTabHelper(scriptInfo.getCurrentEmulatorId(), "Friends");
    }

    public Rectangle findAccountTab() {
        return gameTabs.findTabHelper(scriptInfo.getCurrentEmulatorId(), "Account");
    }

    public Rectangle findLogoutTab() {
        return gameTabs.findTabHelper(scriptInfo.getCurrentEmulatorId(), "Logout");
    }

    public Rectangle findSettingsTab() {
        return gameTabs.findTabHelper(scriptInfo.getCurrentEmulatorId(), "Settings");
    }

    public Rectangle findEmotesTab() {
        return gameTabs.findTabHelper(scriptInfo.getCurrentEmulatorId(), "Emotes");
    }

    public Rectangle findMusicTab() {
        return gameTabs.findTabHelper(scriptInfo.getCurrentEmulatorId(), "Music");
    }


    public void openTab(UITabs UITab) {
        switch (UITab) {
            case ACCOUNT:
                openAccountTab();
                break;
            case CLAN:
                openClanTab();
                break;
            case COMBAT:
                openCombatTab();
                break;
            case EMOTES:
                openEmotesTab();
                break;
            case EQUIP:
                openEquipTab();
                break;
            case FRIENDS:
                openFriendsTab();
                break;
            case INVENTORY:
                openInventoryTab();
                break;
            case LOGOUT:
                openLogoutTab();
                break;
            case MAGIC:
                openMagicTab();
                break;
            case MUSIC:
                openMusicTab();
                break;
            case PRAYER:
                openPrayerTab();
                break;
            case QUESTS:
                openQuestsTab();
                break;
            case SETTINGS:
                openSettingsTab();
                break;
            case STATS:
                openStatsTab();
                break;
            default:
                // Not a correct tab defined?
                break;
        }
    }

    public void closeTab(UITabs UITab) {
        switch (UITab) {
            case ACCOUNT:
                closeAccountTab();
                break;
            case CLAN:
                closeClanTab();
                break;
            case COMBAT:
                closeCombatTab();
                break;
            case EMOTES:
                closeEmotesTab();
                break;
            case EQUIP:
                closeEquipTab();
                break;
            case FRIENDS:
                closeFriendsTab();
                break;
            case INVENTORY:
                closeInventoryTab();
                break;
            case LOGOUT:
                closeLogoutTab();
                break;
            case MAGIC:
                closeMagicTab();
                break;
            case MUSIC:
                closeMusicTab();
                break;
            case PRAYER:
                closePrayerTab();
                break;
            case QUESTS:
                closeQuestsTab();
                break;
            case SETTINGS:
                closeSettingsTab();
                break;
            case STATS:
                closeStatsTab();
                break;
            default:
                // Not a correct tab defined?
                break;
        }
    }

    public boolean isTabOpen(UITabs UITab) {
        switch (UITab) {
            case ACCOUNT:
                return isAccountTabOpen();
            case CLAN:
                return isClanTabOpen();
            case COMBAT:
                return isCombatTabOpen();
            case EMOTES:
                return isEmotesTabOpen();
            case EQUIP:
                return isEquipTabOpen();
            case FRIENDS:
                return isFriendsTabOpen();
            case INVENTORY:
                return isInventoryTabOpen();
            case LOGOUT:
                return isLogoutTabOpen();
            case MAGIC:
                return isMagicTabOpen();
            case MUSIC:
                return isMusicTabOpen();
            case PRAYER:
                return isPrayerTabOpen();
            case QUESTS:
                return isQuestsTabOpen();
            case SETTINGS:
                return isSettingsTabOpen();
            case STATS:
                return isStatsTabOpen();
            default:
                // Not a correct tab defined?
                return false;
        }
    }
}
