package helpers.Color.utils;


import java.awt.*;
import java.util.List;

public class ColorRectanglePair {
    private final List<Color> colors;
    private final Rectangle rectangle;

    public ColorRectanglePair(List<Color> colors, Rectangle rectangle) {
        this.colors = colors;
        this.rectangle = rectangle;
    }

    public List<Color> getColors() {
        return colors;
    }

    public Rectangle getRectangle() {
        return rectangle;
    }
}
