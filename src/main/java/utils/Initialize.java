package utils;

import UI.*;
import UI.components.*;
import UI.GraphUI;
import UI.scripts.AnnotationControls;
import helpers.CacheManager;
import helpers.DiscordWebhook;
import helpers.GetGameView;
import helpers.Logger;
import helpers.OCR.DigitReader;
import helpers.OCR.ReadLevels;
import helpers.OCR.ReadXP;
import helpers.adb.ADBHandler;
import helpers.Color.ColorFinder;
import helpers.Color.TemplateMatcher;
import helpers.cacheHandler.RSPreferenceUpdater;
import helpers.emulator.DirectCapture;
import helpers.emulator.EmulatorHelper;
import helpers.emulator.EmulatorManager;
import helpers.openCV.ImageRecognition;
import helpers.scripts.ScriptCleanup;
import helpers.scripts.ScriptInstanceLoader;
import helpers.services.*;
import helpers.services.utils.AFKServiceSettings;
import helpers.services.utils.BreakServiceSettings;
import helpers.services.utils.SleepServiceSettings;
import helpers.testGrounds.ColorScanner;
import helpers.utils.GameviewCache;
import helpers.utils.IsScriptRunning;
import osr.mapping.*;
import osr.mapping.utils.*;
import osr.utils.ImageUtils;
import osr.walker.utils.TranslatePosition;
import osr.walker.Walker;
import osr.walker.utils.MapChunkHandler;
import osr.walker.utils.MapIR;
import scripts.APIClasses.*;
import scripts.*;

import java.io.*;

import static utils.DependencyExtractor.extractDependencies;

public class Initialize {
    private static final String OS_NAME = System.getProperty("os.name").toLowerCase();
    private static final String OS_ARCH = System.getProperty("os.arch");

    static {
        try {
            System.out.println("Loading OpenCV libraries");
            String libName = getLibraryName();
            String resourcePath = getResourcePath(libName);
            if (!libraryExists()) {
                System.out.println("Libraries not detected, extracting it!");
                extractDependencies();
            }
            System.load(resourcePath);
            System.out.println("OpenCV loaded");
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize library", e);
        }
    }

