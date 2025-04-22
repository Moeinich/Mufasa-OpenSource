package UI.scripts;

import UI.components.IconComboBoxItem;
import UI.components.IconListViewItem;
import UI.components.MultiSelectComboBox;
import UI.components.SliderWithText;
import helpers.annotations.AllowedValue;
import helpers.annotations.ScriptConfiguration;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.controlsfx.control.SearchableComboBox;
import osr.mapping.utils.ItemProcessor;
import osr.mapping.utils.WorldHopperUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static UI.scripts.ComboBoxCellFactory.setupComboBoxCellFactory;
import static utils.Constants.BANK_IMAGE;

public class AnnotationControls {
    private final ItemProcessor itemProcessor;
    private final WorldHopperUtils worldHopperUtils;

    public AnnotationControls(ItemProcessor itemProcessor, WorldHopperUtils worldHopperUtils) {
        this.itemProcessor = itemProcessor;
        this.worldHopperUtils = worldHopperUtils;
    }

    public Control createStringControl(ScriptConfiguration config, String lastValue) {
        SearchableComboBox<IconComboBoxItem> comboBox = new SearchableComboBox<>();
        comboBox.setMaxWidth(Double.MAX_VALUE);

        for (AllowedValue allowedValue : config.allowedValues()) {
            Image icon = null;
            if (!allowedValue.optionIcon().equals("0")) {
                icon = itemProcessor.getItemImageFX(allowedValue.optionIcon());
            }
            IconComboBoxItem item = new IconComboBoxItem(allowedValue.optionName(), icon);
            comboBox.getItems().add(item);
        }

        setupComboBoxCellFactory(comboBox);

        // Set the lastValue or select the first item by default
        if (lastValue != null) {
            comboBox.getItems().stream()
                    .filter(item -> item.getText().equals(lastValue))
                    .findFirst()
                    .ifPresent(comboBox.getSelectionModel()::select);
        } else if (!comboBox.getItems().isEmpty()) {
            comboBox.getSelectionModel().selectFirst(); // Select the first item as default
        }

        return comboBox;
    }

    public Control createIntegerControl(ScriptConfiguration config, String lastValue) {
        Spinner<Integer> spinner = new Spinner<>();
        spinner.setMaxWidth(Double.MAX_VALUE);
        spinner.setEditable(true);

        // Parse the default and last values
        int spinnerValue = lastValue != null ? Integer.parseInt(lastValue) : Integer.parseInt(config.defaultValue());
        int[] minMax = config.minMaxIntValues();

        int min, max;
        if (minMax.length == 2) {
            // Use specified min and max values
            min = minMax[0];
            max = minMax[1];
        } else {
            // Default range if minMaxIntValues is not set properly
            min = 1;
            max = 100;
        }

        // Ensure the value is within bounds
        if (spinnerValue < min) spinnerValue = min;
        if (spinnerValue > max) spinnerValue = max;

        SpinnerValueFactory<Integer> valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(min, max, spinnerValue);
        spinner.setValueFactory(valueFactory);
        return spinner;
    }

    public Control createIntegerSliderControl(ScriptConfiguration config, String lastValue) {
        // Parse the last value or default value
        double sliderValue = lastValue != null ? Double.parseDouble(lastValue) : Double.parseDouble(config.defaultValue());
        int[] sliderMinMax = config.minMaxIntValues();

        double sliderMin, sliderMax;
        if (sliderMinMax.length == 2) {
            // Use specified min and max values
            sliderMin = sliderMinMax[0];
            sliderMax = sliderMinMax[1];
        } else {
            // Default range if minMaxIntValues is not set properly
            sliderMin = 1;
            sliderMax = 100;
        }

        // Ensure the value is within bounds
        if (sliderValue < sliderMin) sliderValue = sliderMin;
        if (sliderValue > sliderMax) sliderValue = sliderMax;

        // Create a SliderWithText with the specified min, max, and value
        SliderWithText slider = new SliderWithText(sliderMin, sliderMax, sliderValue);
        slider.setShowTickLabels(false);
        slider.setShowTickMarks(false);
        return slider;
    }

    public Control createBooleanControl(ScriptConfiguration config, String lastValue) {
        CheckBox checkBox = new CheckBox();
        boolean checkBoxValue = lastValue != null ? Boolean.parseBoolean(lastValue) : Boolean.parseBoolean(config.defaultValue());
        checkBox.setSelected(checkBoxValue);
        return checkBox;
    }

