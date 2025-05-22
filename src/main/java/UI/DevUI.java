package UI;

import UI.components.EmulatorView;
import UI.components.LogArea;
import UI.components.MM2MSProjection;
import helpers.GetGameView;
import helpers.Logger;
import helpers.OCR.DigitReader;
import helpers.OCR.cfOCR;
import helpers.OCR.utils.FontName;
import helpers.ThreadManager;
import helpers.UIDraws.DrawRects;
import helpers.UIDraws.ShapeType;
import helpers.Color.ColorFinder;
import helpers.Color.TemplateMatcher;
import helpers.emulator.EmulatorManager;
import helpers.openCV.ImageRecognition;
import helpers.openCV.utils.MatchedRectangle;
import helpers.mColor.ColorScanner;
import helpers.utils.*;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.Pair;
import org.controlsfx.control.SearchableComboBox;
import org.fxmisc.richtext.StyleClassedTextArea;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import osr.mapping.*;
import osr.mapping.utils.ItemProcessor;
import osr.utils.ImageUtils;
import osr.utils.OverlayType;
import osr.walker.Walker;
import osr.walker.utils.PositionResult;
import scripts.APIClasses.ClientAPI;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.*;
import java.util.concurrent.*;

import static UI.components.EmulatorView.getEmulatorDevListView;
import static UI.components.MapUtils.parseChunksTF;
import static UI.components.utils.Observables.*;
import static helpers.patterns.Bold12Patterns.bold12Patterns;
import static helpers.patterns.Plain11Patterns.plain11Patterns;
import static helpers.patterns.Plain12Patterns.plain12Patterns;
import static helpers.patterns.Quill1Patterns.quill1Patterns;
import static helpers.patterns.Quill8Patterns.quill8Patterns;
import static osr.mapping.Equipment.*;
import static utils.Constants.*;

public class DevUI {
    // Instances
    private final GameOCR gameOCR;
    private final ColorScanner colorScanner;
    private final MM2MSProjection mm2MSProjection;
    private final ImageUtils imageUtils;
    private final EmulatorManager emulatorManager;
    private final MapUI mapUI;
    private final Bank bank;
    private final Chatbox chatbox;
    private final ClientAPI clientAPI;
    private final DepositBox depositBox;
    private final Equipment equipment;
    private final Game game;
    private final Inventory inventory;
    private final Logger logger;
    private final Login login;
    private final Logout logout;
    private final Magic magic;
    private final OverlayFinder overlayFinder;
    private final Player player;
    private final Stats stats;
    private final Walker walker;
    private final XPBar xpBar;
    private final ColorFinder colorFinder;
    private final LogArea logAreaInstance;
    private final ImageRecognition imageRecognition;
    private final ItemProcessor itemProcessor;
    private final GrandExchange grandExchange;
    private final GetGameView getGameView;
    private final DigitReader digitReader;
    private final TemplateMatcher templateMatcher;

    private final ThreadManager threadManager = ThreadManager.getInstance();
    private final Button pauseButton = new Button("Pause capture");
    private final Button clearLogButton = new Button("Clear log");
    private final Button importRectangleButton = new Button("Import rectangle");
    // Toggle buttons for devUI
    private final ToggleButton regionButton = new ToggleButton();
    private final ToggleButton passthroughButton = new ToggleButton();
    private final ToggleButton irToggleSearch = new ToggleButton();
    private final ToggleButton cfToggleSearch = new ToggleButton();
    // Spinner fields for devUI
    private final Spinner<Integer> tabNumberField = new Spinner<>();
    private final Spinner<Double> bankThresholdSpinner = new Spinner<>();
    private final Spinner<Integer> makeOptionSpinner = new Spinner<>();
    private final Spinner<Integer> gameZoomSpinner = new Spinner<>();
    private final Spinner<Double> inventoryThresholdSpinner = new Spinner<>();
    private final Spinner<Integer> overlaysMinPtsSpinner = new Spinner<>();
    private final Spinner<Double> overlaysEPSSpinner = new Spinner<>();
    private final Spinner<Integer> cfToleranceSpinner = new Spinner<>();
    private final Spinner<Double> irSearchThreshold = new Spinner<>();
    private final Spinner<Integer> irSearchSpeed = new Spinner<>();
    // Text fields for devUI
    private final TextField bankItemIDField = new TextField();
    private final TextField equipmentIntField = new TextField();
    private final TextField gameFairyCodeField = new TextField();
    private final TextField inventoryItemIDField = new TextField();

    //VBoxies
    private final TextField walkerCoordField = new TextField();
    private final TextField hexColors = new TextField();
    private final TextField excludedHexColors = new TextField();
    private final TextField findString = new TextField();
    private final TextField irItemIDField = new TextField();
    private final TextField chunksToLoad = new TextField();
    private final TextField planesToLoad = new TextField();
    // ComboBoxes for devUI
    private final SearchableComboBox<String> equipmentDropdown = new SearchableComboBox<>();
    private final SearchableComboBox<String> gameDropdown = new SearchableComboBox<>();
    private final SearchableComboBox<String> spellsDropdown = new SearchableComboBox<>();
    private final SearchableComboBox<String> overlayDropdown = new SearchableComboBox<>();
    // Canvas stuff for devUI
    private final Canvas gameCanvas = new Canvas();
    // Panes for devUI
    private final BorderPane developerView = new BorderPane();
    private final ImageView gameView = new ImageView();
    private final ImageView imageRecogView = new ImageView();
    private final List<java.awt.Color> whiteAndYellow = Arrays.asList(java.awt.Color.decode("#ffffff"), java.awt.Color.decode("#ffff00"));
    // Labels for devUI
    Label coordinateLabel = new Label("coordinateLabel");
    Label hexLabel = new Label("Hex");
    Label hueLabel = new Label("Hue ");
    Label satLabel = new Label("Sat");
    Label rectangleLabel = new Label("rectangleLabel");
    List<java.awt.Color> testTextColors = Arrays.asList(java.awt.Color.decode("#ffffff"), java.awt.Color.decode("#ffff00"), java.awt.Color.decode("#9090ff"), java.awt.Color.decode("#000001"));
    // HBoxies
    private HBox logHBox;
    private HBox bankHBox;
    private HBox chatBoxHBox;
    private HBox clientHBox;
    private HBox colorfinderHBox;
    private HBox depositBoxHBox;
    private HBox equipmentHBox;
    private HBox gameHBox;
    private HBox gameTabsHBox;
    private HBox imageRecogHBox;
    private HBox interfacesHBox;
    private HBox inventoryHBox;
    private HBox loginLogoutHBox;
    private HBox magicHBox;
    private HBox objectsHBox;
    private HBox overlayHBox;
    private HBox playerHBox;
    private HBox statsHBox;
    private HBox walkerHBox;
    private HBox xpBarHBox;
    // Buttons for devUI
    private MenuButton dropdownMenu;
    private boolean isPaused = false;
    private Button containsButton, withdrawItemButton, withdrawItemButtonCF, visualizePinButton, getItemStackButton, openTabButton, isTabSelectedButton, getCurrentTabButton, isBankPinNeededButton, findBankTabButton, bankItemGridButton, closeBankButton, isBankOpenButton, findDynamicBankRegionButton, stepToBankButton;
    private Button findChatboxMenuButton, makeOptionButton, visualizeMakeOptionsButton, readDigitsInAreaButton, isSelectedMake1Button, isSelectedMake5Button, isSelectedMakeAllButton, isSelectedMake10Button, findMaxOptionsButton, readChatboxButton, readLastLineButton, ocrAreaButton;
    private Button tempTestButton;
    private Button findQuantity1, findQuantity5, findQuantity10, findQuantityCustom, findQuantityAll, findDepositInventory, findDepositWorn, findDepositLoot, findCloseDepositBox, findSetCustQty, showBoxGrid;
    private Button itemAt, findHelm, findCape, findAmulet, findAmmo, findWeapon, findBody, findShield, findLegs, findGloves, findFeet, findRing;
    private Button setZoom, setFairyRing, isWorldListOpen, findActionButton, isTapToDropOn, isSingleTapOn, enableTapToDrop, disableTapToDrop, enableSingleTap, disableSingleTap, findOption, isPlayersAroundOption, countPlayersAround;
    private Button isInventFull, calcInventSlots, itemPosition, lastItemPosition, dropInventItems, showInventGrid, count, contains, tapAllItems, eat, getItemCenter, getItemStack;
    private Button onLoginScreen, isLoggedOut, isLoggedIn, findTapToPlay, isCompassNorth, readLoginScreen, closestLoginMessage, findLogoutOption, findExitWorldSwitcher;
    private Button isMagicCastable, castMagicSpell, isMagicInfoEnabled;
    private Button getGameCenter, findOverlays, findNearest, findSecondNearest, findFishingSpots;
    private Button leveledUp, isRunEnabled, toggleRun, enableAutoRet, disableAutoRet, checkPixelShift, isAutoRetOn, getHP, getPray, getRun, getSpec;
    private Button getRealLevelCF, getEffectiveLevelCF, getTotalLevelCF;
    private Button setupMap, getPlayerPosition, isReachAble, step, walkTo, webwalkTo, mapUIBtn, drawTiles;
    private Button readXP, test1, test2, test3, test4, test5;
    private Button findHexColors, scanAreaForColor, isColorAtPoint, isAnyColorInRect, isColorInRect, isPixelColor, scanRectAndDraw, findFishingColors, findAgilityColors, findItemColors, findNPC, readClickMenu;
    private Button uploadImage, findBestResult, findAllResults, findBestWithinRect, findAllWithinRect, uploadToCanvas, findBestItem, findAllItems;
    private ToggleButton drawLines = new ToggleButton();
    private ComboBox<String> skillsDropdown = new ComboBox<>();
    private DrawRects draw;
    // Misc holding variables
    private Point lastClickedPoint;
    private Point firstClickPoint = null;
    private Point secondClickPoint = null;
    private int mapWidth;
    private int mapHeight;
    private File uploadedFile;
    // Misc things for devUI
    private Stage stage;
    private ListView<String> emulatorListView;
    private StyleClassedTextArea logArea = new StyleClassedTextArea();
    private Timeline searchTimeline;
    private Timeline colorSearchTimeline;
    private ScheduledFuture<?> imageUpdateTask;

    public DevUI(GameOCR gameOCR, TemplateMatcher templateMatcher, DigitReader digitReader, ColorScanner colorScanner, MM2MSProjection mm2MSProjection, ImageUtils imageUtils, EmulatorManager emulatorManager, MapUI mapUI, Logger logger, Bank bank, Chatbox chatbox, ClientAPI clientAPI, DepositBox depositBox, Equipment equipment, Game game, Inventory inventory, Login login, Logout logout, Magic magic, OverlayFinder overlayFinder, Player player, Stats stats, Walker walker, XPBar xpBar, LogArea logArea, ColorFinder colorFinder, ImageRecognition imageRecognition, ItemProcessor itemProcessor, GrandExchange grandExchange, GetGameView getGameView) {
        this.gameOCR = gameOCR;
        this.colorScanner = colorScanner;
        this.imageUtils = imageUtils;
        this.emulatorManager = emulatorManager;
        this.mapUI = mapUI;
        this.logger = logger;
        this.bank = bank;
        this.chatbox = chatbox;
        this.clientAPI = clientAPI;
        this.depositBox = depositBox;
        this.equipment = equipment;
        this.game = game;
        this.inventory = inventory;
        this.login = login;
        this.logout = logout;
        this.magic = magic;
        this.overlayFinder = overlayFinder;
        this.player = player;
        this.stats = stats;
        this.walker = walker;
        this.xpBar = xpBar;
        this.logAreaInstance = logArea;
        this.colorFinder = colorFinder;
        this.imageRecognition = imageRecognition;
        this.itemProcessor = itemProcessor;
        this.mm2MSProjection = mm2MSProjection;
        this.grandExchange = grandExchange;
        this.getGameView = getGameView;
        this.digitReader = digitReader;
        this.templateMatcher = templateMatcher;

        initializeLogAreaListener();
        initializeCanvasEvents();
        initializeClickEvents();

        // Call all methods to fill the tabs
        setupBankTab();
        setupChatboxTab();
        setupClientTab();
        setupColorFinderTab();
        setupDepositboxTab();
        setupEquipmentTab();
        setupGameTab();
        setupImageRecogTab();
        setupInventoryTab();
        setupLoginNOutTab();
        setupMagicTab();
        setupOverlaysTab();
        setupPlayerTab();
        setupStatsTab();
        setupWalkerTab();
        setupXPTab();
    }

    public void initializeUI(MenuButton menuButton) {
        initializeImageView();
        dropdownMenu = menuButton; // This is the menu button
        TabPane tabPane = initTabBar();

        StackPane gameViewStack = new StackPane();
        gameCanvas.setWidth(894);
        gameCanvas.setHeight(540);
        gameViewStack.getChildren().addAll(gameView, gameCanvas);

        // Setup the draws
        draw = new DrawRects(gameCanvas);

        HBox topContent = new HBox();
        VBox rightPane = new VBox();
        emulatorListView.setPrefWidth(1150 - 894); // Adjust to fit within the 1150px limit
        emulatorListView.setMaxWidth(1150 - 894);
        dropdownMenu.setPrefWidth(1150 - 894); // Match width with emulatorListView
        dropdownMenu.setMaxWidth(1150 - 894);

        VBox labelsBox = new VBox(5); // 5 pixels of padding between labels
        HBox hueSatBox = new HBox(hueLabel, satLabel);
        HBox.setMargin(hueLabel, new Insets(0, 0, 0, 5)); // 5 pixels left margin for hueLabel

        labelsBox.getChildren().addAll(coordinateLabel, hexLabel, hueSatBox, rectangleLabel, importRectangleButton);
        VBox.setMargin(coordinateLabel, new Insets(0, 0, 0, 5)); // 5 pixels of left margin
        VBox.setMargin(hexLabel, new Insets(0, 0, 0, 5)); // 5 pixels of left margin
        VBox.setMargin(rectangleLabel, new Insets(0, 0, 0, 5)); // 5 pixels of left margin
        VBox.setMargin(importRectangleButton, new Insets(0, 0, 0, 5)); // 5 pixels of left margin

        // Create an HBox for pauseButton and clearLog button
        HBox buttonRow = new HBox(5); // 5 pixels of spacing between buttons
        buttonRow.getChildren().addAll(pauseButton, clearLogButton);
        VBox.setMargin(buttonRow, new Insets(10, 0, 5, 5)); // 5 pixels of margin on top of the HBox

        rightPane.getChildren().addAll(emulatorListView, dropdownMenu, buttonRow, labelsBox);

        topContent.getChildren().addAll(gameViewStack, rightPane);

        VBox mainContent = new VBox();
        mainContent.getChildren().addAll(topContent, tabPane);

        developerView.setTop(mainContent);

        // Limit the width of the entire UI to 1150 pixels
        developerView.setMaxWidth(1150);
    }

    private TabPane initTabBar() {
        TabPane tabPane = new TabPane();
        List<Pair<String, Supplier<HBox>>> tabsInfo = Arrays.asList(
                new Pair<>("Log", this::initLogTab),
                new Pair<>("Bank", this::initBankTab),
                new Pair<>("Chatbox", this::initChatBoxTab),
                new Pair<>("Client", this::initClientTab),
                new Pair<>("ColorFinder", this::initColorFinderTab),
                new Pair<>("Depositbox", this::initDepositBoxTab),
                new Pair<>("Equipment", this::initEquipmentTab),
                new Pair<>("Game", this::initGameTab),
                new Pair<>("Image Recog", this::initImageRecogTab),
                new Pair<>("Inventory", this::initInventoryTab),
                new Pair<>("Login/out", this::initLoginLogoutTab),
                new Pair<>("Magic", this::initMagicTab),
                new Pair<>("Overlays", this::initOverlayTab),
                new Pair<>("Player", this::initPlayerTab),
                new Pair<>("Stats", this::initStatsTab),
                new Pair<>("Walker", this::initWalkerTab),
                new Pair<>("XP", this::initXPBarTab)
        );
        // Loop through the array to create and add tabs to the TabPane
        for (Pair<String, Supplier<HBox>> tabInfo : tabsInfo) {
            String tabName = tabInfo.getKey();
            HBox tabContent = tabInfo.getValue().get();
            Tab tab = new Tab(tabName, tabContent);
            tab.setClosable(false); // Make the tab non-closable
            tabPane.getTabs().add(tab);
        }

        return tabPane;
    }

    public HBox initLogTab() {
        logHBox = new HBox();
        logHBox.getChildren().add(logArea);
        return logHBox;
    }

    public HBox initBankTab() {
        bankHBox = new HBox();
        VBox bankVBox = new VBox(5); // 5 pixels of padding between rows

        // List of all buttons in the Bank tab
        List<Button> buttons = Arrays.asList(
                bankItemGridButton,
                containsButton,
                findBankTabButton,
                getCurrentTabButton,
                isBankPinNeededButton,
                openTabButton,
                isBankOpenButton,
                stepToBankButton,
                withdrawItemButton,
                withdrawItemButtonCF,
                visualizePinButton,
                findDynamicBankRegionButton,
                getItemStackButton
        );

        // Creating rows for buttons
        HBox row1 = new HBox(10);
        HBox row2 = new HBox(10);
        HBox row3 = new HBox(10);

        // Distribute buttons using the reusable method
        distributeButtonsAlphabetically(buttons, row1, row2, row3);

        // Integer fields and other components
        HBox intFieldsRow = new HBox(5);
        intFieldsRow.getChildren().addAll(tabNumberField, bankThresholdSpinner, bankItemIDField);

        // Adding all rows to the VBox
        bankVBox.getChildren().addAll(row1, row2, row3, intFieldsRow);
        bankHBox.getChildren().add(bankVBox);

        return bankHBox;
    }

    public HBox initChatBoxTab() {
        chatBoxHBox = new HBox();
        VBox chatBoxVBox = new VBox(5); // 5 pixels of padding between rows

        // List of all buttons
        List<Button> buttons = Arrays.asList(
                findChatboxMenuButton,
                makeOptionButton,
                visualizeMakeOptionsButton,
                isSelectedMake1Button,
                isSelectedMake5Button,
                isSelectedMake10Button,
                isSelectedMakeAllButton,
                findMaxOptionsButton,
                readChatboxButton,
                readLastLineButton,
                ocrAreaButton,
                readDigitsInAreaButton
        );

        // Creating rows for buttons
        HBox row1 = new HBox(10);
        HBox row2 = new HBox(10);
        HBox row3 = new HBox(10);

        // Distribute buttons using the reusable method
        distributeButtonsAlphabetically(buttons, row1, row2, row3);

        // Add Spinner and TextField row if necessary
        HBox spinnerAndTextFieldRow = new HBox(10);
        spinnerAndTextFieldRow.getChildren().addAll(makeOptionSpinner);

        // Adding all rows to the VBox
        chatBoxVBox.getChildren().addAll(row1, row2, row3, spinnerAndTextFieldRow);
        chatBoxHBox.getChildren().add(chatBoxVBox);

        return chatBoxHBox;
    }

    public HBox initClientTab() {
        clientHBox = new HBox();
        VBox clientVBox = new VBox(5); // Vertical box with spacing

        HBox row1 = new HBox(10, passthroughButton, tempTestButton);
        HBox row2 = new HBox(10);
        HBox row3 = new HBox(10);
        HBox row4 = new HBox(10);
        HBox row5 = new HBox(10);

        // Adding rows and other UI components to the VBox
        clientVBox.getChildren().addAll(row1, row2, row3, row4, row5);
        clientHBox.getChildren().add(clientVBox);

        return clientHBox;
    }

