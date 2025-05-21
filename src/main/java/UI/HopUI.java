package UI;

import UI.components.SliderWithText;
import helpers.Logger;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import utils.SystemUtils;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static utils.Constants.*;

public class HopUI {
    private final Logger logger;
    private Stage hopUI;
    private SliderWithText hopTimerSlider;

    // Checkboxes
    private CheckBox ukCheckbox;
    private CheckBox usCheckbox;
    private CheckBox deCheckbox;
    private CheckBox auCheckbox;
    private CheckBox f2pCheckbox;
    private CheckBox p2pCheckbox;
    private CheckBox wintertodtCheckbox;
    private CheckBox temporossCheckbox;
    private CheckBox blastFurnaceCheckbox;
    private CheckBox othersNormalCheckbox;
    private CheckBox[] skillTotalCheckboxes;

    // Buttons for the UI
    private Button newProfileButton;
    private Button saveProfileButton;
    private Button deleteProfileButton;

    private ComboBox<String> profileDropdown;

    public HopUI(Logger logger) {
        this.logger = logger;
        initializeUI();
    }

    private void initializeUI() {
        hopUI = new Stage();
        hopUI.initModality(Modality.APPLICATION_MODAL);
        hopUI.setTitle("World Hop Configuration");
        hopUI.setResizable(false);
        hopUI.getIcons().add(MUFASA_LOGO);

        VBox mainVBox = new VBox(10);
        mainVBox.setPadding(new Insets(30));

        // Profile dropdown and buttons container
        HBox profileControls = new HBox(10);
        profileControls.setAlignment(Pos.CENTER_LEFT);

        // Profile dropdown
        profileDropdown = new ComboBox<>();
        profileDropdown.setMinWidth(200);
        profileDropdown.setItems(loadProfileNames());
        profileDropdown.getStyleClass().add("main-dropdown");
        profileDropdown.setMinHeight(28);

        // Add listener to the profile dropdown
        profileDropdown.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                loadProfileSettings(newValue);
            }
        });

        // Initialize buttons
        newProfileButton = new Button("New");
        saveProfileButton = new Button("Save");
        deleteProfileButton = new Button("Delete");

        // Set up button actions
        newProfileButton.setOnAction(e -> handleNewProfileAction());
        saveProfileButton.setOnAction(e -> handleSaveProfileAction());
        deleteProfileButton.setOnAction(e -> handleDeleteProfileAction());

        // Add dropdown and buttons to the HBox
        profileControls.getChildren().addAll(profileDropdown, newProfileButton, saveProfileButton, deleteProfileButton);

        // Add the profile controls to the main VBox
        mainVBox.getChildren().add(profileControls);

        // Initialize skillTotalCheckboxes array
        skillTotalCheckboxes = new CheckBox[7];

        // Horizontal grouping of all checkbox groups
        HBox allCheckBoxGroups = new HBox(10);
        allCheckBoxGroups.getChildren().addAll(
                createCheckboxGroup("Locations", "Australia", "Germany", "United Kingdom", "United States"),
                createCheckboxGroup("Membership", "F2P", "P2P"),
                createCheckboxGroup("Activities", "Blast Furnace", "Tempoross", "Wintertodt", "Others/Normal"),
                createCheckboxGroup("Skill Total", "500", "750", "1250", "1500", "1750", "2000", "2200")
        );
        mainVBox.getChildren().add(allCheckBoxGroups);

        // Hop timer slider
        hopTimerSlider = createConfiguredSlider(1, 60, 1);
        mainVBox.getChildren().add(createHBoxWithLabelAndSlider("Hop Timer", hopTimerSlider));

        // Information label
        Label infoLabel = new Label("The hop timer is displayed in minutes. Please use it to slide to your desired amount of minutes before hopping. Keep in mind, that your setting is just a general indication, we'll still randomise it by a lot. Example: you select 10 minutes, we might still hop between 5-15 minutes.");
        infoLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: white;"); // Set font size and text color
        infoLabel.setWrapText(true);
        mainVBox.getChildren().add(infoLabel);

        Scene scene = new Scene(mainVBox, 500, 425);
        scene.getStylesheets().add(STYLESHEET);
        hopUI.setScene(scene);

        // Set first profile as selected
        if (!profileDropdown.getItems().isEmpty()) {
            profileDropdown.getSelectionModel().selectFirst();
        } else {
            createDefaultProfile();
        }
    }

    private HBox createCheckboxGroup(String title, String... options) {
        VBox vbox = new VBox(5);
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        vbox.getChildren().add(titleLabel);

        for (String option : options) {
            CheckBox checkBox = new CheckBox(option);
            checkBox.setStyle("-fx-text-fill: white;");

            // Assigning checkboxes to class variables based on the option
            switch (option) {
                case "United Kingdom":
                    ukCheckbox = checkBox;
                    break;
                case "United States":
                    usCheckbox = checkBox;
                    break;
                case "Germany":
                    deCheckbox = checkBox;
                    break;
                case "Australia":
                    auCheckbox = checkBox;
                    break;
                case "F2P":
                    f2pCheckbox = checkBox;
                    break;
                case "P2P":
                    p2pCheckbox = checkBox;
                    break;
                case "Wintertodt":
                    wintertodtCheckbox = checkBox;
                    break;
                case "Tempoross":
                    temporossCheckbox = checkBox;
                    break;
                case "Blast Furnace":
                    blastFurnaceCheckbox = checkBox;
                    break;
                case "Others/Normal":
                    othersNormalCheckbox = checkBox;
                    break;
                case "500":
                    skillTotalCheckboxes[0] = checkBox;
                    break;
                case "750":
                    skillTotalCheckboxes[1] = checkBox;
                    break;
                case "1250":
                    skillTotalCheckboxes[2] = checkBox;
                    break;
                case "1500":
                    skillTotalCheckboxes[3] = checkBox;
                    break;
                case "1750":
                    skillTotalCheckboxes[4] = checkBox;
                    break;
                case "2000":
                    skillTotalCheckboxes[5] = checkBox;
                    break;
                case "2200":
                    skillTotalCheckboxes[6] = checkBox;
                    break;
            }

            vbox.getChildren().add(checkBox);
        }

        HBox hbox = new HBox(10);
        hbox.getChildren().add(vbox);
        return hbox;
    }

    private HBox createHBoxWithLabelAndSlider(String labelText, SliderWithText slider) {
        Label label = new Label(labelText);
        label.setPrefWidth(180);
        label.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        HBox hbox = new HBox(10);
        hbox.setAlignment(Pos.CENTER_LEFT);

        HBox.setHgrow(slider, Priority.ALWAYS);  // Make the slider grow horizontally
        slider.setMaxWidth(Double.MAX_VALUE);    // Allow the slider to expand to fill space
        slider.setStyle("-fx-font-weight: bold;");

        hbox.getChildren().addAll(label, slider);
        return hbox;
    }

    private SliderWithText createConfiguredSlider(double min, double max, double initialValue) {
        SliderWithText slider = new SliderWithText(min, max, initialValue);
        slider.setShowTickLabels(false);
        slider.setShowTickMarks(false);
        slider.setBlockIncrement(1);
        slider.setMaxWidth(Double.MAX_VALUE);
        return slider;
    }

    private ObservableList<String> loadProfileNames() {
        ObservableList<String> profileNames = FXCollections.observableArrayList();
        Path dir = Paths.get(SystemUtils.getWorldHopperFolderPath());
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.json")) {
            for (Path entry : stream) {
                String fileName = entry.getFileName().toString();
                if (!fileName.equals("ServerList.json") && !fileName.equals("LatestServerList.json")) {
                    profileNames.add(fileName.replace(".json", ""));
                }
            }
        } catch (IOException e) {
            System.out.println("Error loading profile names: " + e.getMessage());
        }
        return profileNames;
    }

    public void display() {
        hopUI.showAndWait();
    }

    private void handleNewProfileAction() {
        Stage newProfileStage = new Stage();
        newProfileStage.initModality(Modality.APPLICATION_MODAL);
        newProfileStage.setTitle("New Profile");
        newProfileStage.getIcons().add(new Image("/assets/mufasa-transparent.png"));

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(10));
        TextField profileNameField = new TextField();
        profileNameField.setPromptText("Enter profile name");

        Button okButton = new Button("OK");
        okButton.setOnAction(e -> {
            String newProfileName = profileNameField.getText();
            if (!newProfileName.isEmpty()) {
                try {
                    Path newProfilePath = Paths.get(SystemUtils.getWorldHopperFolderPath() + "/" + newProfileName + ".json");

                    JSONObject defaultSettings = createDefaultSettings();
                    Files.write(newProfilePath, defaultSettings.toString(4).getBytes());

                    profileDropdown.getItems().add(newProfileName);
                    sortAndSelectProfileDropdown(newProfileName);
                    handleSaveProfileAction();
                    newProfileStage.close();
                } catch (IOException ex) {
                    System.out.println("Error creating new profile: " + ex.getMessage());
                }
            }
        });

        Button cancelButton = new Button("Cancel");
        cancelButton.setOnAction(e -> newProfileStage.close());

        HBox buttons = new HBox(10, okButton, cancelButton);
        buttons.setAlignment(Pos.CENTER);

        layout.getChildren().addAll(profileNameField, buttons);

        Scene newProfileScene = new Scene(layout);
        newProfileScene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        newProfileStage.setScene(newProfileScene);
        newProfileStage.showAndWait();
    }

    private void handleSaveProfileAction() {
        try {
            // Initialize filter settings
            JSONObject filterSettings = new JSONObject();
            JSONObject locations = new JSONObject();
            locations.put("United Kingdom", ukCheckbox.isSelected());
            locations.put("United States", usCheckbox.isSelected());
            locations.put("Germany", deCheckbox.isSelected());
            locations.put("Australia", auCheckbox.isSelected());

            JSONObject activities = new JSONObject();
            activities.put("Blast Furnace", blastFurnaceCheckbox.isSelected());
            activities.put("Tempoross", temporossCheckbox.isSelected());
            activities.put("Wintertodt", wintertodtCheckbox.isSelected());
            for (int i = 0; i < skillTotalCheckboxes.length; i++) {
                String skillLevel = "";
                switch (i) {
                    case 0:
                        skillLevel = "500 skill total";
                        break;
                    case 1:
                        skillLevel = "750 skill total";
                        break;
                    case 2:
                        skillLevel = "1250 skill total";
                        break;
                    case 3:
                        skillLevel = "1500 skill total";
                        break;
                    case 4:
                        skillLevel = "1750 skill total";
                        break;
                    case 5:
                        skillLevel = "2000 skill total";
                        break;
                    case 6:
                        skillLevel = "2200 skill total";
                        break;
                }
                activities.put(skillLevel, skillTotalCheckboxes[i].isSelected());
            }
            activities.put("Normal/Others", othersNormalCheckbox.isSelected());

            JSONObject type = new JSONObject();
            type.put("Free", f2pCheckbox.isSelected());
            type.put("Members", p2pCheckbox.isSelected());
            type.put("PvP", false); // Always false

            filterSettings.put("Locations", locations);
            filterSettings.put("Activities", activities);
            filterSettings.put("Type", type);

            // Add a default value for activities not defined in the UI
            String defaultActivity = "Normal/Others";

            // Load the server list from ServerList.json
            Path serverListPath = Paths.get(SystemUtils.getWorldHopperFolderPath() + "/ServerList.json");
            String serverContent = new String(Files.readAllBytes(serverListPath));
            JSONArray serverList = new JSONArray(serverContent);

            // Filter the server list based on the filter settings
            JSONArray filteredWorldNumbers = new JSONArray();
            for (int i = 0; i < serverList.length(); i++) {
                JSONObject server = serverList.getJSONObject(i);
                String activityKey = server.getString("Activity").strip();

                // Skip if the activity is not allowed
                if (!activityKey.equals("-") && !ALLOWED_ACTIVITIES.contains(activityKey)) {
                    continue;
                }

                boolean locationEnabled = locations.getBoolean(server.getString("Country").strip());
                boolean typeMatch = type.getBoolean(server.getString("Type"));
                boolean activityMatch = activities.has(activityKey) ?
                        activities.getBoolean(activityKey) :
                        activities.getBoolean(defaultActivity);

                if (locationEnabled && typeMatch && activityMatch) {
                    filteredWorldNumbers.put(server.getInt("World"));
                }
            }

            // Create a dictionary to store the filter settings and the filtered list of world numbers
            JSONObject filteredData = new JSONObject();
            filteredData.put("Filter settings", filterSettings);
            filteredData.put("Filtered worlds", filteredWorldNumbers);

            // Save the hop timer value
            double hopTimerValue = hopTimerSlider.getValue();
            filteredData.put("Hop Timer", hopTimerValue);

            // Check if filteredWorldNumbers contains at least one world
            if (filteredWorldNumbers.length() == 0) {
                // Display alert
                showDialog("Incorrect settings", "The current filter settings do not include any worlds. Please adjust your settings and try again.", Alert.AlertType.ERROR);
                return; // Do not proceed with saving
            }

            // Use the selected profile name to save the filtered data
            String selectedProfile = profileDropdown.getSelectionModel().getSelectedItem();
            if (selectedProfile != null && !selectedProfile.isEmpty()) {
                // Save the filtered data to a JSON file in the specified directory
                Path outputPath = Paths.get(SystemUtils.getWorldHopperFolderPath() + "/" + selectedProfile + ".json");
                Files.write(outputPath, filteredData.toString(4).getBytes());

                System.out.println("Updated the " + selectedProfile + " hopping profile.");
                System.out.println("Worlds inside the " + selectedProfile + " profile: " + filteredWorldNumbers);
            } else {
                System.out.println("No profile selected for saving.");
            }
        } catch (IOException | JSONException e) {
            System.out.println("Error saving profile settings: " + e.getMessage());
        }
    }

    private void handleDeleteProfileAction() {
        String selectedProfile = profileDropdown.getSelectionModel().getSelectedItem();
        if (selectedProfile != null && !selectedProfile.isEmpty()) {
            Stage deleteProfileStage = new Stage();
            deleteProfileStage.initModality(Modality.APPLICATION_MODAL);
            deleteProfileStage.setTitle("Delete Profile");
            deleteProfileStage.getIcons().add(new Image("/assets/mufasa-transparent.png"));

            VBox layout = new VBox(10);
            layout.setPadding(new Insets(10));
            Label confirmLabel = new Label("Are you sure you want to delete the " + selectedProfile + " profile?");

            Button okButton = new Button("OK");
            okButton.setOnAction(e -> {
                profileDropdown.getItems().remove(selectedProfile);
                try {
                    Files.deleteIfExists(Paths.get(SystemUtils.getWorldHopperFolderPath() + "/" + selectedProfile + ".json"));
                } catch (IOException ex) {
                    System.out.println("Error deleting profile: " + ex.getMessage());
                }
                deleteProfileStage.close();
            });

            Button cancelButton = new Button("Cancel");
            cancelButton.setOnAction(e -> deleteProfileStage.close());

            HBox buttons = new HBox(10, okButton, cancelButton);
            buttons.setAlignment(Pos.CENTER);

            layout.getChildren().addAll(confirmLabel, buttons);

            Scene deleteProfileScene = new Scene(layout);
            deleteProfileScene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
            deleteProfileStage.setScene(deleteProfileScene);
            deleteProfileStage.showAndWait();
        }
    }

    private void sortAndSelectProfileDropdown(String newProfileName) {
        ObservableList<String> profiles = profileDropdown.getItems();
        FXCollections.sort(profiles);
        profileDropdown.getItems().clear();
        profileDropdown.setItems(loadProfileNames());
        profileDropdown.getSelectionModel().select(newProfileName);
    }

    private void loadProfileSettings(String profileName) {
        // Check if the profileName is ServerList or LatestServerList and return early if true
        if (profileName.equals("ServerList") || profileName.equals("LatestServerList")) {
            return; // Ignore these files
        }

        Path profilePath = Paths.get(SystemUtils.getWorldHopperFolderPath() + "/" + profileName + ".json");
        try {
            String content = new String(Files.readAllBytes(profilePath));
            JSONObject jsonObject = new JSONObject(content);
            JSONObject filterSettings = jsonObject.getJSONObject("Filter settings");

            // Locations settings
            JSONObject locations = filterSettings.getJSONObject("Locations");
            ukCheckbox.setSelected(locations.getBoolean("United Kingdom"));
            usCheckbox.setSelected(locations.getBoolean("United States"));
            deCheckbox.setSelected(locations.getBoolean("Germany"));
            auCheckbox.setSelected(locations.getBoolean("Australia"));

            // Activities settings
            JSONObject activities = filterSettings.getJSONObject("Activities");
            skillTotalCheckboxes[0].setSelected(activities.getBoolean("500 skill total"));
            skillTotalCheckboxes[1].setSelected(activities.getBoolean("750 skill total"));
            skillTotalCheckboxes[2].setSelected(activities.getBoolean("1250 skill total"));
            skillTotalCheckboxes[3].setSelected(activities.getBoolean("1500 skill total"));
            skillTotalCheckboxes[4].setSelected(activities.getBoolean("1750 skill total"));
            skillTotalCheckboxes[5].setSelected(activities.getBoolean("2000 skill total"));
            skillTotalCheckboxes[6].setSelected(activities.getBoolean("2200 skill total"));
            blastFurnaceCheckbox.setSelected(activities.getBoolean("Blast Furnace"));
            temporossCheckbox.setSelected(activities.getBoolean("Tempoross"));
            wintertodtCheckbox.setSelected(activities.getBoolean("Wintertodt"));
            othersNormalCheckbox.setSelected(activities.getBoolean("Normal/Others"));

            // Type settings
            JSONObject type = filterSettings.getJSONObject("Type");
            f2pCheckbox.setSelected(type.getBoolean("Free"));
            p2pCheckbox.setSelected(type.getBoolean("Members"));

            // Load hop timer value
            double hopTimerValue = jsonObject.getDouble("Hop Timer");
            hopTimerSlider.setValue(hopTimerValue);

        } catch (IOException | JSONException e) {
            System.out.println("Error loading profile settings: " + e.getMessage());
        }
    }

    private JSONObject createDefaultSettings() {
        JSONObject filterSettings = new JSONObject();
        JSONObject locations = new JSONObject();
        locations.put("United Kingdom", true);
        locations.put("United States", true);
        locations.put("Germany", true);
        locations.put("Australia", false);
    
        JSONObject activities = new JSONObject();
        activities.put("Blast Furnace", true);
        activities.put("Tempoross", true);
        activities.put("Wintertodt", true);
        activities.put("500 skill total", false);
        activities.put("750 skill total", false);
        activities.put("1250 skill total", false);
        activities.put("1500 skill total", false);
        activities.put("1750 skill total", false);
        activities.put("2000 skill total", false);
        activities.put("2200 skill total", false);
        activities.put("Normal/Others", true);
    
        JSONObject type = new JSONObject();
        type.put("Free", false);
        type.put("Members", true);
        type.put("PvP", false);
    
        filterSettings.put("Locations", locations);
        filterSettings.put("Activities", activities);
        filterSettings.put("Type", type);
    
        JSONObject defaultSettings = new JSONObject();
        defaultSettings.put("Filter settings", filterSettings);
        defaultSettings.put("Filtered worlds", new JSONArray()); // Empty array for filtered worlds
        defaultSettings.put("Hop Timer", 8 + (int)(Math.random() * 5)); // Randomly choose between 8 and 12
    
        return defaultSettings;
    }    

    private void createDefaultProfile() {
        String defaultProfileName = "default";
        TextField profileNameField = new TextField(defaultProfileName);
        Path newProfilePath = Paths.get(SystemUtils.getWorldHopperFolderPath() + "/" + defaultProfileName + ".json");

        try {
            JSONObject defaultSettings = createDefaultSettings();
            Files.write(newProfilePath, defaultSettings.toString(4).getBytes());

            profileDropdown.getItems().add(defaultProfileName);
            sortAndSelectProfileDropdown(defaultProfileName);
            handleSaveProfileAction();
        } catch (IOException ex) {
            System.out.println("Error creating default profile: " + ex.getMessage());
        }
    }

    private void updateProfileWithNewServerList(Path profilePath, List<Map<String, Object>> serverList) {
        try {
            // Load the profile settings
            String profileContent = new String(Files.readAllBytes(profilePath));
            JSONObject profileJson = new JSONObject(profileContent);
            JSONObject filterSettings = profileJson.getJSONObject("Filter settings");

            // Apply filters to the provided server list
            JSONArray filteredWorldNumbers = applyFiltersToServerList(filterSettings, serverList);

            // Update the profile with the new filtered world numbers
            profileJson.put("Filtered worlds", filteredWorldNumbers);

            // Preserve the hop timer value
            double hopTimerValue = profileJson.optDouble("Hop Timer", 10);
            profileJson.put("Hop Timer", hopTimerValue);

            // Save the updated profile
            Files.write(profilePath, profileJson.toString(4).getBytes());
        } catch (IOException | JSONException e) {
            logger.devLog("Error updating profile " + profilePath + ": " + e.getMessage());
        }
    }

    private JSONArray applyFiltersToServerList(JSONObject filterSettings, List<Map<String, Object>> serverList) {
        JSONArray filteredWorldNumbers = new JSONArray();
        JSONObject locations = filterSettings.getJSONObject("Locations");
        JSONObject activities = filterSettings.getJSONObject("Activities");
        JSONObject type = filterSettings.getJSONObject("Type");

        for (Map<String, Object> server : serverList) {
            String activityKey = (String) server.get("Activity");
            System.out.println("Processing server with activity: " + activityKey);

            // Skip if the activity is not allowed
            if (!activityKey.equals("-") && !ALLOWED_ACTIVITIES.contains(activityKey)) {
                continue;
            }

            boolean locationEnabled = locations.getBoolean((String) server.get("Country"));
            boolean typeMatch = type.getBoolean((String) server.get("Type"));
            boolean activityMatch = activities.has(activityKey) ? activities.getBoolean(activityKey) : activities.getBoolean("Normal/Others");

            if (locationEnabled && typeMatch && activityMatch) {
                filteredWorldNumbers.put(server.get("World"));
                System.out.println("Adding server to list: " + server.get("World"));
            }
        }

        return filteredWorldNumbers;
    }

    public void updateAllProfilesWithNewServerList(List<Map<String, Object>> serverList) {
        Path directoryPath = Paths.get(SystemUtils.getWorldHopperFolderPath());
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directoryPath, "*.json")) {
            for (Path entry : stream) {
                String fileName = entry.getFileName().toString();
                if (!fileName.equals("ServerList.json") && !fileName.equals("LatestServerList.json")) {
                    updateProfileWithNewServerList(entry, serverList);
                }
            }
        } catch (IOException e) {
            logger.devLog("Error updating profiles: " + e.getMessage());
        }
    }
}