    public Control createMultiSelectControl(ScriptConfiguration config, String lastValue) {
        MultiSelectComboBox multiSelectComboBox = new MultiSelectComboBox();

        // Populate items for the MultiSelectComboBox
        List<IconListViewItem> items = new ArrayList<>();
        for (AllowedValue allowedValue : config.allowedValues()) {
            Image icon = null;
            if (!allowedValue.optionIcon().equals("0")) {
                icon = itemProcessor.getItemImageFX(allowedValue.optionIcon());
            }
            IconListViewItem item = new IconListViewItem(allowedValue.optionName(), icon);
            items.add(item);
        }
        multiSelectComboBox.setItems(FXCollections.observableArrayList(items));

        // Add saved or default selections
        if (!lastValue.isEmpty()) {
            String[] selections = lastValue.split(",");
            for (String selection : selections) {
                multiSelectComboBox.getItems().stream()
                        .filter(item -> item.getText().equals(selection.trim()))
                        .findFirst()
                        .ifPresent(multiSelectComboBox.getSelectedItems()::add);
            }
        }
        multiSelectComboBox.updatePromptText();

        return multiSelectComboBox;
    }

    public Control createPercentageControl(ScriptConfiguration config, VBox vbox, String lastValue) {
        double initialValue = lastValue != null ? Double.parseDouble(lastValue) : Double.parseDouble(config.defaultValue());
        SliderWithText percentageSlider = new SliderWithText(0, 100, initialValue);
        percentageSlider.setMin(0);
        percentageSlider.setMax(100);
        percentageSlider.setValue(initialValue);
        percentageSlider.setShowTickLabels(false);
        percentageSlider.setShowTickMarks(false);
        percentageSlider.setMajorTickUnit(10);
        percentageSlider.setBlockIncrement(1);
        percentageSlider.setMaxWidth(Double.MAX_VALUE);

        // Add the slider and label to a StackPane
        StackPane sliderPane = new StackPane();
        sliderPane.getChildren().addAll(percentageSlider);
        vbox.getChildren().add(sliderPane);

        // Assigning control for later use
        return percentageSlider;
    }

    public Node createTextAreaControl(ScriptConfiguration configuration, String lastValue) {
        TextArea textArea = new TextArea();
        textArea.setWrapText(true);
        textArea.getStyleClass().add("text-area");
        textArea.setPrefHeight(80);
        textArea.setMaxHeight(80);

        if (lastValue != null && !lastValue.isEmpty() && !lastValue.equals(configuration.defaultValue())) {
            textArea.setText(lastValue);
        } else {
            textArea.setPromptText(configuration.defaultValue());
        }

        return textArea;
    }

    public Node createWorldhopperControl(Map<String, Control> controlMap, ScriptConfiguration config, String lastValue, String lastWDHValue, boolean lastEnabledValue) {
        // Retrieve the default values from the configuration or use saved values
        boolean isWorldHoppingEnabled = lastEnabledValue || Boolean.parseBoolean(config.defaultValue());
        boolean isWDHEnabled = lastWDHValue != null ? Boolean.parseBoolean(lastWDHValue) : Boolean.parseBoolean(config.wdhEnabled());

        // Toggle button for world hopping
        ToggleButton toggleWorldHopping = new ToggleButton(isWorldHoppingEnabled ? "Disable worldhopping" : "Enable worldhopping");
        toggleWorldHopping.setSelected(isWorldHoppingEnabled);

        // Dropdown box for world hopping profiles
        SearchableComboBox<String> worldHoppingOptions = new SearchableComboBox<>();
        worldHoppingOptions.getStyleClass().add("main-dropdown");
        worldHoppingOptions.getItems().addAll(worldHopperUtils.getProfileNames());

        if (!worldHoppingOptions.getItems().isEmpty()) {
            if (lastValue != null && !lastValue.isEmpty()) {
                worldHoppingOptions.getItems().stream()
                        .filter(profile -> profile.equals(lastValue))
                        .findFirst()
                        .ifPresent(worldHoppingOptions.getSelectionModel()::select);
            } else {
                worldHoppingOptions.getSelectionModel().selectFirst();
            }
        }
        worldHoppingOptions.setVisible(isWorldHoppingEnabled);
        worldHoppingOptions.setManaged(isWorldHoppingEnabled);

        // Checkbox for WDH
        CheckBox checkBoxWDH = new CheckBox("Auto hop from players (WDH)");
        checkBoxWDH.getStyleClass().add("label");
        checkBoxWDH.setSelected(isWDHEnabled);
        // Visibility of WDH should depend on whether world hopping is enabled
        checkBoxWDH.setVisible(isWorldHoppingEnabled);
        checkBoxWDH.setManaged(isWorldHoppingEnabled);

        // Listener for the world-hopping toggle button
        toggleWorldHopping.selectedProperty().addListener((obs, wasPreviouslySelected, isNowSelected) -> {
            worldHoppingOptions.setVisible(isNowSelected);
            worldHoppingOptions.setManaged(isNowSelected);
            checkBoxWDH.setVisible(isNowSelected);
            checkBoxWDH.setManaged(isNowSelected);
            toggleWorldHopping.setText(isNowSelected ? "Disable worldhopping" : "Enable worldhopping");
        });

        // Create HBox for alignment
        HBox worldhopperHBox = new HBox(10);
        worldhopperHBox.setAlignment(Pos.CENTER_LEFT);

        // Add elements to HBox
        worldhopperHBox.getChildren().add(toggleWorldHopping);

        // Spacer to push the next elements to the right
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        worldhopperHBox.getChildren().add(spacer);

        // Add worldHoppingOptions and checkBoxWDH to the right
        worldhopperHBox.getChildren().addAll(worldHoppingOptions, checkBoxWDH);

        // Assigning controls for later use
        controlMap.put(config.name() + ".enabled", toggleWorldHopping);
        controlMap.put(config.name() + ".useWDH", checkBoxWDH);
        controlMap.put(config.name(), worldHoppingOptions);  // Assuming you need to retrieve the ComboBox value

        // Return the HBox to be added to the HBox in renderConfiguration
        return worldhopperHBox;
    }

