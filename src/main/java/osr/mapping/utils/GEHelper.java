package osr.mapping.utils;

import helpers.Color.ColorFinder;

import java.awt.*;
import java.util.List;

import helpers.Color.utils.ColorRectanglePair;
import helpers.visualFeedback.FeedbackObservables;
import org.opencv.core.Rect;

public class GEHelper {
    private final ColorFinder colorFinder;

    private static final GESlotRectangle SLOT_1 = new GESlotRectangle(
            new Rectangle(111, 283, 41, 41), // Buy rect
            new Rectangle(167, 283, 42, 40), // Sell rect
            new Rectangle(106, 322, 106, 2), // Progress rect
            new Rectangle(136, 237, 46, 25),  // Type rect
            new Rectangle(106, 270, 38, 33)   // Item rect
    );

    private static final GESlotRectangle SLOT_2 = new GESlotRectangle(
            new Rectangle(228, 283, 41, 40),
            new Rectangle(284, 283, 41, 40),
            new Rectangle(223, 322, 105, 2),
            new Rectangle(255, 237, 44, 25),
            new Rectangle(223, 270, 37, 32)
    );

    private static final GESlotRectangle SLOT_3 = new GESlotRectangle(
            new Rectangle(345, 282, 41, 42),
            new Rectangle(401, 283, 41, 39),
            new Rectangle(340, 322, 107, 2),
            new Rectangle(371, 237, 44, 23),
            new Rectangle(340, 268, 39, 35)
    );

    private static final GESlotRectangle SLOT_4 = new GESlotRectangle(
            new Rectangle(463, 283, 41, 40),
            new Rectangle(519, 283, 41, 40),
            new Rectangle(457, 322, 105, 2),
            new Rectangle(490, 238, 41, 23),
            new Rectangle(458, 270, 37, 33)
    );

    private static final GESlotRectangle SLOT_5 = new GESlotRectangle(
            new Rectangle(111, 402, 41, 42),
            new Rectangle(168, 403, 41, 40),
            new Rectangle(106, 442, 106, 2),
            new Rectangle(136, 359, 43, 21),
            new Rectangle(107, 387, 37, 36)
    );

    private static final GESlotRectangle SLOT_6 = new GESlotRectangle(
            new Rectangle(228, 402, 41, 41),
            new Rectangle(284, 403, 41, 40),
            new Rectangle(223, 442, 105, 2),
            new Rectangle(256, 358, 39, 22),
            new Rectangle(223, 388, 38, 35)
    );

    private static final GESlotRectangle SLOT_7 = new GESlotRectangle(
            new Rectangle(345, 403, 41, 40),
            new Rectangle(401, 403, 42, 41),
            new Rectangle(340, 442, 106, 2),
            new Rectangle(370, 358, 47, 22),
            new Rectangle(340, 390, 38, 33)
    );

    private static final GESlotRectangle SLOT_8 = new GESlotRectangle(
            new Rectangle(462, 403, 41, 40),
            new Rectangle(518, 403, 41, 40),
            new Rectangle(457, 442, 106, 3),
            new Rectangle(487, 357, 45, 23),
            new Rectangle(458, 390, 37, 33)
    );

    // Button rectangles for when buying/selling
    public final static Rectangle QUANTITY_PLUS_1_RECT = new Rectangle(114, 359, 26, 15);
    public final static Rectangle QUANTITY_PLUS_10_RECT = new Rectangle(154, 359, 25, 15);
    public final static Rectangle QUANTITY_PLUS_100_RECT = new Rectangle(196, 359, 24, 15);
    public final static Rectangle QUANTITY_ALL_RECT = new Rectangle(237, 360, 25, 14);
    public final static Rectangle QUANTITY_CUSTOM_RECT = new Rectangle(277, 359, 25, 16);
    public final static Rectangle MINUS_99_PERCENT_RECT = new Rectangle(325, 360, 27, 14);
    public final static Rectangle MINUS_5_PERCENT_RECT = new Rectangle(367, 360, 26, 12);
    public final static Rectangle MID_PRICE_RECT = new Rectangle(409, 359, 22, 15);
    public final static Rectangle CUSTOM_PRICE_RECT = new Rectangle(449, 357, 24, 17);
    public final static Rectangle PLUS_5_PERCENT_RECT = new Rectangle(489, 359, 27, 15);
    public final static Rectangle PLUS_99_PERCENT_RECT = new Rectangle(531, 360, 25, 15);
    public final static Rectangle CONFIRM_SALE_RECT = new Rectangle(263, 426, 142, 32);
    public final static Rectangle BACK_ARROW_RECT = new Rectangle(119, 438, 17, 7);

