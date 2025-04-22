package helpers.UIDraws;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import org.opencv.core.Rect;

import java.awt.*;
import java.util.List;

public class DrawRects {
    private final Canvas canvas;
    private final GraphicsContext gc;

    public DrawRects(Canvas canvas) {
        this.canvas = canvas;
        this.gc = canvas.getGraphicsContext2D();
    }

    public GraphicsContext getGC() {
        return gc;
    }

    public void drawOnCanvas(double x1, double y1, double x2, double y2, Color color, ShapeType shape) {
        gc.setStroke(color);
        gc.setLineWidth(2);
        if (shape == ShapeType.RECTANGLE) {
            gc.strokeRect(x1, y1, x2, y2);
        } else if (shape == ShapeType.LINE) {
            gc.strokeLine(x1, y1, x2, y2);
        } else if (shape == ShapeType.CIRCLE) {
            gc.strokeOval(x1, y1, x2, y2);
        }
    }

    // Overloaded method for polygons
    public void drawOnCanvas(Color color, double[] xPoints, double[] yPoints, int numPoints) {
        gc.setStroke(color);
        gc.setLineWidth(2);
        gc.strokePolygon(xPoints, yPoints, numPoints);
    }

    public void clearCanvas() {
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
    }

    public void drawRectangles(List<Rectangle> rectangles, Color color) {
        for (Rectangle rectangle : rectangles) {
            drawOnCanvas(rectangle.x, rectangle.y, rectangle.width, rectangle.height, color, ShapeType.RECTANGLE);
        }
    }
    public void drawRectangle(Rectangle rectangle, Color color) {
        drawOnCanvas(rectangle.x, rectangle.y, rectangle.width, rectangle.height, color, ShapeType.RECTANGLE);
    }

    public void drawRect(Rect rectangle, Color color) {
        drawOnCanvas(rectangle.x, rectangle.y, rectangle.width, rectangle.height, color, ShapeType.RECTANGLE);
    }

    public void drawRectangle(Rectangle rectangle, String text, Color color) {
        // Draw the rectangle
        drawOnCanvas(rectangle.x, rectangle.y, rectangle.width, rectangle.height, color, ShapeType.RECTANGLE);

        // Set text properties
        gc.setFill(color);  // Set the text color
        gc.setFont(new Font("Consolas", 14));  // Set the font
        gc.setTextAlign(TextAlignment.CENTER);  // Center text

        // Calculate the center of the rectangle
        double textX = rectangle.x + (double) rectangle.width / 2;
        double textY = rectangle.y + (double) rectangle.height / 2;

        // Correct baseline for y to center text vertically
        gc.setTextBaseline(javafx.geometry.VPos.CENTER);

        // Draw the text
        gc.fillText(text, textX, textY);
    }

    public void drawPoint(double x, double y, Color color) {
        gc.setFill(color);
        gc.fillRect(x, y, 1, 1);
    }

    public void drawPoints(List<Point> points, Color color, boolean drawLines) {
        if (points == null || points.isEmpty()) {
            return;
        }

        Point previousPoint = null;

        for (Point point : points) {
            drawPoint(point.getX(), point.getY(), color);
            if (drawLines && previousPoint != null) {
                drawLineOnCanvas(previousPoint.getX(), previousPoint.getY(), point.getX(), point.getY(), color);
            }
            previousPoint = point;
        }
    }

    public void drawTextOnCanvas(double x, double y, String text, Color color) {
        gc.setFill(color);
        gc.fillText(text, x, y);
    }

    public void drawLineOnCanvas(double startX, double startY, double endX, double endY, Color color) {
        drawOnCanvas(startX, startY, endX, endY, color, ShapeType.LINE);
    }

    public void drawPolygons(List<double[]> xPointsList, List<double[]> yPointsList, Color color) {
        for (int i = 0; i < xPointsList.size(); i++) {
            double[] xPoints = xPointsList.get(i);
            double[] yPoints = yPointsList.get(i);
            drawOnCanvas(color, xPoints, yPoints, xPoints.length);
        }
    }

    public void drawPolygon(double[] xPoints, double[] yPoints, Color color) {
        drawOnCanvas(color, xPoints, yPoints, xPoints.length);
    }

    public void drawPolygon(double[] xPoints, double[] yPoints, String text, Color color) {
        // Draw the polygon
        drawOnCanvas(color, xPoints, yPoints, xPoints.length);

        // Calculate the approximate center of the polygon (average of vertices)
        double centerX = 0;
        double centerY = 0;
        for (int i = 0; i < xPoints.length; i++) {
            centerX += xPoints[i];
            centerY += yPoints[i];
        }
        centerX /= xPoints.length;
        centerY /= yPoints.length;

        // Set text properties
        gc.setFill(color);  // Set the text color
        gc.setFont(new Font("Consolas", 14));  // Set the font
        gc.setTextAlign(TextAlignment.CENTER);  // Center text
        gc.setTextBaseline(javafx.geometry.VPos.CENTER);  // Center vertically

        // Draw the text at the calculated center
        gc.fillText(text, centerX, centerY);
    }
}
