package helpers.services.utils;

public class SleepServiceSettings {
    public int TARGET_SLEEP_LENGTH = 180;
    public int TARGET_RUN_LENGTH = 600;
    public int VARIABILITY_SLEEP = 60;
    public int VARIABILITY_RUN = 120;
    public boolean isEnabled = false;

    public void updateSettings(int sleepLength, int runLength, int variabilitySleep, int variabilityRun) {
        TARGET_SLEEP_LENGTH = sleepLength * 60;       // Convert to minutes
        TARGET_RUN_LENGTH = runLength * 60;           // Convert to minutes
        VARIABILITY_SLEEP = variabilitySleep * 60;    // Convert to minutes
        VARIABILITY_RUN = variabilityRun * 60;        // Convert to minutes
    }

    public void setIsEnabled(boolean state) {
        isEnabled = state;
    }
}