package helpers.visualFeedback;

import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;

public class FeedbackDrawHandler {
    private final Canvas drawingCanvas;
    private final double scale = 0.75;
    private final double offsetX;
    private final double offsetY;

    public FeedbackDrawHandler(Canvas drawingCanvas) {
        this.drawingCanvas = drawingCanvas;

        this.offsetX = (drawingCanvas.getWidth() - (894 * scale)) / 2; // 894 is original image width
        this.offsetY = (drawingCanvas.getHeight() - (540 * scale)) / 2; // 540 is original image height
    }

    // Clear the canvas
    public void clearCanvas() {
        Platform.runLater(() -> {
            GraphicsContext gc = drawingCanvas.getGraphicsContext2D();
            gc.clearRect(0, 0, drawingCanvas.getWidth(), drawingCanvas.getHeight());
        });
    }

    private double scaleValue(double value) {return value * scale;}
    private double adjustValue(double value, double offset) {return scaleValue(value) + offset;}

    // Draw a point
    public void drawPoint(Point point) {
        Platform.runLater(() -> {
            GraphicsContext gc = drawingCanvas.getGraphicsContext2D();
            double x = adjustValue(point.x, offsetX);
            double y = adjustValue(point.y, offsetY);
            gc.setFill(Color.CYAN);
            gc.fillOval(x - 2.5, y - 2.5, 5, 5);
        });
    }

    // Draw a rectangle
    public void drawRectangle(Rectangle rectangle, Color color) {
        Platform.runLater(() -> {
            GraphicsContext gc = drawingCanvas.getGraphicsContext2D();
            double x = adjustValue(rectangle.x, offsetX);
            double y = adjustValue(rectangle.y, offsetY);
            double width = scaleValue(rectangle.width);
            double height = scaleValue(rectangle.height);
            gc.setStroke(color);
            gc.strokeRect(x, y, width, height);
        });
    }

    // Draw a polygon
    public void drawPolygon(Polygon polygon) {
        Platform.runLater(() -> {
            GraphicsContext gc = drawingCanvas.getGraphicsContext2D();
            gc.setStroke(Color.GREEN);

            // Convert and scale coordinates
            double[] xPoints = new double[polygon.npoints];
            double[] yPoints = new double[polygon.npoints];
            for (int i = 0; i < polygon.npoints; i++) {
                xPoints[i] = adjustValue(polygon.xpoints[i], offsetX);
                yPoints[i] = adjustValue(polygon.ypoints[i], offsetY);
            }

            gc.strokePolygon(xPoints, yPoints, polygon.npoints);
        });
    }

    // Draw a rectangle and a point
    public void drawRectangleAndPoint(RectangleAndPoint data) {
        drawRectangle(data.getRectangle(), Color.YELLOW);
        drawPoint(data.getPoint());
    }

    // Draw a polygon and a point
    public void drawPolygonAndPoint(PolygonAndPoint data) {
        drawPolygon(data.getPolygon());
        drawPoint(data.getPoint());
    }

    // Draw two rectangles
    public void drawRectangleAndRectangle(RectangleAndRectangle data) {
        drawRectangle(data.getRectangle1(), Color.YELLOW);
        drawRectangle(data.getRectangle2(), Color.PURPLE);
    }

    public void drawListPointsAndPoint(ListPointsAndPoint data) {
        Platform.runLater(() -> {
            GraphicsContext gc = drawingCanvas.getGraphicsContext2D();

            // Draw all points in the list with 50% opacity
            gc.setFill(Color.YELLOW);
            gc.setGlobalAlpha(0.5);
            for (Point point : data.getPoints()) {
                double x = adjustValue(point.x, offsetX);
                double y = adjustValue(point.y, offsetY);
                gc.fillOval(x - 2.5, y - 2.5, 5, 5);
            }

            // Highlight the chosen point with full opacity
            Point chosenPoint = data.getChosenPoint();
            double x = adjustValue(chosenPoint.x, offsetX);
            double y = adjustValue(chosenPoint.y, offsetY);
            gc.setGlobalAlpha(1.0);
            gc.setFill(Color.CYAN);
            gc.fillOval(x - 3.5, y - 3.5, 7, 7);
        });
    }

    public void drawPointAndPoint(PointAndPoint data) {
        Platform.runLater(() -> {
            GraphicsContext gc = drawingCanvas.getGraphicsContext2D();

            // Get the start and end points
            Point startPoint = data.getStartPoint();
            Point endPoint = data.getEndPoint();

            // Adjust and scale the start and end points
            double startX = adjustValue(startPoint.x, offsetX);
            double startY = adjustValue(startPoint.y, offsetY);
            double endX = adjustValue(endPoint.x, offsetX);
            double endY = adjustValue(endPoint.y, offsetY);

            // Draw the line between the points
            gc.setStroke(Color.YELLOW);
            gc.setLineWidth(2);
            gc.strokeLine(startX, startY, endX, endY);

            // Draw the start point
            gc.setFill(Color.GREEN);
            gc.fillOval(startX - 3, startY - 3, 6, 6);

            // Draw the end point
            gc.setFill(Color.RED);
            gc.fillOval(endX - 3, endY - 3, 6, 6);
        });
    }
}