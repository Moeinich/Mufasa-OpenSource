package helpers.emulator;

import helpers.Logger;
import helpers.adb.ADBHandler;
import helpers.utils.GameviewCache;
import helpers.utils.IsScriptRunning;
import org.jetbrains.annotations.NotNull;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static UI.components.EmulatorView.getSelectedEmulator;
import static UI.components.utils.Observables.*;
import static utils.Constants.IS_WINDOWS_USER;

public class EmulatorManager {
    private final Logger logger;
    private final ADBHandler adbHandler;
    private final EmulatorHelper emulatorHelper;
    private final GameviewCache gameviewCache;
    private final IsScriptRunning isScriptRunning;
    private final DirectCapture directCapture;

    private final Map<String, ScheduledFuture<?>> captureTasks = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler;
    private final ExecutorService executorService;

    public EmulatorManager(Logger logger, ADBHandler adbHandler, EmulatorHelper emulatorHelper, GameviewCache gameviewCache, IsScriptRunning isScriptRunning, DirectCapture directCapture) {
        this.logger = logger;
        this.adbHandler = adbHandler;
        this.emulatorHelper = emulatorHelper;
        this.gameviewCache = gameviewCache;
        this.isScriptRunning = isScriptRunning;
        this.directCapture = directCapture;


        ThreadFactory schedulerThreadFactory = new CaptureThreadFactory("CaptureScheduler");
        ThreadFactory executorThreadFactory = new CaptureThreadFactory("CaptureExecutor");

        this.scheduler = Executors.newScheduledThreadPool(2, schedulerThreadFactory);
        this.executorService = Executors.newFixedThreadPool(8, executorThreadFactory);
    }

    public void updateRefreshRateForAll(int newRateMillis) {
        // Update the global refresh rate
        GAME_REFRESHRATE.set(newRateMillis);

        // Stop all existing scheduled tasks and restart them
        for (String device : captureTasks.keySet()) {
            ScheduledFuture<?> existingTask = captureTasks.remove(device);
            if (existingTask != null) {
                existingTask.cancel(false); // Cancel the task but let running tasks finish
            }

            // Restart the capture task for this device
            startCapture(device);
        }
    }

    public void shutdown() {
        scheduler.shutdown();
        executorService.shutdown();
    }

    private void startCapture(String emulator) {
        String selectedDevice = emulator != null ? emulator : getSelectedEmulator();
        if (selectedDevice == null) {
            return; // No selected device, do nothing
        }

        captureTasks.computeIfAbsent(selectedDevice, k -> scheduler.scheduleAtFixedRate(() -> executorService.submit(() -> captureAndStoreScreenshot(selectedDevice)), 0, GAME_REFRESHRATE.get(), TimeUnit.MILLISECONDS));
    }

    private void stopCapture(String emulator) {
        logger.print("Stopping capture on: " + emulator);
        ScheduledFuture<?> captureTask = captureTasks.remove(emulator);
        if (captureTask != null) {
            captureTask.cancel(false);
        }
        gameviewCache.cleanCache(emulator);
    }

    public void stopAllCaptures() {
        logger.print("Stopping all captures");
        for (String emulator : new HashSet<>(captureTasks.keySet())) {
            stopCapture(emulator);
        }
        captureTasks.clear();
        gameviewCache.clearCache();
    }

    public BufferedImage getLatestScreenshot(String device) {
        return gameviewCache.getBuffer(device);
    }

    private void captureAndStoreScreenshot(String device) {
        if (device == null) {
            logger.print("Device is null in captureAndStoreScreenshot.");
            return;
        }

        BufferedImage screenshot;
        if (USE_DIRECT_CAPTURE.get() || USE_PW_CAPTURE.get()) { // Use WIN API
            screenshot = directCapture.takeScreenshot(device);
        } else { // Use ADB
            screenshot = adbHandler.captureScreenshot(device);
        }
        if (screenshot == null || isFallbackImage(screenshot)) {
            handleScreenshotFailure(device);
            return;
        }

        updateLatestScreenshot(device, screenshot);
    }

    private boolean isFallbackImage(BufferedImage image) {
        // Check if the image is completely black with an alpha channel
        if (image.getWidth() == 894 && image.getHeight() == 540 && image.getType() == BufferedImage.TYPE_INT_ARGB) {
            int alpha = image.getRGB(0, 0) >>> 24; // Get the alpha value of the first pixel
            // If the alpha is 0, it's fully transparent
            return alpha == 0;
        }
        return false;
    }

    private void handleScreenshotFailure(String device) {
        if (USE_DIRECT_CAPTURE.get() && !IS_WINDOWS_USER) {
            logger.log("Direct capture does not support macOS", device);
        } else {
            logger.devLog("Failed to capture screenshot for device: " + device);
        }
    }


    private void updateLatestScreenshot(String device, BufferedImage screenshot) {
        if (screenshot != null) {
            gameviewCache.cacheGameview(device, screenshot);
        }
    }

    public void monitorEmulators() {
        Set<String> onlineEmulators = emulatorHelper.getOnlineEmulators();

        for (String emulator : new HashSet<>(captureTasks.keySet())) {
            if (!onlineEmulators.contains(emulator) || (!emulator.equals(getSelectedEmulator()) && !isScriptRunning.isScriptRunningProperty(emulator).get())) {
                stopCapture(emulator);
            }
        }

        for (String emulator : onlineEmulators) {
            if (!captureTasks.containsKey(emulator) && (emulator.equals(getSelectedEmulator()) || isScriptRunning.isScriptRunningProperty(emulator).get())) {
                startCapture(emulator);
            }
        }
    }

    // Custom ThreadFactory for naming threads
    private static class CaptureThreadFactory implements ThreadFactory {
        private final String baseName;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final ThreadGroup group;

        CaptureThreadFactory(String baseName) {
            this.baseName = baseName;
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() :
                    Thread.currentThread().getThreadGroup();
        }

        @Override
        public Thread newThread(@NotNull Runnable r) {
            Thread t = new Thread(group, r,
                    baseName + "-" + threadNumber.getAndIncrement(),
                    0);
            if (t.isDaemon())
                t.setDaemon(false);
            if (t.getPriority() != Thread.MAX_PRIORITY)
                t.setPriority(Thread.MAX_PRIORITY);
            return t;
        }
    }
}