    // Rects instead of Rectangles
    public final static Rect GE_SEARCHAREA_RECT = new Rect(19, 26, 485, 109);

    // General rectangles
    public final static Rectangle CLOSE_GE_RECT = new Rectangle(552, 183, 15, 13);
    public final static Rectangle GE_TOPLEFT_CHECK_RECT = new Rectangle(88, 166, 17, 19);
    public final static Rectangle GE_BOTTOMRIGHT_CHECK_RECT = new Rectangle(568, 466, 14, 15);
    public final static Rectangle GE_TEXTCOLOR_CHECK_RECT = new Rectangle(278, 180, 116, 20);
    private final static Rectangle GE_SEARCHAREA_TOPLEFT_RECT = new Rectangle(9, 1, 13, 10);
    private final static Rectangle GE_SEARCHAREA_BOTTOMRIGHT_RECT = new Rectangle(520, 131, 15, 14);
    private final static Rectangle GE_SEARCHAREA_TEXT_RECT = new Rectangle(169, 7, 190, 16);
    private final static Rectangle GE_CHATBOX_SETQUANTITY_RECT = new Rectangle(169, 42, 203, 20);
    public final static Rectangle GE_COLLECT_ALL_RECT = new Rectangle(494, 217, 70, 14);
    public final static Rectangle GE_COLLECT_ITEM1_SLOT_RECT = new Rectangle(472, 427, 35, 30);
    public final static Rectangle GE_COLLECT_ITEM2_SLOT_RECT = new Rectangle(523, 426, 35, 30);

    // Store colors
    public final static java.util.List<Color> GE_RED_PROGRESS_COLOR = List.of(
            Color.decode("#6F0000"),
            Color.decode("#8F0000")
    );
    public final static java.util.List<Color> GE_GREEN_PROGRESS_COLOR = List.of(
            Color.decode("#005F00"),
            Color.decode("#004900")
    );
    public final static java.util.List<Color> GE_ORANGE_PROGRESS_COLOR = List.of(
            Color.decode("#A76318"),
            Color.decode("#D88020")
    );
    public final static java.util.List<Color> GE_PROGRESS_COLORS = List.of(
            Color.decode("#6F0000"),
            Color.decode("#8F0000"),
            Color.decode("#005F00"),
            Color.decode("#004900"),
            Color.decode("#A76318"),
            Color.decode("#D88020")
    );
    private final static java.util.List<Color> SLOT_AVAILABILITY_COLORS = List.of(
            Color.decode("#745D2D"),
            Color.decode("#7E8904"),
            Color.decode("#AFC903"),
            Color.decode("#894A04"),
            Color.decode("#260A00"),
            Color.decode("#745D2D"),
            Color.decode("#C66903"),
            Color.decode("#A78F56")
    );
    private final static java.util.List<Color> SEARCH_BOX_COLORS = List.of(
            Color.decode("#463214"),
            Color.decode("#4F4835"),
            Color.decode("#A3957A"),
            Color.decode("#575040"),
            Color.decode("#867E6B"),
            Color.decode("#A29A8C"),
            Color.decode("#726955"),
            Color.decode("#968C7A"),
            Color.decode("#483A27"),
            Color.decode("#ADA993"),
            Color.decode("#605A4A"),
            Color.decode("#B3B0A8"),
            Color.decode("#BBAB8B"),
            Color.decode("#C4B392"),
            Color.decode("#897D65"),
            Color.decode("#CCBB9A"),
            Color.decode("#7C725C"),
            Color.decode("#B2A384"),
            Color.decode("#938972"),
            Color.decode("#000000")
    );
    public final static Color GE_ORANGE_TEXT_COLOR = Color.decode("#ff981f");
    public final static java.util.List<Color> GE_SLOT_ITEMBG_COLOR = List.of(
            Color.decode("#3f352a")
    );
    public final static java.util.List<Color> GE_OPEN_CHECK_COLOR = List.of(
            Color.decode("#0e0e0c"),
            Color.decode("#1c1c19")
    );
    public final static java.util.List<Color> GE_COLLECT_ALL_COLOR = List.of(
            Color.decode("#4C4D49"),
            Color.decode("#444441"),
            Color.decode("#595954")
    );

