package helpers.services;

import helpers.ThreadManager;
import helpers.services.utils.BreakServiceSettings;
import helpers.services.utils.IHandlerService;

import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class BreakHandlerService implements IHandlerService {
    private final BreakServiceSettings breakServiceSettings;
    private final ScheduledExecutorService executorService = ThreadManager.getInstance().getScheduler();
    private final AtomicBoolean isActive = new AtomicBoolean(true);

    private final ConcurrentHashMap<String, BreakHandlerService> handlers = new ConcurrentHashMap<>();
    private int TARGET_BREAK_LENGTH = 4;
    private int TARGET_RUN_LENGTH = 20;
    private int VARIABILITY_BREAK = 1;
    private int VARIABILITY_RUN = 10;

    private long uniqueIdentifier;
    private int accumulatedBreakTime = 0;
    private final AtomicBoolean isOnBreak = new AtomicBoolean(false);
    private final AtomicBoolean postponeBreak = new AtomicBoolean(false);
    private final AtomicBoolean isTimeForBreak = new AtomicBoolean(false);
    private ScheduledFuture<?> nextBreakFuture;
    private ScheduledFuture<?> endBreakFuture;
    private boolean isEnabled = true;

    public BreakHandlerService(BreakServiceSettings breakServiceSettings) {
        this.breakServiceSettings = breakServiceSettings;
        updateSettings();
        generateUniqueIdentifier();
    }

    public void start() {
        updateSettings();
        generateUniqueIdentifier();
        breakManager();
    }

    public void updateSettings() {
        TARGET_BREAK_LENGTH = breakServiceSettings.TARGET_BREAK_LENGTH;
        TARGET_RUN_LENGTH = breakServiceSettings.TARGET_RUN_LENGTH;
        VARIABILITY_BREAK = breakServiceSettings.VARIABILITY_BREAK;
        VARIABILITY_RUN = breakServiceSettings.VARIABILITY_RUN;
    }

    public boolean shouldCloseAppOnBreak() {
        return breakServiceSettings.shouldCloseOnBreak();
    }

    private void generateUniqueIdentifier() {
        uniqueIdentifier = new Random().nextLong();
    }

    private void breakManager() {
        Random breakRandom = new Random(uniqueIdentifier);
        scheduleBreak(executorService, breakRandom);
    }

    private void scheduleBreak(ScheduledExecutorService executorService, Random random) {
        if (isOnBreak.get() || (nextBreakFuture != null && !nextBreakFuture.isDone())) {
            return;
        }

        int timeBeforeBreak = generateTimeAroundTarget(random, TARGET_RUN_LENGTH, VARIABILITY_RUN);

        // Schedule the start of the break
        nextBreakFuture = executorService.schedule(() -> {
            isTimeForBreak.set(true);

            // Wait until postponeBreak is cleared
            while (postponeBreak.get() && isActive.get()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    isTimeForBreak.set(false);
                    return;
                }
            }

            isTimeForBreak.set(false);
            int breakLength = generateTimeAroundTarget(random, TARGET_BREAK_LENGTH, VARIABILITY_BREAK);
            isOnBreak.set(true); // Break starts here

            // Schedule the end of the break
            endBreakFuture = executorService.schedule(() -> {
                isOnBreak.set(false); // Break ends here
                accumulatedBreakTime += breakLength;
                // Schedule the next break without checking accumulatedBreakTime
                scheduleBreak(executorService, random);

            }, breakLength, TimeUnit.SECONDS); // Schedule the end of the break after breakLength seconds

        }, timeBeforeBreak, TimeUnit.SECONDS); // Schedule the start of the break after timeBeforeBreak seconds
    }

    private int generateTimeAroundTarget(Random random, int target, int variability) {
        // Generate a time that is around the target, allowing for some variability
        return target - variability + random.nextInt(2 * variability);
    }

    public boolean isTimeForBreak() {
        return isOnBreak.get();
    }

    public boolean postponedButTimeForBreak() {
        return isTimeForBreak.get();
    }

    public boolean isPostponed() {
        return postponeBreak.get();
    }

    public long getTimeUntilNextEvent() {
        if (isOnBreak.get()) {
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
    public void postponeBreak() {
        postponeBreak.set(true);
    }

    // Method to allow breaks again
    public void allowBreaks() {
        postponeBreak.set(false);
    }

    // Static method to get or create a BreakHandler instance for a given deviceID
    public BreakHandlerService getHandlerForEmulator(String deviceID) {
        return handlers.computeIfAbsent(deviceID, k -> {
            synchronized (handlers) {
                if (!handlers.containsKey(deviceID)) {
                    return new BreakHandlerService(breakServiceSettings);
                }
                return handlers.get(deviceID);
            }
        });
    }

    public void forceBreak(String deviceID, long durationMs) {
        System.out.println("Forcing break for: " + deviceID);
        // Retrieve or create the handler for the given deviceID
        BreakHandlerService handler = getHandlerForEmulator(deviceID);
        handler.disable(); // Ensure no active scheduled breaks

        // Force the break
        handler.isEnabled = true;
        handler.isOnBreak.set(true); // Set the handler to "on break"

        // Schedule the forced break as the next break
        handler.nextBreakFuture = handler.executorService.schedule(() -> {
            handler.endBreakFuture = handler.executorService.schedule(() -> {
                System.out.println("forceBreak is done for: " + deviceID);
                handler.isOnBreak.set(false); // End the break
                handler.accumulatedBreakTime += (int) TimeUnit.MILLISECONDS.toSeconds(durationMs);

                // Optionally, remove the service if no further breaks are needed
                handler.isEnabled = false;
                removeService(deviceID);
            }, durationMs, TimeUnit.MILLISECONDS);
        }, 0, TimeUnit.MILLISECONDS); // Force the break immediately
    }

    // Method to specifically retrieve an existing BreakHandler instance for a given deviceID
    public BreakHandlerService getExistingHandler(String deviceID) {
        return handlers.get(deviceID);
    }

    public void removeService(String deviceID) {
        BreakHandlerService handler = handlers.get(deviceID);
        if (handler != null) {
            handler.isActive.set(false); // To make sure we exit the while loop for postponed breaks
            handler.disable(); // Disable the handler, canceling any scheduled breaks
            handlers.remove(deviceID); // Remove the handler from the map
        }
    }

    @Override
    public boolean isActiveNow() {
        return isOnBreak.get();
    }
}