    public static InitializedObjects init() {

        //Initialize objects
        InitializedObjects classes = new InitializedObjects();
        System.out.println("Making object instances..");

        // Objects with no dependencies
        classes.scriptInstanceLoader = new ScriptInstanceLoader();
        classes.scriptInfo = new ScriptInfo();
        classes.logArea = new LogArea();
        classes.isScriptRunning = new IsScriptRunning();
        classes.accountManagerUI = new AccountManagerUI();
        classes.scriptAccountManager = new ScriptAccountManager();
        classes.credMgr = new CredentialsManager();
        classes.AFKServiceSettings = new AFKServiceSettings();
        classes.breakServiceSettings = new BreakServiceSettings();
        classes.sleepServiceSettings = new SleepServiceSettings();
        classes.cacheManager = new CacheManager();

        // Internal client dependencies
        System.out.println("Creating client dependency objects");
        classes.logger = new Logger(classes.logArea);
        classes.adbHandler = new ADBHandler(classes.logger);
        classes.rsPreferenceUpdater = new RSPreferenceUpdater(classes.adbHandler);

        //Services
        System.out.println("Creating service objects");
        classes.runtimeService = new RuntimeService();
        classes.breakHandlerService = new BreakHandlerService(classes.breakServiceSettings);
        classes.afkHandlerService = new AFKHandlerService(classes.AFKServiceSettings);
        classes.sleepHandlerService = new SleepHandlerService(classes.sleepServiceSettings, classes.logger);
        classes.xpService = new XPService(classes.runtimeService);

        classes.mapChunkHandler = new MapChunkHandler(classes.logger);
        classes.directCapture = new DirectCapture(classes.logger, classes.cacheManager);
        classes.itemProcessor = new ItemProcessor(classes.cacheManager);
        classes.paintBar = new PaintBar(classes.cacheManager, classes.itemProcessor);
        classes.scriptResourceManager = new ScriptResourceManager(classes.scriptInfo);
        classes.imageUtils = new ImageUtils(classes.scriptInfo, classes.scriptResourceManager, classes.cacheManager);
        classes.gameviewCache = new GameviewCache(classes.imageUtils, classes.logger);
        classes.getGameView = new GetGameView(classes.gameviewCache, classes.cacheManager);
        classes.digitReader = new DigitReader(classes.cacheManager, classes.getGameView);
        classes.templateMatcher = new TemplateMatcher(classes.getGameView);

        classes.emulatorHelper = new EmulatorHelper(classes.logger, classes.adbHandler);
        classes.emulatorManager = new EmulatorManager(classes.logger, classes.adbHandler, classes.emulatorHelper, classes.gameviewCache, classes.isScriptRunning, classes.directCapture);

        // Utility things
        System.out.println("Creating utility objects");
        classes.discordWebhook = new DiscordWebhook(classes.logger, classes.getGameView, classes.scriptInfo, classes.scriptAccountManager, classes.runtimeService, classes.credMgr, classes.xpService, classes.breakHandlerService, classes.paintBar);

        //Initialize Core methods
        System.out.println("Creating Core method objects");
        classes.colorScanner = new ColorScanner(classes.getGameView);
        classes.mapIR = new MapIR(classes.logger);
        classes.colorFinder = new ColorFinder(classes.getGameView);
        classes.imageRecognition = new ImageRecognition(classes.logger, classes.colorFinder);
        classes.readxp = new ReadXP(classes.digitReader, classes.cacheManager, classes.colorFinder, classes.getGameView);
        classes.readlevels = new ReadLevels(classes.logger, classes.getGameView, classes.cacheManager, classes.digitReader);
        classes.conditionAPI = new ConditionAPI(classes.scriptInfo);
        classes.clientAPI = new ClientAPI(classes.digitReader, classes.cacheManager, classes.conditionAPI, classes.adbHandler, classes.colorFinder, classes.logger, classes.scriptInfo, classes.afkHandlerService, classes.breakHandlerService, classes.sleepHandlerService);
        classes.minimapProjections = new MinimapProjections(classes.logger, classes.getGameView, classes.imageRecognition, classes.imageUtils);
        classes.minimap = new Minimap(classes.cacheManager, classes.logger, classes.minimapProjections, classes.scriptInfo, classes.imageUtils);
        classes.translatePosition = new TranslatePosition(classes.logger, classes.minimap);
        classes.fairyRings = new FairyRings(classes.logger, classes.imageRecognition, classes.getGameView, classes.clientAPI, classes.imageUtils);
        classes.playerHelper = new PlayerHelper(classes.clientAPI, classes.readlevels, classes.colorFinder);
        classes.geHelper = new GEHelper(classes.colorFinder);

        //Initialize mapping classes
        System.out.println("Creating mapping objects");
        classes.gameOCR = new GameOCR(classes.getGameView, classes.logger, classes.digitReader, classes.cacheManager);
        classes.grandExchange = new GrandExchange(classes.geHelper, classes.logger, classes.colorFinder, classes.clientAPI, classes.conditionAPI, classes.getGameView, classes.imageRecognition, classes.itemProcessor);
        classes.walker = new Walker(classes.translatePosition, classes.clientAPI, classes.logger, classes.minimap, classes.mapIR, classes.scriptInfo, classes.playerHelper, classes.mapChunkHandler, classes.imageUtils, classes.cacheManager);
        classes.worldHopperUtils = new WorldHopperUtils(classes.logger, classes.minimapProjections, classes.minimap, classes.colorFinder, classes.translatePosition, classes.walker, classes.getGameView);
        classes.depositBox = new DepositBox(classes.templateMatcher, classes.logger, classes.imageUtils);
        classes.gameTabs = new GameTabs(classes.logger, classes.clientAPI, classes.conditionAPI, classes.colorFinder);
        classes.overlayFinder = new OverlayFinder(classes.logger, classes.getGameView, classes.gameTabs, classes.colorFinder, classes.colorScanner);
        classes.equipment = new Equipment(classes.itemProcessor, classes.logger, classes.getGameView, classes.imageRecognition, classes.gameTabs);
        classes.inventory = new Inventory(classes.digitReader, classes.colorFinder, classes.itemProcessor, classes.cacheManager, classes.logger, classes.getGameView, classes.imageRecognition, classes.gameTabs, classes.clientAPI, classes.scriptInfo);
        classes.chatbox = new Chatbox(classes.templateMatcher, classes.cacheManager, classes.logger, classes.getGameView, classes.imageRecognition, classes.clientAPI, classes.conditionAPI, classes.imageUtils, classes.colorFinder);
        classes.login = new Login(classes.templateMatcher, classes.breakHandlerService, classes.minimapProjections, classes.logger, classes.conditionAPI, classes.getGameView, classes.gameTabs, classes.clientAPI, classes.adbHandler, classes.imageUtils, classes.colorFinder, classes.chatbox);
        classes.logout = new Logout(classes.templateMatcher, classes.logger, classes.gameTabs, classes.clientAPI, classes.conditionAPI, classes.login, classes.imageUtils);
        classes.magic = new Magic(classes.cacheManager, classes.logger, classes.imageRecognition, classes.getGameView, classes.clientAPI, classes.imageUtils, classes.colorFinder);
        classes.prayer = new Prayer(classes.logger, classes.imageRecognition, classes.getGameView, classes.clientAPI, classes.conditionAPI, classes.imageUtils);
        classes.stats = new Stats(classes.logger, classes.readlevels);
        classes.xpBar = new XPBar(classes.logger, classes.readxp, classes.xpService);
        classes.player = new Player(classes.xpBar, classes.playerHelper, classes.logger, classes.getGameView, classes.walker, classes.clientAPI, classes.gameTabs, classes.colorFinder, classes.conditionAPI);
        classes.bank = new Bank(classes.templateMatcher, classes.digitReader, classes.itemProcessor, classes.cacheManager, classes.logger, classes.getGameView, classes.imageRecognition, classes.walker, classes.clientAPI, classes.conditionAPI, classes.imageUtils, classes.scriptInfo, classes.scriptAccountManager, classes.credMgr, classes.colorFinder, classes.player);
        classes.interfaces = new Interfaces(classes.templateMatcher, classes.digitReader, classes.itemProcessor, classes.cacheManager, classes.logger, classes.getGameView, classes.imageRecognition, classes.clientAPI, classes.conditionAPI, classes.imageUtils, classes.colorFinder);
        classes.objects = new Objects(classes.logger, classes.imageRecognition, classes.getGameView, classes.imageUtils);
        classes.game = new Game(classes.templateMatcher, classes.digitReader, classes.breakHandlerService, classes.colorFinder, classes.minimapProjections, classes.translatePosition, classes.objects, classes.cacheManager, classes.worldHopperUtils, classes.gameTabs, classes.getGameView, classes.logger, classes.clientAPI, classes.login, classes.logout, classes.fairyRings, classes.conditionAPI, classes.imageUtils, classes.discordWebhook, classes.scriptAccountManager, classes.credMgr, classes.chatbox, classes.readlevels, classes.login);

        // ScriptConfig helper
        classes.annotationControls = new AnnotationControls(classes.itemProcessor, classes.worldHopperUtils);
        // leveluped service needs a few APIs and will need to be loaded later
        classes.leveledupService = new LeveledupService(classes.player, classes.discordWebhook, classes.getGameView);
        classes.mm2MSProjection = new MM2MSProjection(classes.walker);

        //Initialize API class methods
        System.out.println("Creating API class objects");
        classes.ocrAPI = new OcrAPI(classes.scriptInfo, classes.gameOCR);
        classes.grandExchangeAPI = new GrandExchangeAPI(classes.grandExchange, classes.scriptInfo);
        classes.bankAPI = new BankAPI(classes.bank, classes.scriptInfo);
        classes.depositBoxAPI = new DepositBoxAPI(classes.depositBox, classes.scriptInfo);
        classes.equipmentAPI = new EquipmentAPI(classes.equipment, classes.scriptInfo);
        classes.gameTabsAPI = new GameTabsAPI(classes.gameTabs, classes.scriptInfo);
        classes.gameAPI = new GameAPI(classes.adbHandler, classes.game, classes.scriptInfo, classes.gameTabsAPI, classes.logger, classes.walker);
        classes.inventoryAPI = new InventoryAPI(classes.inventory, classes.scriptInfo);
        classes.loginAPI = new LoginAPI(classes.login, classes.scriptInfo);
        classes.logoutAPI = new LogoutAPI(classes.logout, classes.scriptInfo);
        classes.magicAPI = new MagicAPI(classes.magic, classes.scriptInfo);
        classes.overlayAPI = new OverlayAPI(classes.overlayFinder, classes.scriptInfo);
        classes.playerAPI = new PlayerAPI(classes.player, classes.scriptInfo);
        classes.prayerAPI = new PrayerAPI(classes.prayer, classes.scriptInfo);
        classes.statsAPI = new StatsAPI(classes.stats, classes.scriptInfo);
        classes.walkerAPI = new WalkerAPI(classes.walker, classes.scriptInfo);
        classes.xpBarAPI = new XPBarAPI(classes.xpBar, classes.scriptInfo);
        classes.interfacesAPI = new InterfacesAPI(classes.interfaces, classes.scriptInfo);
        classes.chatboxAPI = new ChatboxAPI(classes.chatbox, classes.scriptInfo, classes.gameOCR);
        classes.loggerAPI = new LoggerAPI(classes.logger, classes.scriptInfo);
        classes.objectsAPI = new ObjectsAPI(classes.objects, classes.scriptInfo);
        classes.paintAPI = new PaintAPI(classes.paintBar, classes.scriptInfo, classes.imageUtils);

        //Initialize the scripthandler
        System.out.print("Initializing the Script Handler objects\n");
        classes.scriptAPIHandler = new ScriptAPIHandler(classes.grandExchangeAPI, classes.interfacesAPI, classes.bankAPI, classes.clientAPI, classes.conditionAPI, classes.depositBoxAPI, classes.equipmentAPI, classes.gameAPI, classes.gameTabsAPI, classes.inventoryAPI, classes.loginAPI, classes.logoutAPI, classes.magicAPI, classes.overlayAPI, classes.playerAPI, classes.prayerAPI, classes.statsAPI, classes.walkerAPI, classes.xpBarAPI, classes.chatboxAPI, classes.loggerAPI, classes.objectsAPI, classes.paintAPI, classes.ocrAPI);
        classes.scriptConfigurator = new ScriptConfigurator(classes.annotationControls, classes.scriptAccountManager, classes.credMgr);
        classes.scriptInitializer = new ScriptInitializer(classes.logger, classes.scriptAPIHandler, classes.scriptInstanceLoader, classes.scriptConfigurator, classes.scriptInfo, classes.breakHandlerService, classes.sleepHandlerService);
        classes.scriptCleanup = new ScriptCleanup(classes.cacheManager, classes.logger, classes.discordWebhook, classes.xpBar, classes.isScriptRunning);
        classes.scriptExecutor = new ScriptExecutor(classes.cacheManager, classes.logger, classes.scriptAPIHandler, classes.scriptCleanup, classes.scriptAccountManager, classes.scriptInitializer, classes.scriptInfo, classes.login, classes.isScriptRunning, classes.runtimeService, classes.breakHandlerService, classes.sleepHandlerService, classes.afkHandlerService, classes.xpService, classes.leveledupService, classes.credMgr, classes.discordWebhook, classes.game);

        //Initialize UI
        System.out.print("Initializing UI objects\n");
        classes.breakUI = new BreakUI(classes.logger, classes.AFKServiceSettings, classes.breakServiceSettings, classes.sleepServiceSettings);
        classes.hopUI = new HopUI(classes.logger);
        classes.scriptSelectionUI = new ScriptSelectionUI(classes.scriptExecutor, classes.scriptInstanceLoader);
        classes.emulatorView = new EmulatorView(classes.scriptInfo, classes.logger, classes.emulatorManager, classes.emulatorHelper, classes.logArea, classes.isScriptRunning, classes.scriptSelectionUI, classes.scriptExecutor);
        classes.graphUI = new GraphUI(classes.logger, classes.mapChunkHandler, classes.walker);
        classes.mapUI = new MapUI(classes.minimap, classes.walker, classes.imageUtils, classes.mapChunkHandler);
        classes.devUI = new DevUI(classes.gameOCR, classes.templateMatcher, classes.digitReader, classes.colorScanner, classes.mm2MSProjection, classes.imageUtils, classes.emulatorManager, classes.mapUI, classes.logger, classes.bank, classes.chatbox, classes.clientAPI, classes.depositBox, classes.equipment, classes.game, classes.inventory, classes.login, classes.logout, classes.magic, classes.overlayFinder, classes.player, classes.stats, classes.walker, classes.xpBar, classes.logArea, classes.colorFinder, classes.imageRecognition, classes.itemProcessor, classes.grandExchange, classes.getGameView);
        classes.mainUI = new MainUI(classes.logArea, classes.scriptInfo, classes.cacheManager, classes.emulatorView, classes.runtimeService, classes.breakHandlerService, classes.sleepHandlerService, classes.xpService, classes.logger);
        classes.clientUI = new ClientUI(classes.scriptExecutor, classes.isScriptRunning, classes.mapUI, classes.graphUI, classes.breakUI, classes.hopUI, classes.accountManagerUI, classes.logger, classes.adbHandler, classes.emulatorManager, classes.mainUI, classes.devUI);
        System.out.println("Object instances done");

        return classes;
    }

