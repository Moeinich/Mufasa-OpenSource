package UI;

import UI.components.EmulatorView;
import UI.components.LogArea;
import UI.components.PaintBar;
import com.google.common.io.Resources;
import helpers.CacheManager;
import helpers.Logger;
import helpers.ThreadManager;
import helpers.annotations.ScriptManifest;
import helpers.services.BreakHandlerService;
import helpers.services.RuntimeService;
import helpers.services.SleepHandlerService;
import helpers.services.XPService;
import helpers.services.utils.IHandlerService;
import helpers.utils.HopTimeInfo;
import helpers.visualFeedback.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import org.fxmisc.richtext.StyleClassedTextArea;
import scripts.ScriptInfo;

import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static UI.components.EmulatorView.getListView;
import static UI.components.utils.Observables.*;
import static utils.Constants.MUFASA_LOGO;

public class MainUI {
    private final LogArea logAreaInstance;
    private final ScriptInfo scriptInfo;
    private final CacheManager cacheManager;
    private final RuntimeService runtimeService;
    private final BreakHandlerService breakHandlerService;
    private final SleepHandlerService sleepHandlerService;
    private final XPService xpService;
    private final ImageView gameView;
    private FeedbackDrawHandler feedbackDrawer;
    Canvas drawingCanvas = new Canvas();
    private FeedbackListenerManager feedbackListenerManager;

    private final Logger logger;
    private BorderPane mainView;
    private ListView<String> emulatorListView;
    private StyleClassedTextArea logArea;
    private String currentSelectedEmulator;
    private ScheduledFuture<?> xpPerHourUpdaterTask;
    private ScheduledFuture<?> breakUpdaterTask;
    private ScheduledFuture<?> scriptNameUpdaterTask;
    private ScheduledFuture<?> runtimeUpdaterTask;
    private ScheduledFuture<?> hopUpdaterTask;
    private final Label[] runStatisticsLabels;
    private MenuButton dropdownMenu = null;

    private final VBox bottomBox = new VBox();

    private final VBox rightContainer = new VBox();
    private boolean isPaintBarVisible = false; // Flag to track the visibility state

    // Reference to ThreadManager should be accessible
    private final ThreadManager threadManager = ThreadManager.getInstance();

    // Define the paths to your icon images
    private static final Image[] ICON_PATHS = {
            new Image(Objects.requireNonNull(Resources.class.getResourceAsStream("/assets/icons/scroll.png"))),
            new Image(Objects.requireNonNull(Resources.class.getResourceAsStream("/assets/icons/clock.png"))),
            new Image(Objects.requireNonNull(Resources.class.getResourceAsStream("/assets/icons/medal.png"))),
            new Image(Objects.requireNonNull(Resources.class.getResourceAsStream("/assets/icons/snooze.png"))),
            new Image(Objects.requireNonNull(Resources.class.getResourceAsStream("/assets/icons/globe-stand.png")))
    };

    // Preload the images and store them in a list
    private final List<Image> preloadedIcons = new ArrayList<>();

    public MainUI(LogArea logArea, ScriptInfo scriptInfo, CacheManager cacheManager, EmulatorView emulatorView,
                  RuntimeService runtimeService, BreakHandlerService breakHandlerService, SleepHandlerService sleepHandlerService, XPService xpService, Logger logger) {
        this.logAreaInstance = logArea;
        this.scriptInfo = scriptInfo;
        this.cacheManager = cacheManager;
        this.runtimeService = runtimeService;
        this.breakHandlerService = breakHandlerService;
        this.sleepHandlerService = sleepHandlerService;
        this.xpService = xpService;
        this.logger = logger;
        gameView = emulatorView.getImageView();
        emulatorListView = getListView();
        runStatisticsLabels = new Label[5]; // Initialize the array to hold labels for the run statistics boxes

        initializeLogAreaListener(); // Listener for the log area
        preloadImages();
    }

