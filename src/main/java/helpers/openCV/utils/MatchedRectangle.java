package helpers.openCV.utils;

import java.awt.*;

public class MatchedRectangle extends Rectangle {
    private double matchValue;

    public MatchedRectangle(int x, int y, int width, int height, double matchValue) {
        super(x, y, width, height);
        this.matchValue = matchValue;
    }

    public MatchedRectangle(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    public double getMatchValue() {
        return matchValue;
    }

    public void setMatchValue(double matchValue) {
        this.matchValue = matchValue;
    }
}