    private static String getLibraryName() {
        String libExtension;

        if (OS_NAME.contains("win")) {
            libExtension = ".dll";
        } else if (OS_NAME.contains("mac")) {
            libExtension = ".dylib";
        } else {
            throw new UnsupportedOperationException("Unsupported OS: " + OS_NAME);
        }

        return "libopencv_java490" + libExtension;
    }

    private static String getResourcePath(String libName) {
        String osDir = getOSDirectory();
        String archDir = getArchDirectory();
        return SystemUtils.getLibsFolderPath() + "/opencv/" + osDir + "/" + archDir + "/" + libName;
    }

    private static String getOSDirectory() {
        if (OS_NAME.contains("win")) {
            return "windows";
        } else if (OS_NAME.contains("mac")) {
            return "osx";
        } else {
            throw new UnsupportedOperationException("Unsupported OS: " + OS_NAME);
        }
    }

    private static String getArchDirectory() {
        if (OS_NAME.contains("win")) {
            if (!OS_ARCH.contains("64")) {
                throw new UnsupportedOperationException("Unsupported Windows architecture: " + OS_ARCH);
            }
            return "x86_64";
        } else if (OS_NAME.contains("mac")) {
            if (!(OS_ARCH.contains("arm") || OS_ARCH.contains("aarch64"))) {
                throw new UnsupportedOperationException("Unsupported Mac architecture: " + OS_ARCH);
            }
            return "ARMv8";
        } else {
            throw new UnsupportedOperationException("Unsupported OS: " + OS_NAME);
        }
    }

    private static boolean libraryExists() {
        String libName = getLibraryName();
        String resourcePath = getResourcePath(libName);
        File destFile = new File(resourcePath);
        return destFile.exists();
    }
}