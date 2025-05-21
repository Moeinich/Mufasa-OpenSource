package UI.components;

import helpers.ThreadManager;
import helpers.utils.LogBuffer;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.fxmisc.richtext.StyleClassedTextArea;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static UI.components.utils.Observables.DEBUGGING_ENABLED;
import static utils.Constants.BUG_ICON;
import static utils.Constants.BUG_ICON_WHITE;

public class LogArea {
    public static final String GLOBAL_LOG_KEY = "GLOBAL_LOG";
    private static final int MAX_LOGS_PER_EMULATOR = 500;
    private final StyleClassedTextArea logArea = new StyleClassedTextArea();
    private final ConcurrentHashMap<String, LogBuffer> logBuffers = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = ThreadManager.getInstance().getScheduler();
    private final StringBuilder logBatch = new StringBuilder();

    private static final Pattern LOG_PATTERN = Pattern.compile(
            "^(\\[\\d{2}:\\d{2}:\\d{2}]):\\s*(\\[[A-Z]+])\\s*(.*)$"
    );

    private static final Map<String, String> KEYWORDS = Map.of(
            // Successes
            "success", "successKW",
            "result", "resultKW",

            // Error things
            "fail", "errorKW",
            "failed", "errorKW",
            "error", "errorKW",
            "invalid", "errorKW",
            "denied", "errorKW"
    );

    private static final Pattern KEYWORD_PATTERN;

    static {
        String joinedKeywords = String.join("|", KEYWORDS.keySet());
        String regex = "\\b(?i)(" + joinedKeywords + ")\\b";
        KEYWORD_PATTERN = Pattern.compile(regex);
    }

    private final Button enableDebugLogs = new Button();
    private final StackPane logAreaContainer = new StackPane();

    private int currentLineCount = 0;

    private String currentEmulatorID;

    public LogArea() {
        setStyling();
        addDebugButton();
        setAutoScroll();
        scheduleLogBatchFlushing();
    }

    private void addDebugButton() {
        ImageView bugIcon = new ImageView(); // Create the ImageView without setting an initial image
        bugIcon.setFitWidth(15);
        bugIcon.setFitHeight(15);

        // Set up the button graphic
        enableDebugLogs.setGraphic(bugIcon);

        // Align the button in the top-right corner
        StackPane.setAlignment(enableDebugLogs, Pos.TOP_RIGHT);
        StackPane.setMargin(enableDebugLogs, new javafx.geometry.Insets(5)); // Adjust margin as needed

        // Add a listener to DEBUGGING_ENABLED to react to state changes
        DEBUGGING_ENABLED.addListener((observable, oldValue, newValue) -> applyButtonState(newValue, bugIcon));

        // Add click behavior to toggle DEBUGGING_ENABLED
        enableDebugLogs.setOnAction(event -> DEBUGGING_ENABLED.set(!DEBUGGING_ENABLED.get()));

        // Trigger the listener initially to ensure correct styling and icon
        applyButtonState(DEBUGGING_ENABLED.get(), bugIcon);

        logAreaContainer.getChildren().addAll(logArea, enableDebugLogs);
    }

    private void applyButtonState(boolean isActive, ImageView bugIcon) {
        // Update the button style
        enableDebugLogs.setStyle(isActive
                ? "-fx-background-color: #e57c23; -fx-border-radius: 5px; -fx-background-radius: 5px;" // Active: orange
                : "-fx-background-color: #1B1817; -fx-border-radius: 5px; -fx-background-radius: 5px;"); // Inactive: grey

        // Update the icon
        bugIcon.setImage(isActive ? BUG_ICON : BUG_ICON_WHITE);
    }

