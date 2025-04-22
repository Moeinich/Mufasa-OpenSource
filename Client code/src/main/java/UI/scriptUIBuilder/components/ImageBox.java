package UI.scriptUIBuilder.components;

import UI.scriptUIBuilder.utils.UIElement;
import javafx.scene.image.Image;

public class ImageBox implements UIElement {
    private final String label;
    private final Image image;
    private final double width;
    private final double height;

    public ImageBox(String label, Image image, double width, double height) {
        this.label = label;
        this.image = image;
        this.width = width;
        this.height = height;
    }

    // Getters
    public String getLabel() {
        return label;
    }

    public javafx.scene.image.Image getImage() {
        return image;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }
}