    private void preloadImages() {
        Collections.addAll(preloadedIcons, ICON_PATHS);
    }

    private void initializeUI() {
        // Set up the sidebar layout
        VBox sidebar = new VBox();
        sidebar.setPadding(new Insets(15));
        sidebar.setSpacing(10);
        sidebar.getStyleClass().add("elevation-1");
        sidebar.setAlignment(Pos.TOP_CENTER);

        // Add the animated logo
        ImageView logo = new ImageView(MUFASA_LOGO);
        logo.setFitWidth(75);
        logo.setPreserveRatio(true);
        logo.setSmooth(true);

        // Set up the emulator list view
        emulatorListView.setMinHeight(280);  // Adjusted height to make space for dropdown menu
        emulatorListView.setPrefWidth(190);

        // Add components to the sidebar
        sidebar.getChildren().addAll(logo, emulatorListView, dropdownMenu);
        VBox.setMargin(logo, new Insets(0, 0, 20, 0));  // Add some margin to the logo
        VBox.setVgrow(emulatorListView, Priority.ALWAYS);
        VBox.setMargin(dropdownMenu, new Insets(20, 0, 0, 0));  // Add margin to the dropdown menu
        dropdownMenu.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(dropdownMenu, Priority.ALWAYS);

        // Right side layout setup with separate boxes
        rightContainer.setSpacing(20);  // Spacing between the vertical boxes

        // Top box with 5 smaller boxes
        VBox topBoxContainer = new VBox();
        topBoxContainer.getStyleClass().add("elevation-1");
        topBoxContainer.setPadding(new Insets(10));

        HBox topBox = new HBox();
        topBox.setAlignment(Pos.CENTER);
        topBox.setSpacing(5);  // Padding between the smaller boxes

        for (int i = 0; i < preloadedIcons.size(); i++) {
            VBox smallBox = new VBox();
            smallBox.setAlignment(Pos.CENTER);
            smallBox.setPadding(new Insets(10));

            // Create HBox to hold the icon and label
            HBox iconAndLabelBox = new HBox();
            iconAndLabelBox.setAlignment(Pos.CENTER);

            // Reuse the preloaded Image
            ImageView icon = new ImageView(preloadedIcons.get(i));
            icon.setFitWidth(15);
            icon.setPreserveRatio(true);

            runStatisticsLabels[i] = new Label();
            runStatisticsLabels[i].getStyleClass().add("label-grey");

            // Add a margin between the icon and the label
            HBox.setMargin(runStatisticsLabels[i], new Insets(0, 0, 0, 5)); // Set left margin of 5px to the label

            // Add the icon and label to the HBox
            iconAndLabelBox.getChildren().addAll(icon, runStatisticsLabels[i]);

            // Add the HBox to the smallBox
            smallBox.getChildren().add(iconAndLabelBox);

            HBox.setHgrow(smallBox, Priority.ALWAYS); // Make the small boxes take up equal space
            topBox.getChildren().add(smallBox);
        }

        topBoxContainer.getChildren().add(topBox);

        // Middle box
        VBox middleBox = new VBox();
        middleBox.setAlignment(Pos.CENTER);
        middleBox.getStyleClass().add("elevation-1");
        middleBox.setPadding(new Insets(10));

        StackPane stackPane = new StackPane();
        stackPane.setAlignment(Pos.CENTER);

        // Create the Canvas
        drawingCanvas.setWidth(670);
        drawingCanvas.setHeight(405);
        feedbackDrawer = new FeedbackDrawHandler(drawingCanvas);
        feedbackListenerManager = new FeedbackListenerManager(feedbackDrawer);

        // Add gameView and Canvas to the StackPane
        stackPane.getChildren().addAll(gameView, drawingCanvas);

        middleBox.getChildren().add(stackPane);
        VBox.setVgrow(middleBox, Priority.ALWAYS);

        // Bottom box
        bottomBox.setPrefHeight(110);
        bottomBox.setAlignment(Pos.CENTER);
        bottomBox.getStyleClass().add("elevation-1");
        bottomBox.setPadding(new Insets(10));

        logArea.getStyleClass().add("logareabox");
        VBox.setVgrow(logArea, Priority.ALWAYS);

        // Add boxes to right container
        rightContainer.getChildren().addAll(topBoxContainer, middleBox, bottomBox);
        setupListeners();

        // Main UI layout setup
        this.mainView = new BorderPane();
        this.mainView.setLeft(sidebar);
        this.mainView.setCenter(rightContainer); // Changed from setRight to setCenter
        BorderPane.setMargin(sidebar, new Insets(15, 0, 15, 15));
        BorderPane.setMargin(rightContainer, new Insets(15, 15, 15, 20)); // Adjusted margin to align properly
    }

