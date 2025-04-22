package helpers.visualFeedback;

import java.awt.*;

public class RectangleAndPoint {
    private final Rectangle rectangle;
    private final Point point;

    public RectangleAndPoint(Rectangle rectangle, Point point) {
        this.rectangle = rectangle;
        this.point = point;
    }

    public Rectangle getRectangle() {
        return rectangle;
    }

    public Point getPoint() {
        return point;
    }

    @Override
    public String toString() {
        return "RectangleAndPoint{" +
                "rectangle=" + rectangle +
                ", point=" + point +
                '}';
    }
}