    public HBox initColorFinderTab() {
        colorfinderHBox = new HBox();
        VBox colorfinderVBox = new VBox(5); // Vertical box with spacing

        // Setup rows for buttons, text fields, and spinner
        HBox row1 = new HBox(10, findHexColors, scanAreaForColor, isColorAtPoint, findAgilityColors, readClickMenu); // Adjust spacing as necessary
        HBox row2 = new HBox(10, isAnyColorInRect, isColorInRect, isPixelColor, findFishingColors, findNPC);
        HBox row3 = new HBox(10, scanRectAndDraw, drawLines, cfToggleSearch, findItemColors);
        HBox row4 = new HBox(10);
        HBox row5 = new HBox(10, hexColors, excludedHexColors, cfToleranceSpinner, findString);

        // Adding rows and other UI components to the VBox
        colorfinderVBox.getChildren().addAll(row1, row2, row3, row4, row5);
        colorfinderHBox.getChildren().add(colorfinderVBox);

        return colorfinderHBox;
    }

    public HBox initDepositBoxTab() {
        depositBoxHBox = new HBox();
        VBox depositBoxVBox = new VBox(5); // Vertical box with spacing

        // Initialize the rows for buttons
        HBox row1 = new HBox(10);
        HBox row2 = new HBox(10);
        HBox row3 = new HBox(10);

        // List of buttons for deposit box
        List<Button> depositBoxButtons = Arrays.asList(
                findQuantity1, findQuantity5, findQuantity10, findQuantityCustom, findQuantityAll,
                findDepositInventory, findDepositWorn, findDepositLoot, findCloseDepositBox,
                findSetCustQty, showBoxGrid
        );

        // Using the generic button distribution method
        distributeButtonsAlphabetically(depositBoxButtons, row1, row2, row3);

        // Adding rows to the VBox
        depositBoxVBox.getChildren().addAll(row1, row2, row3);
        depositBoxHBox.getChildren().add(depositBoxVBox);

        return depositBoxHBox;
    }

    public HBox initEquipmentTab() {
        equipmentHBox = new HBox();
        VBox equipmentVBox = new VBox(5); // Vertical box with spacing

        // Organize buttons into rows using the distributeButtonsAlphabetically method if available
        HBox row1 = new HBox(10);
        HBox row2 = new HBox(10);
        HBox row3 = new HBox(10);

        List<Button> equipmentButtons = Arrays.asList(
                itemAt, findHelm, findCape, findAmulet, findAmmo, findWeapon, findBody,
                findShield, findLegs, findGloves, findFeet, findRing
        );

        distributeButtonsAlphabetically(equipmentButtons, row1, row2, row3);

        // Add Spinner and TextField row if necessary
        HBox otherFields = new HBox(10);
        otherFields.getChildren().addAll(equipmentIntField, equipmentDropdown);

        // Adding rows and other UI components to the VBox
        equipmentVBox.getChildren().addAll(row1, row2, row3, otherFields);
        equipmentHBox.getChildren().add(equipmentVBox);

        return equipmentHBox;
    }

    public HBox initGameTab() {
        gameHBox = new HBox();
        VBox gameVBox = new VBox(5); // Vertical box with spacing

        // Assuming rows are already set up in setupGameTab or elsewhere
        HBox row1 = new HBox(10);
        HBox row2 = new HBox(10);
        HBox row3 = new HBox(10);
        HBox row4 = new HBox(10);

        List<Button> gameButtons = Arrays.asList(
                setZoom, setFairyRing, isWorldListOpen, findActionButton,
                isTapToDropOn, isSingleTapOn, enableTapToDrop, disableTapToDrop,
                enableSingleTap, disableSingleTap, findOption, isPlayersAroundOption, countPlayersAround
        );

        // Use the distribution method if available
        distributeButtonsAlphabetically(gameButtons, row1, row2, row3);
        row4.getChildren().addAll(gameZoomSpinner, gameFairyCodeField, gameDropdown);

        // Adding rows and other UI components to the VBox
        gameVBox.getChildren().addAll(row1, row2, row3, row4);
        gameHBox.getChildren().add(gameVBox);

        return gameHBox;
    }

    public HBox initGameTabsTab() {
        gameTabsHBox = new HBox();

        return gameTabsHBox;
    }

    public HBox initImageRecogTab() {
        imageRecogHBox = new HBox();
        VBox imageRecogVBox = new VBox(5); // Vertical box with spacing

        // Setup rows for buttons
        HBox row1 = new HBox(10, uploadImage, findBestResult, findAllResults); // Adjust spacing as necessary
        HBox row2 = new HBox(10, uploadToCanvas, findBestWithinRect, findAllWithinRect);
        HBox row3 = new HBox(10, irToggleSearch, findBestItem, findAllItems);
        HBox row4 = new HBox(10, irSearchThreshold, irSearchSpeed, irItemIDField);

        // Adding rows to the VBox
        imageRecogVBox.getChildren().addAll(row1, row2, row3, row4);

        // HBox to combine buttons/spinners and image view
        HBox mainContent = new HBox(10, imageRecogVBox, imageRecogView); // Adjust spacing as necessary

        // Adding main content to the HBox
        imageRecogHBox.getChildren().add(mainContent);

        return imageRecogHBox;
    }

    public HBox initInterfacesTab() {
        interfacesHBox = new HBox();

        return interfacesHBox;
    }

    public HBox initInventoryTab() {
        inventoryHBox = new HBox();
        VBox inventoryVBox = new VBox(5); // Vertical box with spacing

        // Setup rows for buttons
        HBox row1 = new HBox(10);
        HBox row2 = new HBox(10);
        HBox row3 = new HBox(10);
        HBox row4 = new HBox(10);

        List<Button> inventoryButtons = Arrays.asList(
                isInventFull, calcInventSlots, itemPosition, lastItemPosition,
                dropInventItems, showInventGrid, count, contains, tapAllItems,
                eat, getItemCenter, getItemStack
        );

        // Use the distribution method if available
        distributeButtonsAlphabetically(inventoryButtons, row1, row2, row3);

        // Adding the text field and spinner to row4
        row4.getChildren().addAll(inventoryItemIDField, inventoryThresholdSpinner);

        // Adding rows and other UI components to the VBox
        inventoryVBox.getChildren().addAll(row1, row2, row3, row4);
        inventoryHBox.getChildren().add(inventoryVBox);

        return inventoryHBox;
    }

    public HBox initLoginLogoutTab() {
        loginLogoutHBox = new HBox();
        VBox loginLogoutVBox = new VBox(5); // Vertical box with spacing

        // Setup rows for buttons
        HBox row1 = new HBox(10); // Adjust spacing as necessary
        HBox row2 = new HBox(10);
        HBox row3 = new HBox(10);

        List<Button> loginLogoutButtons = Arrays.asList(
                onLoginScreen, isLoggedOut, isLoggedIn, findTapToPlay,
                isCompassNorth, readLoginScreen, closestLoginMessage, findLogoutOption,
                findExitWorldSwitcher
        );

        // Use the distribution method if available
        distributeButtonsAlphabetically(loginLogoutButtons, row1, row2, row3);

        // Adding rows and other UI components to the VBox
        loginLogoutVBox.getChildren().addAll(row1, row2, row3);
        loginLogoutHBox.getChildren().add(loginLogoutVBox);

        return loginLogoutHBox;
    }

    public HBox initMagicTab() {
        magicHBox = new HBox();
        VBox magicVBox = new VBox(5); // Vertical box with spacing

        // Setup rows for buttons and dropdowns
        VBox castSpellVBox = new VBox(5);
        castSpellVBox.getChildren().addAll(castMagicSpell, spellsDropdown);

        HBox row1 = new HBox(10, castSpellVBox, isMagicCastable, isMagicInfoEnabled);

        // Adding rows and other UI components to the VBox
        magicVBox.getChildren().addAll(row1);
        magicHBox.getChildren().add(magicVBox);

        return magicHBox;
    }

    public HBox initObjectsTab() {
        objectsHBox = new HBox();

        return objectsHBox;
    }

    public HBox initOverlayTab() {
        overlayHBox = new HBox();
        VBox overlayVBox = new VBox(5); // Vertical box with spacing

        // Setup rows for buttons
        HBox row1 = new HBox(10); // Adjust spacing as necessary
        HBox row2 = new HBox(10);
        HBox row3 = new HBox(10);
        HBox row4 = new HBox(10, overlayDropdown, overlaysEPSSpinner, overlaysMinPtsSpinner);

        List<Button> overlayButtons = Arrays.asList(
                getGameCenter, findOverlays, findNearest,
                findSecondNearest, findFishingSpots
        );

        // Use the distribution method if available
        distributeButtonsAlphabetically(overlayButtons, row1, row2, row3);

        // Add button to 3rd row
        row3.getChildren().add(regionButton);

        // Adding rows and other UI components to the VBox
        overlayVBox.getChildren().addAll(row1, row2, row3, row4);
        overlayHBox.getChildren().add(overlayVBox);

        return overlayHBox;
    }

    public HBox initPlayerTab() {
        playerHBox = new HBox();
        VBox playerVBox = new VBox(5); // Vertical box with spacing

        // Setup rows for buttons
        HBox row1 = new HBox(10); // Adjust spacing as necessary
        HBox row2 = new HBox(10);
        HBox row3 = new HBox(10);

        List<Button> playerButtons = Arrays.asList(
                leveledUp, isRunEnabled, toggleRun, enableAutoRet,
                disableAutoRet, checkPixelShift, isAutoRetOn, getHP, getPray,
                getRun, getSpec
        );

        // Use the distribution method if available
        distributeButtonsAlphabetically(playerButtons, row1, row2, row3);

        // Adding rows and other UI components to the VBox
        playerVBox.getChildren().addAll(row1, row2, row3);
        playerHBox.getChildren().add(playerVBox);

        return playerHBox;
    }

    public HBox initStatsTab() {
        statsHBox = new HBox();
        VBox statsVBox = new VBox(5); // Vertical box with spacing

        // Setup rows for buttons
        HBox row1 = new HBox(10, getEffectiveLevelCF, getRealLevelCF, getTotalLevelCF);
        HBox row2 = new HBox(10, skillsDropdown); // Row for dropdown menu

        // Adding rows and other UI components to the VBox
        statsVBox.getChildren().addAll(row1, row2);
        statsHBox.getChildren().add(statsVBox);

        return statsHBox;
    }

    public HBox initWalkerTab() {
        walkerHBox = new HBox();
        VBox walkerVBox = new VBox(5); // Vertical box with spacing
        // Setup rows for buttons, dropdown, and text field
        HBox row1 = new HBox(10, setupMap, getPlayerPosition, isReachAble); // Adjust spacing as necessary
        HBox row2 = new HBox(10, step, walkTo, webwalkTo);
        HBox row3 = new HBox(10, mapUIBtn, drawTiles);
        HBox row4 = new HBox(10, walkerCoordField, chunksToLoad, planesToLoad);

        // Adding rows and other UI components to the VBox
        walkerVBox.getChildren().addAll(row1, row2, row3, row4);
        walkerHBox.getChildren().add(walkerVBox);

        return walkerHBox;
    }

    public HBox initXPBarTab() {
        xpBarHBox = new HBox();
        VBox xpbarVBox = new VBox(5); // Vertical box with spacing

        // Setup rows for buttons, dropdown, and text field
        HBox row1 = new HBox(10, readXP); // Adjust spacing as necessary
        HBox row2 = new HBox(10, test1, test2, test3); // Adjust spacing as necessary
        HBox row3 = new HBox(10, test4, test5); // Adjust spacing as necessary

        // Adding rows and other UI components to the VBox
        xpbarVBox.getChildren().addAll(row1, row2, row3);
        xpBarHBox.getChildren().add(xpbarVBox);

        return xpBarHBox;
    }

    private void initializeCanvasEvents() {
        gameCanvas.setOnMouseClicked(event -> {
            int x = (int) event.getX();
            int y = (int) event.getY();
            lastClickedPoint = new Point(x, y);

            if (regionButton.isSelected()) {
                handleRegionMapping(x, y);
            } else {
                coordinateLabel.setText("Coordinates: " + (int) event.getX() + ", " + (int) event.getY());
                logger.devLog("Click registered at coordinates: " + (int) event.getX() + ", " + (int) event.getY());

                if (passthroughButton.isSelected() && !getSelectedEmulator().isEmpty()) {
                    logger.devLog("Passthrough is active, clicking on emulator at: " + (int) event.getX() + ", " + (int) event.getY());
                    Point tapPoint = new Point((int) event.getX(), (int) event.getY());
                    clientAPI.tap(tapPoint, getSelectedEmulator());
                }

                Platform.runLater(() -> draw.clearCanvas());

                Platform.runLater(() -> {
                    draw.drawOnCanvas(event.getX() - 5, event.getY() - 5, event.getX() + 5, event.getY() + 5, javafx.scene.paint.Color.RED, ShapeType.LINE);
                    draw.drawOnCanvas(event.getX() + 5, event.getY() - 5, event.getX() - 5, event.getY() + 5, Color.RED, ShapeType.LINE);
                });

                Image currentImage = gameView.getImage();
                if (currentImage != null) {
                    // Convert the Image to a BufferedImage
                    BufferedImage bImage = SwingFXUtils.fromFXImage(currentImage, null);

                    // Ensure the coordinates are within the image bounds
                    if (event.getX() < bImage.getWidth() && event.getY() < bImage.getHeight()) {
                        // Get color data at the clicked pixel
                        int clr = bImage.getRGB((int) event.getX(), (int) event.getY());
                        int red = (clr & 0x00ff0000) >> 16;
                        int green = (clr & 0x0000ff00) >> 8;
                        int blue = clr & 0x000000ff;

                        // Convert RGB to HEX
                        String hex = String.format("#%02x%02x%02x", red, green, blue);

                        // Convert RGB to HSB
                        float[] hsb = java.awt.Color.RGBtoHSB(red, green, blue, null);
                        float hue = hsb[0] * 360; // Hue in degrees (0-360)
                        float saturation = hsb[1] * 100; // Saturation in percentage (0-100)

                        // Update the individual labels with properly formatted values
                        hexLabel.setText(String.format("Color: %s", hex));
                        hueLabel.setText(String.format(Locale.US, "Hue: %.2f ", hue)); // Rounded to two decimal places, no symbol
                        satLabel.setText(String.format(Locale.US, "Sat: %.2f ", saturation)); // Rounded to two decimal places, no symbol
                    }
                }
            }
        });
    }

    private void initializeClickEvents() {

        coordinateLabel.setOnMouseClicked(event -> copyToClipboard(coordinateLabel.getText().replace("Coordinates: ", "").trim()));
        hexLabel.setOnMouseClicked(event -> copyToClipboard(hexLabel.getText().replace("Color: ", "").trim()));
        hueLabel.setOnMouseClicked(event -> copyToClipboard(hueLabel.getText().replace("Hue: ", "").trim()));
        satLabel.setOnMouseClicked(event -> copyToClipboard(satLabel.getText().replace("Sat: : ", "").trim()));
        rectangleLabel.setOnMouseClicked(event -> copyToClipboard(rectangleLabel.getText()));

        importRectangleButton.setOnMouseClicked(event -> {
            // Clear canvas
            draw.clearCanvas();

            try {
                // Get clipboard content
                Clipboard clipboard = Clipboard.getSystemClipboard();
                String content = clipboard.getString();

                if (content == null || content.isEmpty()) {
                    throw new IllegalArgumentException("Clipboard is empty.");
                }

                // Trim and sanitize the clipboard content
                content = content.trim();

                // Remove optional prefixes like "new Rectangle" or "Rectangle"
                if (content.startsWith("new Rectangle")) {
                    content = content.substring("new Rectangle".length()).trim();
                } else if (content.startsWith("Rectangle")) {
                    content = content.substring("Rectangle".length()).trim();
                }

                // Remove trailing semicolon if present
                if (content.endsWith(";")) {
                    content = content.substring(0, content.length() - 1).trim();
                }

                // Ensure content starts with "(" and ends with ")"
                if (content.startsWith("(") && content.endsWith(")")) {
                    content = content.substring(1, content.length() - 1).trim();
                }

                // Split the coordinates by comma
                String[] coords = content.split(",");
                if (coords.length != 4) {
                    throw new IllegalArgumentException("Invalid format: Must contain four values (x, y, width, height).");
                }

                // Parse the coordinates
                int x = Integer.parseInt(coords[0].trim());
                int y = Integer.parseInt(coords[1].trim());
                int width = Integer.parseInt(coords[2].trim());
                int height = Integer.parseInt(coords[3].trim());

                // Create the rectangle
                Rectangle rectangle = new Rectangle(x, y, width, height);

                // Draw the rectangle
                draw.drawRectangle(rectangle, Color.BLUE);

            } catch (Exception e) {
                // Log error message to the dev log
                logger.devLog("Failed to parse rectangle from clipboard. Ensure the format matches one of the examples (trailing semicolon doesn't matter):\n" +
                        "  new Rectangle(x, y, width, height);  /  " +
                        "  Rectangle(x, y, width, height);\n" +
                        "  (x, y, width, height);  /  " +
                        "  x, y, width, height\n" +
                        "Clipboard content: \"" + Clipboard.getSystemClipboard().getString() + "\"");
            }
        });

        clearLogButton.setOnAction(event -> {
            logAreaInstance.getLogTextArea().clear();
        });

    }

    private void setupBankTab() {

        // Initialize Bank tab buttons
        containsButton = new Button("contains");
        withdrawItemButton = new Button("withdrawItem");
        withdrawItemButtonCF = new Button("withdrawItemCF");
        visualizePinButton = new Button("visualizePin");
        getItemStackButton = new Button("getItemStack");
        openTabButton = new Button("openTab");
        isTabSelectedButton = new Button("isTabSelected");
        getCurrentTabButton = new Button("getCurrentTab");
        isBankPinNeededButton = new Button("isBankPinNeeded");
        findBankTabButton = new Button("findBankTab");
        bankItemGridButton = new Button("bankItemGrid");
        closeBankButton = new Button("closeBank");
        isBankOpenButton = new Button("isBankOpen");
        findDynamicBankRegionButton = new Button("findDynamicBank");
        stepToBankButton = new Button("stepToBank");

        // Apply CSS class to buttons
        Button[] buttons = {
                containsButton, withdrawItemButton, withdrawItemButtonCF, getItemStackButton, openTabButton,
                isTabSelectedButton, getCurrentTabButton, isBankPinNeededButton,
                findBankTabButton, bankItemGridButton, closeBankButton, isBankOpenButton,
                findDynamicBankRegionButton, stepToBankButton,
                visualizePinButton
        };
        for (Button btn : buttons) {
            btn.getStyleClass().add("devUITab-button");
        }

        // Bank tab spinners/integers
        SpinnerValueFactory<Integer> valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 9, 0);
        tabNumberField.setValueFactory(valueFactory);
        tabNumberField.setPrefWidth(100); // Set preferred width
        tabNumberField.getStyleClass().add("devUITab-spinner");

        SpinnerValueFactory<Double> doubleValueFactory = new SpinnerValueFactory.DoubleSpinnerValueFactory(0.01, 1.00, 0.80, 0.01);
        bankThresholdSpinner.setValueFactory(doubleValueFactory);
        bankThresholdSpinner.setPrefWidth(100); // Set preferred width
        bankThresholdSpinner.getStyleClass().add("devUITab-spinner");

        // Bank tab text fields
        bankItemIDField.setPrefWidth(100); // Set preferred width
        bankItemIDField.setPromptText("Enter itemID");
        bankItemIDField.getStyleClass().add("devUITab-textfield");
        bankItemIDField.textProperty().addListener((observable, oldValue, newValue) -> { // Make sure only integers are accepted
            if (!newValue.matches("\\d*")) {
                bankItemIDField.setText(newValue.replaceAll("[^\\d]", ""));
            }
        });

