package helpers.services;

import helpers.ThreadManager;
import helpers.services.utils.AFKServiceSettings;

import java.util.Random;
import java.util.concurrent.*;

public class AFKHandlerService {
    private final AFKServiceSettings AFKServiceSettings;
    private final ScheduledExecutorService executorService = ThreadManager.getInstance().getScheduler();

    private final ConcurrentHashMap<String, AFKHandlerService> handlers = new ConcurrentHashMap<>();
    private long uniqueIdentifier;
    private int TARGET_AFK_LENGTH = 4;
    private int TARGET_AFK_RUN_LENGTH = 20;
    private int VARIABILITY_AFK = 1;
    private int VARIABILITY_AFK_RUN = 10;
    private int accumulatedAFKTime = 0;
    private boolean isAFKing = false;
    private ScheduledFuture<?> nextAFKFuture;
    private ScheduledFuture<?> endAFKFuture;
    private boolean isEnabled = true;

    public AFKHandlerService(AFKServiceSettings AFKServiceSettings) {
        this.AFKServiceSettings = AFKServiceSettings;
        generateUniqueIdentifier();
    }

    public void start() {
        if (isEnabled) {
            updateSettings();
            generateUniqueIdentifier();
            AFKManager();
        }
    }

    public void updateSettings() {
        TARGET_AFK_LENGTH = AFKServiceSettings.TARGET_AFK_LENGTH;
        TARGET_AFK_RUN_LENGTH = AFKServiceSettings.TARGET_AFK_RUN_LENGTH;
        VARIABILITY_AFK = AFKServiceSettings.VARIABILITY_AFK;
        VARIABILITY_AFK_RUN = AFKServiceSettings.VARIABILITY_AFK_RUN;
    }

    private void generateUniqueIdentifier() {
        uniqueIdentifier = new Random().nextLong();
    }

    private void AFKManager() {
        Random afkRandom = new Random(uniqueIdentifier);
        scheduleAFK(executorService, afkRandom);
    }

    private void scheduleAFK(ScheduledExecutorService executorService, Random random) {
        int timeBeforeBreak = generateTimeAroundTarget(random, TARGET_AFK_RUN_LENGTH, VARIABILITY_AFK_RUN);

        // Schedule the start of the break
        nextAFKFuture = executorService.schedule(() -> {
            int AFKLength = generateTimeAroundTarget(random, TARGET_AFK_LENGTH, VARIABILITY_AFK);
            isAFKing = true; // Break starts here

            // Schedule the end of the break
            endAFKFuture = executorService.schedule(() -> {
                isAFKing = false; // Break ends here
                accumulatedAFKTime += AFKLength;
                // Schedule the next break without checking accumulatedBreakTime
                scheduleAFK(executorService, random);

            }, AFKLength, TimeUnit.SECONDS); // Schedule the end of the break after breakLength seconds

        }, timeBeforeBreak, TimeUnit.SECONDS); // Schedule the start of the break after timeBeforeBreak seconds
    }

    private int generateTimeAroundTarget(Random random, int target, int variability) {
        // Generate a time that is around the target, allowing for some variability
        return target - variability + random.nextInt(2 * variability);
    }

    public boolean isTimeForAFK() {
        return isAFKing;
    }

    public long getTimeUntilNextEvent() {
        if (isAFKing) {
            return endAFKFuture != null ? endAFKFuture.getDelay(TimeUnit.SECONDS) : 0;
        } else {
            return nextAFKFuture != null ? nextAFKFuture.getDelay(TimeUnit.SECONDS) : 0;
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
        if (nextAFKFuture != null && !nextAFKFuture.isDone()) {
            nextAFKFuture.cancel(true); // Cancel the scheduled AFK
        }
    }

    // Static method to get or create a BreakHandler instance for a given deviceID
    public AFKHandlerService getHandlerForEmulator(String deviceID) {
        return handlers.computeIfAbsent(deviceID, k -> new AFKHandlerService(AFKServiceSettings));
    }

    // Method to specifically retrieve an existing BreakHandler instance for a given deviceID
    public AFKHandlerService getExistingHandler(String deviceID) {
        return handlers.get(deviceID);
    }

    public void removeService(String deviceID) {
        AFKHandlerService handler = handlers.get(deviceID);
        if (handler != null) {
            handlers.remove(deviceID); // Remove the handler from the map
        }
    }

    // A method to return if AFKs are even enabled in the BreakUI
    public boolean isAFKHandlerEnabled() {
        return AFKServiceSettings.isEnabled.get();
    }
}
