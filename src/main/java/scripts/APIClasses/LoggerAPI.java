package scripts.APIClasses;

import helpers.Logger;
import interfaces.iLogger;
import scripts.ScriptInfo;

public class LoggerAPI implements iLogger {
    private final Logger logger;
    private final ScriptInfo scriptInfo;

    public LoggerAPI(Logger logger, ScriptInfo scriptInfo) {
        this.logger = logger;
        this.scriptInfo = scriptInfo;
    }

    public void log(String logMessage) {
        logger.log(logMessage, scriptInfo.getCurrentEmulatorId());
    }

    public void debugLog(String logMessage) {
        logger.debugLog(logMessage, scriptInfo.getCurrentEmulatorId());
    }
}