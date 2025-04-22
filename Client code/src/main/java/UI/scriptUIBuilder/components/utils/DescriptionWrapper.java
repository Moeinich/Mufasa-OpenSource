package UI.scriptUIBuilder.components.utils;

import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

public class DescriptionWrapper extends Control {
    private final Label descriptionLabel;

    public DescriptionWrapper(String text) {
        descriptionLabel = new Label(text);
        descriptionLabel.setWrapText(true); // Allow wrapping for long text
        StackPane stackPane = new StackPane(descriptionLabel); // Wrap in a StackPane for alignment
        getChildren().add(stackPane);
    }

    public Label getDescriptionLabel() {
        return descriptionLabel;
    }
}

