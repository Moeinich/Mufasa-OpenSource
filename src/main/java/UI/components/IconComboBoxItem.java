package UI.components;


import javafx.scene.image.Image;

public class IconComboBoxItem {
    private final String text;
    private final Image icon;

    public IconComboBoxItem(String text, Image icon) {
        this.text = text;
        this.icon = icon;
    }

    // Getters and setters
    public String getText() {
        return text;
    }

    public Image getIcon() {
        return icon;
    }
}

