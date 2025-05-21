package osr.walker.utils;

import java.awt.*;

public class ChunkData {
    private final Point topLeft;
    private final String plane;

    public ChunkData(Point topLeft, String plane) {
        this.topLeft = topLeft;
        this.plane = plane;
    }

    public Point getTopLeft() {
        return topLeft;
    }

    public String getPlane() {
        return plane;
    }
}

