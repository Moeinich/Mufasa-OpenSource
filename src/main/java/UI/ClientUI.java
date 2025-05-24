package UI;

import UI.components.MenuDropdown;
import helpers.Logger;
import helpers.ThreadManager;
import helpers.adb.ADBHandler;
import helpers.emulator.EmulatorManager;
import helpers.utils.IsScriptRunning;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuButton;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import scripts.ScriptExecutor;

import static UI.components.EmulatorView.getListView;
import static UI.components.utils.Observables.*;
import static utils.Constants.*;

public class ClientUI {
    private final Logger logger;
    private final ADBHandler adbHandler;
    private final EmulatorManager emulatorManager;
    private final DevUI devUI;
    private final MainUI mainUI;
    private final MenuDropdown menuDropdown;
    private final ScriptExecutor scriptExecutor;
    // Views
    BorderPane borderPane = new BorderPane();
    private ListView<String> emulatorListview;
    private final MenuButton devMenuButton;

    public static String VERSION_NUMBER = "OpenSource 1.0.4";

    public ClientUI(ScriptExecutor scriptExecutor, IsScriptRunning isScriptRunning, MapUI mapUI, GraphUI graphUI, BreakUI breakUI, HopUI hopUI, AccountManagerUI accountManagerUI, Logger logger, ADBHandler adbHandler, EmulatorManager emulatorManager, MainUI mainUI, DevUI devUI) {
        this.scriptExecutor = scriptExecutor;
        this.logger = logger;
        this.adbHandler = adbHandler;
        this.emulatorManager = emulatorManager;
        this.devUI = devUI;
        this.mainUI = mainUI;
        this.menuDropdown = new MenuDropdown(isScriptRunning, emulatorManager, mapUI, graphUI, breakUI, hopUI, accountManagerUI, logger, adbHandler);
        devMenuButton = new MenuDropdown(isScriptRunning, emulatorManager, mapUI, graphUI, breakUI, hopUI, accountManagerUI, logger, adbHandler).getDropdownMenu();
    }

    public void initUI(Stage primaryStage) {
        initializeEmulatorView();

        // Set the main client properties
        primaryStage.setTitle("Mufasa " + VERSION_NUMBER); // Sets the UI title
        primaryStage.setResizable(false); // Makes the UI not resizable
        primaryStage.getIcons().add(MUFASA_LOGO); // Sets the UI icon

        // Add listeners to update the observables when the stage's size changes
        primaryStage.widthProperty().addListener((obs, oldVal, newVal) -> WIDTH_OBSERVABLE.set(newVal.doubleValue()));
        primaryStage.heightProperty().addListener((obs, oldVal, newVal) -> HEIGHT_OBSERVABLE.set(newVal.doubleValue()));

        // Add listeners to update the stage's size when the observables change
        WIDTH_OBSERVABLE.addListener((obs, oldVal, newVal) -> primaryStage.setWidth(newVal.doubleValue()));
        HEIGHT_OBSERVABLE.addListener((obs, oldVal, newVal) -> primaryStage.setHeight(newVal.doubleValue()));

        // Handle the "Quit" menu item
        primaryStage.setOnCloseRequest(event -> {
            // Additional cleanup if necessary
            scriptExecutor.stopAllScripts();

            emulatorManager.stopAllCaptures();
            emulatorManager.shutdown();
            mainUI.stopAllUpdaters();
            adbHandler.shutdown();
            ThreadManager.getInstance().shutdown();
            Platform.exit();
            System.exit(0);
        });

        // Initially loading all the default UI parts
        borderPane.setCenter(mainUI.display(emulatorListview, menuDropdown.getDropdownMenu())); // Sets the default view to Informative

        Scene scene = new Scene(borderPane);
        scene.getStylesheets().add(STYLESHEET);
        WIDTH_OBSERVABLE.set(980);
        HEIGHT_OBSERVABLE.set(700);
        primaryStage.setScene(scene);
        primaryStage.show();

        logger.devLog("Main view loaded.");

        menuDropdown.developerView.setOnAction(e -> {
            DEVLOGS_ENABLED.set(true);
            logger.devLog("Entered developer UI!");
            devUI.display(devMenuButton);
        });
    }

    // Helper methods
    public void initializeEmulatorView() {
        this.emulatorListview = getListView(); // Set the list view to the one from emulatorView class
    }
}
