package UI;

import UI.components.IconListViewItem;
import helpers.ScriptCategory;
import helpers.scripts.ScriptInstanceLoader;
import helpers.scripts.utils.MetadataDTO;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import scripts.ScriptExecutor;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static utils.Constants.*;

public class ScriptSelectionUI {
    private final ScriptExecutor scriptHandler;
    private final ScriptInstanceLoader scriptInstanceLoader;
    private final ObservableList<MetadataDTO> allLoadedScripts = FXCollections.observableArrayList();
    private final ListView<IconListViewItem> categoryList = new ListView<>();
    private Stage stage;
    private String selectedEmulator;

    private final ScrollPane scriptCardsScrollPane = new ScrollPane();
    private final GridPane scriptCardsGrid = new GridPane();

    // Class-level search text
    private String searchText = "";

    public ScriptSelectionUI(ScriptExecutor scriptHandler, ScriptInstanceLoader scriptInstanceLoader) {
        this.scriptHandler = scriptHandler;
        this.scriptInstanceLoader = scriptInstanceLoader;
    }

    private void initializeUI() {
        // Main HBox layout
        HBox mainLayout = new HBox(10); // Spacing between categoryList and right VBox

        // Left side: categoryList
        categoryList.setPrefWidth(200); // Initial width, can be adjusted
        categoryList.setMinWidth(150);
        categoryList.setMaxWidth(300);
        categoryList.prefHeightProperty().bind(mainLayout.heightProperty());

        // Style and set up the categoryList
        categoryList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(IconListViewItem item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                setGraphic(null);
                if (!empty && item != null) {
                    ImageView imageView = new ImageView();
                    imageView.setFitHeight(20);
                    imageView.setFitWidth(20);
                    setText(item.getText());
                    Image icon = item.getIcon();
                    if (icon != null) {
                        imageView.setImage(icon);
                        setGraphic(imageView);
                    }
                    getStyleClass().add("list-cell");
                }
            }
        });
        categoryList.getStyleClass().add("category-list");
        categoryList.getSelectionModel().clearSelection();

        // Right side: VBox containing search bar and scriptCardsScrollPane
        VBox rightSideLayout = new VBox(10); // Spacing between search bar and grid
        rightSideLayout.setAlignment(Pos.TOP_CENTER);

        // Search bar layout
        HBox searchBarLayout = new HBox(5); // Spacing between label and text field
        searchBarLayout.setAlignment(Pos.CENTER_LEFT);
        searchBarLayout.setPadding(new Insets(5));

        Label searchLabel = new Label("Search:");
        TextField searchField = new TextField();
        searchField.setPromptText("Search by name or author...");
        searchField.setPrefWidth(300); // Adjust width as needed

        searchBarLayout.getChildren().addAll(searchLabel, searchField);

        // Configure scriptCardsScrollPane
        scriptCardsScrollPane.setContent(scriptCardsGrid);
        scriptCardsScrollPane.setFitToWidth(true);
        scriptCardsScrollPane.setFitToHeight(true);
        scriptCardsScrollPane.setStyle("-fx-background-color: transparent;");
        scriptCardsScrollPane.getStyleClass().add("custom-scroll-pane");

        // Configure scriptCardsGrid for a responsive layout
        scriptCardsGrid.setHgap(10);
        scriptCardsGrid.setVgap(10);
        scriptCardsGrid.setPadding(new Insets(10));
        scriptCardsGrid.setStyle("-fx-background-color: #232323;");

        // Add search bar and scroll pane to the right side
        rightSideLayout.getChildren().addAll(searchBarLayout, scriptCardsScrollPane);
        VBox.setVgrow(scriptCardsScrollPane, Priority.ALWAYS);

        // Add categoryList and rightSideLayout to mainLayout
        mainLayout.getChildren().addAll(categoryList, rightSideLayout);
        HBox.setHgrow(rightSideLayout, Priority.ALWAYS);

        // Scene and stage setup
        Scene scene = new Scene(mainLayout, 1000, 600); // Increased width for better layout
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/styles.css")).toExternalForm());
        stage = new Stage();
        stage.setScene(scene);
        stage.setTitle("Script Selection");
        stage.getIcons().add(MUFASA_LOGO);
        stage.setResizable(false);
        setupEventHandlers(searchField);

        // Add a width listener to the ScrollPane to dynamically adjust tile widths
        scriptCardsScrollPane.widthProperty().addListener((obs, oldVal, newVal) -> populateScriptCardsBasedOnCurrentCategoryAndSearch());

        // Ensure scripts are populated after the stage is shown
        stage.setOnShown(event -> populateScriptCardsBasedOnCurrentCategoryAndSearch());
    }

    // Populate the category list
    private void populateCategoryList() {
        List<IconListViewItem> items = new ArrayList<>();

        IconListViewItem allCategoryItem = new IconListViewItem("All", null);
        items.add(allCategoryItem);

        for (ScriptCategory category : ScriptCategory.values()) {
            Image icon = getCategoryIcon(category); // Assuming getCategoryIcon returns an Image
            IconListViewItem item = new IconListViewItem(category.toString(), icon);
            items.add(item);
        }
        categoryList.setItems(FXCollections.observableArrayList(items));
    }

    private void populateScriptCards(List<MetadataDTO> scripts) {
        scriptCardsGrid.getChildren().clear(); // Clear previous cards
        int column = 0;
        int row = 0;

        double scrollPaneWidth = scriptCardsScrollPane.getWidth();
        if (scrollPaneWidth <= 0) {
            // Width not yet initialized, defer
            return;
        }

        double padding = scriptCardsGrid.getPadding().getLeft() + scriptCardsGrid.getPadding().getRight();
        double hgap = scriptCardsGrid.getHgap();
        int desiredColumns = 2;
        double availableWidth = scrollPaneWidth - padding - (hgap * (desiredColumns - 1));
        double tileWidth = availableWidth / desiredColumns;

        for (MetadataDTO metadataDTO : scripts) {
            VBox card = createScriptCard(metadataDTO, tileWidth);
            scriptCardsGrid.add(card, column, row);
            column++;
            if (column == desiredColumns) {
                column = 0;
                row++;
            }
        }
    }

    private List<MetadataDTO> filterScriptsBySearch(List<MetadataDTO> scripts, String query) {
        List<MetadataDTO> filteredScripts = new ArrayList<>();
        for (MetadataDTO script : scripts) {
            String name = script.getMetadata().getName().toLowerCase();
            String author = script.getAuthor().toLowerCase();
            // Add other fields if needed
            String description = script.getMetadata().getDescription().toLowerCase();

            if (name.contains(query) || author.contains(query) || description.contains(query)) {
                filteredScripts.add(script);
            }
        }
        return filteredScripts;
    }

    private void populateScriptCardsBasedOnCurrentCategoryAndSearch() {
        IconListViewItem selectedItem = categoryList.getSelectionModel().getSelectedItem();
        List<MetadataDTO> scriptsToDisplay = new ArrayList<>();

        if (selectedItem != null) {
            String categoryName = selectedItem.getText();
            if ("All".equals(categoryName)) {
                scriptsToDisplay.addAll(allLoadedScripts);
            } else {
                ScriptCategory selectedCategory = ScriptCategory.valueOf(categoryName);
                if (doesCategoryHaveScripts(selectedCategory)) {
                    scriptsToDisplay.addAll(getScriptsForCategory(selectedCategory));
                }
            }
        } else {
            // If no category is selected, default to "All"
            scriptsToDisplay.addAll(allLoadedScripts);
        }

        // Apply search filter
        if (!searchText.isEmpty()) {
            scriptsToDisplay = filterScriptsBySearch(scriptsToDisplay, searchText);
        }

        populateScriptCards(scriptsToDisplay);
    }

    private VBox createScriptCard(MetadataDTO metadataDTO, double tileWidth) {
        VBox card = new VBox(5);
        card.setPrefWidth(tileWidth);
        card.setMaxWidth(tileWidth);
        card.setPadding(new Insets(10));
        card.setStyle("-fx-background-color: #333333; -fx-background-radius: 5; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 10, 0, 0, 5);");

        // Script Name
        Label nameLabel = new Label(metadataDTO.getMetadata().getName());
        nameLabel.getStyleClass().add("subheader-label");

        // Script Description
        Label descLabel = new Label(metadataDTO.getMetadata().getDescription());
        descLabel.setWrapText(true);
        descLabel.setStyle("-fx-text-fill: lightgray; -fx-font-size: 12px;");
        descLabel.setMaxHeight(50); // Limit height to avoid overflow in card

        // Spacer to allow description to expand
        Region expandableSpacer = new Region();
        VBox.setVgrow(expandableSpacer, Priority.ALWAYS);

        // Script Version and Author Labels
        Label versionLabel = new Label("Version: " + metadataDTO.getMetadata().getVersion());
        versionLabel.setStyle("-fx-text-fill: gray; -fx-font-size: 10px;");
        Label authorLabel = new Label("Author: " + metadataDTO.getAuthor());
        authorLabel.setStyle("-fx-text-fill: gray; -fx-font-size: 10px;");

        // Start and Guide Buttons
        Button startButton = new Button("Start");
        startButton.setStyle("-fx-background-color: #e57c23; -fx-text-fill: black; -fx-font-weight: bold;");
        startButton.setOnAction(event -> {
            if (selectedEmulator != null) {
                stage.close();
                if (metadataDTO.getCategories().contains(ScriptCategory.Local)) {
                    scriptHandler.startLocalScript(selectedEmulator, metadataDTO.getJarUrl());
                }
            } else {
                showAlertDialog("Information", "No script or emulator is selected", Alert.AlertType.INFORMATION);
            }
        });

        Button guideButton = new Button("Open Guide");
        guideButton.setStyle("-fx-background-color: #cccccc; -fx-text-fill: black; -fx-font-weight: bold;");
        guideButton.setOnAction(event -> {
            if (metadataDTO.getGuideLink() != null && !metadataDTO.getGuideLink().isEmpty()) {
                try {
                    java.awt.Desktop.getDesktop().browse(new java.net.URI(metadataDTO.getGuideLink()));
                } catch (IOException | URISyntaxException e) {
                    showAlertDialog("Error", "Failed to open guide link.", Alert.AlertType.ERROR);
                }
            }
        });

        HBox buttonBox = new HBox(10, startButton, guideButton); // Button box with spacing
        buttonBox.setAlignment(Pos.CENTER_LEFT);

        // Bottom VBox to contain version, author, and buttons
        VBox bottomSection = new VBox(5, versionLabel, authorLabel, buttonBox);
        bottomSection.setAlignment(Pos.CENTER_LEFT);

        // Add components to card layout
        card.getChildren().addAll(nameLabel, descLabel, expandableSpacer, bottomSection);

        return card;
    }

    private void setupEventHandlers(TextField searchField) {
        // Listen for category selection changes to update the script cards
        categoryList.getSelectionModel().selectedItemProperty().addListener((obs, oldItem, newItem) -> {
            if (newItem != null) {
                populateScriptCardsBasedOnCurrentCategoryAndSearch();
            }
        });

        // Listen for search field text changes to update the script cards
        searchField.textProperty().addListener((obs, oldText, newText) -> {
            this.searchText = newText.trim().toLowerCase();
            populateScriptCardsBasedOnCurrentCategoryAndSearch();
        });
    }

    private Image getCategoryIcon(ScriptCategory category) {
        String imagePath = "/osrsAssets/skillIcons/" + category.toString().toLowerCase() + ".png";
        InputStream is = getClass().getResourceAsStream(imagePath);
        if (is == null) {
            System.err.println("InputStream is null. Failed to load image from path: " + imagePath);
            return null;
        }
        try {
            Image image = new Image(is);
            if (image.isError()) {
                System.err.println("Error loading image from path: " + imagePath + " with error: " + image.getException().getMessage());
                return null;
            }
            return image;
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                System.err.println("Failed to close input stream: " + e.getMessage());
            }
        }
    }

    private boolean doesCategoryHaveScripts(ScriptCategory category) {
        for (MetadataDTO metadata : allLoadedScripts) {
            if (metadata.getCategories().contains(category)) {
                return true; // Found at least one script for this category
            }
        }
        return false; // No scripts found for this category
    }

    private List<MetadataDTO> getScriptsForCategory(ScriptCategory category) {
        List<MetadataDTO> scriptsForCategory = new ArrayList<>();
        for (MetadataDTO metadata : allLoadedScripts) {
            // Check if the script's categories contain the specified category
            if (metadata.getCategories().contains(category)) {
                // Add the MetadataDTO itself to the list
                scriptsForCategory.add(metadata);
            }
        }
        return scriptsForCategory;
    }

    private void refreshScripts() {
        // Clear the cached scripts
        scriptInstanceLoader.invalidateCache();

        // Reload the scripts
        List<MetadataDTO> refreshedScripts = scriptInstanceLoader.loadScripts();

        // Clear and repopulate the observable list to notify observers
        allLoadedScripts.clear();
        allLoadedScripts.addAll(refreshedScripts);
    }

    public void display(String deviceID) {
        selectedEmulator = deviceID;
        if (stage == null) {
            // Initialize UI components only once
            populateCategoryList();
            initializeUI();
        }

        // Always refresh scripts whenever this method is called
        refreshScripts();

        // Select the "All" category by default
        categoryList.getSelectionModel().selectFirst();

        // Defer populateScriptCards to ensure layout is complete
        Platform.runLater(this::populateScriptCardsBasedOnCurrentCategoryAndSearch);

        stage.show();
    }

    private void showAlertDialog(String title, String message, Alert.AlertType alertType) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
