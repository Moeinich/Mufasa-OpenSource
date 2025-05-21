package osr.walker;

import org.opencv.core.Mat;

public class MapInfo {
    private Mat map;

    public MapInfo(Mat map) {
        this.map = map;
    }

    public Mat getMap() {
        return map;
    }

    public void setMap(Mat map) {
        this.map = map;
    }
}
