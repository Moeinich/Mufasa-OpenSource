package helpers.visualFeedback;

import java.awt.Point;

public class PointAndPoint {
    private final Point startPoint;
    private final Point endPoint;

    public PointAndPoint(Point startPoint, Point endPoint) {
        this.startPoint = startPoint;
        this.endPoint = endPoint;
    }

    public Point getStartPoint() {
        return startPoint;
    }

    public Point getEndPoint() {
        return endPoint;
    }
}