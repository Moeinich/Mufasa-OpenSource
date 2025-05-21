package scripts;

import helpers.Logger;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;


public class ScriptThreadFactory implements ThreadFactory{
    private final String deviceID;
    private final ScriptInfo scriptInfo;
    private final Logger logger;
    private final ThreadFactory defaultFactory = Executors.defaultThreadFactory();

    public ScriptThreadFactory(String deviceID, ScriptInfo scriptInfo, Logger logger) {
        this.deviceID = deviceID;
        this.scriptInfo = scriptInfo;
        this.logger = logger;
    }

    @Override
    public Thread newThread(Runnable r) {
        Runnable wrappedRunnable = () -> {
            scriptInfo.setCurrentEmulatorId(deviceID);
            try {
                r.run();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
        Thread thread = defaultFactory.newThread(wrappedRunnable);
        thread.setName("scriptThread-" + deviceID);
        return thread;
    }
}