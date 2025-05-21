package scripts.APIClasses;

import helpers.scripts.CancellationToken;
import interfaces.iCondition;
import scripts.ScriptInfo;

import java.util.Random;
import java.util.concurrent.Callable;

public class ConditionAPI implements iCondition {
    private final ScriptInfo scriptInfo;
    private static final Random random = new Random();

    public ConditionAPI(ScriptInfo scriptInfo) {
        this.scriptInfo = scriptInfo;
    }

    public void sleep(int intervalMillis) {
        int slice = 100; // Duration of each sleep slice in milliseconds
        int elapsed = 0; // Track elapsed time

        String emulatorId = scriptInfo.getCurrentEmulatorId();
        if (emulatorId == null) {
            return; // Handle the case where emulatorId is null
        }
        CancellationToken cancellationToken = scriptInfo.getCancellationToken(emulatorId);
        if (cancellationToken == null) {
            return;
        }

        while (elapsed < intervalMillis) {
            if (cancellationToken.isCancellationRequested()) {
                break; // Exit early if cancellation has been requested
            }
            try {
                Thread.sleep(Math.min(slice, intervalMillis - elapsed));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Properly handle InterruptedException by restoring the interrupted status
                break;
            }
            elapsed += slice;
        }
    }

    public void sleep(int minMillis, int maxMillis) {
        if (minMillis > maxMillis) {
            int temp = minMillis;
            minMillis = maxMillis;
            maxMillis = temp;
        }
        int randomSleepTime = minMillis + random.nextInt(maxMillis - minMillis + 1);
        sleep(randomSleepTime);
    }

    public void wait(Callable<Boolean> conditionCallable, int intervalMillis, int attempts) {
        CancellationToken cancellationToken = scriptInfo.getCancellationToken(scriptInfo.getCurrentEmulatorId());
        if (cancellationToken == null) {
            return;
        }

        for (int i = 0; i < attempts; i++) {
            if (cancellationToken.isCancellationRequested()) {
                break; // Exit early if cancellation has been requested
            }
            try {
                if (conditionCallable.call()) {
                    break; // Exit if the condition is met
                }
                sleep(intervalMillis); // Use the adapted sleep method
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Restore the interrupted status
                System.err.println("Interrupted while waiting: " + e.getMessage());
                break;
            } catch (Exception e) {
                System.err.println("Exception in callable or during sleep: " + e.getMessage());
                e.printStackTrace();
                break;
            }
        }
    }

    public boolean waitWithReturn(Callable<Boolean> conditionCallable, int intervalMillis, int attempts) {
        CancellationToken cancellationToken = scriptInfo.getCancellationToken(scriptInfo.getCurrentEmulatorId());
        if (cancellationToken == null) {
            System.err.println("Cancellation token not available.");
            return false; // Early return if no cancellation token is found
        }

        boolean conditionMet = false;
        for (int i = 0; i < attempts; i++) {
            if (cancellationToken.isCancellationRequested()) {
                System.err.println("Operation cancelled.");
                break; // Exit early if cancellation has been requested
            }
            try {
                if (conditionCallable.call()) {
                    conditionMet = true;
                    break; // Exit if the condition is met
                }
                sleep(intervalMillis); // Use the adapted sleep method
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Restore the interrupted status
                System.err.println("Interrupted while waiting: " + e.getMessage());
                break;
            } catch (Exception e) {
                System.err.println("Exception in callable or during sleep: " + e.getMessage());
                break;
            }
        }
        return conditionMet; // Return true if condition was met, false otherwise
    }
}