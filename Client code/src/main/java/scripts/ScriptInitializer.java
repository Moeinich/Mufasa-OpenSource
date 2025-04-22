package scripts;

import helpers.AbstractScript;
import helpers.Logger;
import helpers.annotations.ScriptManifest;
import helpers.scripts.ScriptInstanceLoader;
import helpers.scripts.utils.ByteArrayClassLoader;
import helpers.scripts.utils.Script;
import helpers.services.BreakHandlerService;
import helpers.services.SleepHandlerService;
import interfaces.iScript;

public class ScriptInitializer {
    private final ScriptAPIHandler scriptAPIHandler;
    private final Logger logger;
    private final ScriptInstanceLoader scriptInstanceLoader;
    private final ScriptConfigurator scriptConfigurator;
    private final ScriptInfo scriptInfo;
    private final BreakHandlerService breakHandlerService;
    private final SleepHandlerService sleepHandlerService;

    public ScriptInitializer(Logger logger, ScriptAPIHandler scriptAPIHandler, ScriptInstanceLoader scriptInstanceLoader, ScriptConfigurator scriptConfigurator, ScriptInfo scriptInfo, BreakHandlerService breakHandlerService, SleepHandlerService sleepHandlerService) {
        this.scriptAPIHandler = scriptAPIHandler;
        this.logger = logger;
        this.scriptInstanceLoader = scriptInstanceLoader;
        this.scriptConfigurator = scriptConfigurator;
        this.scriptInfo = scriptInfo;
        this.breakHandlerService = breakHandlerService;
        this.sleepHandlerService = sleepHandlerService;
    }

    public AbstractScript instantiateScript(Script script, String deviceID) throws Exception {
        ByteArrayClassLoader classLoader = new ByteArrayClassLoader(this.getClass().getClassLoader(), script);

        // Iterate through all class files in the script to find the main script class
        for (String classFilePath : script.listAllClassFiles()) {
            String className = classFilePath.replace("/", ".").replace(".class", "");
            try {
                Class<?> clazz = classLoader.loadClass(className);
                if (clazz.isAnnotationPresent(ScriptManifest.class)) {
                    ScriptManifest manifest = clazz.getAnnotation(ScriptManifest.class);
                    scriptInfo.setScriptManifest(deviceID, manifest); // Set the script manifest

                    if (AbstractScript.class.isAssignableFrom(clazz)) {
                        return (AbstractScript) clazz.getDeclaredConstructor().newInstance();
                    }
                }
            } catch (ClassNotFoundException e) {
                logger.log("Class not found in script", deviceID);
            }
        }
        throw new Exception("Main script class not found");
    }

    public boolean initializeScript(AbstractScript script, iScript iScript, String deviceID) {
        BreakHandlerService breakService = breakHandlerService.getHandlerForEmulator(deviceID);
        if (breakService.isEnabled()) {
            breakService.start();
        }

        SleepHandlerService sleepService = sleepHandlerService.getHandlerForEmulator(deviceID);
        if (sleepService.isEnabled()) {
            sleepService.setDeviceName(deviceID);
            sleepService.start();
        }

        script.initialize(
                scriptAPIHandler.grandExchangeAPI(),
                scriptAPIHandler.interfacesAPI(),
                scriptAPIHandler.loggerAPI(),
                scriptAPIHandler.bankAPI(),
                scriptAPIHandler.clientAPI(),
                scriptAPIHandler.getConditionAPI(),
                scriptAPIHandler.depositBoxAPI(),
                scriptAPIHandler.equipmentAPI(),
                scriptAPIHandler.gameAPI(),
                scriptAPIHandler.gameTabsAPI(),
                scriptAPIHandler.inventoryAPI(),
                scriptAPIHandler.loginAPI(),
                scriptAPIHandler.logoutAPI(),
                scriptAPIHandler.magicAPI(),
                scriptAPIHandler.overlayAPI(),
                scriptAPIHandler.playerAPI(),
                scriptAPIHandler.prayerAPI(),
                scriptAPIHandler.statsAPI(),
                scriptAPIHandler.walkerAPI(),
                scriptAPIHandler.xpBarAPI(),
                scriptAPIHandler.chatboxAPI(),
                scriptAPIHandler.objectsAPI(),
                iScript,
                scriptAPIHandler.paintAPI(),
                scriptAPIHandler.ocrAPI()
                );

        if (!script.isInitialized()) {
            logger.debugLog("Script initialization failed", deviceID);
            return false;
        }
        return true;
    }

    public AbstractScript createAndInitializeScript(Script script, iScript iScript, String deviceID) {
        try {
            AbstractScript instantiatedScript = instantiateScript(script, deviceID);
            //Setup script configurations
            if (!scriptConfigurator.setupScriptConfiguration(instantiatedScript, deviceID)) {
                return null;
            }

            if (initializeScript(instantiatedScript, iScript, deviceID)) {
                return instantiatedScript;
            }
        } catch (Exception e) {
            logger.debugLog("Caught exception while trying to initialize Script", deviceID);
            e.printStackTrace();
        }
        return null;
    }

    public Script createScriptObjectFromFile(String jarFilePath) {
        return scriptInstanceLoader.createScriptObjectFromFile(jarFilePath);
    }

}
