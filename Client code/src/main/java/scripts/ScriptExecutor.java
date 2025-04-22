package scripts;

import helpers.AbstractScript;
import helpers.CacheManager;
import helpers.DiscordWebhook;
import helpers.Logger;
import helpers.scripts.CancellationToken;
import helpers.scripts.ScriptCleanup;
import helpers.scripts.utils.Script;
import helpers.services.*;
import helpers.utils.IsScriptRunning;
import helpers.utils.TabState;
import interfaces.iScript;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import osr.mapping.Game;
import osr.mapping.Login;
import utils.CredentialsManager;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;

public class ScriptExecutor implements iScript {
    public final BooleanProperty paused = new SimpleBooleanProperty(false);
    private final CacheManager cacheManager;
    private final Logger logger;
    private final ScriptAPIHandler scriptAPIHandler;
    private final ScriptCleanup scriptCleanup;
    private final ScriptAccountManager scriptAccountManager;
    private final ScriptInitializer scriptInitializer;
    private final ScriptInfo scriptInfo;
    private final Login login;
    private final Game game;
    private final IsScriptRunning isScriptRunning;
    private final RuntimeService runtimeService;
    private final BreakHandlerService breakHandlerService;
    private final AFKHandlerService afkHandlerService;
    private final XPService xpService;
    private final LeveledupService leveledupService;
    private final CredentialsManager credMgr;
    private final DiscordWebhook discordWebhook;
    private final SleepHandlerService sleepHandlerService;
    public ConcurrentHashMap<String, Thread> emulatorScriptThreads = new ConcurrentHashMap<>();

    public ScriptExecutor(CacheManager cacheManager, Logger logger, ScriptAPIHandler scriptAPIHandler, ScriptCleanup scriptCleanup, ScriptAccountManager scriptAccountManager, ScriptInitializer scriptInitializer, ScriptInfo scriptInfo, Login login, IsScriptRunning isScriptRunning, RuntimeService runtimeService, BreakHandlerService breakHandlerService, SleepHandlerService sleepHandlerService, AFKHandlerService afkHandlerService, XPService xpService, LeveledupService leveledupService, CredentialsManager credMgr, DiscordWebhook discordWebhook, Game game) {
        this.cacheManager = cacheManager;
        this.logger = logger;
        this.scriptAPIHandler = scriptAPIHandler;
        this.scriptCleanup = scriptCleanup;
        this.scriptAccountManager = scriptAccountManager;
        this.scriptInitializer = scriptInitializer;
        this.scriptInfo = scriptInfo;
        this.isScriptRunning = isScriptRunning;
        this.runtimeService = runtimeService;
        this.breakHandlerService = breakHandlerService;
        this.afkHandlerService = afkHandlerService;
        this.sleepHandlerService = sleepHandlerService;
        this.xpService = xpService;
        this.leveledupService = leveledupService;
        this.credMgr = credMgr;
        this.discordWebhook = discordWebhook;

        //Need these for specific client methods
        this.login = login;
        this.game = game;
    }

    public void startLocalScript(String deviceID, String scriptPath) {
        try {
            Script scriptObject = scriptInitializer.createScriptObjectFromFile(scriptPath);
            scriptStartup(deviceID, scriptObject, scriptPath);
        } catch (IllegalArgumentException e) {
            logger.log("Script could not be found: " + scriptPath, deviceID);
            logger.debugLog(e.getMessage(), deviceID);
        }
    }

    private void scriptStartup(String deviceID, Script scriptObject, String scriptIdentifier) {
        try {
            iScript iScript = this;
            CancellationToken cancellationToken = new CancellationToken(); // Create a new cancellation token for this script instance
            scriptInfo.setCancellationToken(deviceID, cancellationToken); // Store the token associated with this deviceID/script

            AbstractScript abstractScript = scriptInitializer.createAndInitializeScript(scriptObject, iScript, deviceID);

            if (abstractScript != null) {
                logger.log("Starting script: " + scriptIdentifier, deviceID);
                scriptInfo.setCurrentScript(deviceID, scriptObject);
                scriptInfo.setCurrentAbstractScript(deviceID, abstractScript);
                startScriptThread(abstractScript, deviceID);

                isScriptRunning.isScriptRunningProperty(deviceID).set(true);
            }
        } catch (IllegalArgumentException e) {
            logger.log("Script could not be found: " + scriptIdentifier, deviceID);
            logger.debugLog(e.getMessage(), deviceID);
        }
    }