        // Set up Bank tab button actions
        containsButton.setOnAction(event -> {
            String itemID = bankItemIDField.getText();
            logger.devLog("Bank contains " + itemID + ": " + bank.contains(itemID, bankThresholdSpinner.getValue(), getSelectedEmulator(), null));
        });
        withdrawItemButton.setOnAction(event -> bank.withdrawItem(bankItemIDField.getText(), false, bankThresholdSpinner.getValue(), getSelectedEmulator(), null));
        withdrawItemButtonCF.setOnAction(event -> bank.withdrawItem(bankItemIDField.getText(), false, bankThresholdSpinner.getValue(), getSelectedEmulator(), java.awt.Color.decode(hexColors.getText())));
        visualizePinButton.setOnAction(event -> {
            List<Map.Entry<Integer, Rectangle>> digitTileAssociations = bank.findBankPinNumbers(getSelectedEmulator());

            // Draw each rectangle on the canvas and its corresponding digit
            for (Map.Entry<Integer, Rectangle> entry : digitTileAssociations) {
                int digit = entry.getKey();
                Rectangle tile = entry.getValue();

                // Draw the tile
                draw.drawRectangle(tile, Color.BLUE);

                // Calculate the center position of the tile
                double centerX = tile.getCenterX();
                double centerY = tile.getCenterY();

                // Draw the digit in the center of the tile
                draw.drawTextOnCanvas(centerX, centerY, String.valueOf(digit), Color.GREEN);

                // Print out the rectangle's coordinates, size, and the digit
                System.out.println("Rectangle at (x: " + tile.x + ", y: " + tile.y +
                        "), Width: " + tile.width + ", Height: " + tile.height + " contains digit: " + digit);
            }
        });
        getItemStackButton.setOnAction(event -> {
            String itemID = bankItemIDField.getText();
            logger.devLog("Bank contains " + bank.getItemStack(getSelectedEmulator(), Integer.parseInt(itemID), null) + " of item: " + itemID);
        });
        openTabButton.setOnAction(event -> {
            bank.openTab(tabNumberField.getValue(), getSelectedEmulator());
            logger.devLog("Opened bank tab: " + tabNumberField.getValue());
        });
        isTabSelectedButton.setOnAction(event -> logger.devLog("Bank tab " + tabNumberField.getValue() + " is selected: " + bank.isTabSelected(getSelectedEmulator(), tabNumberField.getValue())));
        getCurrentTabButton.setOnAction(event -> logger.devLog("Current selected bank tab: " + bank.getCurrentTab(getSelectedEmulator())));
        isBankPinNeededButton.setOnAction(event -> logger.devLog("Bank pin needed: " + bank.isBankPinNeeded(getSelectedEmulator())));
        findBankTabButton.setOnAction(event -> {
            draw.drawRectangle(bank.findBankTab(tabNumberField.getValue(), getSelectedEmulator()), Color.GREEN);
            logger.devLog("Tried to locate bank tab: " + tabNumberField.getValue() + " and drew it on the canvas if found.");
        });
        bankItemGridButton.setOnAction(event -> {
            Rectangle[] rectangles = bank.bankItemGrid(getSelectedEmulator());

            draw.clearCanvas();

            for (Rectangle rectangle : rectangles) {
                draw.drawRectangle(rectangle, Color.GREEN);
            }

            logger.devLog("Located the bank grid, and drew it on the canvas.");
        });
        closeBankButton.setOnAction(event -> {
            bank.closeBank(getSelectedEmulator());
            logger.devLog("Closed the bank!");
        });

        isBankOpenButton.setOnAction(event -> logger.devLog("Bank open is: " + bank.isBankOpen(getSelectedEmulator())));
        findDynamicBankRegionButton.setOnAction(event -> {
            String bankArea = bank.findDynamicBankRegion(getSelectedEmulator(), 0.3);
            logger.devLog("Dynamic bank region is: " + bankArea);
        });

