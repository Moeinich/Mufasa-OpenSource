package osr.utils;

import java.awt.*;

public class NamedRectangle extends Rectangle {
    private String name;

    public NamedRectangle(String name, int x, int y, int width, int height) {
        super(x, y, width, height);
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
