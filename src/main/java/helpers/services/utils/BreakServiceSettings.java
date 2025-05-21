package helpers.services.utils;

public class BreakServiceSettings {
    public int TARGET_BREAK_LENGTH = 4;
    public int TARGET_RUN_LENGTH = 20;
    public int VARIABILITY_BREAK = 1;
    public int VARIABILITY_RUN = 10;
    private boolean closeOnBreak = false;

    public void updateSettings(int breakLength, int runLength, int variabilityBreak, int variabilityRun) {
        TARGET_BREAK_LENGTH = breakLength * 60;       // Convert to minutes
        TARGET_RUN_LENGTH = runLength * 60;           // Convert to minutes
        VARIABILITY_BREAK = variabilityBreak * 60;    // Convert to minutes
        VARIABILITY_RUN = variabilityRun * 60;        // Convert to minutes
    }

    public void setCloseOnBreak(boolean state) {
        closeOnBreak = state;
    }

    public boolean shouldCloseOnBreak() {
        return closeOnBreak;
    }
}
