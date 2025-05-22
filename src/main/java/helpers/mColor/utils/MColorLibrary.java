package helpers.mColor.utils;

import helpers.mColor.MColor;

import java.awt.*;
import java.util.List;

public class MColorLibrary {

    // Single instance of MColor for WaterColor
    public static final MColor WATER_COLOR = new MColor(Color.decode("#3a465c"), 35, 0.20, 0.91);

    // List of MColor instances for FishSpotColors
    public static final List<MColor> FISH_SPOT_COLORS = List.of(
            new MColor(Color.decode("#26ffff"), 5, 0.86, 1.88),
            new MColor(Color.decode("#2d606f"), 5, 0.30, 2.30),
            new MColor(Color.decode("#366a7a"), 5, 0.08, 0.68),
            new MColor(Color.decode("#356a7b"), 5, 0.14, 0.32),
            new MColor(Color.decode("#275863"), 5, 0.12, 3.15)
    );

    public static final List<MColor> FISH_SPOT_COLORS_NEW = List.of(
            new MColor(Color.decode("#26ffff"), 3, 0.86, 1.88),
            new MColor(Color.decode("#2d606f"), 1, 0.30, 2.30),
            new MColor(Color.decode("#366a7a"), 1, 0.08, 0.68),
            new MColor(Color.decode("#356a7b"), 1, 0.14, 0.32),
            new MColor(Color.decode("#275863"), 1, 0.12, 3.15)
    );

    public static final List<MColor> AGILITY_SPOT_COLORS = List.of(
            new MColor(Color.decode("#718969"), 5, 0.86, 1.88),
            new MColor(Color.decode("#21ff27"), 5, 0.30, 2.30),
            new MColor(Color.decode("#748d6d"), 5, 0.08, 0.68),
            new MColor(Color.decode("#5e7856"), 5, 0.14, 0.32),
            new MColor(Color.decode("#607e39"), 5, 0.12, 3.15),
            new MColor(Color.decode("#5a7b37"), 5, 0.08, 0.68),
            new MColor(Color.decode("#51622a"), 5, 0.08, 0.68)
            );

    public static final List<MColor> ITEM_SPOT_COLORS = List.of(
            new MColor(Color.decode("#ff3320"), 5, 0.86, 1.88)
    );
}
