package UI.scripts;

import UI.components.IconComboBoxItem;
import javafx.scene.control.ListCell;
import javafx.scene.image.ImageView;
import javafx.util.StringConverter;
import org.controlsfx.control.SearchableComboBox;

public class ComboBoxCellFactory {
    public static void setupComboBoxCellFactory(SearchableComboBox<IconComboBoxItem> comboBox) {
        // Set the cell factory to dictate how list cells should be filled
        comboBox.setCellFactory(lv -> new ListCell<>() {
            private final ImageView imageView = new ImageView();

            @Override
            protected void updateItem(IconComboBoxItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);  // No text in label of cell
                    setGraphic(null);  // No graphic in cell
                } else {
                    setText(item.getText());  // Set text of the cell

                    // If an icon is present, display it alongside the text
                    if (item.getIcon() != null) {
                        imageView.setImage(item.getIcon());
                        imageView.setPreserveRatio(true);
                        imageView.setFitHeight(20);  // Set the height of the icon, maintaining aspect ratio
                        setGraphic(imageView);  // Set graphic of the cell
                    } else {
                        setGraphic(null);  // If no icon, do not display a graphic
                    }
                }
            }
        });

        // Set the button cell factory to display the selected item with its icon
        comboBox.setButtonCell(new ListCell<>() {
            private final ImageView imageView = new ImageView();

            @Override
            protected void updateItem(IconComboBoxItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);  // No text in label of cell
                    setGraphic(null);  // No graphic in cell
                } else {
                    setText(item.getText());  // Set text of the cell

                    // If an icon is present, display it alongside the text
                    if (item.getIcon() != null) {
                        imageView.setImage(item.getIcon());
                        imageView.setPreserveRatio(true);
                        imageView.setFitHeight(20);  // Set the height of the icon, maintaining aspect ratio
                        setGraphic(imageView);  // Set graphic of the cell
                    } else {
                        setGraphic(null);  // If no icon, do not display a graphic
                    }
                }
            }
        });

        // Optionally, set a converter if needed for editing or other purposes
        comboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(IconComboBoxItem object) {
                return object != null ? object.getText() : "";
            }

            @Override
            public IconComboBoxItem fromString(String string) {
                // This method might not be necessary unless the ComboBox is editable
                // It would be needed to determine how a String should be converted back to an IconComboBoxItem
                return comboBox.getItems().stream()
                        .filter(item -> item.getText().equals(string))
                        .findFirst()
                        .orElse(null);
            }
        });
    }
}