        stepToBankButton.setOnAction(event -> {
            walker.setup(getSelectedEmulator(), new MapChunk(new String[]{"51-49", "50-49", "40-52", "40-51", "41-52", "41-51", "38-48", "43-53", "44-53", "19-55", "19-56", "49-53", "45-51", "46-51", "45-50", "46-50", "48-50", "48-54", "48-53", "47-52", "46-52", "45-52", "19-58", "48-56", "49-56", "49-54", "27-56", "27-55", "26-54", "26-53", "27-54", "27-53", "23-53", "22-59", "22-60", "23-58", "24-58", "42-54", "43-54", "44-54", "23-56", "22-56", "22-55", "23-55", "50-53", "51-53", "25-61", "24-54", "25-54", "40-48", "41-48"}, "0", "1"));
            String dynamicBank = bank.findDynamicBank(getSelectedEmulator());
            bank.stepToBank(getSelectedEmulator(), dynamicBank);
            logger.devLog("Tried to locate dynamic bank, and step to it.");
        });
    }

    private void setupChatboxTab() {

        // Initialize Chatbox tab buttons
        findChatboxMenuButton = new Button("Find Chatbox Menu");
        makeOptionButton = new Button("Make Option");
        visualizeMakeOptionsButton = new Button("Visualize Make Options");
        readDigitsInAreaButton = new Button("Read black digits CF");
        isSelectedMake1Button = new Button("Is Selected Make 1");
        isSelectedMake5Button = new Button("Is Selected Make 5");
        isSelectedMake10Button = new Button("Is Selected Make 10");
        isSelectedMakeAllButton = new Button("Is Selected Make All");
        findMaxOptionsButton = new Button("Find Max Options");
        readChatboxButton = new Button("Read Chatbox");
        readLastLineButton = new Button("Read Last Line");
        ocrAreaButton = new Button("OCR Area");


        // Apply CSS class to buttons
        Button[] buttons = {
                findChatboxMenuButton, makeOptionButton, visualizeMakeOptionsButton, isSelectedMake1Button,
                isSelectedMake5Button, isSelectedMake10Button, isSelectedMakeAllButton, findMaxOptionsButton,
                readChatboxButton, readLastLineButton, ocrAreaButton, readDigitsInAreaButton
        };
        for (Button btn : buttons) {
            btn.getStyleClass().add("devUITab-button");
        }

        // Chatbox tab spinners/integers
        SpinnerValueFactory<Integer> valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 15, 1);
        makeOptionSpinner.setValueFactory(valueFactory);
        makeOptionSpinner.setPrefWidth(100); // Set preferred width
        makeOptionSpinner.getStyleClass().add("devUITab-spinner");

        // Chatbox tab text fields

        // Set up Chatbox tab button actions
        findChatboxMenuButton.setOnAction(event -> {
            draw.clearCanvas();
            draw.drawRectangle(chatbox.findChatboxMenu(getSelectedEmulator()), Color.GREEN);
        });

        makeOptionButton.setOnAction(event -> chatbox.makeOption(makeOptionSpinner.getValue(), getSelectedEmulator()));

        visualizeMakeOptionsButton.setOnAction(event -> {
            List<MatchedRectangle> rectangles = chatbox.visualizeOptions(getSelectedEmulator());
            draw.clearCanvas();

            // Iterate over each MatchedRectangle in the list
            for (MatchedRectangle rectangle : rectangles) {
                draw.drawRectangle(rectangle, Color.GREEN);
            }

            logger.devLog("Located the options grid, and drew it on the canvas.");
        });

        readDigitsInAreaButton.setOnAction(event -> {
            List<java.awt.Color> black = List.of(
                    java.awt.Color.decode("#000001")
            );

            // Get the text from the label, which should be in the format "new Rectangle(x, y, width, height);"
            String rectText = rectangleLabel.getText();

            // Extract the numbers from the string
            String[] parts = rectText.replaceAll("[^0-9,]", "").split(",");
            int x = Integer.parseInt(parts[0].trim());
            int y = Integer.parseInt(parts[1].trim());
            int width = Integer.parseInt(parts[2].trim());
            int height = Integer.parseInt(parts[3].trim());

            // Create the Rectangle
            Rectangle rectangle = new java.awt.Rectangle(x, y, width, height);

            int results = gameOCR.readDigitsInArea(rectangle, black, getSelectedEmulator());

            logger.devLog("CF OCR results: " + results);
        });

        isSelectedMake1Button.setOnAction(event -> logger.devLog("Make1 is selected: " + chatbox.isSelectedMake1(getSelectedEmulator())));

        isSelectedMake5Button.setOnAction(event -> logger.devLog("Make5 is selected: " + chatbox.isSelectedMake5(getSelectedEmulator())));

        isSelectedMake10Button.setOnAction(event -> logger.devLog("Make10 is selected: " + chatbox.isSelectedMake10(getSelectedEmulator())));

        isSelectedMakeAllButton.setOnAction(event -> logger.devLog("MakeAll is selected: " + chatbox.isSelectedMakeAll(getSelectedEmulator())));

        findMaxOptionsButton.setOnAction(event -> {
            Rectangle chatboxRect = chatbox.findChatboxMenu(getSelectedEmulator());
            Rect ocvRect = new Rect(chatboxRect.x, chatboxRect.y, chatboxRect.width, chatboxRect.height);
            logger.devLog("Max makeOptions found: " + chatbox.findMaxOptionAmount(ocvRect, getSelectedEmulator()));
        });

        readChatboxButton.setOnAction(event -> logger.devLog("OCR results for chatbox: " + gameOCR.readChatboxArea(getSelectedEmulator(), new Rectangle(35, 32, 494, 88))));

        readLastLineButton.setOnAction(event -> logger.devLog("OCR results for last chatbox line: " + gameOCR.readChatboxArea(getSelectedEmulator(), new Rectangle(35, 104, 361, 14))));

        ocrAreaButton.setOnAction(event -> {
            // Fetch the text from rectangleLabel
            String rectangleText = rectangleLabel.getText();

            try {
                // Attempt to parse the rectangle dimensions from the label text
                // Expected format: "new Rectangle(x, y, width, height);"
                // Example valid text: "new Rectangle(35, 32, 494, 88);"
                String cleanText = rectangleText.replace("new Rectangle(", "").replace(");", "");
                String[] parts = cleanText.split(", ");

                if (parts.length != 4) {
                    throw new IllegalArgumentException("Rectangle dimensions should contain exactly four integer values.");
                }

                int x = Integer.parseInt(parts[0].trim());
                int y = Integer.parseInt(parts[1].trim());
                int width = Integer.parseInt(parts[2].trim());
                int height = Integer.parseInt(parts[3].trim());

                // Create the rectangle with parsed dimensions
                Rectangle rectangle = new Rectangle(x, y, width, height);

                // Use the rectangle in the chatbox.readChatboxArea method
                String ocrResults = gameOCR.readChatboxArea(getSelectedEmulator(), rectangle);
                logger.devLog("OCR results: " + ocrResults);
            } catch (NumberFormatException e) {
                // Handle case where string parts are not all integers
                showDialog("Error", "Error parsing rectangle dimensions: Ensure all values are integers.", Alert.AlertType.INFORMATION);
            } catch (IllegalArgumentException e) {
                // Handle any other issues with the input format
                showDialog("Error", e.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }

    private void setupClientTab() {

        // Initialize Client tab buttons
        passthroughButton.setSelected(false);
        passthroughButton.setText("Enable click passthrough");
        tempTestButton = new Button("Temp test button");

        // Apply CSS class to buttons, text fields, and spinner
        Button[] buttons = {tempTestButton};
        for (Button btn : buttons) {
            btn.getStyleClass().add("devUITab-button");
        }

        passthroughButton.getStyleClass().add("devUITab-button");

        // Client tab spinners/integers

        // Client tab text fields

        // Set up Client tab button actions

        // Set up listeners
        passthroughButton.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                passthroughButton.setText("Disable click passthrough");
            } else {
                passthroughButton.setText("Enable click passthrough");
            }
        });

        tempTestButton.setOnAction(event -> {
            logger.devLog("Inventory contains unf kwuarm potion: " + inventory.contains(getSelectedEmulator(), 105, 0.7, java.awt.Color.decode("#ada6a5"), java.awt.Color.decode("#cecccc")));
        });

    }

    private void setupColorFinderTab() {
        // Initialize Color Finder tab buttons
        findHexColors = new Button("Find Hex Colors");
        scanAreaForColor = new Button("Scan Area For Color");
        isColorAtPoint = new Button("Is Color At Point");
        isAnyColorInRect = new Button("Is Any Color In Rect");
        isColorInRect = new Button("Is Color In Rect");
        isPixelColor = new Button("Is Pixel Color");
        scanRectAndDraw = new Button("Scan rect and draw");
        drawLines = new ToggleButton("Enable drawing lines");
        findAgilityColors = new Button("Find agility colors");
        findFishingColors = new Button("Find fishing colors");
        findItemColors = new Button("Find item colors");
        findNPC = new Button("Find NPC");
        readClickMenu = new Button("Read LP Menu");
        drawLines.setSelected(false);
        cfToggleSearch.setSelected(false);
        cfToggleSearch.setText("Start search");

        // Initialize text fields
        hexColors.setPromptText("Enter hex colors (comma-separated)");
        hexColors.setMinWidth(300);
        excludedHexColors.setPromptText("Enter excluded hex colors (comma-separated)");
        excludedHexColors.setMinWidth(300);
        findString.setPromptText("String to find");
        findString.setMinWidth(150);

        // Initialize spinner
        SpinnerValueFactory<Integer> valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 100, 5);
        cfToleranceSpinner.setValueFactory(valueFactory);
        cfToleranceSpinner.setPrefWidth(100); // Set preferred width

        // Apply CSS class to buttons, text fields, and spinner
        Button[] buttons = {findHexColors, scanAreaForColor, isColorAtPoint, isAnyColorInRect, isColorInRect, isPixelColor, scanRectAndDraw, findAgilityColors, findFishingColors, findItemColors, findNPC, readClickMenu};
        for (Button btn : buttons) {
            btn.getStyleClass().add("devUITab-button");
        }
        drawLines.getStyleClass().add("devUITab-button");
        hexColors.getStyleClass().add("devUITab-textfield");
        excludedHexColors.getStyleClass().add("devUITab-textfield");
        findString.getStyleClass().add("devUITab-textfield");
        cfToleranceSpinner.getStyleClass().add("devUITab-spinner");
        cfToggleSearch.getStyleClass().add("devUITab-button");

        // Set up Color Finder tab button actions with multiline format
        findHexColors.setOnAction(event -> {
            // Retrieve the hex color from the text field and convert it to a java.awt.Color object
            String hexColor = hexColors.getText();
            java.awt.Color awtColor = java.awt.Color.decode(hexColor);

            // Retrieve the coordinates from the text field and convert them to a Point object
            String coordinateText = coordinateLabel.getText().replace("Coordinates: ", "").trim();
            String[] coordinates = coordinateText.split(",");
            int x = Integer.parseInt(coordinates[0].trim());
            int y = Integer.parseInt(coordinates[1].trim());
            Point point = new Point(x, y);

            // Use the converted Color and Point in the colorFinder method
            List<Point> points = colorFinder.findColorAtPosition(getSelectedEmulator(), awtColor, point, point, cfToleranceSpinner.getValue());

            // Log a message stating the color for each point
            for (Point foundPoint : points) {
                logger.devLog(String.format("Color at point (%d, %d): %s", foundPoint.x, foundPoint.y, awtColor));
            }
        });
        scanAreaForColor.setOnAction(event -> {
            Image image = gameView.getImage();
            if (image != null) {
                // Fetch the text from rectangleLabel and parse it to create a Rectangle
                String rectangleText = rectangleLabel.getText();

                try {
                    String cleanText = rectangleText.replace("new Rectangle(", "").replace(");", "");
                    String[] parts = cleanText.split(", ");

                    if (parts.length != 4) {
                        throw new IllegalArgumentException("Rectangle dimensions should contain exactly four integer values.");
                    }

                    int x = Integer.parseInt(parts[0].trim());
                    int y = Integer.parseInt(parts[1].trim());
                    int width = Integer.parseInt(parts[2].trim());
                    int height = Integer.parseInt(parts[3].trim());

                    // Create the rectangle with parsed dimensions

                    Task<Set<String>> imageProcessingTask = new Task<>() {
                        @Override
                        protected Set<String> call() {
                            PixelReader pixelReader = image.getPixelReader();
                            Set<Color> uniqueColors = new HashSet<>();
                            double tolerance = cfToleranceSpinner.getValue() * 2.55;

                            // Reading pixels within the search area
                            for (int rectY = y; rectY < y + height; rectY++) {
                                for (int rectX = x; rectX < x + width; rectX++) {
                                    Color color = pixelReader.getColor(rectX, rectY);
                                    if (!isSimilarColorExists(uniqueColors, color, tolerance)) {
                                        uniqueColors.add(color);
                                    }
                                }
                            }

                            // Convert unique Color objects to hex strings
                            Set<String> uniqueHexColors = new HashSet<>();
                            for (Color uniqueColor : uniqueColors) {
                                String hexColor = String.format("#%02X%02X%02X",
                                        (int) (uniqueColor.getRed() * 255),
                                        (int) (uniqueColor.getGreen() * 255),
                                        (int) (uniqueColor.getBlue() * 255)
                                );
                                uniqueHexColors.add(hexColor);
                            }
                            return uniqueHexColors;
                        }
                    };

                    imageProcessingTask.setOnSucceeded(event1 -> {
                        Set<String> uniqueHexColors = imageProcessingTask.getValue();
                        StringBuilder hexColorBuilder = new StringBuilder();
                        for (String hex : uniqueHexColors) {
                            hexColorBuilder.append(hex).append(", ");
                        }

                        // Remove trailing comma and space, if present
                        String result = hexColorBuilder.length() > 0 ? hexColorBuilder.substring(0, hexColorBuilder.length() - 2) : "";

                        // Log the found hex colors
                        logger.devLog("Hex colors found in the rectangle area: " + result);
                    });

                    // Handle failed task
                    imageProcessingTask.setOnFailed(workerStateEvent -> {
                        Throwable exception = imageProcessingTask.getException();
                        logger.devLog("Task failed with exception: " + exception.getMessage());
                    });

                    new Thread(imageProcessingTask).start();
                } catch (NumberFormatException e) {
                    showDialog("Error", "Error parsing rectangle dimensions: Ensure all values are integers.", Alert.AlertType.INFORMATION);
                } catch (IllegalArgumentException e) {
                    showDialog("Error", e.getMessage(), Alert.AlertType.ERROR);
                }
            } else {
                logger.devLog("No image available in gameView.");
            }
        });
        isColorAtPoint.setOnAction(event -> {
            // Retrieve the hex color from the text field and convert it to a java.awt.Color object
            String hexColor = hexColors.getText();
            java.awt.Color awtColor = java.awt.Color.decode(hexColor);

            // Retrieve the coordinates from the text field and convert them to a Point object
            String coordinateText = coordinateLabel.getText().replace("Coordinates: ", "").trim();
            String[] coordinates = coordinateText.split(",");
            int x = Integer.parseInt(coordinates[0].trim());
            int y = Integer.parseInt(coordinates[1].trim());
            Point point = new Point(x, y);

            logger.devLog("Color " + hexColor + " found at: " + point + " is: " + colorFinder.isColorAtPoint(getSelectedEmulator(), awtColor, point, cfToleranceSpinner.getValue()));
        });
        isAnyColorInRect.setOnAction(event -> {
            // Fetch the text from rectangleLabel
            String rectangleText = rectangleLabel.getText();

            try {
                // Attempt to parse the rectangle dimensions from the label text
                // Expected format: "new Rectangle(x, y, width, height);"
                String cleanText = rectangleText.replace("new Rectangle(", "").replace(");", "");
                String[] parts = cleanText.split(", ");

                if (parts.length != 4) {
                    throw new IllegalArgumentException("Rectangle dimensions should contain exactly four integer values.");
                }

                int x = Integer.parseInt(parts[0].trim());
                int y = Integer.parseInt(parts[1].trim());
                int width = Integer.parseInt(parts[2].trim());
                int height = Integer.parseInt(parts[3].trim());

                // Create the rectangle with parsed dimensions
                Rectangle searchArea = new Rectangle(x, y, width, height);

                // Parse the hex colors from the hexColors text field
                String hexColorsText = hexColors.getText();
                String[] hexColorsArray = hexColorsText.split(",");
                List<java.awt.Color> colorList = new ArrayList<>();
                for (String hex : hexColorsArray) {
                    colorList.add(java.awt.Color.decode(hex.trim()));
                }

                // Use the rectangle and list of colors in the colorFinder.isAnyColorInRect method
                boolean colorFound = colorFinder.isAnyColorInRect(getSelectedEmulator(), colorList, searchArea, cfToleranceSpinner.getValue());

                // Log the result along with the colors
                StringBuilder colorLog = new StringBuilder("Colors checked: ");
                for (java.awt.Color color : colorList) {
                    colorLog.append(String.format("#%02x%02x%02x ", color.getRed(), color.getGreen(), color.getBlue()));
                }
                colorLog.append(" | Color found in rectangle: ").append(colorFound);
                logger.devLog(colorLog.toString());

            } catch (NumberFormatException e) {
                // Handle case where string parts are not all integers
                showDialog("Error", "Error parsing rectangle dimensions: Ensure all values are integers.", Alert.AlertType.INFORMATION);
            } catch (IllegalArgumentException e) {
                // Handle any other issues with the input format
                showDialog("Error", e.getMessage(), Alert.AlertType.ERROR);
            }
        });
        findNPC.setOnAction(event -> {
            draw.clearCanvas();
            // Parse the hex colors from the hexColors text field
            String hexColorsText = hexColors.getText();
            String[] hexColorsArray = hexColorsText.split(",");
            List<java.awt.Color> colorList = new ArrayList<>();
            for (String hex : hexColorsArray) {
                colorList.add(java.awt.Color.decode(hex.trim()));
            }

            List<Rectangle> foundNPCs = colorFinder.findNPCs(getSelectedEmulator(), colorList, cfToleranceSpinner.getValue(), 12, 10);
            draw.drawRectangles(foundNPCs, Color.GREEN);
        });
        readClickMenu.setOnAction(event -> {
            // Attempt to parse the rectangle dimensions from the label text
            // Expected format: "new Rectangle(x, y, width, height);"
            String cleanText = rectangleLabel.getText().replace("new Rectangle(", "").replace(");", "");
            String[] parts = cleanText.split(", ");

            if (parts.length != 4) {
                throw new IllegalArgumentException("Rectangle dimensions should contain exactly four integer values.");
            }

            int x = Integer.parseInt(parts[0].trim());
            int y = Integer.parseInt(parts[1].trim());
            int width = Integer.parseInt(parts[2].trim());
            int height = Integer.parseInt(parts[3].trim());

            // Create the rectangle with parsed dimensions
            Rectangle searchArea = new Rectangle(x, y, width, height);

            // Retrieve string to find
            String stringToFind = findString.getText();

            // Find the menu options
            Rectangle menuOption = digitReader.findString(10, searchArea, whiteAndYellow, null, stringToFind, getSelectedEmulator());

            // Tap the option if found
            if (menuOption != null) {
                draw.clearCanvas();
                draw.drawRectangle(menuOption, stringToFind, Color.BLUEVIOLET);
            } else {
                logger.devLog("Menu entry " + stringToFind + " could not be located.");
            }
        });
        isColorInRect.setOnAction(event -> {
            // Fetch the text from rectangleLabel
            String rectangleText = rectangleLabel.getText();

            try {
                // Attempt to parse the rectangle dimensions from the label text
                // Expected format: "new Rectangle(x, y, width, height);"
                String cleanText = rectangleText.replace("new Rectangle(", "").replace(");", "");
                String[] parts = cleanText.split(", ");

                if (parts.length != 4) {
                    throw new IllegalArgumentException("Rectangle dimensions should contain exactly four integer values.");
                }

                int x = Integer.parseInt(parts[0].trim());
                int y = Integer.parseInt(parts[1].trim());
                int width = Integer.parseInt(parts[2].trim());
                int height = Integer.parseInt(parts[3].trim());

                // Create the rectangle with parsed dimensions
                Rectangle searchArea = new Rectangle(x, y, width, height);

                // Parse the single hex color from the hexColors text field
                String hexColorText = hexColors.getText().trim();
                java.awt.Color targetColor = java.awt.Color.decode(hexColorText);

                // Use the rectangle and the single color in the colorFinder.isColorInRect method
                boolean colorFound = colorFinder.isColorInRect(getSelectedEmulator(), targetColor, searchArea, cfToleranceSpinner.getValue());

                // Log the result along with the color
                String colorLog = String.format("Color checked: #%02x%02x%02x | Color found in rectangle: %b",
                        targetColor.getRed(), targetColor.getGreen(), targetColor.getBlue(), colorFound);
                logger.devLog(colorLog);

            } catch (NumberFormatException e) {
                // Handle case where string parts are not all integers
                showDialog("Error", "Error parsing rectangle dimensions: Ensure all values are integers.", Alert.AlertType.INFORMATION);
            } catch (IllegalArgumentException e) {
                // Handle any other issues with the input format
                showDialog("Error", e.getMessage(), Alert.AlertType.ERROR);
            }
        });
        isPixelColor.setOnAction(event -> {
            try {
                // Fetch the text from the coordinateLabel and remove the "Coordinates: " prefix
                String coordinateText = coordinateLabel.getText().replace("Coordinates: ", "").trim();
                String[] coordinates = coordinateText.split(",");
                if (coordinates.length != 2) {
                    throw new IllegalArgumentException("Coordinate dimensions should contain exactly two integer values.");
                }

                int x = Integer.parseInt(coordinates[0].trim());
                int y = Integer.parseInt(coordinates[1].trim());
                Point pixel = new Point(x, y);

                // Parse the single hex color from the hexColors text field
                String hexColorText = hexColors.getText().trim();
                java.awt.Color targetColor = java.awt.Color.decode(hexColorText);

                // Get the tolerance value from the spinner
                int tolerance = cfToleranceSpinner.getValue();

                // Use the point and the single color in the colorFinder.isPixelColor method
                boolean colorMatches = colorFinder.isPixelColor(getSelectedEmulator(), pixel, targetColor, tolerance);

                // Log the result along with the color and pixel coordinates
                String colorLog = String.format("Pixel checked at (%d, %d) with color #%02x%02x%02x | Color matches: %b",
                        pixel.x, pixel.y, targetColor.getRed(), targetColor.getGreen(), targetColor.getBlue(), colorMatches);
                logger.devLog(colorLog);

            } catch (NumberFormatException e) {
                // Handle case where string parts are not all integers
                showDialog("Error", "Error parsing coordinate dimensions: Ensure all values are integers.", Alert.AlertType.INFORMATION);
            } catch (IllegalArgumentException e) {
                // Handle any other issues with the input format
                showDialog("Error", e.getMessage(), Alert.AlertType.ERROR);
            }
        });
        scanRectAndDraw.setOnAction(event -> {
            // Fetch the text from the rectangleLabel and parse it to create a Rectangle
            String rectangleText = rectangleLabel.getText();

            // clear canvas
            draw.clearCanvas();

            try {
                String cleanText = rectangleText.replace("new Rectangle(", "").replace(");", "");
                String[] parts = cleanText.split(", ");

                if (parts.length != 4) {
                    throw new IllegalArgumentException("Rectangle dimensions should contain exactly four integer values.");
                }

                int x = Integer.parseInt(parts[0].trim());
                int y = Integer.parseInt(parts[1].trim());
                int width = Integer.parseInt(parts[2].trim());
                int height = Integer.parseInt(parts[3].trim());

                // Create the rectangle with parsed dimensions

                // Create two points: upper left and bottom right of the rectangle
                Point point1 = new Point(x, y);
                Point point2 = new Point(x + width, y + height);

                // Parse the hex colors from the hexColors text field
                String hexColorsText = hexColors.getText();
                String[] hexColorsArray = hexColorsText.split(",");

                // Prepare a list to collect all found points
                List<Point> allFoundPoints = new ArrayList<>();

                for (String hex : hexColorsArray) {
                    java.awt.Color awtColor = java.awt.Color.decode(hex.trim());

                    // Find the color at the position
                    List<Point> foundPoints = colorFinder.findColorAtPosition(
                            getSelectedEmulator(), awtColor, point1, point2, cfToleranceSpinner.getValue());

                    // Add all found points to the list
                    allFoundPoints.addAll(foundPoints);
                }

                // Draw each point to the canvas
                draw.drawPoints(allFoundPoints, Color.GREEN, drawLines.isSelected());

                // Log the result
                logger.devLog("Found and drawn points: " + allFoundPoints.size());

            } catch (NumberFormatException e) {
                showDialog("Error", "Error parsing rectangle dimensions: Ensure all values are integers.", Alert.AlertType.INFORMATION);
            } catch (IllegalArgumentException e) {
                showDialog("Error", e.getMessage(), Alert.AlertType.ERROR);
            }
        });

        findAgilityColors.setOnAction(event -> {
            // Fetch the text from the rectangleLabel and parse it to create a Rectangle
            String rectangleText = rectangleLabel.getText();

            // clear canvas
            draw.clearCanvas();

            try {
                String cleanText = rectangleText.replace("new Rectangle(", "").replace(");", "");
                String[] parts = cleanText.split(", ");

                if (parts.length != 4) {
                    throw new IllegalArgumentException("Rectangle dimensions should contain exactly four integer values.");
                }

                int x = Integer.parseInt(parts[0].trim());
                int y = Integer.parseInt(parts[1].trim());
                int width = Integer.parseInt(parts[2].trim());
                int height = Integer.parseInt(parts[3].trim());

                // Create the rectangle with parsed dimensions

                // Create two points: upper left and bottom right of the rectangle
                Point point1 = new Point(x, y);
                Point point2 = new Point(x + width, y + height);

                // Prepare a list to collect all found points
                List<Point> allFoundPoints = new ArrayList<>();

                for (java.awt.Color color : colorFinder.greenOverlay) {
                    // Find the color at the position
                    List<Point> foundPoints = colorFinder.findColorAtPosition(
                            getSelectedEmulator(), color, point1, point2, cfToleranceSpinner.getValue());

                    // Add all found points to the list
                    allFoundPoints.addAll(foundPoints);
                }

                // Draw each point to the canvas
                draw.drawPoints(allFoundPoints, Color.HOTPINK, drawLines.isSelected());

                // Log the result
                logger.devLog("Found and drawn points: " + allFoundPoints.size());

            } catch (NumberFormatException e) {
                showDialog("Error", "Error parsing rectangle dimensions: Ensure all values are integers.", Alert.AlertType.INFORMATION);
            } catch (IllegalArgumentException e) {
                showDialog("Error", e.getMessage(), Alert.AlertType.ERROR);
            }
        });

        findFishingColors.setOnAction(event -> {
            // Fetch the text from the rectangleLabel and parse it to create a Rectangle
            String rectangleText = rectangleLabel.getText();

            // clear canvas
            draw.clearCanvas();

            try {
                String cleanText = rectangleText.replace("new Rectangle(", "").replace(");", "");
                String[] parts = cleanText.split(", ");

                if (parts.length != 4) {
                    throw new IllegalArgumentException("Rectangle dimensions should contain exactly four integer values.");
                }

                int x = Integer.parseInt(parts[0].trim());
                int y = Integer.parseInt(parts[1].trim());
                int width = Integer.parseInt(parts[2].trim());
                int height = Integer.parseInt(parts[3].trim());

                // Create the rectangle with parsed dimensions

                // Create two points: upper left and bottom right of the rectangle
                Point point1 = new Point(x, y);
                Point point2 = new Point(x + width, y + height);

                // Prepare a list to collect all found points
                List<Point> allFoundPoints = new ArrayList<>();

                for (java.awt.Color color : colorFinder.blueOverlay) {
                    // Find the color at the position
                    List<Point> foundPoints = colorFinder.findColorAtPosition(
                            getSelectedEmulator(), color, point1, point2, cfToleranceSpinner.getValue());

                    // Add all found points to the list
                    allFoundPoints.addAll(foundPoints);
                }

                // Draw each point to the canvas
                draw.drawPoints(allFoundPoints, Color.HOTPINK, drawLines.isSelected());

                // Log the result
                logger.devLog("Found and drawn points: " + allFoundPoints.size());

            } catch (NumberFormatException e) {
                showDialog("Error", "Error parsing rectangle dimensions: Ensure all values are integers.", Alert.AlertType.INFORMATION);
            } catch (IllegalArgumentException e) {
                showDialog("Error", e.getMessage(), Alert.AlertType.ERROR);
            }
        });

        findItemColors.setOnAction(event -> {
            // Fetch the text from the rectangleLabel and parse it to create a Rectangle
            String rectangleText = rectangleLabel.getText();

            // clear canvas
            draw.clearCanvas();

            try {
                String cleanText = rectangleText.replace("new Rectangle(", "").replace(");", "");
                String[] parts = cleanText.split(", ");

                if (parts.length != 4) {
                    throw new IllegalArgumentException("Rectangle dimensions should contain exactly four integer values.");
                }

                int x = Integer.parseInt(parts[0].trim());
                int y = Integer.parseInt(parts[1].trim());
                int width = Integer.parseInt(parts[2].trim());
                int height = Integer.parseInt(parts[3].trim());

                // Create the rectangle with parsed dimensions

                // Create two points: upper left and bottom right of the rectangle
                Point point1 = new Point(x, y);
                Point point2 = new Point(x + width, y + height);

                // Prepare a list to collect all found points
                List<Point> allFoundPoints = new ArrayList<>();

                for (java.awt.Color color : colorFinder.redOverlay) {
                    // Find the color at the position
                    List<Point> foundPoints = colorFinder.findColorAtPosition(
                            getSelectedEmulator(), color, point1, point2, cfToleranceSpinner.getValue());

                    // Add all found points to the list
                    allFoundPoints.addAll(foundPoints);
                }

                // Draw each point to the canvas
                draw.drawPoints(allFoundPoints, Color.HOTPINK, drawLines.isSelected());

                // Log the result
                logger.devLog("Found and drawn points: " + allFoundPoints.size());

            } catch (NumberFormatException e) {
                showDialog("Error", "Error parsing rectangle dimensions: Ensure all values are integers.", Alert.AlertType.INFORMATION);
            } catch (IllegalArgumentException e) {
                showDialog("Error", e.getMessage(), Alert.AlertType.ERROR);
            }
        });

        // Set up listeners
        drawLines.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                drawLines.setText("Disable drawing lines");
            } else {
                drawLines.setText("Enable drawing lines");
            }
        });

        cfToggleSearch.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                startColorSearch();
            } else {
                stopColorSearch();
            }
        });
    }

    private void setupDepositboxTab() {
        // Initialize Depositbox tab buttons
        findQuantity1 = new Button("Find Quantity 1");
        findQuantity5 = new Button("Find Quantity 5");
        findQuantity10 = new Button("Find Quantity 10");
        findQuantityCustom = new Button("Find Quantity Custom");
        findQuantityAll = new Button("Find Quantity All");
        findDepositInventory = new Button("Find Deposit Inventory");
        findDepositWorn = new Button("Find Deposit Worn");
        findDepositLoot = new Button("Find Deposit Loot");
        findCloseDepositBox = new Button("Find Close Deposit Box");
        findSetCustQty = new Button("Find Set Custom Quantity");
        showBoxGrid = new Button("Show Box Grid");

        // Apply CSS class to buttons
        Button[] buttons = {
                findQuantity1, findQuantity5, findQuantity10, findQuantityCustom,
                findQuantityAll, findDepositInventory, findDepositWorn, findDepositLoot,
                findCloseDepositBox, findSetCustQty, showBoxGrid
        };
        for (Button btn : buttons) {
            btn.getStyleClass().add("devUITab-button");
        }

        // Set up Depositbox tab button actions
        findQuantity1.setOnAction(event -> {
            logger.devLog("Attempting to find Qty1 button and drawing on it the canvas.");
            Rectangle rect = depositBox.findQuantity1(getSelectedEmulator());

            draw.clearCanvas();
            draw.drawRectangle(rect, Color.GREEN);
        });
        findQuantity5.setOnAction(event -> {
            logger.devLog("Attempting to find Qty5 button and drawing on it the canvas.");
            Rectangle rect = depositBox.findQuantity5(getSelectedEmulator());

            draw.clearCanvas();
            draw.drawRectangle(rect, Color.GREEN);
        });
        findQuantity10.setOnAction(event -> {
            logger.devLog("Attempting to find Qty10 button and drawing on it the canvas.");
            Rectangle rect = depositBox.findQuantity10(getSelectedEmulator());

            draw.clearCanvas();
            draw.drawRectangle(rect, Color.GREEN);
        });
        findQuantityCustom.setOnAction(event -> {
            logger.devLog("Attempting to find QtyX button and drawing on it the canvas.");
            Rectangle rect = depositBox.findQuantityCustom(getSelectedEmulator());

            draw.clearCanvas();
            draw.drawRectangle(rect, Color.GREEN);
        });
        findQuantityAll.setOnAction(event -> {
            logger.devLog("Attempting to find Qty All button and drawing on it the canvas.");
            Rectangle rect = depositBox.findQuantityAll(getSelectedEmulator());

            draw.clearCanvas();
            draw.drawRectangle(rect, Color.GREEN);
        });
        findDepositInventory.setOnAction(event -> {
            logger.devLog("Attempting to find deposit inventory button and drawing on it the canvas.");
            Rectangle rect = depositBox.findDepositInventory(getSelectedEmulator());

            draw.clearCanvas();
            draw.drawRectangle(rect, Color.GREEN);
        });
        findDepositWorn.setOnAction(event -> {
            logger.devLog("Attempting to find deposit worn button and drawing on it the canvas.");
            Rectangle rect = depositBox.findDepositWorn(getSelectedEmulator());

            draw.clearCanvas();
            draw.drawRectangle(rect, Color.GREEN);
        });
        findDepositLoot.setOnAction(event -> {
            logger.devLog("Attempting to find deposit loot button and drawing on it the canvas.");
            Rectangle rect = depositBox.findDepositLoot(getSelectedEmulator());

            draw.clearCanvas();
            draw.drawRectangle(rect, Color.GREEN);
        });
        findCloseDepositBox.setOnAction(event -> {
            logger.devLog("Attempting to find close deposit box button and drawing on it the canvas.");
            Rectangle rect = depositBox.findCloseDepositBox(getSelectedEmulator());

            draw.clearCanvas();
            draw.drawRectangle(rect, Color.GREEN);
        });
        findSetCustQty.setOnAction(event -> {
            logger.devLog("Attempting to find set custom Qty button and drawing on it the canvas.");
            Rectangle rect = depositBox.findSetCustomQuantity(getSelectedEmulator());

            draw.clearCanvas();
            draw.drawRectangle(rect, Color.GREEN);
        });
        showBoxGrid.setOnAction(event -> {
            logger.devLog("Building deposit box grid on the canvas.");

            Rectangle[] rectangles = depositBox.buildDepositBoxGrid(getSelectedEmulator());
            draw.clearCanvas();

            // Iterate over each Rectangle
            for (Rectangle rectangle : rectangles) {
                draw.drawRectangle(rectangle, Color.GREEN);
            }
        });

        // Depositbox tab spinners/integers (add if needed)

        // Depositbox tab text fields (add if needed)
    }

    private void setupEquipmentTab() {
        // Initialize Equipment tab buttons
        itemAt = new Button("Item At");
        findHelm = new Button("Find Helm");
        findCape = new Button("Find Cape");
        findAmulet = new Button("Find Amulet");
        findAmmo = new Button("Find Ammo");
        findWeapon = new Button("Find Weapon");
        findBody = new Button("Find Body");
        findShield = new Button("Find Shield");
        findLegs = new Button("Find Legs");
        findGloves = new Button("Find Gloves");
        findFeet = new Button("Find Feet");
        findRing = new Button("Find Ring");

        // Apply CSS class to buttons
        Button[] buttons = {itemAt, findHelm, findCape, findAmulet, findAmmo, findWeapon, findBody,
                findShield, findLegs, findGloves, findFeet, findRing};
        for (Button btn : buttons) {
            btn.getStyleClass().add("devUITab-button");
        }


        // Equipment tab spinners/integers
        equipmentIntField.setPrefWidth(100);
        equipmentIntField.getStyleClass().add("devUITab-textfield");
        equipmentIntField.setPromptText("Enter an int");
        equipmentIntField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {  // Regex to allow only digits
                equipmentIntField.setText(newValue.replaceAll("[^\\d]", ""));
            }
        });

        // Dropdown for equipment pieces
        equipmentDropdown.getItems().addAll("Helm", "Cape", "Amulet", "Ammo", "Weapon",
                "Body", "Shield", "Legs", "Gloves", "Feet", "Ring");
        equipmentDropdown.getStyleClass().add("devUITab-dropdown");
        equipmentDropdown.getSelectionModel().selectFirst();

        // Set up Equipment tab button actions with multiline format
        itemAt.setOnAction(event -> {
            EquipmentSlot slot = EquipmentSlot.BODY;  // Default to BODY unless a different selection is made
            if (equipmentDropdown.getValue() != null) {
                switch (equipmentDropdown.getValue()) {
                    case "Helm":
                        slot = EquipmentSlot.HEAD;
                        break;
                    case "Cape":
                        slot = EquipmentSlot.CAPE;
                        break;
                    case "Amulet":
                        slot = EquipmentSlot.NECK;
                        break;
                    case "Ammo":
                        slot = EquipmentSlot.AMMUNITION;
                        break;
                    case "Weapon":
                        slot = EquipmentSlot.WEAPON;
                        break;
                    case "Body":
                        slot = EquipmentSlot.BODY;
                        break;
                    case "Shield":
                        slot = EquipmentSlot.SHIELD;
                        break;
                    case "Legs":
                        slot = EquipmentSlot.LEGS;
                        break;
                    case "Gloves":
                        slot = EquipmentSlot.HANDS;
                        break;
                    case "Feet":
                        slot = EquipmentSlot.FEET;
                        break;
                    case "Ring":
                        slot = EquipmentSlot.RING;
                        break;
                }
            }

            // Ensure the integer field is not empty to avoid errors
            if (!equipmentIntField.getText().isEmpty()) {
                try {
                    int quantity = Integer.parseInt(equipmentIntField.getText());
                    logger.devLog("Item " + equipmentIntField.getText() + " at " + equipmentDropdown.getValue() + " slot is: " + equipment.itemAt(getSelectedEmulator(), slot, quantity, null));
                } catch (NumberFormatException e) {
                    logger.devLog("Error: The quantity must be a valid integer.");
                }
            } else {
                logger.devLog("Error: Quantity field is empty.");
            }
        });
        findHelm.setOnAction(event -> {
            logger.devLog("Attempting to find the Helm slot and drawing it on the canvas.");
            draw.clearCanvas();
            draw.drawRectangle(HELM_RECT, Color.GREEN);
        });
        findCape.setOnAction(event -> {
            logger.devLog("Attempting to find the Cape slot and drawing it on the canvas.");
            draw.clearCanvas();
            draw.drawRectangle(CAPE_RECT, Color.GREEN);
        });
        findAmulet.setOnAction(event -> {
            logger.devLog("Attempting to find the Amulet slot and drawing it on the canvas.");
            draw.clearCanvas();
            draw.drawRectangle(AMULET_RECT, Color.GREEN);
        });
        findAmmo.setOnAction(event -> {
            logger.devLog("Attempting to find the Ammo slot and drawing it on the canvas.");
            draw.clearCanvas();
            draw.drawRectangle(AMMO_RECT, Color.GREEN);
        });
        findWeapon.setOnAction(event -> {
            logger.devLog("Attempting to find the Weapon slot and drawing it on the canvas.");
            draw.clearCanvas();
            draw.drawRectangle(WEAPON_RECT, Color.GREEN);
        });
        findBody.setOnAction(event -> {
            logger.devLog("Attempting to find the Body slot and drawing it on the canvas.");
            draw.clearCanvas();
            draw.drawRectangle(BODY_RECT, Color.GREEN);
        });
        findShield.setOnAction(event -> {
            logger.devLog("Attempting to find the Shield slot and drawing it on the canvas.");
            draw.clearCanvas();
            draw.drawRectangle(SHIELD_RECT, Color.GREEN);
        });
        findLegs.setOnAction(event -> {
            logger.devLog("Attempting to find the Legs slot and drawing it on the canvas.");
            draw.clearCanvas();
            draw.drawRectangle(LEGS_RECT, Color.GREEN);
        });
        findGloves.setOnAction(event -> {
            logger.devLog("Attempting to find the Gloves slot and drawing it on the canvas.");
            draw.clearCanvas();
            draw.drawRectangle(GLOVES_RECT, Color.GREEN);
        });
        findFeet.setOnAction(event -> {
            logger.devLog("Attempting to find the Feet/Boot slot and drawing it on the canvas.");
            draw.clearCanvas();
            draw.drawRectangle(BOOTS_RECT, Color.GREEN);
        });
        findRing.setOnAction(event -> {
            logger.devLog("Attempting to find the Ring slot and drawing it on the canvas.");
            draw.clearCanvas();
            draw.drawRectangle(RING_RECT, Color.GREEN);
        });
    }

    private void setupGameTab() {
        // Initialize Game tab buttons
        setZoom = new Button("Set Zoom");
        setFairyRing = new Button("Set Fairy Ring");
        isWorldListOpen = new Button("Is World List Open");
        findActionButton = new Button("Find Action Button");
        isTapToDropOn = new Button("Is Tap to Drop On");
        isSingleTapOn = new Button("Is Single Tap On");
        enableTapToDrop = new Button("Enable Tap to Drop");
        disableTapToDrop = new Button("Disable Tap to Drop");
        enableSingleTap = new Button("Enable Single Tap");
        disableSingleTap = new Button("Disable Single Tap");
        findOption = new Button("Find Option");
        isPlayersAroundOption = new Button("Check Players");
        countPlayersAround = new Button("Count Players");

        // Apply CSS class to buttons
        Button[] buttons = {setZoom, setFairyRing, isWorldListOpen, findActionButton,
                isTapToDropOn, isSingleTapOn, enableTapToDrop, disableTapToDrop,
                enableSingleTap, disableSingleTap, findOption, isPlayersAroundOption, countPlayersAround};
        for (Button btn : buttons) {
            btn.getStyleClass().add("devUITab-button");
        }

        isPlayersAroundOption.setOnAction(event -> logger.log("Is players around: " + game.isPlayersAround(getSelectedEmulator()), getSelectedEmulator()));
        countPlayersAround.setOnAction(event -> logger.log("Player around count: " + game.countPlayersAround(getSelectedEmulator()), getSelectedEmulator()));

        // Game tab spinners/integers (add if needed)
        SpinnerValueFactory<Integer> valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 5, 3);
        gameZoomSpinner.setValueFactory(valueFactory);
        gameZoomSpinner.setPrefWidth(100); // Set preferred width
        gameZoomSpinner.getStyleClass().add("devUITab-spinner");

        // Game tab text fields (add if needed)
        gameFairyCodeField.setPromptText("Enter fairy ring code");
        gameFairyCodeField.getStyleClass().add("devUITab-textfield");
        gameFairyCodeField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("[abcdijklpqrs]*") || newVal.length() > 3) {
                gameFairyCodeField.setText(oldVal);
            }
        });

        // Game tab dropdown for options
        gameDropdown.getItems().addAll("bank", "collect", "talk-to", "pickpocket", "buy-plank", "bloom", "cast-bloom");
        gameDropdown.getStyleClass().add("devUITab-dropdown");
        gameDropdown.getSelectionModel().selectFirst();

        // Set up Game tab button actions
        setZoom.setOnAction(event -> {
            logger.devLog("Setting zoom to " + gameZoomSpinner.getValue() + ".");
            game.setZoom(getSelectedEmulator(), gameZoomSpinner.getValue().toString());
        });
        setFairyRing.setOnAction(event -> {
            String code = gameFairyCodeField.getText();
            logger.devLog("Setting fairy ring to code: " + code);
            game.setFairyRing(getSelectedEmulator(), code);
        });
        isWorldListOpen.setOnAction(event -> logger.devLog("World list open is: " + game.isWorldListOpen(getSelectedEmulator())));
        findActionButton.setOnAction(event -> {
            logger.devLog("Attempting to find action button and drawing it on the canvas.");
            draw.clearCanvas();
            draw.drawRectangle(game.getActionButtonLocation(getSelectedEmulator()), Color.GREEN);
        });
        isTapToDropOn.setOnAction(event -> logger.devLog("Tap to drop is on: " + game.isTapToDropEnabled(getSelectedEmulator())));
        isSingleTapOn.setOnAction(event -> logger.devLog("Single tap is on: " + game.isSingleTapEnabled(getSelectedEmulator())));
        enableTapToDrop.setOnAction(event -> {
            logger.devLog("Enabling tap to drop.");
            game.enableTapToDrop(getSelectedEmulator());
        });
        disableTapToDrop.setOnAction(event -> {
            logger.devLog("Disabling tap to drop.");
            game.disableTapToDrop(getSelectedEmulator());
        });
        enableSingleTap.setOnAction(event -> {
            logger.devLog("Enabling single tap.");
            game.enableSingleTap(getSelectedEmulator());
        });
        disableSingleTap.setOnAction(event -> {
            logger.devLog("Disable single tap.");
            game.disableSingleTap(getSelectedEmulator());
        });
        findOption.setOnAction(event -> {
            logger.devLog("Trying to find and draw the " + gameDropdown.getValue() + " option.");
            draw.clearCanvas();
            draw.drawRectangle(game.findOption(getSelectedEmulator(), gameDropdown.getValue()), Color.GREEN);
        });
    }

    private void setupImageRecogTab() {
        // Initialize image recognition tab buttons
        irToggleSearch.setSelected(false);
        irToggleSearch.setText("Start search");
        uploadImage = new Button("Upload Image");
        findBestResult = new Button("Find Best Result");
        findAllResults = new Button("Find All Results");
        findBestWithinRect = new Button("Find Best Within Rect");
        findAllWithinRect = new Button("Find All Within Rect");
        uploadToCanvas = new Button("Upload image to canvas");
        findBestItem = new Button("Find best item");
        findAllItems = new Button("Find all items");

        // Initialize spinners
        SpinnerValueFactory<Double> doubleValueFactory = new SpinnerValueFactory.DoubleSpinnerValueFactory(0.01, 1.00, 0.80, 0.01);
        irSearchThreshold.setValueFactory(doubleValueFactory);
        irSearchThreshold.setPrefWidth(100); // Set preferred width
        SpinnerValueFactory<Integer> integerValueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(100, 5000, 250, 1);
        irSearchSpeed.setValueFactory(integerValueFactory);
        irSearchSpeed.setPrefWidth(100); // Set preferred width

        // Initialize image view
        imageRecogView.setPreserveRatio(true);
        imageRecogView.setFitWidth(300);
        imageRecogView.setFitHeight(150);
        imageRecogView.setSmooth(true);

        // Apply CSS class to buttons, spinners, and image view
        Button[] buttons = {uploadImage, findBestResult, findAllResults, findBestWithinRect, findAllWithinRect, uploadToCanvas, findBestItem, findAllItems};
        for (Button btn : buttons) {
            btn.getStyleClass().add("devUITab-button");
        }
        irSearchSpeed.getStyleClass().add("devUITab-spinner");
        irSearchThreshold.getStyleClass().add("devUITab-spinner");
        irToggleSearch.getStyleClass().add("devUITab-button");
        imageRecogView.getStyleClass().add("devUITab-imageview");

        irItemIDField.getStyleClass().add("devUITab-textfield");
        irItemIDField.setMinWidth(100);
        irItemIDField.setPromptText("itemID");

        // Set up image recognition tab button actions with multiline format
        uploadImage.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();
            File file = fileChooser.showOpenDialog(null);
            if (file != null) {
                Image image = new Image(file.toURI().toString());

                // Check if the image size is appropriate for the ImageView
                if (image.getWidth() <= 150 && image.getHeight() <= 150) {
                    Platform.runLater(() -> imageRecogView.setImage(image));
                    uploadedFile = file; // Store the uploaded file
                    logger.devLog("Image uploaded to imageRecogView from " + file.getAbsolutePath());
                } else {
                    // Resize the image if it's larger than 150x150
                    Image resizedImage = new Image(file.toURI().toString(), 150, 150, true, true);
                    Platform.runLater(() -> imageRecogView.setImage(resizedImage));
                    uploadedFile = file; // Store the uploaded file
                    logger.devLog("Image resized and uploaded to imageRecogView from " + file.getAbsolutePath());
                }
            } else {
                logger.devLog("No image selected.");
            }
        });
        findBestResult.setOnAction(event -> {
            if (uploadedFile != null) {
                ExecutorService executor = Executors.newSingleThreadExecutor();
                executor.submit(() -> {
                    try {
                        BufferedImage imageToFind = ImageIO.read(uploadedFile);
                        Long startTime = System.currentTimeMillis();
                        //Mat gameImageMat = getCurrentImageAsMat();
                        //MatchedRectangle bestMatchRectangle = imageRecognition.returnBestMatchObject(uploadedFile, gameImageMat, irSearchThreshold.getValue());
                        Rectangle bestMatchRectangle = templateMatcher.match(EmulatorView.getSelectedEmulator(), imageToFind, 10);
                        Platform.runLater(() -> {
                            draw.clearCanvas(); // Clear the previous drawings on the canvas

                            if (bestMatchRectangle != null) {
                                double foundX = bestMatchRectangle.x;
                                double foundY = bestMatchRectangle.y;
                                double width = bestMatchRectangle.width;
                                double height = bestMatchRectangle.height;
                                //double matchValue = bestMatchRectangle.getMatchValue();
                                draw.drawOnCanvas(foundX, foundY, width, height, Color.GREEN, ShapeType.RECTANGLE);
                                logger.devLog("Best object match found and drawn on canvas (" + bestMatchRectangle.x + "," + bestMatchRectangle.y + "), with size " + bestMatchRectangle.width + "x" + bestMatchRectangle.height + ".");
                                //logger.devLog("Match confidence score: " + matchValue);
                                logger.devLog("Match took: " + (System.currentTimeMillis() - startTime) + " ms");
                            } else {
                                logger.devLog("Best object match not found!");
                            }
                        });
                    } catch (Exception ex) {
                        logger.devLog("Error during image recognition: " + ex.getMessage());
                        ex.printStackTrace();
                    }
                });
                executor.shutdown();
            } else {
                logger.devLog("No image uploaded.");
            }
        });
        findAllResults.setOnAction(event -> {
            if (uploadedFile != null) {
                ExecutorService executor = Executors.newSingleThreadExecutor();
                executor.submit(() -> {
                    try {
                        Mat gameImageMat = getCurrentImageAsMat();

                        // Perform image recognition to find all matching objects
                        List<MatchedRectangle> awtRectangles = imageRecognition.returnAllMatchObjects(uploadedFile, gameImageMat, irSearchThreshold.getValue());

                        Platform.runLater(() -> {
                            draw.clearCanvas(); // Clear the previous drawings on the canvas

                            if (!awtRectangles.isEmpty()) {
                                for (MatchedRectangle awtRectangle : awtRectangles) {
                                    double foundX = awtRectangle.x;
                                    double foundY = awtRectangle.y;
                                    double width = awtRectangle.width;
                                    double height = awtRectangle.height;
                                    double matchValue = awtRectangle.getMatchValue();
                                    draw.drawOnCanvas(foundX, foundY, width, height, Color.GREEN, ShapeType.RECTANGLE);
                                    logger.devLog("Object found and drawn on canvas, match score: " + matchValue);
                                }
                            } else {
                                logger.devLog("No objects found!");
                            }
                        });
                    } catch (Exception ex) {
                        logger.devLog("Error during image recognition: " + ex.getMessage());
                        ex.printStackTrace();
                    }
                });
                executor.shutdown();
            } else {
                logger.devLog("No image uploaded.");
            }
        });
        findBestWithinRect.setOnAction(event -> {
            String rectangleText = rectangleLabel.getText();

            if (uploadedFile != null) {
                ExecutorService executor = Executors.newSingleThreadExecutor();

                executor.submit(() -> {
                    try {
                        // Parse the rectangle dimensions from the label text
                        String cleanText = rectangleText.replace("new Rectangle(", "").replace(");", "");
                        String[] parts = cleanText.split(", ");

                        if (parts.length != 4) {
                            throw new IllegalArgumentException("Rectangle dimensions should contain exactly four integer values.");
                        }

                        int x = Integer.parseInt(parts[0].trim());
                        int y = Integer.parseInt(parts[1].trim());
                        int width = Integer.parseInt(parts[2].trim());
                        int height = Integer.parseInt(parts[3].trim());

                        // Create the rectangle with parsed dimensions
                        Rectangle searchArea = new Rectangle(x, y, width, height);

                        Mat gameImageMat = getCurrentImageAsMat();

                        // Perform image recognition to find all matching objects
                        List<MatchedRectangle> awtRectangles = imageRecognition.returnAllMatchObjects(uploadedFile, gameImageMat, irSearchThreshold.getValue());

                        MatchedRectangle bestMatchRectangle = null;
                        double highestMatchValue = 0;

                        // Filter results to find the best match within the specified rectangle
                        for (MatchedRectangle awtRectangle : awtRectangles) {
                            if (searchArea.contains(awtRectangle.x, awtRectangle.y, awtRectangle.width, awtRectangle.height)) {
                                if (awtRectangle.getMatchValue() > highestMatchValue) {
                                    highestMatchValue = awtRectangle.getMatchValue();
                                    bestMatchRectangle = awtRectangle;
                                }
                            }
                        }

                        MatchedRectangle finalBestMatchRectangle = bestMatchRectangle;
                        Platform.runLater(() -> {
                            draw.clearCanvas(); // Clear the previous drawings on the canvas

                            if (finalBestMatchRectangle != null) {
                                double foundX = finalBestMatchRectangle.x;
                                double foundY = finalBestMatchRectangle.y;
                                double widthRect = finalBestMatchRectangle.width;
                                double heightRect = finalBestMatchRectangle.height;
                                double matchValue = finalBestMatchRectangle.getMatchValue();
                                draw.drawOnCanvas(foundX, foundY, widthRect, heightRect, Color.GREEN, ShapeType.RECTANGLE);
                                logger.devLog("Best object match found and drawn on canvas (" + finalBestMatchRectangle.x + "," + finalBestMatchRectangle.y + "), with size " + finalBestMatchRectangle.width + "x" + finalBestMatchRectangle.height + ".");
                                logger.devLog("Match confidence score: " + matchValue);
                            } else {
                                logger.devLog("Best object match not found within the specified rectangle!");
                            }
                        });
                    } catch (Exception ex) {
                        logger.devLog("Error during image recognition: " + ex.getMessage());
                        ex.printStackTrace();
                    }
                });
                executor.shutdown();
            } else {
                logger.devLog("No image uploaded.");
            }
        });
        findAllWithinRect.setOnAction(event -> {
            String rectangleText = rectangleLabel.getText();

            if (uploadedFile != null) {
                ExecutorService executor = Executors.newSingleThreadExecutor();
                executor.submit(() -> {
                    try {
                        // Parse the rectangle dimensions from the label text
                        String cleanText = rectangleText.replace("new Rectangle(", "").replace(");", "");
                        String[] parts = cleanText.split(", ");

                        if (parts.length != 4) {
                            throw new IllegalArgumentException("Rectangle dimensions should contain exactly four integer values.");
                        }

                        int x = Integer.parseInt(parts[0].trim());
                        int y = Integer.parseInt(parts[1].trim());
                        int width = Integer.parseInt(parts[2].trim());
                        int height = Integer.parseInt(parts[3].trim());

                        // Create the rectangle with parsed dimensions
                        Rectangle searchArea = new Rectangle(x, y, width, height);

                        Mat gameImageMat = getCurrentImageAsMat();

                        // Perform image recognition to find all matching objects
                        List<MatchedRectangle> awtRectangles = imageRecognition.returnAllMatchObjects(uploadedFile, gameImageMat, irSearchThreshold.getValue());

                        List<MatchedRectangle> rectanglesWithinArea = new ArrayList<>();

                        // Filter results to find all matches within the specified rectangle
                        for (MatchedRectangle awtRectangle : awtRectangles) {
                            if (searchArea.contains(awtRectangle.x, awtRectangle.y, awtRectangle.width, awtRectangle.height)) {
                                rectanglesWithinArea.add(awtRectangle);
                            }
                        }

                        Platform.runLater(() -> {
                            draw.clearCanvas(); // Clear the previous drawings on the canvas

                            if (!rectanglesWithinArea.isEmpty()) {
                                for (MatchedRectangle awtRectangle : rectanglesWithinArea) {
                                    double foundX = awtRectangle.x;
                                    double foundY = awtRectangle.y;
                                    double widthRect = awtRectangle.width;
                                    double heightRect = awtRectangle.height;
                                    double matchValue = awtRectangle.getMatchValue();
                                    draw.drawOnCanvas(foundX, foundY, widthRect, heightRect, Color.GREEN, ShapeType.RECTANGLE);
                                    logger.devLog("Object found and drawn on canvas, match score: " + matchValue);
                                }
                            } else {
                                logger.devLog("No objects found within the specified rectangle!");
                            }
                        });
                    } catch (Exception ex) {
                        logger.devLog("Error during image recognition: " + ex.getMessage());
                        ex.printStackTrace();
                    }
                });
                executor.shutdown();
            } else {
                logger.devLog("No image uploaded.");
            }
        });
        uploadToCanvas.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();
            File file = fileChooser.showOpenDialog(null);
            if (file != null) {
                Image image = new Image(file.toURI().toString());

                // Check if the image size is 894x540
                if (image.getWidth() == 894 && image.getHeight() == 540) {
                    Platform.runLater(() -> gameView.setImage(image));
                    logger.devLog("Image uploaded from " + file.getAbsolutePath());
                } else {
                    // You can use any form of alert/notification to let the user know
                    showDialog("Error", "Image dimension should be 894x540, image upload failed!", Alert.AlertType.INFORMATION);
                    logger.devLog("Image dimension should be 894x540, image upload failed!");
                }
            }
        });
        findBestItem.setOnAction(event -> {
            long startTime = System.currentTimeMillis(); // Start timing

            Mat itemImage = getCroppedItemImage(itemProcessor.getItemImage(irItemIDField.getText()));
            if (itemImage != null && !itemImage.empty()) {
                ExecutorService executor = Executors.newSingleThreadExecutor();
                executor.submit(() -> {
                    try {
                        Mat gameImageMat = getCurrentImageAsMat();

                        MatchedRectangle bestMatchRectangle = imageRecognition.returnBestMatchObject(itemImage, gameImageMat, irSearchThreshold.getValue());

                        Platform.runLater(() -> {
                            draw.clearCanvas(); // Clear the previous drawings on the canvas

                            if (bestMatchRectangle != null) {
                                double foundX = bestMatchRectangle.x;
                                double foundY = bestMatchRectangle.y - 10;
                                double width = bestMatchRectangle.width;
                                double height = bestMatchRectangle.height + 10;
                                double matchValue = bestMatchRectangle.getMatchValue();
                                draw.drawOnCanvas(foundX, foundY, width, height, Color.GREEN, ShapeType.RECTANGLE);
                                logger.devLog("Best item match found and drawn on canvas (" + bestMatchRectangle.x + "," + bestMatchRectangle.y + "), with size " + bestMatchRectangle.width + "x" + bestMatchRectangle.height + ".");
                                logger.devLog("Match confidence score: " + matchValue);
                            } else {
                                logger.devLog("Best item match not found!");
                            }

                            // Calculate and log the total elapsed time
                            long endTime = System.currentTimeMillis();
                            long totalTime = endTime - startTime;
                            logger.devLog("Total time for findBestItem event: " + totalTime + " ms");
                        });
                    } catch (Exception ex) {
                        logger.devLog("Error during image recognition: " + ex.getMessage());
                        ex.printStackTrace();
                    }
                });
                executor.shutdown();
            } else {
                logger.devLog("Item image is null or empty.");
            }
        });
        findAllItems.setOnAction(event -> {
            Mat itemImage = getCroppedItemImage(itemProcessor.getItemImage(irItemIDField.getText()));
            if (itemImage != null && !itemImage.empty()) {
                ExecutorService executor = Executors.newSingleThreadExecutor();
                executor.submit(() -> {
                    try {
                        Mat gameImageMat = getCurrentImageAsMat();

                        // Perform image recognition to find all matching objects
                        List<MatchedRectangle> awtRectangles = imageRecognition.returnAllMatchObjects(itemImage, gameImageMat, irSearchThreshold.getValue());

                        Platform.runLater(() -> {
                            draw.clearCanvas(); // Clear the previous drawings on the canvas

                            if (!awtRectangles.isEmpty()) {
                                for (MatchedRectangle awtRectangle : awtRectangles) {
                                    double foundX = awtRectangle.x;
                                    double foundY = awtRectangle.y - 10;
                                    double width = awtRectangle.width;
                                    double height = awtRectangle.height + 10;
                                    double matchValue = awtRectangle.getMatchValue();
                                    draw.drawOnCanvas(foundX, foundY, width, height, Color.GREEN, ShapeType.RECTANGLE);
                                    logger.devLog("Item found and drawn on canvas, match score: " + matchValue);
                                }
                            } else {
                                logger.devLog("No items found!");
                            }
                        });
                    } catch (Exception ex) {
                        logger.devLog("Error during image recognition: " + ex.getMessage());
                        ex.printStackTrace();
                    }
                });
                executor.shutdown();
            } else {
                logger.devLog("Item image is null or empty.");
            }
        });

        // Set up listeners
        irToggleSearch.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                startSearch();
            } else {
                stopSearch();
            }
        });
    }

    private void setupInventoryTab() {
        // Initialize Inventory tab buttons
        isInventFull = new Button("Is Inventory Full");
        calcInventSlots = new Button("Calculate Inventory Slots");
        itemPosition = new Button("Item Position");
        lastItemPosition = new Button("Last Item Position");
        dropInventItems = new Button("Drop Inventory Items");
        showInventGrid = new Button("Show Inventory Grid");
        count = new Button("Count");
        contains = new Button("Contains");
        tapAllItems = new Button("Tap All Items");
        eat = new Button("Eat");
        getItemCenter = new Button("Get Item Center");
        getItemStack = new Button("Get ItemStack");

        // Apply CSS class to buttons
        Button[] buttons = {
                isInventFull, calcInventSlots, itemPosition, lastItemPosition,
                dropInventItems, showInventGrid, count, contains, tapAllItems,
                eat, getItemCenter, getItemStack
        };
        for (Button btn : buttons) {
            btn.getStyleClass().add("devUITab-button");
        }

        // Inventory tab spinners/integers
        SpinnerValueFactory<Double> doubleValueFactory = new SpinnerValueFactory.DoubleSpinnerValueFactory(0.01, 1.00, 0.80, 0.01);
        inventoryThresholdSpinner.setValueFactory(doubleValueFactory);
        inventoryThresholdSpinner.setPrefWidth(100); // Set preferred width
        inventoryThresholdSpinner.getStyleClass().add("devUITab-spinner");

        // Inventory tab text fields
        inventoryItemIDField.setPromptText("Enter Item ID");
        inventoryItemIDField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {  // Regex to allow only digits
                inventoryItemIDField.setText(newValue.replaceAll("[^\\d]", ""));
            }
        });

        // Set up Inventory tab button actions with multiline format
        isInventFull.setOnAction(event -> logger.devLog("Inventory full is: " + inventory.isInventoryFull(getSelectedEmulator())));

        calcInventSlots.setOnAction(event -> {
            logger.devLog("Amount of used inventory spots: " + inventory.getNumberOfUsedInventorySlots(getSelectedEmulator()));
            logger.devLog("Amount of empty inventory spots: " + inventory.getNumberOfEmptyInventorySlots(getSelectedEmulator()));
        });

        itemPosition.setOnAction(event -> {
            Double threshold = inventoryThresholdSpinner.getValue();
            String itemID = inventoryItemIDField.getText();
            logger.devLog("Attempting to find item " + itemID + " with threshold " + threshold + " and drawing it on the canvas.");
            draw.drawRectangle(inventory.itemPosition(getSelectedEmulator(), itemID, threshold), Color.GREEN);
            logger.devLog("Item is in inventory slot: " + inventory.itemSlotPosition(getSelectedEmulator(), new int[]{Integer.parseInt(itemID)}, threshold));
        });

        lastItemPosition.setOnAction(event -> {
            Double threshold = inventoryThresholdSpinner.getValue();
            String itemID = inventoryItemIDField.getText();
            logger.devLog("Attempting to find the last item position of " + itemID + " with threshold " + threshold + " and drawing it on the canvas.");
            draw.drawRectangle(inventory.lastItemPosition(getSelectedEmulator(), itemID, threshold), Color.GREEN);
        });

        dropInventItems.setOnAction(event -> {
            Double threshold = inventoryThresholdSpinner.getValue();
            String itemID = inventoryItemIDField.getText();
            logger.devLog("Dropping all items with ID: " + itemID);
            inventory.tapAllItems(Integer.parseInt(itemID), threshold, getSelectedEmulator());
        });

        showInventGrid.setOnAction(event -> {
            Rectangle[] rectangles = inventory.getInventorySlotBoxes();
            draw.clearCanvas();
            for (Rectangle rectangle : rectangles) {
                draw.drawRectangle(rectangle, Color.GREEN);
            }
            logger.devLog("Drew inventory grid on the canvas.");
        });

        count.setOnAction(event -> {
            String itemID = inventoryItemIDField.getText();
            logger.devLog("There are " + inventory.count(getSelectedEmulator(), Integer.parseInt(itemID), inventoryThresholdSpinner.getValue(), null) + " items in the inventory.");
        });

        contains.setOnAction(event -> {
            String itemID = inventoryItemIDField.getText();
            logger.devLog("Inventory contains item " + itemID + " is: " + inventory.contains(getSelectedEmulator(), Integer.parseInt(itemID), inventoryThresholdSpinner.getValue()));
        });

        tapAllItems.setOnAction(event -> {
            Double threshold = inventoryThresholdSpinner.getValue();
            String itemID = inventoryItemIDField.getText();
            logger.devLog("Tapping all items with ID: " + itemID);
            inventory.tapAllItems(Integer.parseInt(itemID), threshold, getSelectedEmulator());
        });

        eat.setOnAction(event -> {
            Double threshold = inventoryThresholdSpinner.getValue();
            String itemID = inventoryItemIDField.getText();
            logger.devLog("Trying to eat item " + itemID + " with threshold " + threshold + ".");
            inventory.eat(itemID, threshold, getSelectedEmulator(), null);
        });

        getItemCenter.setOnAction(event -> {
            Double threshold = inventoryThresholdSpinner.getValue();
            String itemID = inventoryItemIDField.getText();
            logger.devLog("Center point of item " + itemID + " is at: " + inventory.getItemCenterPoint(itemID, threshold, getSelectedEmulator()) +
                    " in inventory slot: " + inventory.itemSlotPosition(getSelectedEmulator(), new int[]{Integer.parseInt(itemID)}, threshold));
        });

        getItemStack.setOnAction(event -> {
            String itemID = inventoryItemIDField.getText();
            logger.devLog("Item stack of item " + itemID + " is: " + inventory.getItemStack(getSelectedEmulator(), Integer.valueOf(itemID)));
        });
    }

    private void setupLoginNOutTab() {
        // Initialize Login/Logout tab buttons
        onLoginScreen = new Button("On Login Screen");
        isLoggedOut = new Button("Is Logged Out");
        isLoggedIn = new Button("Is Logged In");
        findTapToPlay = new Button("Find Tap to Play");
        isCompassNorth = new Button("Is Compass North");
        readLoginScreen = new Button("Read Login Screen");
        closestLoginMessage = new Button("Closest Login Message");
        findLogoutOption = new Button("Find Logout Option");
        findExitWorldSwitcher = new Button("Find Exit WorldSwitcher");

        // Apply CSS class to buttons
        Button[] buttons = {
                onLoginScreen, isLoggedOut, isLoggedIn, findTapToPlay,
                isCompassNorth, readLoginScreen, closestLoginMessage, findLogoutOption,
                findExitWorldSwitcher
        };
        for (Button btn : buttons) {
            btn.getStyleClass().add("devUITab-button");
        }

        // Set up Login/Logout tab button actions with multiline format
        onLoginScreen.setOnAction(event -> logger.devLog("On login screen is: " + login.onLoginScreen(getSelectedEmulator())));
        isLoggedOut.setOnAction(event -> logger.devLog("Is logged out is: " + login.isLoggedOut(getSelectedEmulator())));
        isLoggedIn.setOnAction(event -> logger.devLog("Is logged in is: " + login.isLoggedIn(getSelectedEmulator())));
        findTapToPlay.setOnAction(event -> {
            logger.devLog("Attempting to find the Tap to Play screen and drawing it on the canvas.");
            draw.drawRectangle(login.findTapToPlayOption(getSelectedEmulator()), Color.GREEN);
        });
        isCompassNorth.setOnAction(event -> logger.devLog("Compass is north is: " + login.isCompassNorth(getSelectedEmulator())));
        readLoginScreen.setOnAction(event -> logger.devLog("Reading login screen. Results: \n" + login.readLoginScreen(getSelectedEmulator())));
        closestLoginMessage.setOnAction(event -> {
            String results = login.readLoginScreen(getSelectedEmulator());
            logger.devLog("Reading login screen. Results: \n" + results);
            logger.devLog("Closest login message is: " + login.findClosestLoginMessage(results));
        });
        findLogoutOption.setOnAction(event -> {
            logger.devLog("Attempting to find the logout option and drawing it on the canvas.");
            draw.drawRectangle(login.findLogoutOption(getSelectedEmulator()), Color.GREEN);
        });
        findExitWorldSwitcher.setOnAction(event -> {
            logger.devLog("Attempting to find the exit world switcher option and drawing it on the canvas.");
            draw.drawRectangle(logout.findExitWorldswitcherOption(getSelectedEmulator()), Color.GREEN);
        });
    }

    private void setupMagicTab() {
        // Initialize Magic tab buttons for each spellbook
        castMagicSpell = new Button("Cast Magic Spell");
        isMagicCastable = new Button("isCastable");
        isMagicInfoEnabled = new Button("Is Info Enabled");

        // Populate dropdowns based on spellbook
        populateDropdown(spellsDropdown);

        // Apply CSS class to buttons and dropdowns
        Button[] buttons = {castMagicSpell, isMagicCastable, isMagicInfoEnabled};
        spellsDropdown.getStyleClass().add("devUITab-dropdown");

        for (Button btn : buttons) {
            btn.getStyleClass().add("devUITab-button");
        }

        // Set up Magic tab button actions
        castMagicSpell.setOnAction(event ->
                magic.castSpell(Spells.valueOf(spellsDropdown.getSelectionModel().getSelectedItem()), getSelectedEmulator())
        );

        isMagicCastable.setOnAction(event ->
                logger.print("Is spell castable: " + magic.isCastable(Spells.valueOf(spellsDropdown.getSelectionModel().getSelectedItem()), getSelectedEmulator()))
        );

        isMagicInfoEnabled.setOnAction(event ->
                logger.print("is info enabled: " + magic.isInfoEnabled(getSelectedEmulator()))
        );
    }

    private void setupOverlaysTab() {
        // Initialize Overlays tab buttons
        regionButton.setSelected(false);
        regionButton.setText("Enable region mapping");
        getGameCenter = new Button("Get Game Center");
        findOverlays = new Button("Find Overlays");
        findFishingSpots = new Button("Find Fishing Spots");
        findNearest = new Button("Find Nearest");
        findSecondNearest = new Button("Find Second Nearest");

        // Apply CSS class to buttons
        Button[] buttons = {
                getGameCenter, findOverlays, findNearest, findSecondNearest, findFishingSpots,
        };
        for (Button btn : buttons) {
            btn.getStyleClass().add("devUITab-button");
        }
        regionButton.getStyleClass().add("devUITab-button");

        // Initialize dropdown menu
        overlayDropdown.getItems().addAll("Fishing", "Items", "Agility");
        overlayDropdown.getSelectionModel().selectFirst(); // Pre-select the first entry
        overlayDropdown.getStyleClass().add("devUITab-dropdown");

        // Set up spinners and shit
        SpinnerValueFactory<Integer> minPtsFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 50, 20, 1);
        overlaysMinPtsSpinner.setValueFactory(minPtsFactory);
        overlaysMinPtsSpinner.setPrefWidth(100); // Set preferred width
        overlaysMinPtsSpinner.getStyleClass().add("devUITab-spinner");

        SpinnerValueFactory<Double> epsFactory = new SpinnerValueFactory.DoubleSpinnerValueFactory(0.01, 25.00, 10.00, 0.01);
        overlaysEPSSpinner.setValueFactory(epsFactory);
        overlaysEPSSpinner.setPrefWidth(100); // Set preferred width
        overlaysEPSSpinner.getStyleClass().add("devUITab-spinner");

        // Set up Overlays tab button actions with multiline format
        getGameCenter.setOnAction(event -> logger.devLog("Gamecenter is: " + overlayFinder.getGameCenter(getSelectedEmulator())));
        findFishingSpots.setOnAction(event -> {
            draw.clearCanvas();
            logger.devLog("Finding fishing spots");
            List<Rectangle> spots = overlayFinder.findFishingSpots(getSelectedEmulator());

            logger.devLog("Fishing spots: " + spots.toString());
            draw.drawRectangles(spots, Color.PINK);
        });
        findOverlays.setOnAction(event -> {
            // Fetch the text from rectangleLabel
            String rectangleText = rectangleLabel.getText();

            try {
                // Attempt to parse the rectangle dimensions from the label text
                // Expected format: "new Rectangle(x, y, width, height);"
                // Example valid text: "new Rectangle(35, 32, 494, 88);"
                String cleanText = rectangleText.replace("new Rectangle(", "").replace(");", "");
                String[] parts = cleanText.split(", ");

                if (parts.length != 4) {
                    throw new IllegalArgumentException("Rectangle dimensions should contain exactly four integer values.");
                }

                int x = Integer.parseInt(parts[0].trim());
                int y = Integer.parseInt(parts[1].trim());
                int width = Integer.parseInt(parts[2].trim());
                int height = Integer.parseInt(parts[3].trim());

                // Create the rectangle with parsed dimensions
                Rectangle searchArea = new Rectangle(x, y, width, height);

                // Use the rectangle in the overlayFinder.findOverlays method
                List<Rectangle> foundRectangles = overlayFinder.findOverlays(getSelectedEmulator(), getOverlayType(), searchArea, overlaysEPSSpinner.getValue(), overlaysMinPtsSpinner.getValue());
                logger.devLog("Finding overlays with color: " + getOverlayType() + " in rectangle: " + searchArea);
                logger.devLog("Using EPS: " + overlaysEPSSpinner.getValue() + " and minPts: " + overlaysMinPtsSpinner.getValue());

                // Perform an action for each found rectangle
                logger.devLog("Drawing all rectangles on the canvas.");
                draw.clearCanvas();
                for (Rectangle rect : foundRectangles) {
                    draw.drawRectangle(rect, Color.GREEN);
                }
            } catch (NumberFormatException e) {
                // Handle case where string parts are not all integers
                showDialog("Error", "Error parsing rectangle dimensions: Ensure all values are integers.", Alert.AlertType.ERROR);
            } catch (IllegalArgumentException e) {
                // Handle any other issues with the input format
                showDialog("Error", e.getMessage(), Alert.AlertType.ERROR);
            }
        });
        findNearest.setOnAction(event -> {
            // Fetch the text from rectangleLabel
            String rectangleText = rectangleLabel.getText();

            try {
                // Attempt to parse the rectangle dimensions from the label text
                // Expected format: "new Rectangle(x, y, width, height);"
                // Example valid text: "new Rectangle(35, 32, 494, 88);"
                String cleanText = rectangleText.replace("new Rectangle(", "").replace(");", "");
                String[] parts = cleanText.split(", ");

                if (parts.length != 4) {
                    throw new IllegalArgumentException("Rectangle dimensions should contain exactly four integer values.");
                }

                int x = Integer.parseInt(parts[0].trim());
                int y = Integer.parseInt(parts[1].trim());
                int width = Integer.parseInt(parts[2].trim());
                int height = Integer.parseInt(parts[3].trim());

                // Create the rectangle with parsed dimensions
                Rectangle searchArea = new Rectangle(x, y, width, height);

                // Use the rectangle in the overlayFinder.findOverlays method
                Rectangle rect = overlayFinder.findNearestOverlay(getSelectedEmulator(), getOverlayType(), searchArea, overlaysEPSSpinner.getValue(), overlaysMinPtsSpinner.getValue());
                logger.devLog("Finding nearest overlay with color: " + getOverlayType() + " in rectangle: " + searchArea);
                logger.devLog("Using EPS: " + overlaysEPSSpinner.getValue() + " and minPts: " + overlaysMinPtsSpinner.getValue());

                // Perform an action for each found rectangle
                logger.devLog("Drawing rectangle on the canvas.");
                draw.clearCanvas();
                draw.drawRectangle(rect, Color.GREEN);
            } catch (NumberFormatException e) {
                // Handle case where string parts are not all integers
                showDialog("Error", "Error parsing rectangle dimensions: Ensure all values are integers.", Alert.AlertType.ERROR);
            } catch (IllegalArgumentException e) {
                // Handle any other issues with the input format
                showDialog("Error", e.getMessage(), Alert.AlertType.ERROR);
            }
        });
        findSecondNearest.setOnAction(event -> {
            // Fetch the text from rectangleLabel
            String rectangleText = rectangleLabel.getText();

            try {
                // Attempt to parse the rectangle dimensions from the label text
                // Expected format: "new Rectangle(x, y, width, height);"
                // Example valid text: "new Rectangle(35, 32, 494, 88);"
                String cleanText = rectangleText.replace("new Rectangle(", "").replace(");", "");
                String[] parts = cleanText.split(", ");

                if (parts.length != 4) {
                    throw new IllegalArgumentException("Rectangle dimensions should contain exactly four integer values.");
                }

                int x = Integer.parseInt(parts[0].trim());
                int y = Integer.parseInt(parts[1].trim());
                int width = Integer.parseInt(parts[2].trim());
                int height = Integer.parseInt(parts[3].trim());

                // Create the rectangle with parsed dimensions
                Rectangle searchArea = new Rectangle(x, y, width, height);

                // Use the rectangle in the overlayFinder.findOverlays method
                Rectangle rect = overlayFinder.findSecondNearestOverlay(getSelectedEmulator(), getOverlayType(), searchArea, overlaysEPSSpinner.getValue(), overlaysMinPtsSpinner.getValue());
                logger.devLog("Finding second nearest overlay with color: " + getOverlayType() + " in rectangle: " + searchArea);
                logger.devLog("Using EPS: " + overlaysEPSSpinner.getValue() + " and minPts: " + overlaysMinPtsSpinner.getValue());

                // Perform an action for each found rectangle
                logger.devLog("Drawing rectangle on the canvas.");
                draw.clearCanvas();
                draw.drawRectangle(rect, Color.GREEN);
            } catch (NumberFormatException e) {
                // Handle case where string parts are not all integers
                showDialog("Error", "Error parsing rectangle dimensions: Ensure all values are integers.", Alert.AlertType.ERROR);
            } catch (IllegalArgumentException e) {
                // Handle any other issues with the input format
                showDialog("Error", e.getMessage(), Alert.AlertType.ERROR);
            }
        });

        // Set up listeners
        regionButton.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                regionButton.setText("Disable region mapping");
            } else {
                draw.clearCanvas();
                regionButton.setText("Enable region mapping");
            }
        });
    }

    private void setupPlayerTab() {
        // Initialize Player tab buttons
        leveledUp = new Button("Leveled Up");
        isRunEnabled = new Button("Is Run Enabled");
        toggleRun = new Button("Toggle Run");
        enableAutoRet = new Button("Enable Auto Retaliate");
        disableAutoRet = new Button("Disable Auto Retaliate");
        checkPixelShift = new Button("Check Pixel Shift");
        isAutoRetOn = new Button("Is Auto Retaliate On");
        getHP = new Button("Get HP");
        getPray = new Button("Get Pray");
        getRun = new Button("Get Run");
        getSpec = new Button("Get Spec");

        // Apply CSS class to buttons
        Button[] buttons = {
                leveledUp, isRunEnabled, toggleRun, enableAutoRet, disableAutoRet, checkPixelShift,
                isAutoRetOn, getHP, getPray, getRun, getSpec
        };
        for (Button btn : buttons) {
            btn.getStyleClass().add("devUITab-button");
        }

        // Set up Player tab button actions with multiline format
        leveledUp.setOnAction(event -> logger.devLog("LeveledUp is: " + player.leveledUp(getSelectedEmulator())));
        isRunEnabled.setOnAction(event -> logger.devLog("Run enabled is: " + player.isRunEnabled(getSelectedEmulator())));
        toggleRun.setOnAction(event -> {
            logger.devLog("Toggling run.");
            player.toggleRun(getSelectedEmulator());
        });
        enableAutoRet.setOnAction(event -> {
            logger.devLog("Enabling auto retaliate");
            player.enableAutoRetaliate(getSelectedEmulator());
        });
        disableAutoRet.setOnAction(event -> {
            logger.devLog("Disabling auto retaliate");
            player.disableAutoRetaliate(getSelectedEmulator());
        });
        checkPixelShift.setOnAction(event -> logger.devLog("Are we idle? : " + player.isIdle(getSelectedEmulator())));
        isAutoRetOn.setOnAction(event -> logger.devLog("Auto retaliate on is: " + player.isAutoRetaliateOn(getSelectedEmulator())));
        getHP.setOnAction(event -> logger.devLog("Current HP is: " + player.getHP(getSelectedEmulator())));
        getPray.setOnAction(event -> logger.devLog("Current Prayer is: " + player.getPray(getSelectedEmulator())));
        getRun.setOnAction(event -> logger.devLog("Current Run is: " + player.getRun(getSelectedEmulator())));
        getSpec.setOnAction(event -> logger.devLog("Current Spec is: " + player.getSpec(getSelectedEmulator())));
    }

    private void setupStatsTab() {
        // Initialize Stats tab buttons
        getRealLevelCF = new Button("Get Real Level");
        getEffectiveLevelCF = new Button("Get Effective Level");
        getTotalLevelCF = new Button("Get Total Level");

        // Initialize dropdown menu
        skillsDropdown = new ComboBox<>();
        populateSkillsDropdown(skillsDropdown);

        // Apply CSS class to buttons and dropdown
        Button[] buttons = {getRealLevelCF, getEffectiveLevelCF, getTotalLevelCF};
        for (Button btn : buttons) {
            btn.getStyleClass().add("devUITab-button");
        }
        skillsDropdown.getStyleClass().add("devUITab-dropdown");

        // Set up Stats tab button actions with multiline format
        getRealLevelCF.setOnAction(event -> {
            Skills skill = Skills.valueOf(skillsDropdown.getValue());
            logger.devLog("Real level for " + skill + " is: " + stats.getRealLevelCF(skill, getSelectedEmulator()));
        });
        getEffectiveLevelCF.setOnAction(event -> {
            Skills skill = Skills.valueOf(skillsDropdown.getValue());
            logger.devLog("Effective level for " + skill + " is: " + stats.getEffectiveLevelCF(skill, getSelectedEmulator()));
        });
        getTotalLevelCF.setOnAction(event -> logger.devLog("Total level is: " + stats.getTotalLevelCF(getSelectedEmulator())));
    }

    private void setupWalkerTab() {
        // Initialize Walker tab buttons
        setupMap = new Button("Setup Map");
        getPlayerPosition = new Button("Get Player Position");
        drawTiles = new Button("Draw Tiles");
        isReachAble = new Button("Is Reachable");
        step = new Button("Step");
        walkTo = new Button("Walk To");
        webwalkTo = new Button("Webwalk To");
        mapUIBtn = new Button("Open MapUI");
        chunksToLoad.setPromptText("Chunks");
        planesToLoad.setPromptText("Planes");

        walkerCoordField.setPromptText("x,y,z");

        // Apply CSS class to buttons, dropdown, and text field
        Button[] buttons = {setupMap, getPlayerPosition, isReachAble, step, walkTo, webwalkTo, mapUIBtn, drawTiles};
        for (Button btn : buttons) {
            btn.getStyleClass().add("devUITab-button");
        }
        walkerCoordField.getStyleClass().add("devUITab-textfield");

        drawTiles.setOnAction(event -> mm2MSProjection.drawGrid(getSelectedEmulator(), draw.getGC()));

        getPlayerPosition.setOnAction(event -> {
            // Run the task in a background thread
            Task<Void> task = new Task<>() {
                @Override
                protected Void call() {
                    walker.setup(getSelectedEmulator(), parseChunksTF(chunksToLoad, planesToLoad));
                    Tile result = walker.getPlayerPosition(getSelectedEmulator()).getWorldCoordinates(getSelectedEmulator()).getTile();

                    if (result != null) {
                        // Logging must be done on the JavaFX thread
                        Platform.runLater(() -> logger.devLog("Player position found using: " + result));
                    }
                    return null;
                }
            };

            // Start the background task
            new Thread(task).start();
        });
        isReachAble.setOnAction(event -> {
            String coords = walkerCoordField.getText();
            if (validateCoordinates(coords)) {
                String[] parts = coords.split(",");
                int x = Integer.parseInt(parts[0].trim());
                int y = Integer.parseInt(parts[1].trim());
                int z = Integer.parseInt(parts[2].trim());

                Tile tile = new Tile(x, y, z);
                Platform.runLater(() -> logger.devLog(tile + " is reachable is: " + walker.isReachable(tile, getSelectedEmulator())));
            } else {
                showDialog("Error", "Invalid coordinates format. Please enter in xxx,yyy,zzz format.", Alert.AlertType.ERROR);
            }
        });
        step.setOnAction(event -> {
            String coords = walkerCoordField.getText();
            if (validateCoordinates(coords)) {
                // Run the task in a background thread
                Task<Void> task = new Task<>() {
                    @Override
                    protected Void call() {
                        try {
                            String[] parts = coords.split(",");
                            int x = Integer.parseInt(parts[0].trim());
                            int y = Integer.parseInt(parts[1].trim());
                            int z = Integer.parseInt(parts[2].trim());

                            Tile tile = new Tile(x, y, z);

                            Platform.runLater(() -> logger.devLog("Getting position result"));

                            walker.setup(getSelectedEmulator(), parseChunksTF(chunksToLoad, planesToLoad));
                            PositionResult position = walker.getPlayerPosition(getSelectedEmulator());

                            Platform.runLater(() -> logger.devLog("Stepping to " + tile));

                            walker.stepToPoint(position.getPosition(), getSelectedEmulator(), null);
                        } catch (NumberFormatException e) {
                            Platform.runLater(() -> showDialog("Error", "Invalid number format in coordinates.", Alert.AlertType.ERROR));
                        }
                        return null;
                    }
                };

                // Start the background task
                new Thread(task).start();
            } else {
                showDialog("Error", "Invalid coordinates format. Please enter in xxx,yyy,zzz format.", Alert.AlertType.ERROR);
            }
        });
        walkTo.setOnAction(event -> {
            String coords = walkerCoordField.getText();
            if (validateCoordinates(coords)) {
                // Run the task in a background thread
                Task<Void> task = new Task<>() {
                    @Override
                    protected Void call() {
                        try {
                            String[] parts = coords.split(",");
                            int x = Integer.parseInt(parts[0].trim());
                            int y = Integer.parseInt(parts[1].trim());
                            int z = Integer.parseInt(parts[2].trim());

                            Tile tile = new Tile(x, y, z);

                            Platform.runLater(() -> logger.devLog("Getting position result"));

                            walker.setup(getSelectedEmulator(), parseChunksTF(chunksToLoad, planesToLoad));
                            PositionResult position = walker.getPlayerPosition(getSelectedEmulator());

                            org.opencv.core.Point playerPoint = position.getPosition();

                            Platform.runLater(() -> logger.devLog("Position result is: " + playerPoint));
                            Platform.runLater(() -> logger.devLog("Walking to " + tile));

                            walker.walkTo(position.getPosition(), playerPoint, getSelectedEmulator(), null);
                        } catch (NumberFormatException e) {
                            Platform.runLater(() -> showDialog("Error", "Invalid number format in coordinates.", Alert.AlertType.ERROR));
                        }
                        return null;
                    }
                };

                // Start the background task
                new Thread(task).start();
            } else {
                showDialog("Error", "Invalid coordinates format. Please enter in xxx,yyy,zzz format.", Alert.AlertType.ERROR);
            }
        });
        webwalkTo.setOnAction(event -> {
            // Run the task in a background thread
            Task<Void> task = new Task<>() {
                @Override
                protected Void call() {
                    Tile tile = new Tile(12415, 12865, 0);

                    walker.setup(getSelectedEmulator(), parseChunksTF(chunksToLoad, planesToLoad));

                    // Perform the web walk operation
                    walker.webWalk(tile, null, getSelectedEmulator(), false);

                    return null;
                }
            };

            // Start the background task
            new Thread(task).start();
        });
        mapUIBtn.setOnAction(event -> mapUI.display());
    }

    private void setupXPTab() {

        // Initialize XP tab buttons
        readXP = new Button("Read XP");
        test1 = new Button("Detect font");
        test2 = new Button("OCR Specific Font");
        test3 = new Button("OCR Any Font");
        test4 = new Button("OCR List Font");
        test5 = new Button("OCR Color Based");

        // Apply CSS class to buttons
        Button[] buttons = {readXP, test1, test2, test3, test4, test5};
        for (Button btn : buttons) {
            btn.getStyleClass().add("devUITab-button");
        }

        // XP tab spinners/integers

        // XP tab text fields

        // Set up XP tab button actions
        readXP.setOnAction(event -> logger.devLog("Read XP value is: " + xpBar.readXP(getSelectedEmulator())));


        test1.setOnAction(event -> {
            // Fetch the text from the rectangleLabel and parse it to create a Rectangle
            String rectangleText = rectangleLabel.getText();
            String cleanText = rectangleText.replace("new Rectangle(", "").replace(");", "");
            String[] parts = cleanText.split(", ");

            if (parts.length != 4) {
                throw new IllegalArgumentException("Rectangle dimensions should contain exactly four integer values.");
            }

            int x = Integer.parseInt(parts[0].trim());
            int y = Integer.parseInt(parts[1].trim());
            int width = Integer.parseInt(parts[2].trim());
            int height = Integer.parseInt(parts[3].trim());

            // Create the rectangle and Rect with parsed dimensions
            Rectangle searchArea = new Rectangle(x, y, width, height);

            BufferedImage searchImage = getGameView.getSubBuffered(getSelectedEmulator(), searchArea);

            // List of font patterns
            Map<FontName, ConcurrentHashMap<String, int[][]>> fontPatterns = Map.of(
                    FontName.BOLD_12, bold12Patterns,
                    FontName.PLAIN_11, plain11Patterns,
                    FontName.PLAIN_12, plain12Patterns,
                    FontName.QUILL_8, quill8Patterns,
                    FontName.QUILL, quill1Patterns
            );

            Map<FontName, String> results = new HashMap<>();
            Map<FontName, Long> executionTimes = new HashMap<>();

            // Run text recognition for each font pattern and measure execution time
            for (Map.Entry<FontName, ConcurrentHashMap<String, int[][]>> entry : fontPatterns.entrySet()) {
                FontName fontName = entry.getKey();
                ConcurrentHashMap<String, int[][]> pattern = entry.getValue();

                long startTime = System.currentTimeMillis();
                String result = cfOCR.findAllPatternsInImage(0, searchImage, testTextColors, fontName);
                long elapsedTime = System.currentTimeMillis() - startTime;

                results.put(fontName, result);
                executionTimes.put(fontName, elapsedTime);

                // Log execution time
                logger.devLog(fontName + " took " + elapsedTime + "ms");

                // Log result (even if empty)
                logger.devLog(fontName + " result: " + (result.isEmpty() ? "No match found" : result));
            }

            // Determine the best font based on the most results found
            FontName bestFont = results.entrySet().stream()
                    .max(Comparator.comparingInt(entry -> entry.getValue().length()))
                    .map(Map.Entry::getKey)
                    .orElse(FontName.NONE);

            logger.devLog("Best matching font: " + bestFont);
        });

        test2.setOnAction(event -> {
            // Fetch the text from the rectangleLabel and parse it to create a Rectangle
            String rectangleText = rectangleLabel.getText();

            String cleanText = rectangleText.replace("new Rectangle(", "").replace(");", "");
            String[] parts = cleanText.split(", ");

            if (parts.length != 4) {
                throw new IllegalArgumentException("Rectangle dimensions should contain exactly four integer values.");
            }

            int x = Integer.parseInt(parts[0].trim());
            int y = Integer.parseInt(parts[1].trim());
            int width = Integer.parseInt(parts[2].trim());
            int height = Integer.parseInt(parts[3].trim());

            // Create the rectangle and Rect with parsed dimensions
            Rectangle searchArea = new Rectangle(x, y, width, height);

            BufferedImage searchImage = getGameView.getSubBuffered(getSelectedEmulator(), searchArea);

            long startTime = System.currentTimeMillis();
            String result = cfOCR.findAllPatternsInImage(0, searchImage, testTextColors, FontName.BOLD_12);
            long elapsedTime = System.currentTimeMillis() - startTime;
            logger.devLog("OCR Results BOLD 12: \n" + result + " in " + elapsedTime + "ms.");
        });

        test3.setOnAction(event -> {
            // Fetch the text from the rectangleLabel and parse it to create a Rectangle
            String rectangleText = rectangleLabel.getText();

            String cleanText = rectangleText.replace("new Rectangle(", "").replace(");", "");
            String[] parts = cleanText.split(", ");

            if (parts.length != 4) {
                throw new IllegalArgumentException("Rectangle dimensions should contain exactly four integer values.");
            }

            int x = Integer.parseInt(parts[0].trim());
            int y = Integer.parseInt(parts[1].trim());
            int width = Integer.parseInt(parts[2].trim());
            int height = Integer.parseInt(parts[3].trim());

            // Create the rectangle and Rect with parsed dimensions
            Rectangle searchArea = new Rectangle(x, y, width, height);

            BufferedImage searchImage = getGameView.getSubBuffered(getSelectedEmulator(), searchArea);

            long startTime = System.currentTimeMillis();
            String result = cfOCR.findAllPatternsInImage(0, searchImage, testTextColors, FontName.BOLD_12);
            long elapsedTime = System.currentTimeMillis() - startTime;
            logger.devLog("OCR Results BOLD 12: \n" + result + "\nin " + elapsedTime + "ms.");
            long startTime2 = System.currentTimeMillis();
            String result2 = cfOCR.findAllPatternsInImage(0, searchImage, testTextColors, FontName.PLAIN_11);
            long elapsedTime2 = System.currentTimeMillis() - startTime2;
            logger.devLog("OCR Results PLAIN 11: \n" + result2 + "\nin " + elapsedTime2 + "ms.");
            long startTime3 = System.currentTimeMillis();
            String result3 = cfOCR.findAllPatternsInImage(0, searchImage, testTextColors, FontName.PLAIN_12);
            long elapsedTime3 = System.currentTimeMillis() - startTime3;
            logger.devLog("OCR Results PLAIN 12: \n" + result3 + "\nin " + elapsedTime3 + "ms.");
            long startTime5 = System.currentTimeMillis();
            String result5 = cfOCR.findAllPatternsInImage(0, searchImage, testTextColors, FontName.QUILL);
            long elapsedTime5 = System.currentTimeMillis() - startTime5;
            logger.devLog("OCR Results Quill: \n" + result5 + "\nin " + elapsedTime5 + "ms.");
            long startTime4 = System.currentTimeMillis();
            String result4 = cfOCR.findAllPatternsInImage(0, searchImage, testTextColors, FontName.QUILL_8);
            long elapsedTime4 = System.currentTimeMillis() - startTime4;
            logger.devLog("OCR Results Quill 8: \n" + result4 + "\nin " + elapsedTime4 + "ms.");
        });

        test5.setOnAction(event -> {
            // Fetch the text from the rectangleLabel and parse it to create a Rectangle
            String rectangleText = rectangleLabel.getText();

            String cleanText = rectangleText.replace("new Rectangle(", "").replace(");", "");
            String[] parts = cleanText.split(", ");

            if (parts.length != 4) {
                throw new IllegalArgumentException("Rectangle dimensions should contain exactly four integer values.");
            }

            int x = Integer.parseInt(parts[0].trim());
            int y = Integer.parseInt(parts[1].trim());
            int width = Integer.parseInt(parts[2].trim());
            int height = Integer.parseInt(parts[3].trim());

            // Create the rectangle with parsed dimensions
            Rectangle searchArea = new Rectangle(x, y, width, height);

            //String ocrResults = cfOCR.ocrDefaultColors(searchArea, getSelectedEmulator());
            logger.devLog("OCR Results Default Colors: ");
        });
    }

    private void handleRegionMapping(int x, int y) {
        Point currentClickPoint = new Point(x, y);

        // Check if two points are already selected
        if (firstClickPoint != null && secondClickPoint != null) {
            draw.clearCanvas();
        }

        // Store the coordinates
        if (firstClickPoint == null) {
            draw.clearCanvas();
            firstClickPoint = currentClickPoint;
            draw.drawOnCanvas(x, y, 3, 3, Color.BLUE, ShapeType.CIRCLE);
        } else if (secondClickPoint == null) {
            secondClickPoint = currentClickPoint;
            draw.drawOnCanvas(secondClickPoint.x, secondClickPoint.y, 3, 3, Color.BLUE, ShapeType.CIRCLE);

            draw.clearCanvas();

            // Draw the rectangle between the two points
            double width = secondClickPoint.x - firstClickPoint.x;
            double height = secondClickPoint.y - firstClickPoint.y;
            draw.drawOnCanvas(firstClickPoint.x, firstClickPoint.y, width, height, Color.BLUE, ShapeType.RECTANGLE);

            // Process region selection
            processRegionSelection();
        }
    }

    private void processRegionSelection() {
        int width = Math.abs(secondClickPoint.x - firstClickPoint.x);
        int height = Math.abs(secondClickPoint.y - firstClickPoint.y);

        String rectangleText = String.format("new Rectangle(%d, %d, %d, %d);",
                firstClickPoint.x, firstClickPoint.y, width, height);
        rectangleLabel.setText(rectangleText);

        firstClickPoint = null;
        secondClickPoint = null;
    }

    private String getSelectedEmulator() {
        String selectedEmulator = emulatorListView.getSelectionModel().getSelectedItem();
        if (selectedEmulator == null || selectedEmulator.isEmpty()) {
            return "";
        }
        return selectedEmulator;
    }

    private void copyToClipboard(String text) {
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString(text);
        clipboard.setContent(content);
        logger.devLog("Copied to clipboard: " + text);
    }

    private void distributeButtonsAlphabetically(List<Button> buttons, HBox row1, HBox row2, HBox row3) {
        // Sort buttons by their text labels
        buttons.sort(Comparator.comparing(Button::getText));

        // Calculate the number of rows
        int numberOfRows = 3;
        // Calculate the number of columns based on the number of buttons and number of rows
        int numberOfColumns = (int) Math.ceil(buttons.size() / (double) numberOfRows);

        // Assign buttons to rows in a column-wise fashion
        for (int i = 0; i < buttons.size(); i++) {
            int row = i % numberOfRows;
            int col = i / numberOfRows;

            if (row == 0) {
                if (col < numberOfColumns) row1.getChildren().add(buttons.get(i));
            } else if (row == 1) {
                if (col < numberOfColumns) row2.getChildren().add(buttons.get(i));
            } else if (row == 2) {
                if (col < numberOfColumns) row3.getChildren().add(buttons.get(i));
            }
        }
    }

    private OverlayType getOverlayType() {
        switch (overlayDropdown.getValue()) {
            case "Fishing":
                return OverlayType.FISHING;
            case "Items":
                return OverlayType.GROUND_ITEM;
            case "Agility":
                return OverlayType.AGILITY;
            default:
                throw new IllegalArgumentException("Unexpected value: " + overlayDropdown.getValue());
        }
    }

    private void populateDropdown(SearchableComboBox<String> dropdown) {
        ObservableList<String> items = FXCollections.observableArrayList();
        for (Spells spell : Spells.values()) {
            items.add(spell.name());
        }
        dropdown.setItems(items);
    }

    private void populateSkillsDropdown(ComboBox<String> dropdown) {
        for (Skills skill : Skills.values()) {
            if (skill != Skills.TOTAL) {
                // Add skill name in uppercase
                dropdown.getItems().add(skill.name());
            }
        }
        dropdown.getSelectionModel().selectFirst(); // Pre-select the first entry
    }

    // Validate coordinates format (xxx,yyy)
    private boolean validateCoordinates(String coords) {
        return coords.matches("\\d+\\s*,\\s*\\d+\\s*,\\s*\\d+");
    }

    private void tapSpellFromDropdown(ComboBox<String> dropdown, String spellbook) {
        String selectedSpell = dropdown.getValue();
        if (selectedSpell != null) {
            String device = getSelectedEmulator();
            System.out.println("Tapping spell: " + selectedSpell + " in spellbook: " + spellbook);
            magic.tapSpell(device, selectedSpell);
        } else {
            System.out.println("No spell selected from the " + spellbook + " spellbook.");
        }
    }

    private boolean isSimilarColorExists(Set<Color> colorSet, Color color, double tolerance) {
        for (Color existingColor : colorSet) {
            if (calculateColorDistance(existingColor, color) <= tolerance) {
                return true;
            }
        }
        return false;
    }

    private double calculateColorDistance(Color c1, Color c2) {
        double redDiff = c1.getRed() - c2.getRed();
        double greenDiff = c1.getGreen() - c2.getGreen();
        double blueDiff = c1.getBlue() - c2.getBlue();
        return Math.sqrt(redDiff * redDiff + greenDiff * greenDiff + blueDiff * blueDiff) * 255;
    }

    private void startSearch() {
        // Check if the necessary components are available
        if (uploadedFile == null) {
            logger.devLog("Source image null");
            showDialog("Error", "Source image null", Alert.AlertType.ERROR);
            irToggleSearch.setSelected(false);
            irToggleSearch.setText("Start search");
            return;
        }

        // Disable the spinner
        irSearchSpeed.setDisable(true);

        if (searchTimeline != null) {
            searchTimeline.stop(); // If there's an ongoing search, stop it before starting a new one.
        }

        searchTimeline = new Timeline(new KeyFrame(Duration.millis(irSearchSpeed.getValue()), t -> processImageChange(getCurrentImageAsMat(), irSearchThreshold.getValue(), uploadedFile)));

        searchTimeline.setCycleCount(Timeline.INDEFINITE); // The timeline runs indefinitely until stopped.
        searchTimeline.play(); // Start the timeline.
        irToggleSearch.setText("Stop searching");
    }

    private void stopSearch() {
        // Stop the timeline, effectively stopping the search.
        if (searchTimeline != null) {
            searchTimeline.stop();
        }

        // Re-enable the spinner, ensuring this UI change is done in the JavaFX Application Thread.
        Platform.runLater(() -> irSearchSpeed.setDisable(false));
        irToggleSearch.setText("Start search");
    }

    private boolean validateColorSearchInputs() {
        // Validate that the necessary components are available
        if (rectangleLabel.getText().isEmpty()) {
            logger.devLog("Rectangle dimensions are not specified.");
            return false;
        }
        if (hexColors.getText().isEmpty()) {
            logger.devLog("Hex colors are not specified.");
            return false;
        }
        return true;
    }

    private void startColorSearch() {
        // Validate necessary components before starting the search
        if (!validateColorSearchInputs()) {
            cfToggleSearch.setSelected(false);
            cfToggleSearch.setText("Start search");
            return;
        }

        // Disable the spinner
        cfToleranceSpinner.setDisable(true);

        if (colorSearchTimeline != null) {
            colorSearchTimeline.stop(); // If there's an ongoing search, stop it before starting a new one.
        }

        colorSearchTimeline = new Timeline(new KeyFrame(Duration.millis(300), t -> performColorSearch()));

        colorSearchTimeline.setCycleCount(Timeline.INDEFINITE); // The timeline runs indefinitely until stopped.
        colorSearchTimeline.play(); // Start the timeline.
        cfToggleSearch.setText("Stop searching");
    }

    private void performColorSearch() {
        // Fetch the text from the rectangleLabel and parse it to create a Rectangle
        String rectangleText = rectangleLabel.getText();

        // clear canvas
        draw.clearCanvas();

        try {
            String cleanText = rectangleText.replace("new Rectangle(", "").replace(");", "");
            String[] parts = cleanText.split(", ");

            if (parts.length != 4) {
                throw new IllegalArgumentException("Rectangle dimensions should contain exactly four integer values.");
            }

            int x = Integer.parseInt(parts[0].trim());
            int y = Integer.parseInt(parts[1].trim());
            int width = Integer.parseInt(parts[2].trim());
            int height = Integer.parseInt(parts[3].trim());

            // Create the rectangle with parsed dimensions

            // Create two points: upper left and bottom right of the rectangle
            Point point1 = new Point(x, y);
            Point point2 = new Point(x + width, y + height);

            // Parse the hex colors from the hexColors text field
            String hexColorsText = hexColors.getText();
            String[] hexColorsArray = hexColorsText.split(",");

            // Prepare a list to collect all found points
            List<Point> allFoundPoints = new ArrayList<>();

            for (String hex : hexColorsArray) {
                java.awt.Color awtColor = java.awt.Color.decode(hex.trim());

                // Find the color at the position
                List<Point> foundPoints = colorFinder.findColorAtPosition(
                        getSelectedEmulator(), awtColor, point1, point2, cfToleranceSpinner.getValue());

                // Add all found points to the list
                allFoundPoints.addAll(foundPoints);
            }

            // Draw each point to the canvas
            draw.drawPoints(allFoundPoints, Color.GREEN, drawLines.isSelected());

            // Log the result
            logger.devLog("Found and drawn points: " + allFoundPoints.size());

        } catch (NumberFormatException e) {
            showDialog("Error", "Error parsing rectangle dimensions: Ensure all values are integers.", Alert.AlertType.ERROR);
        } catch (IllegalArgumentException e) {
            showDialog("Error", e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void stopColorSearch() {
        // Stop the timeline, effectively stopping the search.
        if (colorSearchTimeline != null) {
            colorSearchTimeline.stop();
        }

        // Re-enable the spinner, ensuring this UI change is done in the JavaFX Application Thread.
        Platform.runLater(() -> cfToleranceSpinner.setDisable(false));
        cfToggleSearch.setText("Start search");
    }

    private void processImageChange(Mat gameImageMat, double searchThreshold, File sourceImageFile) {
        if (gameImageMat == null || gameImageMat.empty()) {
            logger.devLog("Game image null or empty");
            Platform.runLater(() -> {
                irToggleSearch.setSelected(false);
                irToggleSearch.setText("Start search");
            });
            return;
        }

        List<MatchedRectangle> rectangles = imageRecognition.returnAllMatchObjects(sourceImageFile, gameImageMat, searchThreshold);

        Platform.runLater(() -> {
            draw.clearCanvas(); // Clear the previous drawings on the canvas

            if (!rectangles.isEmpty()) {
                for (MatchedRectangle rectangle : rectangles) {
                    double foundX = rectangle.x;
                    double foundY = rectangle.y;
                    double width = rectangle.width;
                    double height = rectangle.height;
                    double matchValue = rectangle.getMatchValue();
                    draw.drawOnCanvas(foundX, foundY, width, height, Color.GREEN, ShapeType.RECTANGLE);
                    logger.devLog("Object found and drawn on canvas, match score: " + matchValue);
                }
            } else {
                logger.devLog("No objects found!");
            }
        });
    }

    private void initializeImageView() {
        emulatorListView = getEmulatorDevListView();
        gameView.setFitWidth(894);
        gameView.setFitHeight(540);
        emulatorListView.setPrefWidth(1150 - 894); // Adjust to fit within the 1150px limit
        emulatorListView.setMaxWidth(1150 - 894);

        emulatorListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            emulatorManager.monitorEmulators();

            if (oldValue != null) {
                stopImageUpdater(oldValue, false);
            }

            if (newValue != null) {
                logger.print("New emulator selected: " + newValue);
                startImageUpdater(newValue);
            }
        });

        // Add action handler for the pause button
        pauseButton.setOnAction(event -> {
            isPaused = !isPaused;
            Platform.runLater(() -> {
                if (isPaused) {
                    pauseButton.setText("Resume capture");
                    // Stop updating the image view when paused
                    stopImageUpdater(emulatorListView.getSelectionModel().getSelectedItem(), true);
                } else {
                    pauseButton.setText("Pause capture");
                    // Resume updating the image view with the selected emulator
                    String selectedEmulator = emulatorListView.getSelectionModel().getSelectedItem();
                    if (selectedEmulator != null) {
                        startImageUpdater(selectedEmulator);
                    }
                }
            });
        });
    }

    /**
     * Starts a new image updater task for the specified emulator.
     *
     * @param emulatorID The identifier of the emulator.
     */
    private void startImageUpdater(String emulatorID) {
        logger.print("Starting image updater for: " + emulatorID + " at " + System.currentTimeMillis());

        // Cancel any existing update task to avoid multiple concurrent tasks
        if (imageUpdateTask != null && !imageUpdateTask.isCancelled()) {
            imageUpdateTask.cancel(true);
            logger.print("Existing image update task canceled for: " + emulatorID + " at " + System.currentTimeMillis());
        }

        // Schedule a new task to periodically update the ImageView
        imageUpdateTask = threadManager.getScheduler().scheduleWithFixedDelay(() -> {
            try {
                BufferedImage latestScreenshot = emulatorManager.getLatestScreenshot(getSelectedEmulator()) != null
                        ? emulatorManager.getLatestScreenshot(getSelectedEmulator())
                        : null;

                if (latestScreenshot != null) {
                    updateImageView(emulatorID, latestScreenshot);
                } else {
                    logger.print("No new screenshot available for: " + emulatorID + ", will retry in " + GAME_REFRESHRATE.get() + " ms");
                }
            } catch (Exception e) {
                logger.err("Error while updating screenshot for " + emulatorID + ": " + e.getMessage());
                e.printStackTrace();
            }
        }, (GAME_REFRESHRATE.get()), (GAME_REFRESHRATE.get()), TimeUnit.MILLISECONDS);
    }

    /**
     * Stops the image updater task for the specified emulator and clears the ImageView.
     *
     * @param emulatorID The identifier of the emulator.
     */
    private void stopImageUpdater(String emulatorID, boolean pause) {
        logger.print("Stopping image updater for: " + emulatorID);

        if (imageUpdateTask != null && !imageUpdateTask.isCancelled()) {
            imageUpdateTask.cancel(true);
            logger.print("Image update task canceled for: " + emulatorID);
            imageUpdateTask = null;
        }

        if (!pause) {
            // Clear the ImageView
            Platform.runLater(() -> {
                gameView.setImage(null);
                logger.print("ImageView cleared for: " + emulatorID);
            });
        }
    }

    /**
     * Updates the ImageView with the latest image from the emulator.
     *
     * @param emulatorID  The identifier of the emulator.
     * @param latestImage The latest screenshot to be displayed.
     */
    private void updateImageView(String emulatorID, BufferedImage latestImage) {
        if (latestImage == null) {
            return;
        }

        threadManager.getUnifiedExecutor().submit(() -> Platform.runLater(() -> {
            try {
                gameView.setImage(SwingFXUtils.toFXImage(latestImage, null));
            } catch (Exception e) {
                logger.err("Failed to update ImageView for device: " + emulatorID + ". Error: " + e.getMessage());
                e.printStackTrace();
            }
        }));
    }

    private Mat getCurrentImageAsMat() {
        Image img = gameView.getImage();
        return imageUtils.convertFXImageToMat(img);
    }

    // Helper method for cropping the top 10 pixels from the item image
    private Mat getCroppedItemImage(Mat itemImage) {
        Rect itemROI = new Rect(0, 10, itemImage.width(), itemImage.height() - 10);
        return new Mat(itemImage, itemROI);
    }

    public void initializeLogAreaListener() {
        // Add a listener to isDevUIOpen property
        IS_DEVUI_OPEN.addListener((observable, oldValue, newValue) -> {
            // Check if the property changed from false to true
            if (!oldValue && newValue) {
                logHBox.getChildren().remove(logArea);
                logArea = logAreaInstance.getLogTextArea();
                logHBox.getChildren().add(logArea);
            }
        });
    }

    public void display(MenuButton menuButton) {
        if (stage == null) {
            stage = new Stage();
            initializeUI(menuButton);
            stage.initModality(Modality.NONE);
            stage.setTitle("Mufasa Development UI");
            stage.setResizable(false); // Makes the UI not resizable
            stage.setWidth(1150); // Set the width limit
            stage.getIcons().add(MUFASA_LOGO); // Sets the UI icon
            Scene scene = new Scene(developerView);
            scene.getStylesheets().add(STYLESHEET);
            stage.setScene(scene);
            stage.setOnCloseRequest(event -> {
                DEVLOGS_ENABLED.set(false);
                IS_DEVUI_OPEN.set(false);
                emulatorListView.getSelectionModel().clearSelection();

                // Make sure we stop the image updater in here!
                if (imageUpdateTask != null && !imageUpdateTask.isCancelled()) {
                    imageUpdateTask.cancel(true);
                    logger.print("Image update task canceled on window close.");
                }
            });
        }

        IS_DEVUI_OPEN.set(true);
        stage.show();
    }

    @FunctionalInterface
    private interface Supplier<T> {
        T get();
    }
}