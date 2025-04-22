package helpers;

import UI.components.LogArea;
import javafx.application.Platform;
import utils.SystemUtils;

import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

import static UI.ClientUI.VERSION_NUMBER;
import static UI.components.EmulatorView.getSelectedEmulator;
import static UI.components.utils.Observables.DEBUGGING_ENABLED;
import static UI.components.utils.Observables.DEVLOGS_ENABLED;
import static utils.Constants.getPublicIp;

public class Logger {
    private final LogArea logArea;

    public Logger(LogArea logArea) {
        this.logArea = logArea;
    }

    public void log(String logMessage, String emulatorID) {
        String formattedMessage = formatLogMessage("[INFO] " + logMessage);
        updateLogArea(emulatorID, formattedMessage);
    }

    public void debugLog(String logMessage, String emulatorID) {
        if (DEBUGGING_ENABLED.get()) {
            String formattedMessage = formatLogMessage("[DEBUG] " + logMessage);
            if (getSelectedEmulator().equals(emulatorID)) {
                System.out.println(formattedMessage);
            }
            updateLogArea(emulatorID, formattedMessage);
        }
    }

    public void print(String logMessage) {
        String formattedMessage = formatLogMessage("[CONSOLE] " + logMessage);
        System.out.println(formattedMessage);
    }

    public void logException(IOException logMessage) {
        String formattedMessage = formatLogMessage("[CONSOLE] " + logMessage);
        System.out.println(formattedMessage);
    }

    public void logException(InterruptedException logMessage) {
        String formattedMessage = formatLogMessage("[CONSOLE] " + logMessage);
        System.out.println(formattedMessage);
    }

    public void devLog(String logMessage) {
        if (DEVLOGS_ENABLED.get()) {
            String formattedMessage = formatLogMessage("[DEV] " + logMessage);
            updateGlobalLogs(formattedMessage);
        }
    }

    public void errorLog(String logMessage) {
        if (DEVLOGS_ENABLED.get()) {
            String formattedMessage = formatLogMessage("[ERROR] " + logMessage);
            updateGlobalLogs(formattedMessage);
        }
    }

    public void globalLog(String logMessage) {
        String formattedMessage = formatLogMessage("[ERROR] " + logMessage);
        updateGlobalLogs(formattedMessage);
    }

    private String createLogFilePath() {
        // Use your existing code to create a file path
        String folderPath = SystemUtils.getLogsFolderPath();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss");
        LocalDateTime currentTime = LocalDateTime.now();
        String formattedDateTime = currentTime.format(formatter);
        String logFileName = "Log-" + formattedDateTime + ".txt";
        return folderPath + "/" + logFileName;
    }

    public void clearLogsForEmulator(String deviceID) {
        logArea.removeLogBuffer(deviceID);
    }

    public void err(String logMessage) {
        System.err.println(logMessage);
    }

    // Helper method to format log messages
    private String formatLogMessage(String logMessage) {
        SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        String timeStr = df.format(new Date());
        return "[" + timeStr + "]: " + logMessage;
    }

    // Helper method to update log area
    private void updateLogArea(String emulatorID, String logMessage) {
        Platform.runLater(() -> {
            if (logArea.getLogArea() != null) {
                logArea.updateLog(emulatorID, logMessage);
            } else {
                System.out.println("logArea is not set. Cannot append log message: " + logMessage);
            }
        });
    }

    // Helper method to update log area
    private void updateGlobalLogs(String logMessage) {
        Platform.runLater(() -> {
            if (logArea.getLogArea() != null) {
                logArea.updateGlobalLog(logMessage);
                System.out.println(logMessage);
            } else {
                System.out.println("logArea is not set. Cannot append log message: " + logMessage);
            }
        });
    }

    private String prepareExceptionLog(String message, StackTraceElement[] stackTrace) {
        String logFilePath = createLogFilePath();
        try (FileWriter writer = new FileWriter(logFilePath, true)) {
            // Write the exception message at the top of the log
            writer.write("Exception Message: " + message + "\n");
            writer.write("Stack Trace:\n");

            // Write the stack trace elements
            for (StackTraceElement element : stackTrace) {
                writer.write(element.toString() + "\n");
            }
        } catch (IOException e) {
            devLog("Error writing to log file: " + e.getMessage());
            e.printStackTrace();
        }
        return logFilePath;
    }
}
