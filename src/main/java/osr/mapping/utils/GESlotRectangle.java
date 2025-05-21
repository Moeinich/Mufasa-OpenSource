package osr.mapping.utils;

import java.awt.*;

public class GESlotRectangle {
    private final Rectangle buyRect;
    private final Rectangle sellRect;
    private final Rectangle progressRect;
    private final Rectangle typeRect;
    private final Rectangle itemRect;

    public GESlotRectangle(Rectangle buyRect, Rectangle sellRect, Rectangle progressRect, Rectangle typeRect, Rectangle itemRect) {
        this.buyRect = buyRect;
        this.sellRect = sellRect;
        this.progressRect = progressRect;
        this.typeRect = typeRect;
        this.itemRect = itemRect;
    }

    public Rectangle getBuyRect() {
        return buyRect;
    }

    public Rectangle getSellRect() {
        return sellRect;
    }

    public Rectangle getProgressRect() {
        return progressRect;
    }

    public Rectangle getTypeRect() {
        return typeRect;
    }

    public Rectangle getItemRect() {
        return itemRect;
    }
}
