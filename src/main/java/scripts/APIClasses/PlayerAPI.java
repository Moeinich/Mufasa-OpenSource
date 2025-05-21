package scripts.APIClasses;

import helpers.utils.Area;
import helpers.utils.Tile;
import interfaces.iPlayer;
import osr.mapping.Player;
import scripts.ScriptInfo;

public class PlayerAPI implements iPlayer {
    private final Player player;
    private final ScriptInfo scriptInfo;

    public PlayerAPI(Player player, ScriptInfo scriptInfo) {
        this.player = player;
        this.scriptInfo = scriptInfo;
    }

    public boolean xpGained() {
        return player.xpGained(scriptInfo.getCurrentEmulatorId(), 4000);
    }

    public boolean xpGained(int resetThreshold) {
        return player.xpGained(scriptInfo.getCurrentEmulatorId(), resetThreshold);
    }

    public boolean leveledUp() {
        return player.leveledUp(scriptInfo.getCurrentEmulatorId());
    }

    public boolean within(Area areaToSearchIn) {
        return player.within(scriptInfo.getCurrentEmulatorId(), areaToSearchIn);
    }

    public boolean atTile(Tile tileToCheck) {
        return player.atTile(scriptInfo.getCurrentEmulatorId(), tileToCheck);
    }

    public boolean isTileWithinArea(Tile tileToCheck, Area areaToCheckWithin) {
        // Check if the Z index matches between the tile and the area
        if (tileToCheck.z() != areaToCheckWithin.getTopTile().z()) {
            return false;
        }

        // Define top-left and bottom-right points of the area
        int topLeftX = Math.min(areaToCheckWithin.getTopTile().x(), areaToCheckWithin.getBottomTile().x());
        int topLeftY = Math.max(areaToCheckWithin.getTopTile().y(), areaToCheckWithin.getBottomTile().y());
        int bottomRightX = Math.max(areaToCheckWithin.getTopTile().x(), areaToCheckWithin.getBottomTile().x());
        int bottomRightY = Math.min(areaToCheckWithin.getTopTile().y(), areaToCheckWithin.getBottomTile().y());

        // Create a rectangle from these points
        java.awt.Rectangle areaRectangle = new java.awt.Rectangle(
                topLeftX,
                bottomRightY,
                bottomRightX - topLeftX + 1,
                topLeftY - bottomRightY + 1
        );

        // Define the tile's point
        java.awt.Point tilePoint = new java.awt.Point(tileToCheck.x(), tileToCheck.y());

        // Check if the tile is within the rectangle
        return areaRectangle.contains(tilePoint);
    }

    public boolean tileEquals(Tile originTile, Tile tileToCheck) {
        return player.tileEquals(originTile, tileToCheck);
    }

    public int getHP() {return player.getHP(scriptInfo.getCurrentEmulatorId());}

    public int getPray() {
        return player.getPray(scriptInfo.getCurrentEmulatorId());
    }

    public int getRun() {
        return player.getRun(scriptInfo.getCurrentEmulatorId());
    }

    public int getSpec() {
        return player.getSpec(scriptInfo.getCurrentEmulatorId());
    }

    public void useSpec() {player.useSpec(scriptInfo.getCurrentEmulatorId());}

    public boolean isRunEnabled() {
        return player.isRunEnabled(scriptInfo.getCurrentEmulatorId());
    }

    public void toggleRun() {
        player.toggleRun(scriptInfo.getCurrentEmulatorId());
    }

    public void enableAutoRetaliate() {
        player.enableAutoRetaliate(scriptInfo.getCurrentEmulatorId());
    }

    public void disableAutoRetaliate() {
        player.disableAutoRetaliate(scriptInfo.getCurrentEmulatorId());
    }

    public boolean isAutoRetaliateOn() {
        return player.isAutoRetaliateOn(scriptInfo.getCurrentEmulatorId());
    }

    public boolean isIdle() {return player.isIdle(scriptInfo.getCurrentEmulatorId());}

    public double currentPixelShift() {return player.currentPixelShift(scriptInfo.getCurrentEmulatorId());}

    public boolean waitTillMoving(int checkTimes) {return player.checkMovement(checkTimes, true, scriptInfo.getCurrentEmulatorId());}

    public boolean waitTillNotMoving(int checkTimes) {return player.checkMovement(checkTimes, false, scriptInfo.getCurrentEmulatorId());}
}
