package helpers.utils;

public class HopTimeInfo {
    public long lastHopTime;
    public long nextHopTime;
    public boolean isHopsPostponed;

    // Constructor when you don't specify isHopsPostponed
    public HopTimeInfo(long lastHopTime, long nextHopTime) {
        this.lastHopTime = lastHopTime;
        this.nextHopTime = nextHopTime;
        this.isHopsPostponed = false; // default
    }

    // Constructor if you want to specify isHopsPostponed
    public HopTimeInfo(long lastHopTime, long nextHopTime, boolean isHopsPostponed) {
        this.lastHopTime = lastHopTime;
        this.nextHopTime = nextHopTime;
        this.isHopsPostponed = isHopsPostponed;
    }
}