package scripts;

import helpers.AbstractScript;
import helpers.annotations.ScriptManifest;
import helpers.scripts.CancellationToken;
import helpers.scripts.utils.Script;

import java.util.concurrent.*;

public class ScriptInfo {
    private final ThreadLocal<String> currentEmulatorId = ThreadLocal.withInitial(() -> null);
    private final ConcurrentMap<String, Script> currentScripts = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, AbstractScript> currentAbstractScripts = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, ScriptManifest> currentScriptManifests = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CancellationToken> cancellationTokens = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, ScheduledExecutorService> scriptSchedulers = new ConcurrentHashMap<>();

    // Getters
    public String getCurrentEmulatorId() {
        String emulatorId = currentEmulatorId.get();
        return emulatorId != null ? emulatorId : "none";
    }

    public void setScriptScheduler(String device, ScheduledExecutorService scheduler) {
        scriptSchedulers.put(device, scheduler);
    }

    public ScheduledExecutorService getScriptScheduler(String device) {
        return scriptSchedulers.get(device);
    }

    public void removeScriptScheduler(String device) {
        ScheduledExecutorService scheduler = scriptSchedulers.get(device);
        if (scheduler != null) {
            scheduler.shutdownNow();
            try {
                // Wait for tasks to complete for a specified time (e.g., 1 second)
                if (!scheduler.awaitTermination(1, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow(); // Force shutdown if tasks are not completed in time
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow(); // Interrupt the shutdown and force stop
                Thread.currentThread().interrupt(); // Preserve the interrupt status
            }
            scriptSchedulers.remove(device); // Remove from the map after shutdown
        }
    }

    public void setCurrentEmulatorId(String device) {
        currentEmulatorId.set(device);
    }

    public Script getCurrentScript(String deviceId) {
        return currentScripts.get(deviceId);
    }

    public AbstractScript getCurrentAbstractScript(String deviceId) {
        return currentAbstractScripts.get(deviceId);
    }

    public ScriptManifest getScriptManifest(String deviceId) {
        return currentScriptManifests.getOrDefault(deviceId, null);
    }

    public CancellationToken getCancellationToken(String deviceId) {
        if (deviceId == null) {
            return CancellationToken.getDefaultCancelledToken();
        }

        return cancellationTokens.getOrDefault(deviceId, CancellationToken.getDefaultCancelledToken());
    }

    // Setters
    public void setCurrentScript(String deviceId, Script script) {
        currentScripts.putIfAbsent(deviceId, script);
    }

    public void setCurrentAbstractScript(String deviceId, AbstractScript abstractScript) {
        currentAbstractScripts.putIfAbsent(deviceId, abstractScript);
    }

    public void setScriptManifest(String deviceId, ScriptManifest scriptManifest) {
        currentScriptManifests.putIfAbsent(deviceId, scriptManifest);
    }

    public void setCancellationToken(String deviceId, CancellationToken cancellationToken) {
        cancellationTokens.putIfAbsent(deviceId, cancellationToken);
    }

    // Cleanup
    public void cleanScriptStatics(String deviceID) {
        currentAbstractScripts.remove(deviceID);
        currentScripts.remove(deviceID);
        currentEmulatorId.remove();
        currentScriptManifests.remove(deviceID);
        cancellationTokens.remove(deviceID);
    }
}