    private void scheduleLogBatchFlushing() {
        scheduler.scheduleAtFixedRate(() -> {
            String logText;
            synchronized (logBatch) {
                logText = logBatch.toString();
                logBatch.setLength(0);
            }
            if (!logText.isEmpty()) {
                ThreadManager.getInstance().getUnifiedExecutor().submit(() -> {
                    StyleSpans<Collection<String>> spans = computeStylesForChunk(logText);
                    Platform.runLater(() -> {
                        int start = logArea.getLength();
                        logArea.appendText(logText);
                        logArea.setStyleSpans(start, spans);
                        int addedLines = logText.split("\n", -1).length;
                        currentLineCount += addedLines;
                        if (currentLineCount > MAX_LOGS_PER_EMULATOR) {
                            trimLogArea();
                        }
                    });
                });
            }
        }, 200, 200, TimeUnit.MILLISECONDS);
    }

    private void appendToLogArea(String chunkText) {
        if (chunkText.isEmpty()) return;

        int start = logArea.getLength();
        logArea.appendText(chunkText);
        StyleSpans<Collection<String>> spans = computeStylesForChunk(chunkText);
        logArea.setStyleSpans(start, spans);

        // Count how many new lines we added
        int addedLineCount = chunkText.split("\n", -1).length;
        currentLineCount += addedLineCount;

        if (currentLineCount > MAX_LOGS_PER_EMULATOR) {
            trimLogArea();
        }
    }

    private StyleSpans<Collection<String>> computeStylesForChunk(String chunkText) {
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();

        // Split by newlines (retain empty strings if chunkText ends with \n)
        String[] lines = chunkText.split("\n", -1);

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            // Re-use your existing logic for a single line:
            StyleSpans<Collection<String>> lineSpans = computeLineStyleSpans(line);

            // Merge this line’s spans into the overall builder
            lineSpans.forEach(span ->
                    spansBuilder.add(span.getStyle(), span.getLength())
            );

            // For each line except maybe the last, add a newline's style.
            // If you're always appending a trailing "\n", do it here:
            if (i < lines.length - 1) {
                // The newline character itself is default-text
                spansBuilder.add(Collections.singleton("default-text"), 1);
            }
        }

