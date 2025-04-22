package UI.scriptUIBuilder.utils;


import UI.scriptUIBuilder.components.*;
import UI.scriptUIBuilder.components.Tab;
import UI.scriptUIBuilder.components.utils.DescriptionWrapper;
import UI.scriptUIBuilder.components.utils.StackPaneWrapper;
import javafx.collections.FXCollections;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class UIConfigurationRenderer {
    private final Map<String, Control> elementControls = new HashMap<>(); // Stores controls by label
    private final Map<String, String> configurations = new HashMap<>();  // Stores final configurations
    private final Stage stage;

    public UIConfigurationRenderer(String title) {
        stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle(title);
    }

    public Map<String, String> showAndGetConfigurations(UIConfiguration configuration) {
        TabPane tabPane = new TabPane();

        // Generate UI for each tab
        for (Tab tab : configuration.getTabs()) {
            javafx.scene.control.Tab javafxTab = new javafx.scene.control.Tab(tab.getName());
            VBox content = new VBox(10); // Vertical layout with spacing

            for (UIElement element : tab.getElements()) {
                Control control = createControl(element);
                if (control != null) {
                    elementControls.put(element.getLabel(), control);
                    content.getChildren().add(new HBox(new Label(element.getLabel()), control));
                }
            }

            javafxTab.setContent(content);
            tabPane.getTabs().add(javafxTab);
        }

        // Render the UI
        VBox root = new VBox(10, tabPane, createActionButtons());
        Scene scene = new Scene(root, 600, 400);
        stage.setScene(scene);
        stage.showAndWait();

        return collectConfigurations();
    }

    private Control createControl(UIElement element) {
        // Handle StringDropdown
        if (element instanceof StringDropdown) {
            StringDropdown dropdown = (StringDropdown) element;
            ComboBox<String> comboBox = new ComboBox<>(FXCollections.observableArrayList(dropdown.getOptions()));
            comboBox.setValue(dropdown.getDefaultValue());

            // Handle dependencies
            if (dropdown.getParentLabel() != null && dropdown.getDependencyMap() != null) {
                setupDependency(comboBox, dropdown);
            }
            return comboBox;
        }

        // Handle MultiSelectDropdown
        if (element instanceof MultiSelectDropdown) {
            MultiSelectDropdown dropdown = (MultiSelectDropdown) element;
            ListView<String> listView = new ListView<>();
            listView.getItems().addAll(dropdown.getOptions());
            listView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
            dropdown.getDefaultSelections().forEach(s -> listView.getSelectionModel().select(s));

            // Handle dependencies
            if (dropdown.getParentLabel() != null && dropdown.getDependencyMap() != null) {
                setupDependency(listView, dropdown);
            }
            return listView;
        }

        // Handle PercentageSlider
        if (element instanceof PercentageSlider) {
            PercentageSlider slider = (PercentageSlider) element;
            Slider sliderControl = new Slider(slider.getMin(), slider.getMax(), slider.getDefaultValue());
            sliderControl.setShowTickMarks(true);
            sliderControl.setShowTickLabels(true);
            return sliderControl;
        }

        // Handle BooleanCheckbox
        if (element instanceof BooleanCheckbox) {
            BooleanCheckbox checkbox = (BooleanCheckbox) element;
            CheckBox checkBoxControl = new CheckBox();
            checkBoxControl.setSelected(checkbox.getDefaultValue());
            return checkBoxControl;
        }

        // Handle ImageBox
        if (element instanceof ImageBox) {
            ImageBox imageBox = (ImageBox) element;
            ImageView imageView = new ImageView(imageBox.getImage());
            imageView.setFitWidth(imageBox.getWidth());
            imageView.setFitHeight(imageBox.getHeight());
            imageView.setPreserveRatio(true); // Optional: maintain aspect ratio

            // Wrap the ImageView in a StackPaneWrapper
            return new StackPaneWrapper(imageView);
        }

        // Handle Description
        if (element instanceof Description) {
            Description description = (Description) element;
            return new DescriptionWrapper(description.getText());
        }

        return null; // Unsupported UIElement
    }

    private void setupDependency(Control control, UIElement dependentElement) {
        String parentLabel = dependentElement.getParentLabel();
        Map<String, List<String>> dependencyMap = dependentElement.getDependencyMap();

        if (parentLabel != null && dependencyMap != null) {
            Control parentControl = elementControls.get(parentLabel);

            if (parentControl instanceof ComboBox<?>) {
                ComboBox<String> parentComboBox = (ComboBox<String>) parentControl;
                parentComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
                    List<String> updatedOptions = dependencyMap.getOrDefault(newVal, List.of());
                    if (control instanceof ComboBox<?>) {
                        ComboBox<String> dependentComboBox = (ComboBox<String>) control;
                        dependentComboBox.setItems(FXCollections.observableArrayList(updatedOptions));
                        if (!updatedOptions.isEmpty()) {
                            dependentComboBox.setValue(updatedOptions.get(0));
                        }
                    }
                    if (control instanceof ListView<?>) {
                        ListView<String> dependentListView = (ListView<String>) control;
                        dependentListView.getItems().clear();
                        dependentListView.getItems().addAll(updatedOptions);
                    }
                });
                // Initialize with the current parent value
                parentComboBox.getSelectionModel().selectFirst();
            }
        }
    }

    private HBox createActionButtons() {
        Button okButton = new Button("OK");
        Button cancelButton = new Button("Cancel");

        okButton.setOnAction(e -> stage.close());
        cancelButton.setOnAction(e -> {
            configurations.clear(); // Clear configurations if canceled
            stage.close();
        });

        return new HBox(10, okButton, cancelButton);
    }

    private Map<String, String> collectConfigurations() {
        configurations.clear();
        for (Map.Entry<String, Control> entry : elementControls.entrySet()) {
            String label = entry.getKey();
            Control control = entry.getValue();

            if (control instanceof ComboBox<?>) {
                ComboBox<?> comboBox = (ComboBox<?>) control;
                Object value = comboBox.getValue();
                configurations.put(label, value != null ? value.toString() : "");
            } else if (control instanceof ListView<?>) {
                ListView<?> listView = (ListView<?>) control;
                // Explicitly cast items to String and join using Collectors.toList()
                List<String> selectedItems = listView.getSelectionModel().getSelectedItems()
                        .stream()
                        .map(Object::toString)
                        .collect(Collectors.toList());
                configurations.put(label, String.join(", ", selectedItems));
            } else if (control instanceof Slider) {
                Slider slider = (Slider) control;
                configurations.put(label, String.valueOf(slider.getValue()));
            } else if (control instanceof CheckBox) {
                CheckBox checkBox = (CheckBox) control;
                configurations.put(label, String.valueOf(checkBox.isSelected()));
            }
        }
        return configurations;
    }
}
