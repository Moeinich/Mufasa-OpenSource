package UI.components;

import helpers.CacheManager;
import helpers.ThreadManager;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import osr.mapping.utils.ItemProcessor;

import java.awt.image.BufferedImage;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import static utils.Constants.MUFASA_LOGO_ANIMATED;
import static utils.Constants.STYLESHEET;

public class PaintBar {
    private final CacheManager cacheManager;
    private final ItemProcessor itemProcessor;

    private final VBox paintBox = new VBox();
    private final VBox topSection = new VBox();
    private final VBox middleSection = new VBox();

    private final ImageView logoImageView = new ImageView();
    private final Label headerLabel = new Label("Header");
    private final Label actionLabel = new Label("Action Label");
    private final Label statisticLabel = new Label("Statistic Label");
    private final Label[] boxLabels = new Label[8];
    private final ImageView[] boxImages = new ImageView[8];
    private final Label[] boxIntegers = new Label[8];
    private final boolean[] boxCreated = new boolean[8];

    public PaintBar(CacheManager cacheManager, ItemProcessor itemProcessor) {
        this.cacheManager = cacheManager;
        this.itemProcessor = itemProcessor;
        createUI();
        disableAll(); // Disable all elements by default
    }

    // API side: PaintBuilder.create() to initiate it.
    public void createInstance(String device, String scriptName, Image optionalImage) {
        PaintBar newInstance = new PaintBar(cacheManager, itemProcessor);
        cacheManager.setPaintBar(device, newInstance); // cache it

        if (optionalImage != null) {
            Platform.runLater(() -> {
                newInstance.setLogoImage(optionalImage);
                newInstance.setHeaderLabelText(scriptName);
            });
        } else {
            Platform.runLater(() -> newInstance.setHeaderLabelText(scriptName));
        }
    }

    // UI part
    private void createUI() {
        // Set the paintBox properties
        paintBox.setPrefSize(235, 690); // Reduced width by 50 pixels
        paintBox.setAlignment(Pos.TOP_CENTER);
        paintBox.setSpacing(10); // Add spacing between sections
        paintBox.setPadding(new Insets(10));

        // Configure and add the logo ImageView
        logoImageView.setFitWidth(75);
        logoImageView.setPreserveRatio(true);
        setLogoImage(MUFASA_LOGO_ANIMATED); // Set initial logo

        // Configure header label
        configureHeaderLabel(headerLabel);

        // Configure sections
        configureSection(topSection);
        configureSection(middleSection);

        // Add logo and header label to topSection
        topSection.getChildren().addAll(logoImageView, headerLabel, actionLabel, statisticLabel);

        // Add the grid with 8 small boxes to middleSection
        GridPane gridPane = createGridPane();
        middleSection.getChildren().add(gridPane);

        // Add sections to paintBox
        paintBox.getChildren().addAll(topSection, middleSection);

        // Apply CSS
        applyCSS();
    }

    private void configureHeaderLabel(Label label) {
        label.setWrapText(true);
        label.setMaxWidth(215); // Adjusted max width for the new paintBox width
        label.setAlignment(Pos.CENTER); // Center align the header
    }

    private void configureSection(VBox section) {
        section.setAlignment(Pos.TOP_CENTER);
        section.setSpacing(5);
        section.setPadding(new Insets(10));
        section.setStyle("-fx-background-color: #1B1817; -fx-border-radius: 10; -fx-background-radius: 10;");
    }

    private GridPane createGridPane() {
        GridPane gridPane = new GridPane();
        gridPane.setHgap(5); // Adjusted horizontal gap for the reduced width
        gridPane.setVgap(15); // Increased vertical gap by 10 pixels
        gridPane.setAlignment(Pos.TOP_CENTER); // Align grid to top center

        for (int i = 0; i < 8; i++) {
            VBox box = createBox(i);
            int row = i / 2;
            int col = i % 2;
            gridPane.add(box, col, row);
        }

        return gridPane;
    }

