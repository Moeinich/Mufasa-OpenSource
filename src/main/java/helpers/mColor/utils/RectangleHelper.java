package helpers.mColor.utils;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class RectangleHelper {
    /**
     * Removes any rectangle that is fully contained within another rectangle in the list.
     *
     * @param rectangles The list of rectangles to filter.
     * @return A list of rectangles with contained rectangles removed.
     */
    public static List<Rectangle> filterContainedRectangles(List<Rectangle> rectangles) {
        List<Rectangle> filteredRectangles = new ArrayList<>();

        for (Rectangle rect : rectangles) {
            boolean isContained = false;

            for (Rectangle other : rectangles) {
                if (other != rect && other.contains(rect)) {
                    isContained = true;
                    break;
                }
            }

            if (!isContained) {
                filteredRectangles.add(rect);
            }
        }

        return filteredRectangles;
    }

    /**
     * Checks if rect1 is completely contained within rect2.
     *
     * @param rect1 The rectangle to check if it is contained.
     * @param rect2 The rectangle to check if it contains rect1.
     * @return True if rect1 is fully contained within rect2, false otherwise.
     */
    private static boolean isContained(Rectangle rect1, Rectangle rect2) {
        return rect2.contains(rect1);
    }
}