    public void startScriptThread(AbstractScript script, String deviceID) {
        runtimeService.getHandler(deviceID);

        Thread scriptThread = new Thread(() -> runScript(script, deviceID));
        scriptThread.setName("ScriptThread-" + deviceID);
        emulatorScriptThreads.putIfAbsent(deviceID, scriptThread);
        scriptThread.start();
    }

    private void runScript(AbstractScript script, String deviceID) {
        scriptInfo.setCurrentEmulatorId(deviceID);

        if (credMgr.isBreaksEnabled(scriptAccountManager.getSelectedAccount(deviceID))) {
            breakHandlerService.getHandlerForEmulator(deviceID).enable();
        } else {
            breakHandlerService.getHandlerForEmulator(deviceID).disable();
        }

        leveledupService.getHandlerForEmulator(deviceID).start(deviceID);

        // AFKHandler Setup
        AFKHandlerService afkService = afkHandlerService.getHandlerForEmulator(deviceID);
        if (!afkService.isAFKHandlerEnabled()) {
            afkService.disable();
        } else {
            afkService.enable();
            afkService.start();
        }

        // Webhook Setup
        if (credMgr.getWebhookURL(scriptAccountManager.getSelectedAccount(deviceID)) != null) {
            discordWebhook.sendStartMessage(deviceID);
            discordWebhook.startWebhookSchedule(deviceID);
        }

        // Start the script execution once
        if (!scriptStartup(script, deviceID)) {
            return; // Exit if the script couldn't start
        }

        // Initialize Scheduler for Repetitive Execution
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(
                new ScriptThreadFactory(deviceID, scriptInfo, logger)
        );

        scheduler.scheduleAtFixedRate(() -> {
            try {
                if (isCancellationRequested(deviceID) || Thread.currentThread().isInterrupted()) {
                    logger.print("Cancellation requested or thread interrupted, stopping scheduler on: " + deviceID);
                    stopScriptOnEmulator(deviceID);
                    return;
                }

                handleScriptExecution(script, deviceID);
            }  catch (Exception e) {
                logger.errorLog("Exception occurred in client");
                logger.errorLog(e.getMessage());
                stopScriptOnEmulator(deviceID);
            } catch (Throwable t) {
                logger.errorLog("Exception occurred in client");
                logger.errorLog(t.getMessage());

                // Console part of it
                logger.log("Exception in script executor: " + t.getMessage(), deviceID);
                t.printStackTrace();
                stopScriptOnEmulator(deviceID);
            }
        }, 0, 200, TimeUnit.MILLISECONDS);

        // Store the scheduler for later shutdown
        scriptInfo.setScriptScheduler(deviceID, scheduler);
    }

