package UI;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.json.JSONObject;
import utils.Encryption;
import utils.SystemUtils;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Base64;

import static utils.Constants.*;

public class AccountManagerUI {
    private Stage stage;
    private PasswordField passwordField;
    private TextField passwordTextField; // Used to show password when needed
    private CheckBox breaksEnabledCheckBox;

    public AccountManagerUI() {
        initializeUI();
    }

    private void initializeUI() {
        Label loadAccountLabel = new Label("Load account");
        loadAccountLabel.getStyleClass().add("subheader-label");

        // "Breaks enabled" CheckBox
        breaksEnabledCheckBox = new CheckBox("Use breaks for this account");
        breaksEnabledCheckBox.setPrefHeight(30);
        breaksEnabledCheckBox.setSelected(true);

        VBox mainLayout = new VBox(10);
        mainLayout.setPadding(new Insets(15));
        mainLayout.getStyleClass().add("main-layout");

        // Dropdown for existing entries
        ComboBox<String> dropdown = new ComboBox<>();
        dropdown.getStyleClass().add("main-dropdown");
        dropdown.setPrefWidth(200);
        dropdown.setPrefHeight(28);

        loadUsers(dropdown);
        if (!dropdown.getItems().isEmpty()) {
            dropdown.getSelectionModel().selectFirst();
        } else {
            createDummyUser(dropdown);
        }

        // Edit button with pen icon
        Button editButton = new Button("Edit");

        // Remove button with minus icon
        Button removeButton = new Button("Delete");

        HBox dropdownLayout = new HBox(10, dropdown, editButton, removeButton);
        dropdownLayout.setPadding(new Insets(10, 0, 10, 0));

        // Text fields for username, password, bank pin, and webhook URL
        Label accountInfoLabel = new Label("Account info");
        accountInfoLabel.getStyleClass().add("subheader-label");

        TextField usernameField = new TextField();
        usernameField.setPrefHeight(32); // Adjusted height
        usernameField.setPromptText("Username");
        usernameField.getStyleClass().add("main-text-field");

        passwordField = new PasswordField();
        passwordField.setPrefHeight(32); // Adjusted height
        passwordField.setPromptText("Password");
        passwordField.getStyleClass().add("main-text-field");
        passwordField.managedProperty().bind(passwordField.visibleProperty());

        passwordTextField = new TextField();
        passwordTextField.setPrefHeight(32); // Adjusted height
        passwordTextField.setPromptText("Password");
        passwordTextField.getStyleClass().add("main-text-field");
        passwordTextField.setVisible(false);
        passwordTextField.managedProperty().bind(passwordTextField.visibleProperty());

        TextField bankPinField = new TextField();
        bankPinField.setPrefWidth(80); // Adjusted width for 4 characters
        bankPinField.setMaxWidth(80);
        bankPinField.setPrefHeight(32); // Adjusted height
        bankPinField.setPromptText("Bank PIN");
        bankPinField.getStyleClass().add("main-text-field");

        // Webhook field
        Label webhookLabel = new Label("Webhook setup");
        TextField webhookURLField = new TextField();
        webhookURLField.setPrefHeight(32); // Adjusted height
        webhookURLField.setPromptText("Webhook URL");
        webhookURLField.setPrefWidth(300); // Adjusted width to accommodate frequency ComboBox
        webhookURLField.getStyleClass().add("main-text-field");

        // Frequency ComboBox for webhook interval
        ComboBox<String> frequencyComboBox = new ComboBox<>();
        for (int i = 1; i <= 60; i++) {
            frequencyComboBox.getItems().add(String.valueOf(i));
        }
        frequencyComboBox.setPromptText("Minutes");
        frequencyComboBox.setPrefWidth(80);
        frequencyComboBox.setPrefHeight(32);
        frequencyComboBox.getStyleClass().add("main-dropdown");

        HBox webhookLayout = new HBox(10, webhookURLField, frequencyComboBox);

        Button showHidePasswordButton = new Button("Show");
        showHidePasswordButton.setPrefHeight(30);
        showHidePasswordButton.setOnAction(event -> {
            if (passwordField.isVisible()) {
                passwordTextField.setText(passwordField.getText());
                passwordTextField.setVisible(true);
                passwordField.setVisible(false);
                showHidePasswordButton.setText("Hide");
            } else {
                passwordField.setText(passwordTextField.getText());
                passwordField.setVisible(true);
                passwordTextField.setVisible(false);
                showHidePasswordButton.setText("Show");
            }
        });

        // Add button with plus icon
        Button addButton = new Button("Save");
        addButton.setPrefHeight(30);

        HBox passwordLayout = new HBox(10, passwordField, passwordTextField, showHidePasswordButton);
        HBox addEntryLayout = new HBox(10, usernameField, addButton);

        mainLayout.getChildren().addAll(loadAccountLabel, dropdownLayout, accountInfoLabel, addEntryLayout, passwordLayout, bankPinField, breaksEnabledCheckBox, webhookLabel, webhookLayout);

        Scene scene = new Scene(mainLayout, 450, 390); // Adjusted for additional field
        scene.getStylesheets().add(STYLESHEET); // Add CSS stylesheet

        stage = new Stage();
        stage.setScene(scene);
        stage.setTitle("Account Manager");
        stage.getIcons().add(MUFASA_LOGO); // Use your logo icon

        // Define button actions
        addButton.setOnAction(event -> addOrUpdateUser(usernameField, passwordField, passwordTextField, bankPinField, webhookURLField, frequencyComboBox, dropdown));
        editButton.setOnAction(event -> loadUserForEditing(dropdown, usernameField, passwordField, passwordTextField, bankPinField, webhookURLField, frequencyComboBox));
        removeButton.setOnAction(event -> removeUser(dropdown, usernameField, passwordField, passwordTextField, bankPinField, webhookURLField));

        if (!dropdown.getItems().isEmpty()) {
            dropdown.getSelectionModel().selectFirst();
            editButton.fire();
        }
    }

