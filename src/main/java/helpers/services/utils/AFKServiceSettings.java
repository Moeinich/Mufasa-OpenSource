package helpers.services.utils;

import java.util.concurrent.atomic.AtomicBoolean;

public class AFKServiceSettings {
    public final AtomicBoolean isEnabled = new AtomicBoolean(false);
    public int TARGET_AFK_LENGTH = 4;
    public int TARGET_AFK_RUN_LENGTH = 20;
    public int VARIABILITY_AFK = 1;
    public int VARIABILITY_AFK_RUN = 10;

    public void setIsEnabled(boolean enabledState) {
        isEnabled.set(enabledState);
    }

    public void updateSettings(int afkLenght, int runLength, int variabilityAfk, int variabilityRun) {
        TARGET_AFK_LENGTH = afkLenght * 60;       // Convert to minutes
        TARGET_AFK_RUN_LENGTH = runLength * 60;           // Convert to minutes
        VARIABILITY_AFK = variabilityAfk * 60;    // Convert to minutes
        VARIABILITY_AFK_RUN = variabilityRun * 60;        // Convert to minutes
    }
}