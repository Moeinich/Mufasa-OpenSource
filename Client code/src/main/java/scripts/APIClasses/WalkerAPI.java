package scripts.APIClasses;

import helpers.utils.MapChunk;
import helpers.utils.Tile;
import interfaces.iWalker;
import org.opencv.core.Point;
import osr.walker.Walker;
import osr.walker.utils.ChunkCoordinates;
import osr.walker.utils.PositionResult;
import scripts.ScriptInfo;

import static utils.Constants.convertTileArrayToOpenCVPointArray;

public class WalkerAPI implements iWalker {
    private final Walker walker;
    private final ScriptInfo scriptInfo;

    public WalkerAPI(Walker walker, ScriptInfo scriptInfo) {
        this.walker = walker;
        this.scriptInfo = scriptInfo;
    }

    public void setup(MapChunk mapChunk) {
        walker.setup(scriptInfo.getCurrentEmulatorId(), mapChunk);
    }

    // Get player position with a custom minimap region size
    public Tile getPlayerPosition(int regionSize) {
        return walker.getPlayerPosition(scriptInfo.getCurrentEmulatorId(), regionSize).getWorldCoordinates(scriptInfo.getCurrentEmulatorId()).getTile();
    }

    // Get player position
    public Tile getPlayerPosition() {
        return getPlayerPosition(92);
    }

    public Tile getNearestBank() {
        return walker.getNearestBank(scriptInfo.getCurrentEmulatorId());
    }

    public void step(Tile worldmapPoint) {
        Point worldPoint = new Point(worldmapPoint.x, worldmapPoint.y);
        walker.stepToPoint(worldPoint, scriptInfo.getCurrentEmulatorId(), null);
    }

    public void step(Tile worldmapPoint, Runnable actionWhileStepping) {
        Point worldPoint = new Point(worldmapPoint.x, worldmapPoint.y);
        walker.stepToPoint(worldPoint, scriptInfo.getCurrentEmulatorId(), actionWhileStepping);
    }

    public boolean walkTo(Tile worldmapTile) {
        // Get the player's position on the world map
        PositionResult position = walker.getPlayerPosition(scriptInfo.getCurrentEmulatorId());
        ChunkCoordinates playerPoint = position.getWorldCoordinates(scriptInfo.getCurrentEmulatorId());

        return walker.walkTo(new Point(worldmapTile.x, worldmapTile.y), new Point(playerPoint.x, playerPoint.y), scriptInfo.getCurrentEmulatorId(), null);
    }

    public boolean walkTo(Tile worldmapTile, Runnable actionWhileWalking) {
        // Get the player's position on the world map
        PositionResult position = walker.getPlayerPosition(scriptInfo.getCurrentEmulatorId());
        ChunkCoordinates playerPoint = position.getWorldCoordinates(scriptInfo.getCurrentEmulatorId());

        return walker.walkTo(new org.opencv.core.Point(worldmapTile.x, worldmapTile.y), new Point(playerPoint.x, playerPoint.y), scriptInfo.getCurrentEmulatorId(), actionWhileWalking);
    }

    // Walks a path without a specified region
    public Boolean walkPath(Tile[] path) {
        org.opencv.core.Point[] opencvPath = convertTileArrayToOpenCVPointArray(path);
        return walker.walkPath(opencvPath, null, scriptInfo.getCurrentEmulatorId());
    }

    // Walks a path without a specified region with a runnable
    public Boolean walkPath(Tile[] path, Runnable whileRunning) {
        org.opencv.core.Point[] opencvPath = convertTileArrayToOpenCVPointArray(path);
        return walker.walkPath(opencvPath, whileRunning, scriptInfo.getCurrentEmulatorId());
    }

    public boolean webWalk(Tile destinationTile) {
        return walker.webWalk(destinationTile, null, scriptInfo.getCurrentEmulatorId(), false);
    }

    public boolean webWalk(Tile destinationTile, Runnable whileRunning) {
        return walker.webWalk(destinationTile, whileRunning, scriptInfo.getCurrentEmulatorId(), false);
    }

    public boolean webWalk(Tile destinationTile, Runnable whileRunning, boolean stepToEnd) {
        return walker.webWalk(destinationTile, whileRunning, scriptInfo.getCurrentEmulatorId(), stepToEnd);
    }

    public boolean webWalk(Tile destinationTile, boolean stepToEnd) {
        return walker.webWalk(destinationTile, null, scriptInfo.getCurrentEmulatorId(), stepToEnd);
    }

    public Tile[] buildPath(Tile endTile) {
        return walker.buildTilePath(scriptInfo.getCurrentEmulatorId(), endTile, true);
    }

    public boolean isReachable(Tile destinationTile) {
        return walker.isReachable(destinationTile, scriptInfo.getCurrentEmulatorId());
    }
}
