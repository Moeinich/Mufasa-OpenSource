package helpers.testGrounds;

import java.awt.*;

/**
 * Represents color settings used for color matching, including tolerance,
 * hue, and saturation modifiers to determine if colors are close enough to a target.
 */
public class MColor {
    private final Color color;        // RGB color value as an integer
    private final int tolerance;    // Tolerance level for matching colors
    private final double hueMod;    // Modifier for hue comparison
    private final double satMod;    // Modifier for saturation comparison

    /**
     * Constructs an MColor instance with the specified color settings.
     * This constructor is modeled after Pascal's CTS2 function.
     *
     * @param color     The target color as an integer (usually RGB).
     * @param tolerance The tolerance level for RGB matching.
     * @param hueMod    The acceptable variation in hue for color matching.
     * @param satMod    The acceptable variation in saturation for color matching.
     */
    public MColor(Color color, int tolerance, double hueMod, double satMod) {
        this.color = color;
        this.tolerance = tolerance;
        this.hueMod = hueMod;
        this.satMod = satMod;
    }

    /**
     * Gets the target color value.
     *
     * @return The color as an integer (usually RGB).
     */
    public Color getColor() {
        return color;
    }

    /**
     * Gets the tolerance level for matching colors.
     *
     * @return The tolerance level as an integer.
     */
    public int getTolerance() {
        return tolerance;
    }

    /**
     * Gets the hue modifier for color matching.
     *
     * @return The hue modifier as a double.
     */
    public double getHueMod() {
        return hueMod;
    }

    /**
     * Gets the saturation modifier for color matching.
     *
     * @return The saturation modifier as a double.
     */
    public double getSatMod() {
        return satMod;
    }
}