package UI;

import UI.components.LogArea;
import UI.components.SliderWithText;
import helpers.Logger;
import helpers.services.utils.AFKServiceSettings;
import helpers.services.utils.BreakServiceSettings;
import helpers.services.utils.SleepServiceSettings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.json.JSONObject;
import utils.SystemUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static utils.Constants.*;


public class BreakUI {
    private final Logger logger;
    private final AFKServiceSettings AFKServiceSettings;
    private final BreakServiceSettings breakServiceSettings;
    private final SleepServiceSettings sleepServiceSettings;
    private Stage breakUI;
    // BREAKS
    private final int DEFAULT_BREAK_DURATION = 8;
    private final int DEFAULT_BREAK_THRESHOLD = 20;
    private final int BREAK_DURATION_VARIANCE = 4;
    private final int BREAK_THRESHOLD_VARIANCE = 15;
    // AFKs
    private final CheckBox AFKCheckbox = new CheckBox();
    private final boolean DEFAULT_AFK_CHECKBOX_STATE = false;
    private final int DEFAULT_AFK_DURATION = 40;
    private final int DEFAULT_AFK_THRESHOLD = 280;
    private final int AFK_DURATION_VARIANCE = 20;
    private final int AFK_THRESHOLD_VARIANCE = 100;
    // SLEEPS
    private final CheckBox SLEEP_CHECKBOX = new CheckBox();
    private final boolean SLEEP_CHECKBOX_DEFAULT = false;
    // For a random duration between 2 and 4 hours
    private final int DEFAULT_SLEEP_DURATION = 180;
    private final int SLEEP_DURATION_VARIANCE = 60;
    private final int DEFAULT_SLEEP_THRESHOLD = 600;
    private final int SLEEP_THRESHOLD_VARIANCE = 120;

    // SLIDERS
    private SliderWithText breakDurationSlider;
    private SliderWithText breakThresholdSlider;
    private SliderWithText breakDurationVarianceSlider;
    private SliderWithText breakThresholdVarianceSlider;

    private SliderWithText afkDurationSlider;
    private SliderWithText afkThresholdSlider;
    private SliderWithText afkDurationVarianceSlider;
    private SliderWithText afkThresholdVarianceSlider;

    private SliderWithText sleepDurationSlider;
    private SliderWithText sleepThresholdSlider;
    private SliderWithText sleepDurationVarianceSlider;
    private SliderWithText sleepThresholdVarianceSlider;

    private final CheckBox CLOSE_APP_ON_BREAK_CHECKBOX = new CheckBox();
    private final boolean CLOSE_APP_ON_BREAK_STATE = false;

    TabPane tabPane = new TabPane();

    public BreakUI(Logger logger, AFKServiceSettings AFKServiceSettings, BreakServiceSettings breakServiceSettings, SleepServiceSettings sleepServiceSettings) {
        this.logger = logger;
        this.AFKServiceSettings = AFKServiceSettings;
        this.breakServiceSettings = breakServiceSettings;
        this.sleepServiceSettings = sleepServiceSettings;
        initializeUI();
    }

