package helpers.visualFeedback;

import java.awt.*;

public class RectangleAndRectangle {
    private final Rectangle rectangle1;
    private final Rectangle rectangle2;

    public RectangleAndRectangle(Rectangle rectangle1, Rectangle rectangle2) {
        this.rectangle1 = rectangle1;
        this.rectangle2 = rectangle2;
    }

    public Rectangle getRectangle1() {
        return rectangle1;
    }

    public Rectangle getRectangle2() {
        return rectangle2;
    }

    @Override
    public String toString() {
        return "RectangleAndRectangle{" +
                "rectangle1=" + rectangle1 +
                ", rectangle2=" + rectangle2 +
                '}';
    }
}
