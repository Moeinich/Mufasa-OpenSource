package osr.walker.utils;

import helpers.utils.Tile;

import java.awt.*;

public class ChunkCoordinates {
    public final int z;
    public final int x;
    public final int y;

    public ChunkCoordinates(Point localCoordinates, int plane) {
        this.z = plane;
        this.x = localCoordinates.x;
        this.y = localCoordinates.y;
    }

    public Point getPoint() {
        return new Point(x, y);
    }

    public Tile getTile() {
        return new Tile(x, y, z);
    }
}

