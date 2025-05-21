package scripts;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class ScriptRunnable implements ThreadFactory {
    private final String deviceID;
    private final ScriptInfo scriptInfo;
    private final ThreadFactory defaultFactory = Executors.defaultThreadFactory();

    public ScriptRunnable(String deviceID, ScriptInfo scriptInfo) {
        this.deviceID = deviceID;
        this.scriptInfo = scriptInfo;
    }

    @Override
    public Thread newThread(Runnable r) {
        return defaultFactory.newThread(wrapRunnableWithDeviceContext(r));
    }

    // Method to wrap Runnable without creating a new thread, for use with scheduler
    public Runnable wrapRunnableWithDeviceContext(Runnable r) {
        return () -> {
            scriptInfo.setCurrentEmulatorId(deviceID);
            try {
                r.run();
            } finally {
                scriptInfo.setCurrentEmulatorId(null);
            }
        };
    }
}
