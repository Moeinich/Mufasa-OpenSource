package helpers.visualFeedback;

import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;

public class FeedbackObservables {
    // Specific observables for Rectangle, Point, and Polygon
    public static VisualObservable<Rectangle> rectangleObservable = new VisualObservable<>();
    public static VisualObservable<Point> pointObservable = new VisualObservable<>();
    public static VisualObservable<Polygon> polygonObservable = new VisualObservable<>();

    // Observables for combinations
    public static VisualObservable<RectangleAndPoint> rectangleAndPointObservable = new VisualObservable<>();
    public static VisualObservable<PolygonAndPoint> polygonAndPointObservable = new VisualObservable<>();
    public static VisualObservable<RectangleAndRectangle> rectangleAndRectangleObservable = new VisualObservable<>();
    public static VisualObservable<ListPointsAndPoint> listPointsAndPointObservable = new VisualObservable<>();
    public static VisualObservable<PointAndPoint> pointAndPointObservable = new VisualObservable<>();
}
