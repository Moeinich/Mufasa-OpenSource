package UI.scriptUIBuilder.components.utils;

import javafx.scene.control.Control;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

public class StackPaneWrapper extends Control {
    private final StackPane stackPane;

    public StackPaneWrapper(ImageView imageView) {
        stackPane = new StackPane(imageView);
        getChildren().add(stackPane);
    }

    public StackPane getStackPane() {
        return stackPane;
    }
}

