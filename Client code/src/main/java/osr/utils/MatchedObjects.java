package osr.utils;

import java.awt.*;
import java.util.List;

public class MatchedObjects {
    private final List<Rectangle> rectangles;
    private final String type;

    public MatchedObjects(List<Rectangle> rectangles, String type) {
        this.rectangles = rectangles;
        this.type = type;
    }

    public List<Rectangle> getRectangles() {
        return rectangles;
    }

    public String getType() {
        return type;
    }
}