    private void setupListeners() {
        emulatorListView.getSelectionModel().selectedItemProperty().addListener((obs, oldEmulator, newEmulator) -> {
            if (newEmulator != null) {
                currentSelectedEmulator = newEmulator; // Update the currently selected emulator

                // Check if the new emulator should show or hide the paintbar
                boolean hasPaintBar = cacheManager.getPaintbarInstance().containsKey(newEmulator);
                if (IS_PAINT_ENABLED.get() && hasPaintBar) {
                    Platform.runLater(() -> showPaintBar(cacheManager.getPaintbarInstance().get(newEmulator)));
                } else {
                    Platform.runLater(this::hidePaintBar);
                }

                // VISUAL DRAWS
                // Clear all previous listeners
                feedbackListenerManager.clearAllListeners();
                // Add listeners for the selected emulator
                feedbackListenerManager.addListenersForCurrentEmulator();
            }
        });

        // Add remove listener to paintbars
        cacheManager.getPaintbarInstance().addRemoveListener((key, value) -> {
            if (IS_PAINT_ENABLED.get() && key.equals(currentSelectedEmulator)) {
                System.out.println("Hiding paintbar for: " + key);
                Platform.runLater(this::hidePaintBar);
            }
        });

        // Add add listener to paintbars
        cacheManager.getPaintbarInstance().addAddListener((key, value) -> {
            if (IS_PAINT_ENABLED.get() && key.equals(currentSelectedEmulator)) {
                System.out.println("Showing paintbar on: " + key);
                Platform.runLater(() -> showPaintBar(value));
            }
        });

        // Add listener to isPaintEnabled to enable/disable the listeners dynamically
        IS_PAINT_ENABLED.addListener((obs, oldValue, newValue) -> {
            if (!newValue) {
                Platform.runLater(this::hidePaintBar);
            } else {
                // If true, recheck the current selected emulator
                if (currentSelectedEmulator != null) {
                    boolean hasPaintBar = cacheManager.getPaintbarInstance().containsKey(currentSelectedEmulator);
                    if (hasPaintBar) {
                        Platform.runLater(() -> showPaintBar(cacheManager.getPaintbarInstance().get(currentSelectedEmulator)));
                    }
                }
            }
        });

        if (currentSelectedEmulator != null) {
            feedbackListenerManager.addListenersForCurrentEmulator(); // Hook up the 1st selected emulator
        }
    }

    private void showPaintBar(PaintBar paintBar) {
        VBox paintBarVBox = paintBar.getPaintBox();
        paintBarVBox.setPrefWidth(295);
        paintBarVBox.setMinWidth(295);
        WIDTH_OBSERVABLE.set(1280);
        HEIGHT_OBSERVABLE.set(700);
        BorderPane.setMargin(paintBarVBox, new Insets(15, 15, 15, 0)); // Add margin to the paintBar
        mainView.setRight(paintBarVBox); // Set the paintBar to the right of the mainView
        paintBarVBox.setVisible(true); // Make the paintBox visible
        BorderPane.setMargin(rightContainer, new Insets(15, 10, 15, 20));
        isPaintBarVisible = true; // Update the flag to indicate the paint bar is now visible
    }