    private void initializeUI() {
        breakUI = new Stage();
        breakUI.initModality(Modality.APPLICATION_MODAL);
        breakUI.setTitle("Break Configuration");
        breakUI.setResizable(false);
        breakUI.getIcons().add(MUFASA_LOGO);

        VBox contentBox = new VBox(10);
        contentBox.setPadding(new Insets(30));

        // Initialize UI components
        initializeSliders();
        loadSettings();

        // Scene header and description
        Label sceneHeader = new Label("Break handler setup");
        sceneHeader.getStyleClass().add("header-label");

        Label descriptionLabel = new Label(
                "Every script start will generate a unique seed for that bot session. " +
                        "Values you enter here will be randomized further on every script start. " +
                        "All break and sleep values are in minutes; AFK values are in seconds."
        );
        descriptionLabel.setWrapText(true); // Enable text wrap
        descriptionLabel.setMaxWidth(480);
        descriptionLabel.setPrefHeight(100);

        // TabPane for Breaks, AFKs, and Sleep settings
        TabPane tabPane = new TabPane();

        // Breaks Tab
        Tab breakTab = new Tab("Breaks");
        VBox breakBox = new VBox(10);
        breakBox.getChildren().addAll(
                new Label("All break values are in minutes"),
                createHBoxWithLabelAndCheckbox("Close app on break", CLOSE_APP_ON_BREAK_CHECKBOX),
                createHBoxWithLabelAndSlider("Duration of break", breakDurationSlider),
                createHBoxWithLabelAndSlider("Duration to run before breaking", breakThresholdSlider),
                createHBoxWithLabelAndSlider("Randomisation of break", breakDurationVarianceSlider),
                createHBoxWithLabelAndSlider("Randomisation of run time", breakThresholdVarianceSlider)
        );
        breakTab.setContent(breakBox);
        breakTab.setClosable(false);

        // AFK Tab
        Tab afkTab = new Tab("AFKs");
        VBox afkBox = new VBox(10);
        afkBox.getChildren().addAll(
                new Label("All AFK values are in seconds"),
                createHBoxWithLabelAndCheckbox("Enable AFKs", AFKCheckbox),
                createHBoxWithLabelAndSlider("Duration of AFK action", afkDurationSlider),
                createHBoxWithLabelAndSlider("Duration to run before AFKing", afkThresholdSlider),
                createHBoxWithLabelAndSlider("Randomisation of AFK action", afkDurationVarianceSlider),
                createHBoxWithLabelAndSlider("Randomisation before AFKing", afkThresholdVarianceSlider)
        );
        afkTab.setContent(afkBox);
        afkTab.setClosable(false);

        // Sleep Tab
        Tab sleepTab = new Tab("Sleeps");
        VBox sleepBox = new VBox(10);
        sleepBox.getChildren().addAll(
                new Label("All sleep values are in minutes"),
                createHBoxWithLabelAndCheckbox("Enable sleeps", SLEEP_CHECKBOX),
                createHBoxWithLabelAndSlider("Duration of sleep", sleepDurationSlider),
                createHBoxWithLabelAndSlider("Duration to run before sleeping", sleepThresholdSlider),
                createHBoxWithLabelAndSlider("Randomisation of sleep action", sleepDurationVarianceSlider),
                createHBoxWithLabelAndSlider("Randomisation of run time before sleep", sleepThresholdVarianceSlider)
        );
        sleepTab.setContent(sleepBox);
        sleepTab.setClosable(false);

        // Add tabs to TabPane
        tabPane.getTabs().addAll(breakTab, afkTab, sleepTab);

        // Buttons for reset and save
        HBox buttonBox = new HBox(10);
        Button resetButton = new Button("Reset to Default");
        resetButton.setOnAction(e -> resetToDefault());
        Button saveButton = new Button("Save Settings");
        saveButton.setOnAction(e -> saveSettings());
        buttonBox.getChildren().addAll(resetButton, saveButton);

        // Final layout, including header, description, and tab pane
        contentBox.getChildren().addAll(sceneHeader, descriptionLabel, tabPane, buttonBox);

        Scene scene = new Scene(contentBox, 550, 460);
        scene.getStylesheets().add(STYLESHEET);
        breakUI.setScene(scene);
    }

    public void display() {
        breakUI.showAndWait();
    }

    private HBox createHBoxWithLabelAndCheckbox(String labelText, CheckBox checkBox) {
        Label label = new Label(labelText);
        label.setPrefWidth(180);

        HBox hbox = new HBox(10);
        hbox.setAlignment(Pos.CENTER_LEFT);

        hbox.getChildren().addAll(label, checkBox);
        return hbox;
    }

    private HBox createHBoxWithLabelAndSlider(String labelText, SliderWithText slider) {
        Label label = new Label(labelText);
        label.setPrefWidth(230);

        HBox hbox = new HBox(10);
        hbox.setAlignment(Pos.CENTER_LEFT);

        HBox.setHgrow(slider, Priority.ALWAYS);  // Make the slider grow horizontally

        hbox.getChildren().addAll(label, slider);
        return hbox;
    }

