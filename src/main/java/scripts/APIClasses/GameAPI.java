package scripts.APIClasses;

import helpers.Logger;
import helpers.adb.ADBHandler;
import helpers.utils.*;
import interfaces.iGame;
import osr.mapping.Game;
import osr.walker.Walker;
import osr.walker.utils.PositionResult;
import scripts.ScriptInfo;

import java.awt.*;
import java.time.Duration;
import java.time.Instant;
import java.util.Random;

public class GameAPI implements iGame {
    private final ADBHandler adbHandler;
    private final Game game;
    private final ScriptInfo scriptInfo;
    private final GameTabsAPI gameTabsAPI;
    private final Logger logger;
    private final Walker walker;
    private final Random random = new Random();
    private Instant lastActionTime = Instant.now();
    private Duration nextActionDuration = Duration.ofMinutes(random.nextInt(3) + 2);  // Randomly between 2 and 4 minutes

    public GameAPI(ADBHandler adbHandler, Game game, ScriptInfo scriptInfo, GameTabsAPI gameTabsAPI, Logger logger, Walker walker) {
        this.adbHandler = adbHandler;
        this.game = game;
        this.scriptInfo = scriptInfo;
        this.gameTabsAPI = gameTabsAPI;
        this.logger = logger;
        this.walker = walker;
    }

    public void setCompassAngle(CompassAngle compassAngle) {
        game.setCompassAngle(compassAngle, scriptInfo.getCurrentEmulatorId());
    }

    public void sendChatMessage(String message) {
        // Send a complete string message followed by pressing the Enter key
        adbHandler.executeADBCommand(String.format("shell input text \"%s\\n\"", message), scriptInfo.getCurrentEmulatorId());
    }

    public boolean isGameObjectAt(GameObject gameObject, Point playerPosition, Point worldPosition) {
        org.opencv.core.Point playerPos = new org.opencv.core.Point(playerPosition.x, playerPosition.y);
        org.opencv.core.Point worldPos = new org.opencv.core.Point(worldPosition.x, worldPosition.y);
        return game.isGameObjectAt(scriptInfo.getCurrentEmulatorId(), gameObject, playerPos, worldPos);
    }

    public boolean systemUpdateGoing() {
        return game.isSystemUpdate(scriptInfo.getCurrentEmulatorId());
    }

    public boolean isGameObjectAt(GameObject gameObject, Point worldPosition) {
        org.opencv.core.Point worldPos = new org.opencv.core.Point(worldPosition.x, worldPosition.y);

        PositionResult positionResult = walker.getPlayerPosition(scriptInfo.getCurrentEmulatorId());
        return game.isGameObjectAt(scriptInfo.getCurrentEmulatorId(), gameObject, positionResult.getPosition(), worldPos);
    }

    public boolean isPlayersAround() {
        return game.isPlayersAround(scriptInfo.getCurrentEmulatorId());
    }

    public boolean isPlayersUnderUs() {
        return game.isPlayersUnderUs(scriptInfo.getCurrentEmulatorId());
    }

    public boolean isPlayerAt(Tile tileToCheck) {
        return game.isPlayerAt(scriptInfo.getCurrentEmulatorId(), tileToCheck);
    }

    public boolean isPlayersAround(Tile tileToCheck, int radius) {
        return game.isPlayersAround(scriptInfo.getCurrentEmulatorId(), tileToCheck, radius);
    }

    public void setZoom(String level) {
        game.setZoom(scriptInfo.getCurrentEmulatorId(), level);
    }

    public void hop(String profileName, Boolean useWDH, Boolean useOnlyWDH) {
        if (profileName != null) {
            if (useOnlyWDH) {
                // If useOnlyWDH is true, only use hopIfPlayersAround and do nothing else
                game.hopIfPlayersAround(profileName, scriptInfo.getCurrentEmulatorId());
            } else {
                // If useOnlyWDH is false, use hopWithOptionalWDH and pass useWDH to it
                game.hopWithOptionalWDH(profileName, scriptInfo.getCurrentEmulatorId(), useWDH);
            }
        }
    }

    public void instantHop(String profileName) {
        if (profileName != null) {
            game.instantHop(profileName, scriptInfo.getCurrentEmulatorId());
        }
    }

    public void instantHop(String profileName, Integer world) {
        if (profileName != null) {
            game.performHopSpecifiedWorld(profileName, world, scriptInfo.getCurrentEmulatorId());
        }
    }

    public void switchWorld(String profileName) {
        if (profileName != null) {
            game.switchWorld(profileName, scriptInfo.getCurrentEmulatorId());
        }
    }
    public void switchWorld(String profileName, Integer world) {
        if (profileName != null) {
            game.performSwitchSpecifiedWorld(profileName, world, scriptInfo.getCurrentEmulatorId());
        }
    }
    public void switchWorld() {
        game.switchWorldNoProfile(scriptInfo.getCurrentEmulatorId());
    }

    public boolean timeToHop() {return game.isTimeToHop(scriptInfo.getCurrentEmulatorId(), false);}

    public void postponeHops(boolean state) {
        game.postponeHops(scriptInfo.getCurrentEmulatorId(), state);
    }

