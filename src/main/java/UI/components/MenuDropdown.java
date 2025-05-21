package UI.components;

import UI.AccountManagerUI;
import UI.BreakUI;
import UI.MapUI;
import UI.GraphUI;
import UI.HopUI;
import UI.components.utils.Observables;
import helpers.Logger;
import helpers.emulator.EmulatorManager;
import helpers.emulator.LDPlayerInstanceMgr;
import helpers.adb.ADBHandler;
import helpers.utils.IsScriptRunning;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import utils.SystemUtils;

import java.awt.*;
import java.io.*;
import java.util.Optional;

import static UI.components.utils.Observables.*;
import static UI.components.utils.Observables.USE_PW_CAPTURE;
import static utils.Constants.*;

public class MenuDropdown {
    private final IsScriptRunning isScriptRunning;
    private final EmulatorManager emulatorManager;
    private final BreakUI breakUI;
    private final HopUI hopUI;
    private final AccountManagerUI accountManagerUI;
    private final Logger logger;
    private final GraphUI graphUI;
    private final MapUI mapUI;
    private final ADBHandler adbHandler;

    // Non-static menu entries
    public CheckMenuItem enable_debugging = new CheckMenuItem("Enable debugging");
    public CheckMenuItem enable_devlogs = new CheckMenuItem("Enable developer logs");
    public CheckMenuItem developerView = new CheckMenuItem("Developer UI");

    private final MenuButton dropdownMenu = new MenuButton("Menu");

    // APP Install stuff
    private static final String GAME_FEED_URL = "https://mufasaclient.com/game/version.txt";
    private static final String GAME_DOWNLOAD_URL = "https://mufasaclient.com/game/";
    private static final String BROWSER_FEED_URL = "https://mufasaclient.com/browser/version.txt";
    private static final String BROWSER_DOWNLOAD_URL = "https://mufasaclient.com/browser/";
    private static final String PROXY1_FEED_URL = "https://mufasaclient.com/proxy1/version.txt";
    private static final String PROXY1_DOWNLOAD_URL = "https://mufasaclient.com/proxy1/";
    private static final String PROXY2_FEED_URL = "https://mufasaclient.com/proxy2/version.txt";
    private static final String PROXY2_DOWNLOAD_URL = "https://mufasaclient.com/proxy2/";
    private static final String STORE_FEED_URL = "https://mufasaclient.com/store/version.txt";
    private static final String STORE_DOWNLOAD_URL = "https://mufasaclient.com/store/";

    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir");

    // File naming formats
    private static final String GAME_FILE_FORMAT = "OSRS%s.zip";
    private static final String BROWSER_FILE_FORMAT = "BROWSER%s.apk";
    private static final String PROXY1_FILE_FORMAT = "tun2socks%s.zip";
    private static final String PROXY2_FILE_FORMAT = "socksdroid%s.apk";
    private static final String STORE_FILE_FORMAT = "STORE%s.apk";

    public MenuDropdown(IsScriptRunning isScriptRunning, EmulatorManager emulatorManager, MapUI mapUI, GraphUI graphUI, BreakUI breakUI, HopUI hopUI, AccountManagerUI accountManagerUI, Logger logger, ADBHandler adbHandler) {
        this.isScriptRunning = isScriptRunning;
        this.emulatorManager = emulatorManager;
        this.mapUI = mapUI;
        this.graphUI = graphUI;
        this.breakUI = breakUI;
        this.hopUI = hopUI;
        this.accountManagerUI = accountManagerUI;
        this.logger = logger;
        this.adbHandler = adbHandler;

        createMenu();

        // Bind the properties to the BooleanProperty instances
        enable_debugging.selectedProperty().bindBidirectional(DEBUGGING_ENABLED);
        enable_devlogs.selectedProperty().bindBidirectional(Observables.DEVLOGS_ENABLED);
    }

    public MenuButton getDropdownMenu() {return dropdownMenu;}

