package helpers.services;

import helpers.DiscordWebhook;
import helpers.GetGameView;
import helpers.ThreadManager;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import osr.mapping.Player;
import utils.SystemUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class LeveledupService {
    private final Player player;
    private final DiscordWebhook discordWebhook;
    private final GetGameView getGameView;
    private final ConcurrentHashMap<String, LeveledupService> levelServices = new ConcurrentHashMap<>();
    private ScheduledFuture<?> periodicTask;
    private final ScheduledExecutorService executorService = ThreadManager.getInstance().getScheduler();
    private final AtomicBoolean leveledUpSent = new AtomicBoolean(false);

    public LeveledupService(Player player, DiscordWebhook discordWebhook, GetGameView getGameView) {
        this.player = player;
        this.discordWebhook = discordWebhook;
        this.getGameView = getGameView;
    }

    public void start(String device) {
        startPeriodicUpdate(device);
    }

    private void startPeriodicUpdate(String device) {
        periodicTask = executorService.scheduleAtFixedRate(() -> {
            boolean leveledUp = player.leveledUp(device);
            if (leveledUp) {
                if (!leveledUpSent.get()) {
                    discordWebhook.sendLevelupMessage(device);
                    try {
                        convertMatToFile(getGameView.getMat(device));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                leveledUpSent.set(true);
            } else {
                leveledUpSent.set(false);
            }
        }, 0, 200, TimeUnit.MILLISECONDS);
    }

    // Static method to get or create a RuntimeService instance for a given ID
    public LeveledupService getHandlerForEmulator(String deviceID) {
        return levelServices.computeIfAbsent(deviceID, k -> new LeveledupService(player, discordWebhook, getGameView));
    }

    // Method to specifically retrieve an existing RuntimeService instance for a given ID
    public LeveledupService getExistingHandler(String deviceID) {
        return levelServices.get(deviceID); // Returns null if not found
    }

    // Stops the periodic update task and shuts down the executor
    private void stopPeriodicUpdate() {
        if (periodicTask != null && !periodicTask.isCancelled()) {
            periodicTask.cancel(true);  // Cancel the task for this specific device
        }
    }

    public synchronized void removeService(String deviceID) {
        LeveledupService service = levelServices.get(deviceID);
        if (service != null) {
            service.stopPeriodicUpdate();
        }
        levelServices.remove(deviceID);
    }

    private void convertMatToFile(Mat imageMat) throws IOException {
        // Define the root path where screenshots should be saved
        String rootPath = SystemUtils.getScreenshotFolderPath();

        // Format the current date and time for the filename
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        String dateTime = LocalDateTime.now().format(formatter);
        String filename = "Levelup_" + dateTime + ".png";

        // Create a file in the screenshots folder with the specified filename
        File file = new File(Paths.get(rootPath, filename).toString());

        // Attempt to write the Mat to the file
        boolean result = Imgcodecs.imwrite(file.getAbsolutePath(), imageMat);
        if (!result) {
            throw new IOException("Failed to save levelup image: " + file.getAbsolutePath());
        }
    }
}