    public Control createDescriptionControl(ScriptConfiguration config) {
        // Create a Label for the description text
        Label descriptionLabel = new Label(config.description());
        descriptionLabel.getStyleClass().add("text-white");
        descriptionLabel.setWrapText(true);
        descriptionLabel.setMaxWidth(Double.MAX_VALUE);
        return descriptionLabel;
    }

    public Node createBankControl(ScriptConfiguration config, String lastValue, MutableValue<String> selectedBankValue) {
        // Load the bank tab image
        Image bankTabImage = BANK_IMAGE;
        ImageView bankTabImageView = new ImageView(bankTabImage);
        bankTabImageView.setPreserveRatio(true);
        bankTabImageView.setFitWidth(bankTabImage.getWidth());
        bankTabImageView.setFitHeight(bankTabImage.getHeight());

        // Using AnchorPane for better control
        AnchorPane anchorPane = new AnchorPane();
        anchorPane.getChildren().add(bankTabImageView);
        AnchorPane.setTopAnchor(bankTabImageView, 0.0);
        AnchorPane.setLeftAnchor(bankTabImageView, 0.0);

        // Ensure the Pane for clickable areas matches the ImageView size and position
        Pane clickableOverlay = new Pane();
        clickableOverlay.setPrefSize(bankTabImage.getWidth(), bankTabImage.getHeight());
        anchorPane.getChildren().add(clickableOverlay);
        AnchorPane.setTopAnchor(clickableOverlay, 0.0);
        AnchorPane.setLeftAnchor(clickableOverlay, 0.0);

        // Create a container (e.g., VBox) to hold the AnchorPane
        VBox container = new VBox(anchorPane);

        int selectedIndex = lastValue != null ? Integer.parseInt(lastValue) : Integer.parseInt(config.defaultValue());

        // Create clickable areas
        int numberOfAreas = 10;
        double areaWidth = bankTabImage.getWidth() / numberOfAreas;
        Color highlightColor = Color.web("#f3c244", 0.5);

        selectedBankValue.setValue(String.valueOf(selectedIndex));

        for (int i = 0; i < numberOfAreas; i++) {
            final int areaIndex = i;
            Rectangle clickableArea = new Rectangle(areaWidth, bankTabImage.getHeight());
            clickableArea.setFill(Color.TRANSPARENT);
            clickableArea.setX(areaWidth * areaIndex);

            // Check if this area is the default and set highlight if so
            if (areaIndex == selectedIndex) {
                clickableArea.setFill(highlightColor);
            }

            clickableArea.setOnMouseClicked(event -> {
                selectedBankValue.setValue(String.valueOf(areaIndex));
                // Clear previous highlights
                clickableOverlay.getChildren().forEach(child -> {
                    if (child instanceof Rectangle) {
                        ((Rectangle) child).setFill(Color.TRANSPARENT);
                    }
                });
                // Highlight the selected area
                clickableArea.setFill(highlightColor);
            });

            clickableOverlay.getChildren().add(clickableArea);
        }

        // Return the container to be added to the HBox
        return container;
    }

}