        return spansBuilder.create();
    }

    private StyleSpans<Collection<String>> computeLineStyleSpans(String text) {
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();

        if (text.isEmpty()) {
            spansBuilder.add(Collections.singleton("default-text"), 0);
            return spansBuilder.create();
        }

        Matcher matcher = LOG_PATTERN.matcher(text);

        if (matcher.matches()) {
            String bracketedTime   = matcher.group(1); // "[12:35:46]"
            String bracketedLevel  = matcher.group(2); // "[DEV]"
            String message         = matcher.group(3); // e.g. "Everything was a Success, but it Failed!"

            spansBuilder.add(Collections.singleton("timestamp"), bracketedTime.length());
            spansBuilder.add(Collections.singleton("default-text"), 2);

            if ("[DEV]".equals(bracketedLevel)) {
                spansBuilder.add(Collections.singleton("dev"), bracketedLevel.length());
            } else if ("[DEBUG]".equals(bracketedLevel)) {
                spansBuilder.add(Collections.singleton("debug"), bracketedLevel.length());
            } else if ("[INFO]".equals(bracketedLevel)) {
                spansBuilder.add(Collections.singleton("info"), bracketedLevel.length());
            } else if ("[WARNING]".equals(bracketedLevel)) {
                spansBuilder.add(Collections.singleton("warning"), bracketedLevel.length());
            } else if ("[ERROR]".equals(bracketedLevel)) {
                spansBuilder.add(Collections.singleton("error"), bracketedLevel.length());
            } else {
                spansBuilder.add(Collections.singleton("default-text"), bracketedLevel.length());
            }

            spansBuilder.add(Collections.singleton("default-text"), 1);
            addMessageWithKeywords(spansBuilder, message);

        } else {
            addMessageWithKeywords(spansBuilder, text);
        }

        return spansBuilder.create();
    }

    private void addMessageWithKeywords(StyleSpansBuilder<Collection<String>> spansBuilder, String message) {
        if (message.isEmpty()) {
            spansBuilder.add(Collections.singleton("default-text"), 0);
            return;
        }

        Matcher matcher = KEYWORD_PATTERN.matcher(message);

        int lastEnd = 0;
        while (matcher.find()) {
            if (matcher.start() > lastEnd) {
                spansBuilder.add(Collections.singleton("default-text"), matcher.start() - lastEnd);
            }
            String matchedKeyword = matcher.group(1);
            String styleClass = KEYWORDS.get(matchedKeyword.toLowerCase());
            if (styleClass == null) {
                styleClass = "default-text";
            }

            spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());

            lastEnd = matcher.end();
        }
        if (lastEnd < message.length()) {
            spansBuilder.add(Collections.singleton("default-text"), message.length() - lastEnd);
        }
    }

    public void updateLog(String emulatorID, String logMessage) {
        synchronized (logBuffers) {
            LogBuffer logBuffer = logBuffers.computeIfAbsent(emulatorID, k -> new LogBuffer(MAX_LOGS_PER_EMULATOR));
            logBuffer.addLog(logMessage);

            if (emulatorID != null && emulatorID.equals(currentEmulatorID)) {
                synchronized (logBatch) {
                    logBatch.append(logMessage).append("\n");
                }
            }
        }
    }

    public void updateGlobalLog(String logMessage) {
        boolean buffersExist = false;
        synchronized (logBuffers) {
            if (!logBuffers.isEmpty()) {
                buffersExist = true;
                for (Map.Entry<String, LogBuffer> entry : logBuffers.entrySet()) {
                    LogBuffer logBuffer = entry.getValue();
                    logBuffer.addLog(logMessage);
                    appendToLogArea(logMessage + "\n");
                }
            }
        }
        if (!buffersExist) {
            Platform.runLater(() -> appendToLogArea(logMessage + "\n"));
        }
    }

    private void trimLogArea() {
        Platform.runLater(() -> {
            if (currentLineCount > MAX_LOGS_PER_EMULATOR) {
                int linesToRemove = currentLineCount - MAX_LOGS_PER_EMULATOR;
                int endIndex = logArea.getText().indexOf("\n");
                for (int i = 0; i < linesToRemove; i++) {
                    endIndex = logArea.getText().indexOf("\n", endIndex + 1);
                    if (endIndex == -1) break;
                }
                if (endIndex > 0) {
                    logArea.deleteText(0, endIndex + 1);
                    currentLineCount = MAX_LOGS_PER_EMULATOR;

                    Platform.runLater(() -> {
                        logArea.moveTo(logArea.getLength());
                        logArea.requestFollowCaret();
                    });
                }
            }
        });
    }

    public void setCurrentEmulatorID(String emulatorID) {
        synchronized (logBuffers) {
            if (!emulatorID.equals(currentEmulatorID)) {
                this.currentEmulatorID = emulatorID;
                Platform.runLater(() -> {
                    logArea.clear();
                    LogBuffer logs = logBuffers.getOrDefault(emulatorID, new LogBuffer(MAX_LOGS_PER_EMULATOR));
                    appendToLogArea(logs.getAllLogs());
                });
            }
        }
    }

    public void setStyling() {
        logArea.setEditable(false);
        VBox.setVgrow(logArea, Priority.ALWAYS);  // For VBox containers
        HBox.setHgrow(logArea, Priority.ALWAYS);
        logArea.setWrapText(true);
        logArea.getStyleClass().add("logareabox");
    }

    private void setAutoScroll() {
        logArea.plainTextChanges().subscribe(change -> Platform.runLater(() -> {
            logArea.moveTo(logArea.getLength());
            logArea.requestFollowCaret();
        }));
    }

    public StackPane getLogArea() {
        return logAreaContainer;
    }

    public StyleClassedTextArea getLogTextArea() {
        return logArea;
    }

    public void removeLogBuffer(String emulatorID) {
        synchronized (logBuffers) {
            logBuffers.remove(emulatorID);
        }
    }
}