    public VBox createBox(int index) {
        VBox box = new VBox();
        box.setAlignment(Pos.CENTER);
        box.setSpacing(2); // Reduced spacing between label, image, and integer
        box.setPrefSize(115, 90); // Adjusted size to accommodate integer label and image

        Label boxLabel = new Label("Label " + (index + 1));
        boxLabel.setMaxWidth(115); // Ensure label does not cut off
        boxLabel.setAlignment(Pos.CENTER); // Center align the label
        boxLabels[index] = boxLabel;

        Rectangle background = new Rectangle(45, 45);
        background.setArcWidth(10);
        background.setArcHeight(10);
        background.setFill(Color.rgb(27, 24, 23));

        ImageView boxImage = new ImageView(new Image("/assets/placeholder.png")); // Placeholder image
        boxImage.setFitWidth(36);
        boxImage.setFitHeight(36);
        boxImages[index] = boxImage;

        Label boxInteger = new Label("0");
        boxInteger.setAlignment(Pos.CENTER);
        boxIntegers[index] = boxInteger;

        StackPane imageContainer = new StackPane(background, boxImage);
        imageContainer.setAlignment(Pos.CENTER);

        VBox imageAndIntegerBox = new VBox(imageContainer, boxInteger);
        imageAndIntegerBox.setAlignment(Pos.CENTER);

        box.getChildren().addAll(boxLabel, imageAndIntegerBox);
        return box;
    }

    private void applyCSS() {
        Pane root = new Pane();
        root.getChildren().add(paintBox);
        Scene scene = new Scene(root);
        scene.getStylesheets().add(STYLESHEET);
        paintBox.getStyleClass().add("paintBar-root");
        logoImageView.getStyleClass().add("paintBar-logo");
        headerLabel.getStyleClass().add("paintBar-header-label");
        actionLabel.getStyleClass().add("paintBar-label"); // Applying the same CSS class for both labels
        statisticLabel.getStyleClass().add("paintBar-label"); // Applying the same CSS class for both labels
        for (int i = 0; i < 8; i++) {
            boxLabels[i].getStyleClass().add("paintBar-subheader-label"); // Updated to use subheader style
            boxIntegers[i].getStyleClass().add("paintBar-box-integer");
        }
    }

    // Getters
    public VBox getPaintBox() {
        return paintBox;
    }

    // For internal use, so we can both check & grab an instance of a PaintBar.
    public PaintBar getInstance(String device) {
        return cacheManager.getPaintBar(device);
    }

    public String getHeaderLabelText() {
        return headerLabel.getText();
    }

    public ImageView getLogoImageView() {
        return logoImageView;
    }

    public Label getActionLabel() {
        return actionLabel;
    }

    public Label getStatisticLabel() {
        return statisticLabel;
    }

    public Label getBoxLabel(int index) {
        if (index >= 0 && index < 8) {
            return boxLabels[index];
        }
        return null;
    }

    public ImageView getBoxImage(int index) {
        if (index >= 0 && index < 8) {
            return boxImages[index];
        }
        return null;
    }

    public Label getBoxInteger(int index) {
        if (index >= 0 && index < 8) {
            return boxIntegers[index];
        }
        return null;
    }

    public int getFirstAvailableBoxIndex() {
        for (int i = 0; i < 8; i++) {
            if (!boxCreated[i]) {
                return i;
            }
        }
        return -1;
    }

    // Setters
    public void setHeaderLabelText(String text) {
        Platform.runLater(() -> headerLabel.setText(text));
    }

    public void setLogoImage(Image image) {
        Platform.runLater(() -> {
            Image currentImage = logoImageView.getImage();
            if (currentImage != null) {
                logoImageView.setImage(null);
            }

            // Set the new image
            logoImageView.setImage(image);
        });
    }

    public void setActionLabelText(String text) {
        Platform.runLater(() -> actionLabel.setText("State: " + text));
    }

    public void setStatisticLabelText(String text) {
        Platform.runLater(() -> statisticLabel.setText(text));
    }

    public void setBoxLabelText(int index, String text) {
        if (index >= 0 && index < 8) {
            Platform.runLater(() -> {
                boxLabels[index].setText(text);
                boxCreated[index] = true;
                enableBox(index);
            });
        }
    }

