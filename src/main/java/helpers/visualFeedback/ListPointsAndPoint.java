package helpers.visualFeedback;

import java.awt.Point;
import java.util.List;

public class ListPointsAndPoint {
    private final List<Point> points;
    private final Point chosenPoint;

    public ListPointsAndPoint(List<Point> points, Point chosenPoint) {
        this.points = points;
        this.chosenPoint = chosenPoint;
    }

    // Get the list of points
    public List<Point> getPoints() {
        return points;
    }

    // Get the chosen point
    public Point getChosenPoint() {
        return chosenPoint;
    }

    @Override
    public String toString() {
        return "ListPointsAndPoint{" +
                "points=" + points +
                ", chosenPoint=" + chosenPoint +
                '}';
    }
}