    public GEHelper(ColorFinder colorFinder) {
        this.colorFinder = colorFinder;
    }

    // Getter methods for Buy rectangles
    public Rectangle getBuyRect(int slotId) {
        switch (slotId) {
            case 1: return SLOT_1.getBuyRect();
            case 2: return SLOT_2.getBuyRect();
            case 3: return SLOT_3.getBuyRect();
            case 4: return SLOT_4.getBuyRect();
            case 5: return SLOT_5.getBuyRect();
            case 6: return SLOT_6.getBuyRect();
            case 7: return SLOT_7.getBuyRect();
            case 8: return SLOT_8.getBuyRect();
            default: throw new IllegalArgumentException("Invalid slot ID: " + slotId);
        }
    }

    // Getter methods for Sell rectangles
    public Rectangle getSellRect(int slotId) {
        switch (slotId) {
            case 1: return SLOT_1.getSellRect();
            case 2: return SLOT_2.getSellRect();
            case 3: return SLOT_3.getSellRect();
            case 4: return SLOT_4.getSellRect();
            case 5: return SLOT_5.getSellRect();
            case 6: return SLOT_6.getSellRect();
            case 7: return SLOT_7.getSellRect();
            case 8: return SLOT_8.getSellRect();
            default: throw new IllegalArgumentException("Invalid slot ID: " + slotId);
        }
    }

    // Getter methods for Progress rectangles
    public Rectangle getProgressRect(int slotId) {
        switch (slotId) {
            case 1: return SLOT_1.getProgressRect();
            case 2: return SLOT_2.getProgressRect();
            case 3: return SLOT_3.getProgressRect();
            case 4: return SLOT_4.getProgressRect();
            case 5: return SLOT_5.getProgressRect();
            case 6: return SLOT_6.getProgressRect();
            case 7: return SLOT_7.getProgressRect();
            case 8: return SLOT_8.getProgressRect();
            default: throw new IllegalArgumentException("Invalid slot ID: " + slotId);
        }
    }

    // Getter methods for Type rectangles
    public Rectangle getTypeRect(int slotId) {
        switch (slotId) {
            case 1: return SLOT_1.getTypeRect();
            case 2: return SLOT_2.getTypeRect();
            case 3: return SLOT_3.getTypeRect();
            case 4: return SLOT_4.getTypeRect();
            case 5: return SLOT_5.getTypeRect();
            case 6: return SLOT_6.getTypeRect();
            case 7: return SLOT_7.getTypeRect();
            case 8: return SLOT_8.getTypeRect();
            default: throw new IllegalArgumentException("Invalid slot ID: " + slotId);
        }
    }

    // Getter methods for Item rectangles
    public Rectangle getItemRect(int slotId) {
        switch (slotId) {
            case 1: return SLOT_1.getItemRect();
            case 2: return SLOT_2.getItemRect();
            case 3: return SLOT_3.getItemRect();
            case 4: return SLOT_4.getItemRect();
            case 5: return SLOT_5.getItemRect();
            case 6: return SLOT_6.getItemRect();
            case 7: return SLOT_7.getItemRect();
            case 8: return SLOT_8.getItemRect();
            default: throw new IllegalArgumentException("Invalid slot ID: " + slotId);
        }
    }