    private void addOrUpdateUser(TextField usernameField, PasswordField passwordField, TextField passwordTextField, TextField bankPinField, TextField webhookURLField, ComboBox<String> frequencyComboBox, ComboBox<String> dropdown) {
        String username = usernameField.getText();
        String password = passwordField.getText();
        String bankPin = bankPinField.getText();
        String webhookURL = webhookURLField.getText();
        String frequency = frequencyComboBox.getValue() != null ? frequencyComboBox.getValue() : "1";
        boolean breaksEnabled = breaksEnabledCheckBox.isSelected();

        if (!username.isEmpty() && !password.isEmpty()) {
            try {
                SecureRandom random = new SecureRandom();
                byte[] ivBytes = new byte[16];
                random.nextBytes(ivBytes);

                String encryptedPassword = Encryption.encrypt(password, ivBytes);
                String encryptedBankPin = bankPin.isEmpty() ? "" : Encryption.encrypt(bankPin, ivBytes);
                String encryptedWebhookURL = Encryption.encrypt(webhookURL, ivBytes);
                String ivString = Base64.getEncoder().encodeToString(ivBytes);

                java.nio.file.Path pathToFile = Paths.get(SystemUtils.getSystemPath() + "creds.json");
                JSONObject jsonObject = Files.exists(pathToFile) ? new JSONObject(new String(Files.readAllBytes(pathToFile))) : new JSONObject();

                JSONObject userData = new JSONObject();
                userData.put("password", encryptedPassword);
                if (!bankPin.isEmpty()) {
                    userData.put("bankPin", encryptedBankPin);
                }
                userData.put("webhookURL", encryptedWebhookURL);
                userData.put("iv", ivString);
                userData.put("frequency", frequency);
                userData.put("breaksEnabled", breaksEnabled); // Save the breaksEnabled state

                jsonObject.put(username, userData);
                Files.write(pathToFile, jsonObject.toString().getBytes());

                usernameField.clear();
                passwordField.clear();
                passwordTextField.clear();
                bankPinField.clear();
                webhookURLField.clear();
                frequencyComboBox.getSelectionModel().clearSelection();
                breaksEnabledCheckBox.setSelected(true);

                boolean wasDropdownEmpty = dropdown.getItems().isEmpty();
                if (!dropdown.getItems().contains(username)) {
                    dropdown.getItems().add(username);
                }
                if (wasDropdownEmpty) {
                    dropdown.getSelectionModel().select(username);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        showDialog(
                "Account saved!",
                "Settings have been successfully saved.",
                Alert.AlertType.INFORMATION
        );
    }

    private void loadUserForEditing(ComboBox<String> dropdown, TextField usernameField, PasswordField passwordField, TextField passwordTextField, TextField bankPinField, TextField webhookURLField, ComboBox<String> frequencyComboBox) {
        String selectedUsername = dropdown.getValue();
        if (selectedUsername != null) {
            try {
                java.nio.file.Path pathToFile = Paths.get(SystemUtils.getSystemPath() + "creds.json");
                String content = new String(Files.readAllBytes(pathToFile));
                JSONObject jsonObject = new JSONObject(content);

                if (jsonObject.has(selectedUsername)) {
                    JSONObject userData = jsonObject.getJSONObject(selectedUsername);
                    String encryptedPassword = userData.getString("password");
                    String encryptedBankPin = userData.optString("bankPin", "");
                    String encryptedWebhookURL = userData.optString("webhookURL", "");
                    String ivString = userData.getString("iv");
                    String frequency = userData.optString("frequency", "1");
                    boolean breaksEnabled = userData.optBoolean("breaksEnabled", false); // Load the breaksEnabled state

                    byte[] ivBytes = Base64.getDecoder().decode(ivString);

                    String password = Encryption.decrypt(encryptedPassword, ivBytes);
                    String bankPin = !encryptedBankPin.isEmpty() ? Encryption.decrypt(encryptedBankPin, ivBytes) : "";
                    String webhookURL = !encryptedWebhookURL.isEmpty() ? Encryption.decrypt(encryptedWebhookURL, ivBytes) : "";

                    usernameField.setText(selectedUsername);
                    passwordField.setText(password);
                    passwordTextField.setText(password);
                    bankPinField.setText(bankPin);
                    webhookURLField.setText(webhookURL);
                    frequencyComboBox.setValue(frequency);
                    breaksEnabledCheckBox.setSelected(breaksEnabled);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void removeUser(ComboBox<String> dropdown, TextField usernameField, PasswordField passwordField, TextField passwordTextField, TextField bankPinField, TextField webhookURLField) {
        String selectedUsername = dropdown.getValue();
        if (selectedUsername != null) {
            try {
                java.nio.file.Path pathToFile = Paths.get(SystemUtils.getSystemPath() + "creds.json");
                String content = new String(Files.readAllBytes(pathToFile));
                JSONObject jsonObject = new JSONObject(content);

                if (jsonObject.has(selectedUsername)) {
                    jsonObject.remove(selectedUsername);
                    Files.write(pathToFile, jsonObject.toString().getBytes());
                    dropdown.getItems().remove(selectedUsername);

                    usernameField.clear();
                    passwordField.clear();
                    passwordTextField.clear();
                    bankPinField.clear();
                    webhookURLField.clear();
                    breaksEnabledCheckBox.setSelected(true);

                    if (!dropdown.getItems().isEmpty()) {
                        dropdown.getSelectionModel().selectFirst();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void loadUsers(ComboBox<String> dropdown) {
        String systemPath = SystemUtils.getSystemPath();
        java.nio.file.Path pathToFile = Paths.get(systemPath + "creds.json");

        try {
            if (Files.exists(pathToFile)) {
                String content = new String(Files.readAllBytes(pathToFile));
                JSONObject jsonObject = new JSONObject(content);
                jsonObject.keys().forEachRemaining(key -> dropdown.getItems().add(key));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (dropdown.getItems().isEmpty()) {
            dropdown.setPromptText("No users found");
        } else {
            dropdown.getSelectionModel().selectFirst();
        }
    }

    private void createDummyUser(ComboBox<String> dropdown) {
        TextField dummyUsernameField = new TextField("mufasa");
        PasswordField dummyPasswordField = new PasswordField();
        dummyPasswordField.setText("mufasa");
        TextField dummyPasswordTextField = new TextField("mufasa");
        TextField dummyBankPinField = new TextField("");
        TextField dummyWebhookURLField = new TextField("");
        ComboBox<String> dummyFrequencyComboBox = new ComboBox<>();
        dummyFrequencyComboBox.getItems().add("1");
        dummyFrequencyComboBox.setValue("1");

        breaksEnabledCheckBox.setSelected(true);

        addOrUpdateUser(dummyUsernameField, dummyPasswordField, dummyPasswordTextField, dummyBankPinField, dummyWebhookURLField, dummyFrequencyComboBox, dropdown);
    }

    public void display() {
        if (stage != null) {
            stage.show();
        } else {
            Platform.runLater(this::initializeUI);
        }
    }
}
