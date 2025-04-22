package helpers.services;

import helpers.Logger;
import helpers.ThreadManager;
import helpers.services.utils.IHandlerService;
import helpers.services.utils.SleepServiceSettings;

import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class SleepHandlerService implements IHandlerService {
    private final SleepServiceSettings sleepHandlerSettings;
    private final Logger logger;
    private final ScheduledExecutorService executorService = ThreadManager.getInstance().getScheduler();
    private final AtomicBoolean isActive = new AtomicBoolean(true);

    private final ConcurrentHashMap<String, SleepHandlerService> handlers = new ConcurrentHashMap<>();
    private int TARGET_SLEEP_LENGTH = 4;
    private int TARGET_RUN_LENGTH = 20;
    private int VARIABILITY_SLEEP = 1;
    private int VARIABILITY_RUN = 10;

    private long uniqueIdentifier;
    private int accumulatedBreakTime = 0;
    private final AtomicBoolean isSleeping = new AtomicBoolean(false);
    private final AtomicBoolean postponeSleep = new AtomicBoolean(false);
    private final AtomicBoolean isTimeForSleep = new AtomicBoolean(false);
    private ScheduledFuture<?> nextBreakFuture;
    private ScheduledFuture<?> endBreakFuture;
    private boolean isEnabled = true;

    private String deviceName = "none";

    public SleepHandlerService(SleepServiceSettings sleepHandlerSettings, Logger logger) {
        this.sleepHandlerSettings = sleepHandlerSettings;
        this.logger = logger;
        updateSettings();
        generateUniqueIdentifier();
        this.isEnabled = sleepHandlerSettings.isEnabled;
    }

    public void start() {
        updateSettings();
        generateUniqueIdentifier();

        if (isEnabled) {
            sleepManager();
        }
    }

    public void setDeviceName(String device) {
        this.deviceName = device;
    }

    public void updateSettings() {
        TARGET_SLEEP_LENGTH = sleepHandlerSettings.TARGET_SLEEP_LENGTH;
        TARGET_RUN_LENGTH = sleepHandlerSettings.TARGET_RUN_LENGTH;
        VARIABILITY_SLEEP = sleepHandlerSettings.VARIABILITY_SLEEP;
        VARIABILITY_RUN = sleepHandlerSettings.VARIABILITY_RUN;
    }

    private void generateUniqueIdentifier() {
        uniqueIdentifier = new Random().nextLong();
    }

    private void sleepManager() {
        Random breakRandom = new Random(uniqueIdentifier);
        scheduleSleep(executorService, breakRandom);
    }

    private void scheduleSleep(ScheduledExecutorService executorService, Random random) {
        if (isSleeping.get() || (nextBreakFuture != null && !nextBreakFuture.isDone())) {
            return;
        }

        int timeBeforeBreak = generateTimeAroundTarget(random, TARGET_RUN_LENGTH, VARIABILITY_RUN);
        logger.log("We will sleep next in: " + prettyPrintTimeInMinutes(timeBeforeBreak), deviceName);

        // Schedule the start of the break
        nextBreakFuture = executorService.schedule(() -> {
            isTimeForSleep.set(true);

            // Wait until postponeBreak is cleared
            while (postponeSleep.get() && isActive.get()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    isTimeForSleep.set(false);
                    return;
                }
            }

            isTimeForSleep.set(false);
            int sleepLenght = generateTimeAroundTarget(random, TARGET_SLEEP_LENGTH, VARIABILITY_SLEEP);
            logger.log("Starting to sleep for: " + prettyPrintTimeInMinutes(sleepLenght), deviceName);
            isSleeping.set(true); // Sleep starts here

            // Schedule the end of the break
            endBreakFuture = executorService.schedule(() -> {
                isSleeping.set(false); // Break ends here
                accumulatedBreakTime += sleepLenght;
                // Schedule the next break without checking accumulatedBreakTime
                scheduleSleep(executorService, random);

            }, sleepLenght, TimeUnit.SECONDS); // Schedule the end of the break after sleepLenght seconds

        }, timeBeforeBreak, TimeUnit.SECONDS); // Schedule the start of the break after timeBeforeBreak seconds
    }

    private int generateTimeAroundTarget(Random random, int target, int variability) {
        // Generate a time that is around the target, allowing for some variability
        return target - variability + random.nextInt(2 * variability);
    }

    public boolean isTimeForSleep() {
        return isSleeping.get();
    }

    public boolean postponedButTimeForBreak() {
        return isTimeForSleep.get();
    }

    public boolean isPostponed() {
        return postponeSleep.get();
    }

    public long getTimeUntilNextEvent() {
        if (isSleeping.get()) {
            return endBreakFuture != null ? endBreakFuture.getDelay(TimeUnit.SECONDS) : 0;
        } else {
            return nextBreakFuture != null ? nextBreakFuture.getDelay(TimeUnit.SECONDS) : 0;
        }
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void enable() {
        this.isEnabled = true;
    }

    public void disable() {
        this.isEnabled = false;
        if (nextBreakFuture != null && !nextBreakFuture.isDone()) {
            nextBreakFuture.cancel(true);  // Cancel the scheduled break
        }
    }

    // Method to postpone breaks
    public void postponeSleeps() {
        postponeSleep.set(true);
    }

    // Method to allow breaks again
    public void allowSleeps() {
        postponeSleep.set(false);
    }

    // Static method to get or create a BreakHandler instance for a given deviceID
    public SleepHandlerService getHandlerForEmulator(String deviceID) {
        return handlers.computeIfAbsent(deviceID, k -> {
            synchronized (handlers) {
                if (!handlers.containsKey(deviceID)) {
                    return new SleepHandlerService(sleepHandlerSettings, logger);
                }
                return handlers.get(deviceID);
            }
        });
    }

    public void forceSleep(String deviceID, long durationMs) {
        System.out.println("Forcing break for: " + deviceID);
        // Retrieve or create the handler for the given deviceID
        SleepHandlerService handler = getHandlerForEmulator(deviceID);
        handler.disable(); // Ensure no active scheduled breaks

        // Force the break
        handler.isEnabled = true;
        handler.isSleeping.set(true); // Set the handler to "on break"

        // Schedule the forced break as the next break
        handler.nextBreakFuture = handler.executorService.schedule(() -> {
            handler.endBreakFuture = handler.executorService.schedule(() -> {
                System.out.println("forceBreak is done for: " + deviceID);
                handler.isSleeping.set(false); // End the break
                handler.accumulatedBreakTime += (int) TimeUnit.MILLISECONDS.toSeconds(durationMs);

                // Optionally, remove the service if no further breaks are needed
                handler.isEnabled = false;
                removeService(deviceID);
            }, durationMs, TimeUnit.MILLISECONDS);
        }, 0, TimeUnit.MILLISECONDS); // Force the break immediately
    }

    // Method to specifically retrieve an existing BreakHandler instance for a given deviceID
    public SleepHandlerService getExistingHandler(String deviceID) {
        return handlers.get(deviceID);
    }

    public void removeService(String deviceID) {
        SleepHandlerService handler = handlers.get(deviceID);
        if (handler != null) {
            handler.isActive.set(false); // To make sure we exit the while loop for postponed breaks
            handler.disable(); // Disable the handler, canceling any scheduled breaks
            handlers.remove(deviceID); // Remove the handler from the map
        }
    }

    private String prettyPrintTimeInMinutes(long totalSeconds) {
        long hours = totalSeconds / 3600;
        long remainingSeconds = totalSeconds % 3600;
        long minutes = remainingSeconds / 60;

        if (hours > 0 && minutes > 0) {
            return hours + " hours " + minutes + " minutes";
        } else if (hours > 0) {
            return hours + " hours";
        } else if (minutes > 0) {
            return minutes + " minutes";
        } else {
            return "less than a minute";
        }
    }

    @Override
    public boolean isActiveNow() {
        return isSleeping.get();
    }
}