    public void setBoxImage(int index, int itemID) {
        if (index >= 0 && index < 8) {
            Platform.runLater(() -> {
                // Check if there's an existing image and clear it
                Image currentImage = boxImages[index].getImage();
                if (currentImage != null) {
                    boxImages[index].setImage(null);
                }

                // Load the new image and set it
                Image itemImage = grabItemImage(itemID);
                boxImages[index].setImage(itemImage);
                boxCreated[index] = true;
                enableBox(index);
            });
        }
    }

    public void setBoxInteger(int index, int value) {
        if (index >= 0 && index < 8) {
            Platform.runLater(() -> {
                boxIntegers[index].setText(Integer.toString(value));
                boxCreated[index] = true;
                enableBox(index);
            });
        }
    }

    // Enablers/Disablers
    public void enableActionLabel() {
        Platform.runLater(() -> actionLabel.setVisible(true));
    }

    public void disableActionLabel() {
        actionLabel.setVisible(false);
    }

    public void enableStatisticLabel() {
        Platform.runLater(() -> statisticLabel.setVisible(true));
    }

    public void disableStatisticLabel() {
        statisticLabel.setVisible(false);
    }

    public void enableBoxLabel(int index) {
        if (index >= 0 && index < 8) {
            boxLabels[index].setVisible(true);
        }
    }

    public void disableBoxLabel(int index) {
        if (index >= 0 && index < 8) {
            boxLabels[index].setVisible(false);
        }
    }

    public void enableBoxImage(int index) {
        if (index >= 0 && index < 8) {
            boxImages[index].setVisible(true);
        }
    }

    public void disableBoxImage(int index) {
        if (index >= 0 && index < 8) {
            boxImages[index].setVisible(false);
        }
    }

    public void enableBoxInteger(int index) {
        if (index >= 0 && index < 8) {
            boxIntegers[index].setVisible(true);
        }
    }

    public void disableBoxInteger(int index) {
        if (index >= 0 && index < 8) {
            boxIntegers[index].setVisible(false);
        }
    }

    public void enableBox(int index) {
        if (index >= 0 && index < 8) {
            enableBoxLabel(index);
            enableBoxImage(index);
            enableBoxInteger(index);
        }
    }

    public void disableAll() {
        actionLabel.setVisible(false);
        statisticLabel.setVisible(false);
        for (int i = 0; i < 8; i++) {
            boxLabels[i].setVisible(false);
            boxImages[i].setVisible(false);
            boxImages[i].setImage(null);
            boxIntegers[i].setVisible(false);
        }
    }

    // Webhook stuff
    // Canvas dimensions
    private final int canvasWidth = 1094;
    private final int canvasHeight = 555;

    // Predefined dimensions and colors
    private final int boxWidth = 90;
    private final int boxHeight = 66;
    private final int imageSize = 36;
    private final Color backgroundColor = Color.rgb(16, 12, 12); // #100c0c
    private final Color boxBackgroundColor = Color.rgb(27, 24, 23); // #1b1817
    private final Color textColor = Color.WHITE;
    private final ExecutorService scheduler = ThreadManager.getInstance().getUnifiedExecutor();

    // Fonts
    private final Font headerFont = Font.font("Consolas", 12);
    private final Font labelFont = Font.font("Consolas", 10);
    private final Font valueFont = Font.font("Consolas", 12);

    public BufferedImage getWebhookPaintImage(String device) {
        PaintBar paintBar = cacheManager.getPaintBar(device);
        if (paintBar == null) {
            return null;
        }

        Canvas canvas = new Canvas(canvasWidth, canvasHeight);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // Set background and draw elements
        gc.setFill(backgroundColor);
        gc.fillRect(0, 0, canvasWidth, canvasHeight);
        drawHeaderAndLabels(gc, paintBar, canvasWidth, headerFont);

        int boxY = 30;
        for (int i = 0; i < 8; i++) {
            drawBox(gc, paintBar, i, boxWidth, boxHeight, imageSize, boxY, canvasWidth, boxBackgroundColor, textColor, labelFont, valueFont);
            if (i % 2 != 0) {
                boxY += boxHeight + imageSize + 30;
            }
        }

        // Asynchronously convert the canvas to a BufferedImage
        return convertCanvasToBufferedImage(canvas, canvasWidth, canvasHeight);
    }

