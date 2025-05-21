package UI.components;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Slider;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;

public class SliderWithText extends Slider {
    Text text;

    public SliderWithText(double min, double max, double value) {
        super(min, max, value);
        this.setShowTickLabels(false);
        this.setShowTickMarks(false);
        this.setMaxWidth(Double.MAX_VALUE);

        // Force the slider to snap to integer values by rounding down
        this.valueProperty().addListener((obs, old, val) -> {
            double roundedValue = Math.floor(val.doubleValue());  // Round down to the nearest integer
            setValue(roundedValue);
            if (text != null) {
                text.setText(String.valueOf((int) roundedValue));
            }
        });
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();

        if (text == null) {
            text = new Text(formatValue((int) getValue()));

            // Set the text properties for appearance and alignment
            text.setStyle("-fx-font-size: 10px;"); // Adjust font size

            // Locate the thumb and center the text within it
            StackPane thumb = (StackPane) lookup(".thumb");
            if (thumb != null) {
                thumb.getChildren().add(text);

                // Ensure the thumb stays consistent in size
                thumb.setPrefWidth(35);  // Fixed width for the thumb
                thumb.setMinWidth(35);   // Minimum width
                thumb.setMaxWidth(35);   // Maximum width

                // Ensure the text is centered within the fixed thumb size
                StackPane.setAlignment(text, Pos.CENTER);
                thumb.setPadding(new Insets(5));
            }
        }
    }

    private String formatValue(int value) {
        return String.valueOf(value);
    }
}