package UI;

import UI.components.IconComboBoxItem;
import UI.components.IconListViewItem;
import UI.components.MultiSelectComboBox;
import UI.components.SliderWithText;
import UI.scripts.AnnotationControls;
import UI.scripts.MutableValue;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import helpers.annotations.ScriptConfiguration;
import helpers.scripts.utils.ScriptConfigurationWrapper;
import helpers.utils.OptionType;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.controlsfx.control.SearchableComboBox;
import utils.CredentialsManager;
import utils.SystemUtils;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static utils.Constants.*;

public class ScriptConfigurationUI {
    private final CredentialsManager credMgr;
    private Stage stage;
    private final List<ScriptConfiguration> configurations;
    private final Map<String, Control> controlMap = new HashMap<>();
    private String selectedAccount;
    private final MutableValue<String> selectedBankValue = new MutableValue<>("");
    private final String scriptName;
    SearchableComboBox<String> accountComboBox;

    private final AnnotationControls annotationControls;

    public ScriptConfigurationUI(AnnotationControls annotationControls, CredentialsManager credMgr, ScriptConfigurationWrapper wrapper, String scriptName) {
        this.credMgr = credMgr;
        this.scriptName = scriptName;
        this.configurations = new ArrayList<>();
        this.annotationControls = annotationControls;

        // Merge standalone configurations and tab group configurations
        configurations.addAll(wrapper.getStandaloneConfigurations());
        for (ScriptConfigurationWrapper.ScriptTabGroup tabGroup : wrapper.getTabGroups()) {
            configurations.addAll(tabGroup.getConfigurations());
        }

        initUI(wrapper);
    }

    private void initUI(ScriptConfigurationWrapper configurations) {
        Map<String, String> lastConfigurations = loadConfigurations(scriptName);

        stage = new Stage();
        stage.setTitle("Script Configuration for " + scriptName);
        stage.getIcons().add(MUFASA_LOGO);

        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(10));
        vbox.setFillWidth(true);

        // Process configurations
        if (!configurations.getStandaloneConfigurations().isEmpty() && !configurations.getTabGroups().isEmpty()) {
            vbox.getChildren().add(createTabPaneWithMainTab(configurations, lastConfigurations));
        } else if (!configurations.getStandaloneConfigurations().isEmpty()) {
            VBox standaloneContent = createStandaloneContent(configurations.getStandaloneConfigurations(), lastConfigurations);
            standaloneContent.getChildren().add(0, createAccountSelection());
            vbox.getChildren().add(standaloneContent);
        } else if (!configurations.getTabGroups().isEmpty()) {
            vbox.getChildren().add(createTabPane(configurations.getTabGroups(), lastConfigurations));
        } else {
            // Default case: add account selection if there are no configurations
            vbox.getChildren().add(createAccountSelection());
        }

        vbox.getChildren().add(createStartScriptButton(accountComboBox));