    private void hidePaintBar() {
        if (isPaintBarVisible) {
            mainView.setRight(null); // Remove the paintBar from the right of the mainView
            BorderPane.setMargin(rightContainer, new Insets(15, 15, 15, 20));
            WIDTH_OBSERVABLE.set(980);
            HEIGHT_OBSERVABLE.set(700);
            isPaintBarVisible = false; // Update the flag to indicate the paint bar is now hidden
        }
    }

    // Refactored updater methods using ThreadManager's scheduler
    private void startScriptNameUpdater() {
        if (scriptNameUpdaterTask != null && !scriptNameUpdaterTask.isCancelled()) {
            scriptNameUpdaterTask.cancel(true);
        }

        scriptNameUpdaterTask = threadManager.getScheduler().scheduleAtFixedRate(() -> {
            try {
                String scriptName = getScriptName();
                Platform.runLater(() -> setRunStatisticsScriptName(scriptName));
            } catch (Exception e) {
                logger.err("Error updating script name: " + e.getMessage());
                e.printStackTrace();
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    private void startXPPerHourUpdater() {
        if (xpPerHourUpdaterTask != null && !xpPerHourUpdaterTask.isCancelled()) {
            xpPerHourUpdaterTask.cancel(true);
        }

        xpPerHourUpdaterTask = threadManager.getScheduler().scheduleAtFixedRate(() -> {
            try {
                String xpPerHour = getXPPerHour();
                Platform.runLater(() -> setRunStatisticsXP(xpPerHour));
            } catch (Exception e) {
                logger.err("Error updating XP per hour: " + e.getMessage());
                e.printStackTrace();
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    private void startBreakUpdater() {
        if (breakUpdaterTask != null && !breakUpdaterTask.isCancelled()) {
            breakUpdaterTask.cancel(true);
        }

        breakUpdaterTask = threadManager.getScheduler().scheduleAtFixedRate(() -> {
            try {
                String nextBreak = getNextBreak();
                Platform.runLater(() -> setRunStatisticsBreaks(nextBreak));
            } catch (Exception e) {
                logger.err("Error updating next break: " + e.getMessage());
                e.printStackTrace();
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    private void startRuntimeUpdater() {
        if (runtimeUpdaterTask != null && !runtimeUpdaterTask.isCancelled()) {
            runtimeUpdaterTask.cancel(true);
        }

        runtimeUpdaterTask = threadManager.getScheduler().scheduleAtFixedRate(() -> {
            try {
                String runtime = getRuntime();
                Platform.runLater(() -> setRunStatisticsRuntime(runtime));
            } catch (Exception e) {
                logger.err("Error updating runtime: " + e.getMessage());
                e.printStackTrace();
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    private void startHopUpdater() {
        if (hopUpdaterTask != null && !hopUpdaterTask.isCancelled()) {
            hopUpdaterTask.cancel(true);
        }

        hopUpdaterTask = threadManager.getScheduler().scheduleAtFixedRate(() -> {
            try {
                String nextHop = getNextHopTime();
                Platform.runLater(() -> setRunStatisticsHops(nextHop));
            } catch (Exception e) {
                logger.err("Error updating next hop time: " + e.getMessage());
                e.printStackTrace();
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    // Helper methods to retrieve data
    private String getScriptName() {
        if (currentSelectedEmulator != null) {
            ScriptManifest manifest = scriptInfo.getScriptManifest(currentSelectedEmulator);
            return (manifest != null) ? manifest.name() + " (" + manifest.version() + ")" : "-";
        }
        return "-";
    }

    private String getXPPerHour() {
        if (currentSelectedEmulator == null || !xpService.hasXPDataForDevice(currentSelectedEmulator)) {
            return "-";
        }
        String xpData = String.valueOf(xpService.getXPLabelFormat(currentSelectedEmulator));
        return xpData.equals("0") ? "-" : xpData;
    }

    private String getRuntime() {
        if (currentSelectedEmulator != null) {
            RuntimeService service = runtimeService.getExistingHandler(currentSelectedEmulator);
            if (service != null) {
                long elapsedTime = service.getElapsedTime() / 1000;
                long days = elapsedTime / 86400;
                long hours = (elapsedTime % 86400) / 3600;
                long minutes = (elapsedTime % 3600) / 60;
                long seconds = elapsedTime % 60;

                if (days > 0) {
                    return String.format("%02d:%02d:%02d:%02d", days, hours, minutes, seconds);
                } else if (hours > 0) {
                    return String.format("%02d:%02d:%02d", hours, minutes, seconds);
                } else {
                    return String.format("%02d:%02d", minutes, seconds);
                }
            }
        }
        return "-";
    }

    private String getNextBreak() {
        if (currentSelectedEmulator == null) {
            return "-";
        }
        BreakHandlerService breakHandler = breakHandlerService.getExistingHandler(currentSelectedEmulator);
        SleepHandlerService sleepHandler = sleepHandlerService.getExistingHandler(currentSelectedEmulator);
        boolean isBreakEnabled = (breakHandler != null && breakHandler.isEnabled());
        boolean isSleepEnabled = (sleepHandler != null && sleepHandler.isEnabled());

        if (!isBreakEnabled && !isSleepEnabled) {
            return "-";
        }

        long breakTime = isBreakEnabled ? breakHandler.getTimeUntilNextEvent() : Long.MAX_VALUE;
        long sleepTime = isSleepEnabled ? sleepHandler.getTimeUntilNextEvent() : Long.MAX_VALUE;
        boolean showSleep = (sleepTime < breakTime);

        // If they're the same, show sleep timer (?)
        if (sleepTime == breakTime && isSleepEnabled) {
            showSleep = true;
        }

        if (showSleep) {
            // Sleep event is sooner
            return buildDisplayText(sleepHandler, sleepTime, true);
        } else {
            // Break event is sooner
            return buildDisplayText(breakHandler, breakTime, false);
        }
    }

    private String getNextHopTime() {
        if (currentSelectedEmulator != null) {
            HopTimeInfo hopTimeInfo = cacheManager.getHopTimeInfo(currentSelectedEmulator);

            if (hopTimeInfo != null && hopTimeInfo.isHopsPostponed) {
                return "postponed";
            }

            long currentTimeInSeconds = System.currentTimeMillis() / 1000;
            long nextHopTimestamp = (hopTimeInfo != null) ? hopTimeInfo.nextHopTime : 0;
            long timeToNextHop = nextHopTimestamp - currentTimeInSeconds;

            if (timeToNextHop > 0) {
                long hours = timeToNextHop / 3600;
                long minutes = (timeToNextHop % 3600) / 60;
                long seconds = timeToNextHop % 60;

                if (hours > 0) {
                    return String.format("%02d:%02d:%02d", hours, minutes, seconds);
                } else {
                    return String.format("%02d:%02d", minutes, seconds);
                }
            }
        }
        return "-";
    }

    // Methods to set the labels for the run statistics boxes individually
    public void setRunStatisticsLabel(int index, String text) {
        if (index >= 0 && index < runStatisticsLabels.length) {
            runStatisticsLabels[index].setText(text);
        }
    }

    public void setRunStatisticsScriptName(String text) {
        setRunStatisticsLabel(0, text);
    }

    public void setRunStatisticsRuntime(String text) {
        setRunStatisticsLabel(1, text);
    }

    public void setRunStatisticsXP(String text) {
        setRunStatisticsLabel(2, text);
    }

    public void setRunStatisticsBreaks(String text) {
        setRunStatisticsLabel(3, text);
    }

    public void setRunStatisticsHops(String text) {
        setRunStatisticsLabel(4, text);
    }

    private void initializeLogAreaListener() {
        logArea = logAreaInstance.getLogTextArea();
        bottomBox.getChildren().add(logArea);

        // Add a listener to isDevUIOpen property
        IS_DEVUI_OPEN.addListener((observable, oldValue, newValue) -> {
            // Check if the property changed from true to false
            if (oldValue && !newValue) {
                bottomBox.getChildren().remove(logArea);
                logArea = logAreaInstance.getLogTextArea();
                bottomBox.getChildren().add(logArea);
            }
        });
    }

    public Node display(ListView<String> emulatorList, MenuButton menuDropdown) {
        this.emulatorListView = emulatorList;
        this.dropdownMenu = menuDropdown;
        initializeUI();
        startAllUpdaters(); // Start all updater tasks upon UI initialization
        return mainView;
    }

    // Start all updater tasks
    private void startAllUpdaters() {
        startScriptNameUpdater();
        startXPPerHourUpdater();
        startBreakUpdater();
        startRuntimeUpdater();
        startHopUpdater();
    }

    // Stop all updater tasks
    public void stopAllUpdaters() {
        if (scriptNameUpdaterTask != null && !scriptNameUpdaterTask.isCancelled()) {
            scriptNameUpdaterTask.cancel(true);
            scriptNameUpdaterTask = null;
            logger.print("Script Name Updater task cancelled.");
        }

        if (xpPerHourUpdaterTask != null && !xpPerHourUpdaterTask.isCancelled()) {
            xpPerHourUpdaterTask.cancel(true);
            xpPerHourUpdaterTask = null;
            logger.print("XP Per Hour Updater task cancelled.");
        }

        if (breakUpdaterTask != null && !breakUpdaterTask.isCancelled()) {
            breakUpdaterTask.cancel(true);
            breakUpdaterTask = null;
            logger.print("Break Updater task cancelled.");
        }

        if (runtimeUpdaterTask != null && !runtimeUpdaterTask.isCancelled()) {
            runtimeUpdaterTask.cancel(true);
            runtimeUpdaterTask = null;
            logger.print("Runtime Updater task cancelled.");
        }

        if (hopUpdaterTask != null && !hopUpdaterTask.isCancelled()) {
            hopUpdaterTask.cancel(true);
            hopUpdaterTask = null;
            logger.print("Hop Updater task cancelled.");
        }

        logger.print("Stopping Visual Feedback manager");
        feedbackDrawer.clearCanvas();
        feedbackListenerManager.clearAllListeners();
    }

    /**
     * isSleep = true => we know it's from SleepHandler.
     * isSleep = false => it's from BreakHandler.
     */
    private String buildDisplayText(IHandlerService handler, long timeSecs, boolean isSleep) {
        // If time is zero or negative:
        if (timeSecs <= 0) {
            if (handler.isPostponed()) {
                return "Postponed";
            }
            return "...";
        }

        // Format HH:MM:SS or MM:SS
        String timeText = formatTime(timeSecs);

        // If the handler is actively on break/sleep
        if (handler.isActiveNow()) {
            // OnSleep: or OnBrk:
            return isSleep ? "OnSleep: " + timeText
                    : "OnBrk: "   + timeText;
        } else {
            // If it's the sleep handler, we show "Sleep in:"
            if (isSleep) {
                return "Sleep in: " + timeText;
            } else {
                // If it's break handler, just show the time
                return timeText;
            }
        }
    }

    private String formatTime(long timeInSeconds) {
        long hours = timeInSeconds / 3600;
        long minutes = (timeInSeconds % 3600) / 60;
        long seconds = timeInSeconds % 60;

        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%02d:%02d", minutes, seconds);
        }
    }

    /**
     * Gracefully shuts down the MainUI by cancelling all scheduled tasks and performing necessary cleanup.
     */
    public void shutdown() {
        logger.print("Shutting down MainUI...");

        Platform.runLater(() -> {
            gameView.setImage(null);
            logger.print("ImageView cleared during shutdown.");
        });
    }
}