    public int getFirstAvailableSlot(String device) {
        for (int i = 1; i <= 8; i++) {
            List<ColorRectanglePair> colorRectPairs = List.of(
                    new ColorRectanglePair(SLOT_AVAILABILITY_COLORS, getBuyRect(i)),
                    new ColorRectanglePair(SLOT_AVAILABILITY_COLORS, getSellRect(i))
            );
            if (colorFinder.areAllColorsInPairs(device, colorRectPairs, 10)) {
                return i; // Return the slot number if both checks are true
            }
        }
        return -1; // Return -1 if no available slot is found
    }

    public boolean isSearchAreaVisible(String device) {
        List<ColorRectanglePair> colorRectPairs = List.of(
                new ColorRectanglePair(SEARCH_BOX_COLORS, GE_SEARCHAREA_TOPLEFT_RECT),
                new ColorRectanglePair(SEARCH_BOX_COLORS, GE_SEARCHAREA_BOTTOMRIGHT_RECT),
                new ColorRectanglePair(SEARCH_BOX_COLORS, GE_SEARCHAREA_TEXT_RECT)
        );

        return colorFinder.areAllColorsInPairs(device, colorRectPairs, 5);
    }


    public boolean isSetQuantityVisible(String device) {
        List<ColorRectanglePair> colorRectPairs = List.of(
                new ColorRectanglePair(SEARCH_BOX_COLORS, GE_SEARCHAREA_TOPLEFT_RECT),
                new ColorRectanglePair(SEARCH_BOX_COLORS, GE_SEARCHAREA_BOTTOMRIGHT_RECT),
                new ColorRectanglePair(SEARCH_BOX_COLORS, GE_CHATBOX_SETQUANTITY_RECT)
        );

        return colorFinder.areAllColorsInPairs(device, colorRectPairs, 5);
    }


    public boolean isSlotCancelled(String device, int slot) {
        // Get the progress rectangle for the given slot
        Rectangle progressRect = getProgressRect(slot);
        FeedbackObservables.rectangleObservable.setValue(device, progressRect);

        return !colorFinder.processColorPointsInRect(device, GE_RED_PROGRESS_COLOR, progressRect, 5).isEmpty();
    }

    public boolean hasCollectableItems(String device) {
        return !colorFinder.processColorPointsInRect(device, GE_COLLECT_ALL_COLOR, GE_COLLECT_ALL_RECT, 5).isEmpty();
    }

    public boolean hasSlot1CollectableItems(String device) {
        List<Point> colorPoints = colorFinder.processColorPointsInRect(device, GE_SLOT_ITEMBG_COLOR, GE_COLLECT_ITEM1_SLOT_RECT, 5);
        FeedbackObservables.rectangleObservable.setValue(device, GE_COLLECT_ITEM1_SLOT_RECT);

        return colorPoints.size() < 800;
    }

    public boolean hasSlot2CollectableItems(String device) {
        List<Point> colorPoints = colorFinder.processColorPointsInRect(device, GE_SLOT_ITEMBG_COLOR, GE_COLLECT_ITEM2_SLOT_RECT, 5);
        FeedbackObservables.rectangleObservable.setValue(device, GE_COLLECT_ITEM2_SLOT_RECT);

        return colorPoints.size() < 800;
    }

    public boolean isSlotAvailable(String device, int slot) {
        List<ColorRectanglePair> colorRectPairs = List.of(
                new ColorRectanglePair(SLOT_AVAILABILITY_COLORS, getBuyRect(slot)),
                new ColorRectanglePair(SLOT_AVAILABILITY_COLORS, getSellRect(slot))
        );

        return colorFinder.areAllColorsInPairs(device, colorRectPairs, 10);
    }


    public int slotsAvailable(String device) {
        int availableSlots = 0;

        for (int i = 1; i <= 8; i++) {
            List<ColorRectanglePair> colorRectPairs = List.of(
                    new ColorRectanglePair(SLOT_AVAILABILITY_COLORS, getBuyRect(i)),
                    new ColorRectanglePair(SLOT_AVAILABILITY_COLORS, getSellRect(i))
            );

            if (colorFinder.areAllColorsInPairs(device, colorRectPairs, 10)) {
                availableSlots++;
            }
        }

        return availableSlots;
    }
}
