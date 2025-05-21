package UI.components;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.util.Callback;
import org.controlsfx.control.SearchableComboBox;

public class MultiSelectComboBox extends SearchableComboBox<IconListViewItem> {
    private final ObservableList<IconListViewItem> selectedItems = FXCollections.observableArrayList();

    public MultiSelectComboBox() {
        super();
        selectedItems.clear();

        // Set cell factory to add checkboxes and bind the icon property
        setCellFactory(new Callback<>() {
            @Override
            public ListCell<IconListViewItem> call(ListView<IconListViewItem> param) {
                return new ListCell<>() {
                    private final CheckBox checkBox = new CheckBox();
                    private final ImageView iconView = new ImageView();
                    private final Label textLabel = new Label();
                    private final HBox content = new HBox(checkBox, iconView, textLabel); // Order: checkbox, icon, text
                    {
                        content.setSpacing(5); // Add spacing between elements if needed
                        content.setAlignment(Pos.CENTER_LEFT); // Center items vertically
                        // Resize the checkbox to match the icon's size
                        checkBox.setStyle("-fx-font-size: 12px;"); // Adjust font size to increase checkbox size
                        iconView.setFitHeight(16); // Set icon size (adjust as necessary)
                        iconView.setPreserveRatio(true);

                        // Add mouse click event to the entire cell
                        content.setOnMouseClicked(event -> toggleSelection());

                        checkBox.setOnAction(event -> toggleSelection());
                    }

                    private void toggleSelection() {
                        IconListViewItem item = getItem();
                        if (item != null) {
                            if (selectedItems.contains(item)) {
                                selectedItems.remove(item);
                                checkBox.setSelected(false);
                            } else {
                                selectedItems.add(item);
                                checkBox.setSelected(true);
                            }
                            updatePromptText();
                        }
                    }

                    @Override
                    protected void updateItem(IconListViewItem item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item != null && !empty) {
                            textLabel.setText(item.getText()); // Set label text
                            iconView.imageProperty().bind(item.iconProperty()); // Bind icon property
                            checkBox.setSelected(selectedItems.contains(item));
                            setGraphic(content); // Set the HBox as the cell's graphic
                        } else {
                            setGraphic(null);
                        }
                    }
                };
            }
        });

        // Display selected items' summary in ComboBox prompt
        setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(IconListViewItem item, boolean empty) {
                super.updateItem(item, empty);
                setText(getSelectedSummaryText());
            }
        });
    }

    public void updatePromptText() {
        setPromptText(getSelectedSummaryText());
        // Update the ButtonCell text to reflect the selected items
        if (getButtonCell() != null) {
            getButtonCell().setText(getSelectedSummaryText());
        }
    }

    private String getSelectedSummaryText() {
        return selectedItems.stream()
                .map(IconListViewItem::getText)
                .reduce((text1, text2) -> text1 + ", " + text2)
                .orElse("Select Options");
    }

    public ObservableList<IconListViewItem> getSelectedItems() {
        return selectedItems;
    }
}
