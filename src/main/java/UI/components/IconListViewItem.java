package UI.components;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.image.Image;

public class IconListViewItem {
    private final String text;
    private final ObjectProperty<Image> iconProperty = new SimpleObjectProperty<>();

    public IconListViewItem(String text, Image icon) {
        this.text = text;
        this.iconProperty.set(icon);
    }

    public String getText() {
        return text;
    }

    public ObjectProperty<Image> iconProperty() {
        return iconProperty;
    }

    public Image getIcon() {
        return iconProperty.get();
    }

    public void setIcon(Image icon) {
        this.iconProperty.set(icon);
    }
}