    private boolean scriptStartup(AbstractScript script, String deviceID) {
        try {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException("Thread was interrupted before script startup.");
            }


            if (!scriptInfo.getScriptManifest(deviceID).skipClientSetup()) {
                login.preSetup(deviceID, scriptInfo.getScriptManifest(deviceID).skipZoomSetup());
            } else {
                if (!login.isLoggedIn(deviceID)) {
                    login.login(deviceID);
                }
            }

            script.onStart();
            return true;
        } catch (InterruptedException e) {
            logger.print("Script startup was interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            logger.print("Client error, check the logs: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private void handleScriptExecution(AbstractScript script, String deviceID) {
        // Ensure OSRS app is in foreground and user is logged in before executing the script
        if (!login.isOSRSInForeground(deviceID)) {
            login.openOSRSApp(deviceID);
            // Run the pre-setup instead of regular login if OSRS app was closed.
            if (!scriptInfo.getScriptManifest(deviceID).skipClientSetup()) {
                login.preSetup(deviceID, scriptInfo.getScriptManifest(deviceID).skipZoomSetup());
            } else {
                login.login(deviceID);
            }
            return;
        }

        if (!login.isLoggedIn(deviceID)) {
            logger.print("we are not logged in on: " + deviceID);
            scriptAPIHandler.loginAPI().login();
        } else {
            executeScriptPolling(script, deviceID);
        }
    }

    private boolean shouldExecuteScript(String deviceID, AbstractScript script) {
        return !isTimeForBreak(deviceID) && !isCancellationRequested(deviceID) && !isTimeForAFK(deviceID) && !script.isPaused();
    }

    private void executeScriptPolling(AbstractScript script, String deviceID) {
        try {
            script.run();
        } catch (NullPointerException e) {
            logger.debugLog("NullPointerException during script.poll(): " + e.getMessage(), deviceID);
            e.printStackTrace();
        }
    }

    public void sendPauseWebhook(String webhookURL, String deviceID) {
        if (webhookURL != null) {
            discordWebhook.pauseWebhookSchedule(deviceID);
            discordWebhook.sendStartBreakMessage(deviceID);
        }
    }

    private void sendResumeWebhook(String webhookURL, String deviceID) {
        if (webhookURL != null && !isCancellationRequested(deviceID)) {
            discordWebhook.resumeWebhookSchedule(deviceID);
            discordWebhook.sendResumeBreakMessage(deviceID);
        }
    }

    private void handleBreakLogic(String deviceID, AbstractScript script) {
        String webhookURL = credMgr.getWebhookURL(scriptAccountManager.getSelectedAccount(deviceID));

        if (shouldExecuteScript(deviceID, script)) {
            script.setPaused(false);
            return;
        }

        if (isTimeForAFK(deviceID)) {
            logger.log("Going AFK!", deviceID);
            sendPauseWebhook(webhookURL, deviceID);
            waitForBreakToEnd(deviceID);
            sendResumeWebhook(webhookURL, deviceID);
            logger.log("Resuming from AFK!", deviceID);
            scriptInfo.getCurrentAbstractScript(deviceID).setPaused(false);
        } else if (isTimeForBreak(deviceID)) {
            sendPauseWebhook(webhookURL, deviceID);
            logger.log("Taking a break!", deviceID);
            login.logout(deviceID, true);
            waitForBreakToEnd(deviceID);
            sendResumeWebhook(webhookURL, deviceID);
            resumeFromBreak(deviceID);
        }
    }

    private void waitForBreakToEnd(String deviceID) {
        scriptInfo.getCurrentAbstractScript(deviceID).setPaused(true); // Pause the script execution
        BreakHandlerService breakHandler = breakHandlerService.getHandlerForEmulator(deviceID);

        while (isWaitingForBreakOrAFKToEnd(deviceID)) {
            try {
                if (!breakHandler.shouldCloseAppOnBreak()) { // ONLY run the worldhop while on break when shouldCloseApp is false!
                    if (!login.isLoggedIn(deviceID)) { // Only try and switch world if we are not logged in (are on a break instead of AFK)
                        game.hopDuringBreak(deviceID);
                    }
                }
                Thread.sleep(1000); // Short sleep to yield control and reduce CPU usage
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Restore interrupt status.
                logger.print("waitForBreakToEnd interrupted for device: " + deviceID);
                break;
            }
        }
    }

    private void resumeFromBreak(String deviceID) {
        if (!isCancellationRequested(deviceID)) {
            logger.log("Resuming from break!", deviceID);
            login.login(deviceID);
            scriptInfo.getCurrentAbstractScript(deviceID).setPaused(false); //Resume the script state

            TabState tabState = cacheManager.getTabState(deviceID);
            if (tabState != null) {
                logger.devLog("Tabstate cache:" + " gametab state: " + tabState.isInventoryTabOpen() + " magic state: " + tabState.isMagicTabOpen());
                if (tabState.isInventoryTabOpen()) {
                    scriptAPIHandler.gameTabsAPI().openInventoryTab();
                    logger.devLog("Opening the inventory tab as the tab was open before taking a break!");
                } else if (tabState.isMagicTabOpen()) {
                    scriptAPIHandler.gameTabsAPI().openMagicTab();
                    logger.devLog("Opening the magic tab as the tab was open before taking a break!");
                }
            }
            cacheManager.removeTabState(deviceID);
        }
    }

    public void stopScriptOnEmulator(String deviceID) {
        stopScript(deviceID);
    }

    public void pauseOrResumeScriptOnEmulator(String deviceID) {
        AbstractScript currentScript = scriptInfo.getCurrentAbstractScript(deviceID);
        if (currentScript != null) {
            boolean isPaused = currentScript.isPaused();
            currentScript.setPaused(!isPaused);
            paused.set(!isPaused);
        }
    }

    private void stopScript(String deviceID) {
        logger.log("Stopping script on: ", deviceID);
        stopScriptExecution(deviceID);
        handleWebhookNotification(deviceID);
        attemptThreadTermination(deviceID);
        updateUIAfterScriptStop(deviceID);
    }

    private void handleWebhookNotification(String deviceID) {
        if (credMgr.getWebhookURL(scriptAccountManager.getSelectedAccount(deviceID)) != null) {
            discordWebhook.sendStopMessage(deviceID);

            if (discordWebhook.isWebhookTaskRunning(deviceID)) {
                discordWebhook.stopWebhookSchedule(deviceID);
            }
        }
    }

    private void stopScriptExecution(String deviceID) {
        AbstractScript abstractScript = scriptInfo.getCurrentAbstractScript(deviceID);
        if (abstractScript != null) {
            abstractScript.setPaused(true);
        }

        CancellationToken cancellationToken = scriptInfo.getCancellationToken(deviceID);
        if (cancellationToken != null) {
            cancellationToken.requestCancellation();
        }

        scriptInfo.removeScriptScheduler(deviceID);
    }

    private void attemptThreadTermination(String deviceID) {
        Thread scriptThread = emulatorScriptThreads.get(deviceID);
        terminateThreadIfAlive(scriptThread);
    }

    private void terminateThreadIfAlive(Thread scriptThread) {
        if (scriptThread != null && scriptThread.isAlive()) {
            scriptThread.interrupt();
            waitForThreadToFinish(scriptThread);
        }
    }

    private void waitForThreadToFinish(Thread scriptThread) {
        try {
            scriptThread.join(1000);
        } catch (InterruptedException e) {
            logger.devLog("Interrupted while waiting for script thread to join: " + e.getMessage());
            Thread.currentThread().interrupt(); // Restore interrupt status.
        }
    }

    private void updateUIAfterScriptStop(String deviceID) {
        Platform.runLater(() -> {
            Thread scriptThread = emulatorScriptThreads.get(deviceID);

            if (scriptThread != null && scriptThread.isAlive()) {
                logger.print("[Forceful stop was required] Script thread is still alive, attempting termination");
                try {
                    scriptThread.interrupt();
                    scriptThread.join(1000); // Wait for up to 5 seconds for termination
                } catch (InterruptedException e) {
                    logger.print("Interrupted while waiting for script thread to terminate for deviceID: " + deviceID);
                    Thread.currentThread().interrupt(); // Restore interrupt status.
                }
            }

            // Log script stopped and cleanups initiation
            logger.log("Script stopped on: " + deviceID, deviceID);
            isScriptRunning.isScriptRunningProperty(deviceID).set(false);
            emulatorScriptThreads.remove(deviceID);

            // Debug log to confirm cleanup timing
            logger.print("Starting script cleanup for: " + deviceID);

            // Perform cleanup
            scriptCleanup.clean(deviceID);
            runScriptCleanups(deviceID);
        });
    }

    private void runScriptCleanups(String deviceID) {
        // List of actions to clean up all services related to a specific device ID
        List<Runnable> cleanupTasks = Arrays.asList(
                () -> performScriptCleanup(deviceID),
                () -> breakHandlerService.removeService(deviceID),
                () -> sleepHandlerService.removeService(deviceID),
                () -> runtimeService.removeService(deviceID),
                () -> afkHandlerService.removeService(deviceID),
                () -> xpService.removeService(deviceID),
                () -> leveledupService.removeService(deviceID),
                () -> cleanScriptInfoStatics(deviceID),
                () -> removeScriptAccount(deviceID)
        );

        // Execute all cleanup tasks
        cleanupTasks.forEach(task -> {
            try {
                task.run();
            } catch (Exception e) {
                logger.print("Error during cleanup task for device: " + deviceID + e);
            }
        });
    }

    @Override
    public void stop() {
        stopScriptOnEmulator(scriptInfo.getCurrentEmulatorId());
    }

    @Override
    public boolean isScriptStopping() {
        return scriptInfo.getCancellationToken(scriptInfo.getCurrentEmulatorId()).isCancellationRequested();
    }

    @Override
    public boolean isAccountBreaksEnabled() {
        return credMgr.isBreaksEnabled(scriptAccountManager.getSelectedAccount(scriptInfo.getCurrentEmulatorId()));
    }

    @Override
    public boolean isPaused() {
        return scriptInfo.getCurrentAbstractScript(scriptInfo.getCurrentEmulatorId()).isPaused();
    }

    private void cleanScriptInfoStatics(String deviceID) {
        try {
            scriptInfo.cleanScriptStatics(deviceID);
        } catch (Exception e) {
            logger.devLog("Error cleaning scriptInfo statics for device " + deviceID + ": " + e.getMessage());
        }
    }

    private void performScriptCleanup(String deviceID) {
        try {
            scriptCleanup.clean(deviceID);
        } catch (Exception e) {
            logger.devLog("Error performing script cleanup for device " + deviceID + ": " + e.getMessage());
        }
    }

    private void removeScriptAccount(String deviceID) {
        try {
            scriptAccountManager.removeAccountForEmulator(deviceID);
        } catch (Exception e) {
            logger.devLog("Error removing script account for device " + deviceID + ": " + e.getMessage());
        }
    }

    private boolean isTimeForBreak(String deviceID) {
        BreakHandlerService breakService = breakHandlerService.getHandlerForEmulator(deviceID);
        SleepHandlerService sleepService = sleepHandlerService.getHandlerForEmulator(deviceID);
        return (breakService.isEnabled() && breakService.isTimeForBreak()) || (sleepService.isEnabled() && sleepService.isTimeForSleep());
    }

    private boolean isCancellationRequested(String deviceID) {
        return scriptInfo.getCancellationToken(deviceID).isCancellationRequested();
    }

    private boolean isTimeForAFK(String deviceID) {
        AFKHandlerService afkService = afkHandlerService.getHandlerForEmulator(deviceID);
        return afkService.isEnabled() && afkService.isTimeForAFK();
    }

    private boolean isWaitingForBreakOrAFKToEnd(String deviceID) {
        BreakHandlerService breakService = breakHandlerService.getHandlerForEmulator(deviceID);
        AFKHandlerService afkService = afkHandlerService.getHandlerForEmulator(deviceID);
        SleepHandlerService sleepService = sleepHandlerService.getHandlerForEmulator(deviceID);

        return (breakService.isTimeForBreak() || afkService.isTimeForAFK() || sleepService.isTimeForSleep()) && !isCancellationRequested(deviceID);
    }

    public void stopAllScripts() {
        logger.print("Stopping all running scripts...");
        // Loop through all device IDs in emulatorScriptThreads
        for (String deviceID : emulatorScriptThreads.keySet()) {
            try {
                // Stop the script for the given device ID
                stopScriptOnEmulator(deviceID);
            } catch (Exception e) {
                logger.log("Failed to stop script for device: " + deviceID + " due to: " + e.getMessage(), deviceID);
                e.printStackTrace();
            }
        }
        logger.print("All scripts stopped.");
    }

    public BooleanProperty pausedProperty() {
        return paused;
    }
}