    public void createMenu() {
        // Create the dropdown menu
        dropdownMenu.setStyle("-fx-background-color: #292623;");
        dropdownMenu.setPrefWidth(125);

        // Adding sub-menu items for each menu
        Menu subMenu1 = new Menu("Client settings");
        MenuItem subMenu1Item1 = new MenuItem("Break settings");
        subMenu1Item1.setOnAction(event -> breakUI.display());
        MenuItem subMenu1Item2 = new MenuItem("World hop settings");
        subMenu1Item2.setOnAction(event -> hopUI.display());
        MenuItem subMenu1Item3 = new MenuItem("Account manager");
        subMenu1Item3.setOnAction(event -> accountManagerUI.display());
        MenuItem subMenu1Item4 = new MenuItem("Open screenshot folder");
        subMenu1Item4.setOnAction(event -> {
            try {
                SystemUtils.openFolder(SystemUtils.getScreenshotFolderPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        MenuItem subMenu1Item5 = new MenuItem("Open local script folder");
        subMenu1Item5.setOnAction(event -> {
            try {
                SystemUtils.openFolder(SystemUtils.getLocalScriptFolderPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        MenuItem subMenu1Item6 = new MenuItem("Open logs folder");
        subMenu1Item6.setOnAction(event -> {
            try {
                SystemUtils.openFolder(SystemUtils.getLogsFolderPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        subMenu1.getItems().addAll(subMenu1Item1, subMenu1Item2, subMenu1Item3, subMenu1Item4, subMenu1Item5, subMenu1Item6);

        Menu subMenu2 = new Menu("Help");
        MenuItem subMenu2Item1 = new MenuItem("Join Discord server");
        subMenu2Item1.setOnAction(event -> {
            String url = "https://discord.gg/ydtk4XRrgu";
            Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.BROWSE)) {
                try {
                    java.net.URI uri = java.net.URI.create(url);
                    desktop.browse(uri);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        enable_debugging.setOnAction(event -> enable_debugging.setSelected(enable_debugging.isSelected()));
        enable_devlogs.setOnAction(event -> enable_devlogs.setSelected(enable_devlogs.isSelected()));
        subMenu2.getItems().addAll(subMenu2Item1, enable_debugging, enable_devlogs);

        MenuItem showGrapher = new MenuItem("Show Grapher");
        showGrapher.setOnAction(e -> graphUI.display());

        MenuItem mapUIBtn = new MenuItem("Show Map UI");
        mapUIBtn.setOnAction(event -> mapUI.display());

        Menu subMenu3 = new Menu("Dev options");
        if (!subMenu3.getItems().contains(developerView)) {
            subMenu3.getItems().addAll(developerView, mapUIBtn, showGrapher);
        }
        dropdownMenu.getItems().addAll(subMenu3);

        Menu subMenu4 = new Menu("Emulator");
        MenuItem subMenu4Item2 = new MenuItem("Create LDPlayer instance(s)");
        subMenu4Item2.setOnAction(event -> {
            // Create a simple input dialog with default value "1"
            TextInputDialog dialog = new TextInputDialog("1");
            dialog.setTitle("Create LDPlayer Instances");
            dialog.setHeaderText(null); // Remove unnecessary header
            dialog.setContentText("Enter number of instances to create:");

            // Apply CSS
            dialog.getDialogPane().getStylesheets().add(STYLESHEET);

            // Add custom logo
            Stage dialogStage = (Stage) dialog.getDialogPane().getScene().getWindow();
            dialogStage.getIcons().add(MUFASA_LOGO);

            // Show dialog and process input
            Optional<String> result = dialog.showAndWait();
            result.ifPresent(input -> {
                try {
                    int count = Integer.parseInt(input.trim()); // Ensure valid input
                    LDPlayerInstanceMgr instanceMgr = new LDPlayerInstanceMgr();
                    String output = instanceMgr.createInstances(count);
                    logger.devLog(output);
                } catch (NumberFormatException e) {
                    // Show error dialog if the input is not a valid number
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Invalid Input");
                    alert.setHeaderText(null);
                    alert.setContentText("Please enter a valid number.");

                    // Apply CSS to alert and add logo
                    alert.getDialogPane().getStylesheets().add(STYLESHEET);
                    Stage alertStage = (Stage) alert.getDialogPane().getScene().getWindow();
                    alertStage.getIcons().add(MUFASA_LOGO);

                    alert.showAndWait();
                }
            });
        });
        MenuItem subMenu4Item3 = new MenuItem("Manually connect emulator");
        subMenu4Item3.setOnAction(event -> {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Manual Emulator Connection");
            dialog.setHeaderText(null);
            dialog.setContentText("Please enter the emulator (ADB) port number:");

            dialog.getDialogPane().getStylesheets().add(STYLESHEET);
            Stage dialogStage = (Stage) dialog.getDialogPane().getScene().getWindow();
            dialogStage.getIcons().add(MUFASA_LOGO);

            Optional<String> result = dialog.showAndWait();
            result.ifPresent(portNumber -> {
                try {
                    String output = connectToEmulator(portNumber);
                    showDialog("Manual connection output", output, Alert.AlertType.INFORMATION);
                } catch (IOException | InterruptedException e) {
                    showDialog("Error", "Failed to connect to emulator: " + e.getMessage(), Alert.AlertType.INFORMATION);
                }
            });
        });

        subMenu4.getItems().addAll(subMenu4Item2, subMenu4Item3);

        Menu subMenu5 = new Menu("View settings");
        MenuItem refreshRate = new MenuItem("Set Refresh rate");
        refreshRate.setOnAction(e -> refreshRateDialogue());

        CheckMenuItem isPaintEnabledMenuItem = new CheckMenuItem("Set paint visibility");
        isPaintEnabledMenuItem.setSelected(IS_PAINT_ENABLED.get());
        isPaintEnabledMenuItem.selectedProperty().bindBidirectional(IS_PAINT_ENABLED);

        CheckMenuItem useDirectCapture = new CheckMenuItem("Use DirectCapture(WIN ONLY)");
        useDirectCapture.selectedProperty().bindBidirectional(Observables.USE_DIRECT_CAPTURE);
        useDirectCapture.setSelected(IS_WINDOWS_USER);
        useDirectCapture.setOnAction(event -> {
            USE_PW_CAPTURE.set(false);
        });

        CheckMenuItem usePrintWindowCapture = new CheckMenuItem("Use PWCapture(WIN ONLY)");
        usePrintWindowCapture.selectedProperty().bindBidirectional(Observables.USE_PW_CAPTURE);
        usePrintWindowCapture.setSelected(false);
        usePrintWindowCapture.setOnAction(event -> {
            Observables.USE_DIRECT_CAPTURE.set(false);
        });

        subMenu5.getItems().addAll(isPaintEnabledMenuItem, refreshRate, useDirectCapture, usePrintWindowCapture);

        // Add submenus to the dropdown
        dropdownMenu.getItems().addAll(subMenu1, subMenu2, subMenu4, subMenu5);
    }

    private void refreshRateDialogue() {
        Alert alert = new Alert(Alert.AlertType.NONE);

        Label label = new Label("Set refresh rate");
        Spinner<Integer> spinner = new Spinner<>();
        spinner.setEditable(true); // Allow manual number input
        spinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(50, 1000, GAME_REFRESHRATE.get()));

        // Create a VBox to hold the label and spinner, with a margin
        VBox vbox = new VBox(10, label, spinner);
        vbox.setStyle("-fx-padding: 20;");

        // Apply CSS to the dialog
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(STYLESHEET);
        dialogPane.setContent(vbox);

        alert.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        alert.showAndWait().ifPresent(buttonType -> {
            if (buttonType == ButtonType.OK) {
                showDialog("Refresh rate updated!", "Refresh rate has been set to: " + spinner.getValue(), Alert.AlertType.INFORMATION);
                GAME_REFRESHRATE.set(spinner.getValue());
                emulatorManager.updateRefreshRateForAll(spinner.getValue());
            }
        });
    }

    private String connectToEmulator(String portNumber) throws IOException, InterruptedException {
        String adbFolderPath = SystemUtils.getADBFolderPath();
        ProcessBuilder processBuilder = new ProcessBuilder(adbFolderPath + "/adb", "connect", "localhost:" + portNumber);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        // Read the output
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        process.waitFor();
        return output.toString();
    }
}