        Scene scene = new Scene(vbox);
        scene.getStylesheets().add(STYLESHEET);
        Objects.requireNonNull(stage).initModality(Modality.APPLICATION_MODAL);
        stage.setScene(scene);
    }

    private Button createStartScriptButton(SearchableComboBox<String> accountComboBox) {
        Button btnOk = new Button("Start script");
        btnOk.setOnAction(e -> {
            selectedAccount = accountComboBox.getValue();
            stage.close();
        });
        btnOk.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(btnOk, Priority.ALWAYS);
        VBox.setMargin(btnOk, new Insets(10, 0, 0, 0));
        return btnOk;
    }

    private HBox createAccountSelection() {
        Label accountLabel = new Label("Account");
        accountLabel.getStyleClass().add("subheader-label");

        Label accountDescr = new Label("Select the account config you'd like to use");

        accountComboBox = setupAccountConfig();
        accountComboBox.getStyleClass().add("main-dropdown");
        accountComboBox.setPrefWidth(300);
        accountComboBox.setMaxHeight(25);

        VBox accountVBox = new VBox(5, accountLabel, accountDescr);
        HBox accountHBox = new HBox(10, accountVBox, createSpacer(), accountComboBox);
        accountHBox.setAlignment(Pos.CENTER_LEFT);
        return accountHBox;
    }

    private VBox createStandaloneContent(List<ScriptConfiguration> standaloneConfigs, Map<String, String> lastConfigurations) {
        VBox standaloneContent = new VBox(10);
        standaloneContent.setPadding(new Insets(10));

        for (ScriptConfiguration config : standaloneConfigs) {
            renderConfiguration(config, lastConfigurations, standaloneContent);
        }

        return standaloneContent;
    }

    private TabPane createTabPaneWithMainTab(ScriptConfigurationWrapper configurations, Map<String, String> lastConfigurations) {
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // Create standalone content and add account selection at the top
        VBox mainTabContent = createStandaloneContent(configurations.getStandaloneConfigurations(), lastConfigurations);
        mainTabContent.getChildren().add(0, createAccountSelection());

        // Add "Main" tab
        Tab mainTab = new Tab("Main", mainTabContent);
        tabPane.getTabs().add(mainTab);

        // Add existing tab groups
        for (ScriptConfigurationWrapper.ScriptTabGroup tabGroup : configurations.getTabGroups()) {
            tabPane.getTabs().add(createTab(tabGroup, lastConfigurations));
        }

        return tabPane;
    }

    private TabPane createTabPane(List<ScriptConfigurationWrapper.ScriptTabGroup> tabGroups, Map<String, String> lastConfigurations) {
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // Add account selection to the first tab
        boolean isFirstTab = true;

        for (ScriptConfigurationWrapper.ScriptTabGroup tabGroup : tabGroups) {
            VBox tabContent = new VBox(10);
            tabContent.setPadding(new Insets(10));

            // Add account selection to the first tab only
            if (isFirstTab) {
                tabContent.getChildren().add(createAccountSelection());
                isFirstTab = false; // Ensure it only happens for the first tab
            }

            for (ScriptConfiguration config : tabGroup.getConfigurations()) {
                renderConfiguration(config, lastConfigurations, tabContent);
            }

            Tab tab = new Tab(tabGroup.getTabName(), tabContent);
            tabPane.getTabs().add(tab);
        }

        return tabPane;
    }

    private Tab createTab(ScriptConfigurationWrapper.ScriptTabGroup tabGroup, Map<String, String> lastConfigurations) {
        VBox tabContent = new VBox(10);
        tabContent.setPadding(new Insets(10));

        for (ScriptConfiguration config : tabGroup.getConfigurations()) {
            renderConfiguration(config, lastConfigurations, tabContent);
        }

        return new Tab(tabGroup.getTabName(), tabContent);
    }

    private Region createSpacer() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    private void renderConfiguration(ScriptConfiguration config, Map<String, String> lastConfigurations, VBox parent) {
        // Title for the configuration (name)
        Label titleLabel = new Label(config.name());
        titleLabel.getStyleClass().add("subheader-label");

        // Description
        Text descriptionText = new Text(config.description());
        descriptionText.getStyleClass().add("text-white");
        TextFlow descriptionFlow = new TextFlow(descriptionText);
        descriptionFlow.setMaxWidth(300);
        descriptionFlow.setPrefWidth(300);

        // Determine if the control should be placed below the description
        boolean isSpecialControl = config.optionType() == OptionType.BANKTABS || config.optionType() == OptionType.WORLDHOPPER || config.optionType() == OptionType.TEXT_AREA;

        if (isSpecialControl) {
            // For BANKTABS and WORLDHOPPER, add the description and control vertically
            parent.getChildren().addAll(titleLabel, descriptionFlow);

            // Create control
            String lastValue = lastConfigurations.getOrDefault(config.name(), config.defaultValue());
            Node controlNode = createControlForType(config, parent, lastValue, lastConfigurations);

            if (controlNode != null) {
                parent.getChildren().add(controlNode); // Add control below description
            }
        } else {
            // For regular controls, add description and control horizontally within an HBox
            // Create control
            String lastValue = lastConfigurations.getOrDefault(config.name(), config.defaultValue());
            Node controlNode = createControlForType(config, parent, lastValue, lastConfigurations);

            // Create HBox to hold description and control
            HBox hBox = new HBox(10); // Spacing of 10
            hBox.setAlignment(Pos.CENTER_LEFT);

            // Create spacer to push the control to the right
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            if (controlNode != null) {
                if (controlNode instanceof Control) {
                    ((Control) controlNode).setPrefWidth(200); // Set preferred width for controls
                    controlMap.put(config.name(), (Control) controlNode);
                }
                // Add description, spacer, and control to HBox
                hBox.getChildren().addAll(descriptionFlow, spacer, controlNode);
            }

            // Add title and HBox to parent VBox
            parent.getChildren().addAll(titleLabel, hBox);
        }
    }

    private Map<String, String> collectControls(Map<String, String> selectedConfigurations) {
        for (ScriptConfiguration config : configurations) {

            if (config.optionType() == OptionType.DESCRIPTION) {
                continue;
            }

            Control control = controlMap.get(config.name());
            String selectedValue = null; // Initialize with null

            switch (config.optionType()) {
                case STRING:
                    if (control instanceof SearchableComboBox<?>) {
                        SearchableComboBox<?> comboBox = (SearchableComboBox<?>) control;
                        if (comboBox.getValue() instanceof IconComboBoxItem) {
                            IconComboBoxItem selectedItem = (IconComboBoxItem) comboBox.getValue();
                            selectedValue = selectedItem != null ? selectedItem.getText() : null;
                        } else if (comboBox.getValue() != null) {
                            selectedValue = comboBox.getValue().toString();
                        }
                    }
                    break;
                case INTEGER:
                    if (control instanceof Spinner<?>) {
                        Spinner<?> spinner = (Spinner<?>) control;
                        if (spinner.getValue() instanceof Integer) {
                            selectedValue = String.valueOf(spinner.getValue());
                        }
                    }
                    break;
                case INTEGER_SLIDER:
                    if (control instanceof SliderWithText) {
                        SliderWithText slider = (SliderWithText) control;
                        selectedValue = String.format("%.0f", slider.getValue());  // Assuming you want integer value as string
                    }
                    break;
                case BOOLEAN:
                    if (control instanceof CheckBox) {
                        selectedValue = String.valueOf(((CheckBox) control).isSelected());
                    }
                    break;
                case BANKTABS:
                    selectedValue = selectedBankValue.getValue();
                    break;
                case PERCENTAGE:
                    if (control instanceof Slider) {
                        Slider slider = (Slider) control;
                        selectedValue = String.format("%.0f", slider.getValue());
                    }
                    break;
                case MULTI_SELECTION:
                    if (control instanceof MultiSelectComboBox) {
                        MultiSelectComboBox multiSelectComboBox = (MultiSelectComboBox) control;
                        selectedValue = multiSelectComboBox.getSelectedItems().stream()
                                .map(IconListViewItem::getText)
                                .reduce((item1, item2) -> item1 + "," + item2)
                                .orElse("");
                    }
                    break;
                case TEXT_AREA:
                    if (control instanceof TextArea) {
                        selectedValue = ((TextArea) control).getText();
                    }
                    break;
                case WORLDHOPPER:
                    ToggleButton toggleButton = (ToggleButton) controlMap.get(config.name() + ".enabled");
                    if (toggleButton != null) {
                        boolean isWorldHoppingEnabled = toggleButton.isSelected();
                        selectedConfigurations.put(config.name() + ".enabled", String.valueOf(isWorldHoppingEnabled));
                        System.out.println(config.name() + ".enabled: " + isWorldHoppingEnabled);
                    }

                    CheckBox wdhCheckBox = (CheckBox) controlMap.get(config.name() + ".useWDH");
                    if (wdhCheckBox != null) {
                        boolean isWDHEnabled = wdhCheckBox.isSelected();
                        selectedConfigurations.put(config.name() + ".useWDH", String.valueOf(isWDHEnabled));
                        System.out.println(config.name() + ".useWDH: " + isWDHEnabled);
                    }

                    ComboBox<?> worldHoppingOptions = (ComboBox<?>) controlMap.get(config.name());
                    if (worldHoppingOptions != null && worldHoppingOptions.getValue() != null) {
                        selectedValue = worldHoppingOptions.getValue().toString();
                        selectedConfigurations.put(config.name(), selectedValue);
                        System.out.println(config.name() + ": " + selectedValue);
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported configuration type: " + config.optionType());

            }
            selectedConfigurations.put(config.name(), selectedValue);
        }
        return selectedConfigurations;
    }

    private Node createControlForType(ScriptConfiguration config, VBox vbox, String lastValue, Map<String, String> lastConfigurations) {
        Node control;

        switch (config.optionType()) {
            case STRING:
                control = annotationControls.createStringControl(config, lastValue);
                break;
            case INTEGER:
                control = annotationControls.createIntegerControl(config, lastValue);
                break;
            case INTEGER_SLIDER:
                control = annotationControls.createIntegerSliderControl(config, lastValue);
                break;
            case BOOLEAN:
                control = annotationControls.createBooleanControl(config, lastValue);
                break;
            case MULTI_SELECTION:
                control = annotationControls.createMultiSelectControl(config, lastValue);
                break;
            case BANKTABS:
                control = annotationControls.createBankControl(config, lastValue, selectedBankValue);
                break;
            case PERCENTAGE:
                control = annotationControls.createPercentageControl(config, vbox, lastValue);
                break;
            case TEXT_AREA:
                control = annotationControls.createTextAreaControl(config, lastValue);
                break;
            case WORLDHOPPER:
                control = annotationControls.createWorldhopperControl(
                        controlMap,
                        config,
                        lastConfigurations.get(config.name()),
                        lastConfigurations.get(config.name() + ".useWDH"),
                        Boolean.parseBoolean(lastConfigurations.getOrDefault(config.name() + ".enabled", config.defaultValue()))
                );
                break;
            case DESCRIPTION:
                control = annotationControls.createDescriptionControl(config);
                break;
            default:
                throw new IllegalArgumentException("Unsupported configuration type: " + config.optionType());
        }
        return control;
    }

    public SearchableComboBox<String> setupAccountConfig() {
        SearchableComboBox<String> accountComboBox = new SearchableComboBox<>();
        accountComboBox.getStyleClass().add("main-dropdown");
        accountComboBox.setMaxWidth(Double.MAX_VALUE);

        // Populate accountComboBox with account names using CredMgr
        Set<String> users = credMgr.loadUsers();
        if (users != null && !users.isEmpty()) {
            accountComboBox.getItems().addAll(users);
            accountComboBox.getSelectionModel().selectFirst();
        } else {
            accountComboBox.setPromptText("No users found");
        }

        return accountComboBox;
    }

    private void saveConfigurations(String scriptName, Map<String, String> configurations) {
        System.out.println("Saving configurations for " + scriptName + ": " + configurations);
        Path configFilePath = Paths.get(SystemUtils.getSystemPath(), "ScriptConfigurations.json");

        // Load existing configurations if the file exists
        Map<String, Map<String, String>> allConfigurations = loadAllConfigurations();

        // Update or add the new configuration for the current script
        allConfigurations.put(scriptName, configurations);

        // Write the updated configurations back to the file
        try (Writer writer = new FileWriter(configFilePath.toFile())) {
            Gson gson = new Gson();
            gson.toJson(allConfigurations, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Map<String, String> loadConfigurations(String scriptName) {
        Map<String, Map<String, String>> allConfigurations = loadAllConfigurations();
        return allConfigurations.getOrDefault(scriptName, Map.of());
    }

    private Map<String, Map<String, String>> loadAllConfigurations() {
        Path configFilePath = Paths.get(SystemUtils.getSystemPath(), "ScriptConfigurations.json");

        if (Files.exists(configFilePath)) {
            try (Reader reader = new FileReader(configFilePath.toFile())) {
                Gson gson = new Gson();
                Type type = new TypeToken<Map<String, Map<String, String>>>() {}.getType();
                return gson.fromJson(reader, type);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return new HashMap<>(); // Return an empty map if the file does not exist
    }

    private Map<String, String> collectConfigurations() {
        Map<String, String> selectedConfigurations = new HashMap<>();

        // Include the selected account or bank pin
        selectedConfigurations.put("selectedAccount", selectedAccount);
        selectedConfigurations.putAll(collectControls(selectedConfigurations));

        saveConfigurations(scriptName, selectedConfigurations);
        return selectedConfigurations;
    }

    public Map<String, String> showAndGetConfigurations() {
        stage.showAndWait();
        return collectConfigurations();
    }
}