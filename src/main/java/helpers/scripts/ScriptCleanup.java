package helpers.scripts;

import helpers.CacheManager;
import helpers.DiscordWebhook;
import helpers.Logger;
import helpers.utils.IsScriptRunning;
import osr.mapping.XPBar;

public class ScriptCleanup {
    private final CacheManager cacheManager;
    private final Logger logger;
    private final DiscordWebhook discordWebhook;
    private final XPBar xpBar;
    private final IsScriptRunning isScriptRunning;

    public ScriptCleanup(CacheManager cacheManager, Logger logger, DiscordWebhook discordWebhook, XPBar xpBar, IsScriptRunning isScriptRunning) {
        this.cacheManager = cacheManager;
        this.logger = logger;
        this.discordWebhook = discordWebhook;
        this.xpBar = xpBar;
        this.isScriptRunning = isScriptRunning;
    }
    public void clean(String deviceID) {
        isScriptRunning.isScriptRunningProperty(deviceID).set(false);
        // Remove XP stuff from XPBar.
        xpBar.removeDeviceData(deviceID);

        logger.clearLogsForEmulator(deviceID);
        discordWebhook.stopWebhookSchedule(deviceID);

        // Clear cachemanager
        cacheManager.cleanCaches(deviceID);
    }
}