    public java.util.List<Integer> getWorldList(String profileName) {
        return game.getWorldHopList(profileName);
    }

    public void setFairyRing(String Letters) {
        game.setFairyRing(scriptInfo.getCurrentEmulatorId(), Letters);
    }

    // Action button stuff here
    public boolean isTapToDropEnabled() {
        return game.isTapToDropEnabled(scriptInfo.getCurrentEmulatorId());
    }

    public boolean isSingleTapEnabled() {
        return game.isSingleTapEnabled(scriptInfo.getCurrentEmulatorId());
    }

    public void enableTapToDrop() {
        game.enableTapToDrop(scriptInfo.getCurrentEmulatorId());
    }

    public void disableTapToDrop() {
        game.disableTapToDrop(scriptInfo.getCurrentEmulatorId());
    }

    public void enableSingleTap() {
        game.enableSingleTap(scriptInfo.getCurrentEmulatorId());
    }

    public void disableSingleTap() {
        game.disableSingleTap(scriptInfo.getCurrentEmulatorId());
    }

    public void openHotkeymenu() {game.openHotkeymenu(scriptInfo.getCurrentEmulatorId());}
    public void closeHotkeymenu() {game.closeHotkeymenu(scriptInfo.getCurrentEmulatorId());}

    public Rectangle findBankOption() {
        return game.findOption(scriptInfo.getCurrentEmulatorId(), "bank");
    }

    public Rectangle findCollectOption() {
        return game.findOption(scriptInfo.getCurrentEmulatorId(), "collect");
    }

    public Rectangle findTalkToOption() {
        return game.findOption(scriptInfo.getCurrentEmulatorId(), "talk-to");
    }

    public Rectangle findPickpocketOption() {
        return game.findOption(scriptInfo.getCurrentEmulatorId(), "pickpocket");
    }

    public Rectangle findBuyPlanksOption() { return game.findOption(scriptInfo.getCurrentEmulatorId(), "buy-planks");}

    public Rectangle findBloomOption() {
        Rectangle castBloom = game.findOption(scriptInfo.getCurrentEmulatorId(), "bloom");
        if (castBloom != null) {
            return castBloom;
        }

        return game.findOption(scriptInfo.getCurrentEmulatorId(), "cast-bloom");
    }

    public void antiAFK() {
        int actionNumber = random.nextInt(5);

        if (Duration.between(lastActionTime, Instant.now()).compareTo(nextActionDuration) >= 0) {
            switch (actionNumber) {
                case 0:
                    logger.debugLog("Performing Anti-AFK action 1", scriptInfo.getCurrentEmulatorId());
                    gameTabsAPI.openStatsTab();
                    sleepRandomly();
                    gameTabsAPI.closeStatsTab();
                    break;
                case 1:
                    logger.debugLog("Performing Anti-AFK action 2", scriptInfo.getCurrentEmulatorId());
                    gameTabsAPI.openSettingsTab();
                    sleepRandomly();
                    gameTabsAPI.closeSettingsTab();
                    break;
                case 2:
                    logger.debugLog("Performing Anti-AFK action 3", scriptInfo.getCurrentEmulatorId());
                    gameTabsAPI.openEquipTab();
                    sleepRandomly();
                    gameTabsAPI.closeEquipTab();
                    break;
                case 3:
                    logger.debugLog("Performing Anti-AFK action 4", scriptInfo.getCurrentEmulatorId());
                    gameTabsAPI.openFriendsTab();
                    sleepRandomly();
                    gameTabsAPI.closeFriendsTab();
                    break;
                case 4:
                    logger.debugLog("Performing Anti-AFK action 5", scriptInfo.getCurrentEmulatorId());
                    gameTabsAPI.openInventoryTab();
                    sleepRandomly();
                    gameTabsAPI.closeInventoryTab();
                    break;
            }

            // Reset the last action time
            lastActionTime = Instant.now();
            nextActionDuration = Duration.ofMinutes(random.nextInt(3) + 2);  // Randomly between 2 and 4 minutes
        }
    }

    public void antiBan() {
        game.performAntiBan(scriptInfo.getCurrentEmulatorId());
    }

    public void enableOptionalAntiBan(AntiBan antiBanOption) {
        game.enableOptionalAntiBan(scriptInfo.getCurrentEmulatorId(), antiBanOption);
    }

    public void addAntiBanSkillList(Skills skill) {
        game.addOptionalAntiBanSkill(scriptInfo.getCurrentEmulatorId(), skill);
    }

    public boolean isXPDropsEnabled() {
        return game.isXPEnabled(scriptInfo.getCurrentEmulatorId());
    }

    public void showXPDrops() {
        game.showXPDrops(scriptInfo.getCurrentEmulatorId());
    }

    public void hideXPDrops() {
        game.hideXPDrops(scriptInfo.getCurrentEmulatorId());
    }

    private void sleepRandomly() {
        try {
            int sleepTime = random.nextInt(1701) + 300; // 300 to 2000 milliseconds
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();  // handle interrupted exception
            logger.devLog("Interrupted during sleep in Anti-AFK action");
        }
    }
}
