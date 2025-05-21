package helpers.Color.utils;

import java.awt.*;

public class ColorPointPair {
    private final Color color;
    private final Point point;
    private final int tolerance;

    public ColorPointPair(Color color, Point point, int tolerance) {
        this.color = color;
        this.point = point;
        this.tolerance = tolerance;
    }

    public Color getColor() {
        return color;
    }

    public Point getPoint() {
        return point;
    }

    public int getTolerance() {
        return tolerance;
    }
}
