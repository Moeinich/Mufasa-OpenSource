import UI.ClientUI;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import utils.Initialize;
import utils.InitializedObjects;
import utils.SystemUtils;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static utils.Constants.*;

public class Mufasa extends Application {
    private Stage loadingStage;
    private ClientUI client;

    public static void main() {
        launch();
    }

    @Override
    public void start(Stage primaryStage) {
        checkJavaVersion();
        doStart(primaryStage);
    }

    private void doStart(Stage primaryStage) {
        primaryStage.setTitle("Mufasa"); // Set UI title
        primaryStage.setResizable(false); // Makes the UI not resizable
        primaryStage.getIcons().add(MUFASA_LOGO); // Set UI icon

        if (!IS_WINDOWS_USER) {
            Platform.runLater(() -> {
                if (Taskbar.isTaskbarSupported()) {
                    Taskbar taskbar = Taskbar.getTaskbar();
                    try {
                        Image taskbarIcon = new ImageIcon(Objects.requireNonNull(getClass().getResource("/assets/mufasa-taskbar-icon.png"))).getImage();
                        taskbar.setIconImage(taskbarIcon);
                    } catch (UnsupportedOperationException | SecurityException e) {
                        System.err.println("Failed to set taskbar icon: " + e.getMessage());
                    }
                }
            });
        }

        showLoadingDialog(); // Show the loading dialog

        Platform.runLater(() -> {
            if (!initialize()) {
                closeLoadingDialog();
                Platform.exit();
                return;
            }

            closeLoadingDialog();
            client.initUI(primaryStage); // Start up the client!
        });
    }

    private boolean initialize() {
        //Init the folders
        SystemUtils.initializeFolders();
        //Initialize the client if login IS successful
        InitializedObjects classes = Initialize.init();
        client = classes.clientUI;
        return true;
    }

    private void showLoadingDialog() {
        loadingStage = new Stage();
        loadingStage.setTitle("Mufasa");
        loadingStage.setResizable(false); // Makes the UI not resizable
        loadingStage.getIcons().add(MUFASA_LOGO); // Set UI icon

        Label messageLabel = new Label("Loading the Mufasa client...");
        messageLabel.setAlignment(Pos.CENTER);

        VBox vbox = new VBox();
        vbox.setAlignment(Pos.CENTER);
        vbox.getChildren().addAll(messageLabel);

        Scene scene = new Scene(vbox, 225, 50);
        scene.getStylesheets().add(STYLESHEET);

        loadingStage.setScene(scene);

        // Get the primary screen bounds and calculate center position
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        loadingStage.setX((screenBounds.getWidth() - scene.getWidth()) / 2);
        loadingStage.setY((screenBounds.getHeight() - scene.getHeight()) / 2);

        loadingStage.show();
    }

    private void closeLoadingDialog() {
        if (loadingStage != null && loadingStage.isShowing()) {
            loadingStage.close();
        }
    }

    private void checkJavaVersion() {
        List<String> requiredJavaVersions = Arrays.asList("11.0.20", "11.0.21", "11.0.22", "11.0.23", "11.0.25");
        String currentJavaVersion = System.getProperty("java.version");
        System.out.println("DETECTED JAVA VERSION: " + currentJavaVersion);
        if (!requiredJavaVersions.contains(currentJavaVersion)) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Version Error");
            alert.setHeaderText(null);
            alert.setContentText("This application requires Java version 11.0.20, 11.0.21, 11.0.22, 11.0.23 or 11.0.25. Your version: " + currentJavaVersion);
            // Styling the Alert
            DialogPane dialogPane = alert.getDialogPane();
            dialogPane.setStyle("-fx-background-color: #1d1314;");
            ((Label) dialogPane.lookup(".content.label")).setTextFill(Color.WHITE);
            // Style buttons of the alert
            ButtonBar buttonBar = (ButtonBar) dialogPane.lookup(".button-bar");
            buttonBar.getButtons().forEach(b -> b.setStyle("-fx-background-color: #f3c244; -fx-text-fill: black; -fx-font-size: 14px;"));
            // Set icon for the alert
            Stage alertStage = (Stage) dialogPane.getScene().getWindow();
            alertStage.getIcons().add(MUFASA_LOGO);
            alert.setOnCloseRequest(event -> System.exit(0));
            alert.showAndWait();
        }
    }
}