    private void drawHeaderAndLabels(GraphicsContext gc, PaintBar paintBar, double canvasWidth, Font font) {
        final double topBarHeight = 15;
        gc.setFont(font);
        gc.setFill(Color.WHITE);

        // Header
        String headerText = paintBar.getHeaderLabelText();
        drawCenteredText(gc, headerText, canvasWidth / 2, topBarHeight - 3);

        // State Label
        String actionLabelText = paintBar.getActionLabel().getText();
        if (!"Action Label".equals(actionLabelText)) {
            gc.fillText(actionLabelText, 10, topBarHeight - 3);
        }

        // Statistics Label
        String statisticLabelText = paintBar.getStatisticLabel().getText();
        if (!"Statistic Label".equals(statisticLabelText)) {
            double xPosition = canvasWidth - gc.getFont().getSize() * statisticLabelText.length() / 2 - 10;
            gc.fillText(statisticLabelText, xPosition, topBarHeight - 3);
        }
    }

    private void drawBox(GraphicsContext gc, PaintBar paintBar, int index, int boxWidth, int boxHeight, int imageSize,
                         int boxY, int canvasWidth, Color boxBackgroundColor, Color textColor, Font labelFont, Font valueFont) {
        Label boxLabel = paintBar.getBoxLabel(index);
        ImageView boxImage = paintBar.getBoxImage(index);
        Label boxInteger = paintBar.getBoxInteger(index);

        if (boxLabel != null && boxImage != null && boxInteger != null &&
                !boxLabel.getText().matches("Label [1-8]")) {

            int xOffset = (index % 2 == 0) ? 5 : canvasWidth - boxWidth - 5;

            // Draw box background
            gc.setFill(boxBackgroundColor);
            gc.fillRoundRect(xOffset, boxY - 12, boxWidth, boxHeight + 20, 10, 10);

            // Draw label
            gc.setFill(textColor);
            gc.setFont(labelFont);
            drawCenteredText(gc, boxLabel.getText(), xOffset + (double) boxWidth / 2, boxY + 10);

            // Draw image
            Image image = boxImage.getImage();
            gc.drawImage(image, xOffset + (double) (boxWidth - imageSize) / 2, boxY + 20, imageSize, imageSize);

            // Draw integer
            gc.setFont(valueFont);
            drawCenteredText(gc, boxInteger.getText(), xOffset + (double) boxWidth / 2, boxY + 20 + imageSize + 15);
        }
    }

    private void drawCenteredText(GraphicsContext gc, String text, double centerX, double centerY) {
        double textWidth = gc.getFont().getSize() * text.length() * 0.6; // Approximation for width
        gc.fillText(text, centerX - textWidth / 2, centerY);
    }

    private BufferedImage convertCanvasToBufferedImage(Canvas canvas, int width, int height) {
        CompletableFuture<BufferedImage> futureImage = new CompletableFuture<>();

        scheduler.submit(() -> {
            try {
                WritableImage writableImage = new WritableImage(width, height);
                CountDownLatch latch = new CountDownLatch(1);

                // Use Platform.runLater to ensure JavaFX thread executes the snapshot
                Platform.runLater(() -> {
                    canvas.snapshot(null, writableImage);
                    latch.countDown();
                });

                // Wait for the snapshot to complete
                latch.await();

                // Convert WritableImage to BufferedImage
                BufferedImage image = SwingFXUtils.fromFXImage(writableImage, null);

                // Complete the future with the image
                futureImage.complete(image);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Restore interrupt status
                futureImage.completeExceptionally(e);
            } catch (Exception e) {
                e.printStackTrace();
                futureImage.completeExceptionally(e);
            }
        });

        // Block to retrieve the result
        try {
            return futureImage.get();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            e.printStackTrace();
            return null;
        }
    }

    // Helpers
    public Image grabItemImage(int ID) {
        return itemProcessor.getItemImageFX(ID);
    }
}