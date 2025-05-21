package helpers.visualFeedback;

import java.awt.*;

public class PolygonAndPoint {
    private final Polygon polygon;
    private final Point point;

    public PolygonAndPoint(Polygon polygon, Point point) {
        this.polygon = polygon;
        this.point = point;
    }

    public Polygon getPolygon() {
        return polygon;
    }

    public Point getPoint() {
        return point;
    }

    @Override
    public String toString() {
        return "PolygonAndPoint{" +
                "polygon=" + polygon +
                ", point=" + point +
                '}';
    }
}