    private void loadSettings() {
        String systemPath = SystemUtils.getSystemPath();
        Path settingsFilePath = Paths.get(systemPath + "breaks.json");

        try {
            if (Files.exists(settingsFilePath)) {
                String jsonString = Files.readString(settingsFilePath);
                JSONObject jsonObject = new JSONObject(jsonString);

                // -------------------------
                // Break
                // -------------------------
                int targetBreakLength = jsonObject.getInt("targetBreakLength");
                int targetRunLength = jsonObject.getInt("targetRunLength");
                int variabilityBreakLength = jsonObject.getInt("variabilityBreakLength");
                int variabilityRunLength = jsonObject.getInt("variabilityRunLength");

                // -------------------------
                // AFKs
                // -------------------------
                boolean AFKsEnabled = jsonObject.getBoolean("AFKsEnabled");
                int targetAFKLength = jsonObject.getInt("targetAFKLenght");
                int targetAFKRunLength = jsonObject.getInt("targetAFKRunLenght");
                int variabilityAFKLength = jsonObject.getInt("variabilityAFKLenght");
                int variabilityAFKRunLength = jsonObject.getInt("variabilityAFKRunLenght");

                // -------------------------
                // Sleeps
                // -------------------------
                boolean sleepsEnabled = jsonObject.optBoolean("sleepsEnabled", SLEEP_CHECKBOX_DEFAULT);
                int targetSleepLength = jsonObject.optInt("targetSleepLength", DEFAULT_SLEEP_DURATION);
                int targetSleepThreshold = jsonObject.optInt("targetSleepThreshold", DEFAULT_SLEEP_THRESHOLD);
                int variabilitySleepLength = jsonObject.optInt("variabilitySleepLength", SLEEP_DURATION_VARIANCE);
                int variabilitySleepThreshold = jsonObject.optInt("variabilitySleepThreshold", SLEEP_THRESHOLD_VARIANCE);

                // Close on break
                boolean closeOnBreak = jsonObject.has("closeOnBreak")
                        ? jsonObject.getBoolean("closeOnBreak")
                        : CLOSE_APP_ON_BREAK_STATE;

                // -------------------------
                // Populate UI sliders
                // -------------------------
                breakDurationSlider.setValue(targetBreakLength);
                breakThresholdSlider.setValue(targetRunLength);
                breakDurationVarianceSlider.setValue(variabilityBreakLength);
                breakThresholdVarianceSlider.setValue(variabilityRunLength);

                AFKCheckbox.setSelected(AFKsEnabled);
                afkDurationSlider.setValue(targetAFKLength);
                afkThresholdSlider.setValue(targetAFKRunLength);
                afkDurationVarianceSlider.setValue(variabilityAFKLength);
                afkThresholdVarianceSlider.setValue(variabilityAFKRunLength);

                SLEEP_CHECKBOX.setSelected(sleepsEnabled);
                sleepDurationSlider.setValue(targetSleepLength);
                sleepThresholdSlider.setValue(targetSleepThreshold);
                sleepDurationVarianceSlider.setValue(variabilitySleepLength);
                sleepThresholdVarianceSlider.setValue(variabilitySleepThreshold);

                CLOSE_APP_ON_BREAK_CHECKBOX.setSelected(closeOnBreak);

            } else {
                breakDurationSlider.setValue(DEFAULT_BREAK_DURATION);
                breakThresholdSlider.setValue(DEFAULT_BREAK_THRESHOLD);
                breakDurationVarianceSlider.setValue(BREAK_DURATION_VARIANCE);
                breakThresholdVarianceSlider.setValue(BREAK_THRESHOLD_VARIANCE);

                AFKCheckbox.setSelected(DEFAULT_AFK_CHECKBOX_STATE);
                afkDurationSlider.setValue(DEFAULT_AFK_DURATION);
                afkThresholdSlider.setValue(DEFAULT_AFK_THRESHOLD);
                afkDurationVarianceSlider.setValue(AFK_DURATION_VARIANCE);
                afkThresholdVarianceSlider.setValue(AFK_THRESHOLD_VARIANCE);

                SLEEP_CHECKBOX.setSelected(SLEEP_CHECKBOX_DEFAULT);
                sleepDurationSlider.setValue(DEFAULT_SLEEP_DURATION);
                sleepThresholdSlider.setValue(DEFAULT_SLEEP_THRESHOLD);
                sleepDurationVarianceSlider.setValue(SLEEP_DURATION_VARIANCE);
                sleepThresholdVarianceSlider.setValue(SLEEP_THRESHOLD_VARIANCE);

                CLOSE_APP_ON_BREAK_CHECKBOX.setSelected(CLOSE_APP_ON_BREAK_STATE);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        // Break
        breakServiceSettings.updateSettings(
                (int) breakDurationSlider.getValue(),
                (int) breakThresholdSlider.getValue(),
                (int) breakDurationVarianceSlider.getValue(),
                (int) breakThresholdVarianceSlider.getValue()
        );
        breakServiceSettings.setCloseOnBreak(CLOSE_APP_ON_BREAK_CHECKBOX.isSelected());

        // AFK
        AFKServiceSettings.updateSettings(
                (int) afkDurationSlider.getValue(),
                (int) afkThresholdSlider.getValue(),
                (int) afkDurationVarianceSlider.getValue(),
                (int) afkThresholdVarianceSlider.getValue()
        );
        AFKServiceSettings.setIsEnabled(AFKCheckbox.isSelected());

        // Sleep
        sleepServiceSettings.updateSettings(
                (int) sleepDurationSlider.getValue(),
                (int) sleepThresholdSlider.getValue(),
                (int) sleepDurationVarianceSlider.getValue(),
                (int) sleepThresholdVarianceSlider.getValue()
        );
        sleepServiceSettings.setIsEnabled(SLEEP_CHECKBOX.isSelected());
    }

    private void initializeSliders() {
        breakDurationSlider = createConfiguredSlider(1, 120, 4);
        breakThresholdSlider = createConfiguredSlider(1, 345, 20);
        breakDurationVarianceSlider = createConfiguredSlider(1, 120, 1);
        breakThresholdVarianceSlider = createConfiguredSlider(10, 345, 10);

        afkDurationSlider = createConfiguredSlider(1, 60, 4);
        afkThresholdSlider = createConfiguredSlider(1, 1200, 20);
        afkDurationVarianceSlider = createConfiguredSlider(1, 30, 1);
        afkThresholdVarianceSlider = createConfiguredSlider(1, 900, 10);

        sleepDurationSlider = createConfiguredSlider(1, 480, DEFAULT_SLEEP_DURATION);
        sleepThresholdSlider = createConfiguredSlider(1, 1440, DEFAULT_SLEEP_THRESHOLD);
        sleepDurationVarianceSlider = createConfiguredSlider(0, 180, SLEEP_DURATION_VARIANCE);
        sleepThresholdVarianceSlider = createConfiguredSlider(0, 360, SLEEP_THRESHOLD_VARIANCE);

        // Add a listener to targetRunLengthSlider to adjust the bounds of variabilityRunLengthSlider
        breakThresholdSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            double targetRunLength = newValue.doubleValue();
            double maxAllowedVariability = Math.max(15, 345 - targetRunLength); // Ensure at least 15 minutes range

            // Adjust the variability slider's min and max bounds
            breakThresholdVarianceSlider.setMin(15);
            breakThresholdVarianceSlider.setMax(maxAllowedVariability);

            // Ensure the current value of variabilityRunLengthSlider is within the new bounds
            if (breakThresholdVarianceSlider.getValue() > maxAllowedVariability) {
                breakThresholdVarianceSlider.setValue(maxAllowedVariability);
            }
        });

        // Add a listener to variabilityRunLengthSlider to adjust the bounds of targetRunLengthSlider
        breakThresholdVarianceSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            double variabilityRunLength = newValue.doubleValue();
            double maxAllowedTarget = Math.max(15, 345 - variabilityRunLength); // Ensure at least 15 minutes range

            // Adjust the target slider's max bounds
            breakThresholdSlider.setMax(maxAllowedTarget);

            // Ensure the current value of targetRunLengthSlider is within the new bounds
            if (breakThresholdSlider.getValue() > maxAllowedTarget) {
                breakThresholdSlider.setValue(maxAllowedTarget);
            }
        });

    }

    private SliderWithText createConfiguredSlider(double min, double max, double initialValue) {
        SliderWithText slider = new SliderWithText(min, max, initialValue);
        slider.setShowTickLabels(false);
        slider.setShowTickMarks(false);
        slider.setBlockIncrement(1);
        slider.setMaxWidth(Double.MAX_VALUE);
        return slider;
    }

    private void resetToDefault() {
        breakDurationSlider.setValue(DEFAULT_BREAK_DURATION);
        breakThresholdSlider.setValue(DEFAULT_BREAK_THRESHOLD);
        breakDurationVarianceSlider.setValue(BREAK_DURATION_VARIANCE);
        breakThresholdVarianceSlider.setValue(BREAK_THRESHOLD_VARIANCE);

        AFKCheckbox.setSelected(DEFAULT_AFK_CHECKBOX_STATE);
        afkDurationSlider.setValue(DEFAULT_AFK_DURATION);
        afkThresholdSlider.setValue(DEFAULT_AFK_THRESHOLD);
        afkDurationVarianceSlider.setValue(AFK_DURATION_VARIANCE);
        afkThresholdVarianceSlider.setValue(AFK_THRESHOLD_VARIANCE);

        SLEEP_CHECKBOX.setSelected(SLEEP_CHECKBOX_DEFAULT);
        sleepDurationSlider.setValue(DEFAULT_SLEEP_DURATION);
        sleepThresholdSlider.setValue(DEFAULT_SLEEP_THRESHOLD);
        sleepDurationVarianceSlider.setValue(SLEEP_DURATION_VARIANCE);
        sleepThresholdVarianceSlider.setValue(SLEEP_THRESHOLD_VARIANCE);

        CLOSE_APP_ON_BREAK_CHECKBOX.setSelected(CLOSE_APP_ON_BREAK_STATE);

        saveSettings();
    }

    private void saveSettings() {
        int targetRunLength = (int) breakThresholdSlider.getValue();
        int variabilityRunLength = (int) breakThresholdVarianceSlider.getValue();

        // Combined check for both conditions
        if (targetRunLength + variabilityRunLength > 345 || variabilityRunLength < 15) {
            showDialog(
                    "Warning",
                    "The combined value of Target Run Length and Variability Run Length cannot exceed 5 hours and 45 minutes. Additionally, Variability Run Length must be at least 10 minutes.",
                    Alert.AlertType.WARNING
            );
            return; // Exit the method without saving
        }

        String systemPath = SystemUtils.getSystemPath();
        Path settingsFilePath = Paths.get(systemPath + "breaks.json");

        JSONObject jsonObject = new JSONObject();
        //Breaks
        jsonObject.put("targetBreakLength", (int) breakDurationSlider.getValue());
        jsonObject.put("targetRunLength", (int) breakThresholdSlider.getValue());
        jsonObject.put("variabilityBreakLength", (int) breakDurationVarianceSlider.getValue());
        jsonObject.put("variabilityRunLength", (int) breakThresholdVarianceSlider.getValue());
        //AFKs
        jsonObject.put("AFKsEnabled", AFKCheckbox.selectedProperty().getValue());
        jsonObject.put("targetAFKLenght", (int) afkDurationSlider.getValue());
        jsonObject.put("targetAFKRunLenght", (int) afkThresholdSlider.getValue());
        jsonObject.put("variabilityAFKLenght", (int) afkDurationVarianceSlider.getValue());
        jsonObject.put("variabilityAFKRunLenght", (int) afkThresholdVarianceSlider.getValue());
        jsonObject.put("closeOnBreak", CLOSE_APP_ON_BREAK_CHECKBOX.isSelected());

        // SLEEPs
        jsonObject.put("sleepsEnabled", SLEEP_CHECKBOX.isSelected());
        jsonObject.put("targetSleepLength", (int) sleepDurationSlider.getValue());
        jsonObject.put("targetSleepThreshold", (int) sleepThresholdSlider.getValue());
        jsonObject.put("variabilitySleepLength", (int) sleepDurationVarianceSlider.getValue());
        jsonObject.put("variabilitySleepThreshold", (int) sleepThresholdVarianceSlider.getValue());

        try {
            Files.writeString(settingsFilePath, jsonObject.toString());

            // Update settings globally in BreakHandler class
            breakServiceSettings.updateSettings(
                    (int) breakDurationSlider.getValue(),
                    (int) breakThresholdSlider.getValue(),
                    (int) breakDurationVarianceSlider.getValue(),
                    (int) breakThresholdVarianceSlider.getValue()
            );
            breakServiceSettings.setCloseOnBreak(CLOSE_APP_ON_BREAK_CHECKBOX.isSelected());

            // Update settings for AFKs
            AFKServiceSettings.updateSettings(
                    (int) afkDurationSlider.getValue(),
                    (int) afkThresholdSlider.getValue(),
                    (int) afkDurationVarianceSlider.getValue(),
                    (int) afkThresholdVarianceSlider.getValue()
            );
            AFKServiceSettings.setIsEnabled(AFKCheckbox.selectedProperty().getValue());

            // Update Sleep settings
            sleepServiceSettings.updateSettings(
                    (int) sleepDurationSlider.getValue(),
                    (int) sleepThresholdSlider.getValue(),
                    (int) sleepDurationVarianceSlider.getValue(),
                    (int) sleepThresholdVarianceSlider.getValue()
            );
            sleepServiceSettings.setIsEnabled(SLEEP_CHECKBOX.isSelected());

            logger.log("Breaks & AFKs saved locally.", LogArea.GLOBAL_LOG_KEY);
        } catch (IOException e) {
            logger.devLog("We failed to save breaks and AFK settings");
            e.printStackTrace();
        }

        showDialog(
                "Breaks saved!",
                "Settings have been successfully saved.",
                Alert.AlertType.INFORMATION
        );
    }
}
