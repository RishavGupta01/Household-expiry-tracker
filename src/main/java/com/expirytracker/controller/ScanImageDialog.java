package com.expirytracker.controller;

import com.expirytracker.database.ItemDAO;
import com.expirytracker.model.Item;
import com.expirytracker.service.OcrService;
import com.expirytracker.util.DateParser;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.io.File;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Dialog for scanning images with OCR and extracting item information.
 */
public class ScanImageDialog extends Dialog<Item> {
    
    private final File imageFile;
    private final OcrService ocrService;
    private final ItemDAO itemDAO;
    
    private TextField nameField;
    private DatePicker purchaseDatePicker;
    private DatePicker expiryDatePicker;
    private ComboBox<String> categoryCombo;
    private TextField customCategoryField;
    private Spinner<Integer> quantitySpinner;
    private TextArea ocrResultArea;
    private TextArea notesArea;
    private ProgressIndicator progressIndicator;
    private Label statusLabel;
    private Button processButton;
    private ImageView imageView;
    
    private String extractedText = "";

    public ScanImageDialog(File imageFile, OcrService ocrService, ItemDAO itemDAO) {
        this.imageFile = imageFile;
        this.ocrService = ocrService;
        this.itemDAO = itemDAO;
        
        setTitle("Scan Image - OCR Expiry Detection");
        setHeaderText("Process image to extract product information");
        setResizable(true);
        getDialogPane().setPrefWidth(700);

        // Create UI
        VBox content = new VBox(12);
        content.setPadding(new Insets(20));
        content.setAlignment(Pos.TOP_CENTER);

        // Image preview
        try {
            Image image = new Image(imageFile.toURI().toString());
            imageView = new ImageView(image);
            imageView.setFitWidth(500);
            imageView.setFitHeight(350);
            imageView.setPreserveRatio(true);
            
            // Add border to image
            imageView.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 10, 0, 0, 0);");
            
            content.getChildren().add(imageView);
        } catch (Exception e) {
            Label errorLabel = new Label("⚠ Could not load image preview");
            errorLabel.setStyle("-fx-text-fill: red;");
            content.getChildren().add(errorLabel);
        }

        // Process button and progress
        HBox processBox = new HBox(10);
        processBox.setAlignment(Pos.CENTER);
        processBox.setPadding(new Insets(10, 0, 10, 0));
        
        processButton = new Button("🔍 Process Image with OCR");
        processButton.setStyle("-fx-font-size: 14px; -fx-padding: 8 16 8 16;");
        processButton.setOnAction(e -> processImage());
        
        progressIndicator = new ProgressIndicator();
        progressIndicator.setVisible(false);
        progressIndicator.setPrefSize(30, 30);
        
        statusLabel = new Label("Click 'Process Image' to extract text automatically");
        statusLabel.setStyle("-fx-font-size: 12px;");
        
        processBox.getChildren().addAll(processButton, progressIndicator, statusLabel);
        content.getChildren().add(processBox);

        // Extracted info form
        GridPane formGrid = new GridPane();
        formGrid.setHgap(15);
        formGrid.setVgap(12);
        formGrid.setPadding(new Insets(10));

        int row = 0;

        nameField = new TextField();
        nameField.setPromptText("Product name (auto-filled from OCR)");
        nameField.setPrefWidth(400);
        formGrid.add(new Label("Product Name:*"), 0, row);
        formGrid.add(nameField, 1, row++);
        
        categoryCombo = new ComboBox<>();
        categoryCombo.getItems().addAll(
            "Dairy & Eggs",
            "Meat & Poultry", 
            "Seafood",
            "Vegetables", 
            "Fruits",
            "Bread & Bakery",
            "Grains & Cereals",
            "Canned Goods",
            "Frozen Foods",
            "Beverages",
            "Snacks & Sweets",
            "Condiments & Sauces",
            "Baby Food",
            "Pet Food",
            "Supplements & Vitamins",
            "Other"
        );
        categoryCombo.setPromptText("Select category (auto-suggested)");
        categoryCombo.setPrefWidth(400);
        
        customCategoryField = new TextField();
        customCategoryField.setPromptText("Enter custom category");
        customCategoryField.setPrefWidth(400);
        customCategoryField.setVisible(false);
        customCategoryField.setManaged(false);
        
        categoryCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            boolean isOther = "Other".equals(newVal);
            customCategoryField.setVisible(isOther);
            customCategoryField.setManaged(isOther);
        });
        
        formGrid.add(new Label("Category:*"), 0, row);
        VBox categoryBox = new VBox(5);
        categoryBox.getChildren().addAll(categoryCombo, customCategoryField);
        formGrid.add(categoryBox, 1, row++);
        
        purchaseDatePicker = new DatePicker(LocalDate.now());
        purchaseDatePicker.setPromptText("Purchase date");
        purchaseDatePicker.setPrefWidth(400);
        formGrid.add(new Label("Purchase Date:"), 0, row);
        formGrid.add(purchaseDatePicker, 1, row++);
        
        expiryDatePicker = new DatePicker();
        expiryDatePicker.setPromptText("Expiry date (auto-filled from OCR)");
        expiryDatePicker.setPrefWidth(400);
        formGrid.add(new Label("Expiry Date:*"), 0, row);
        formGrid.add(expiryDatePicker, 1, row++);
        
        quantitySpinner = new Spinner<>(1, 999, 1);
        quantitySpinner.setEditable(true);
        quantitySpinner.setPrefWidth(400);
        formGrid.add(new Label("Quantity:"), 0, row);
        formGrid.add(quantitySpinner, 1, row++);
        
        notesArea = new TextArea();
        notesArea.setPromptText("Additional notes (optional)");
        notesArea.setPrefRowCount(2);
        notesArea.setPrefWidth(400);
        formGrid.add(new Label("Notes:"), 0, row);
        formGrid.add(notesArea, 1, row++);

        content.getChildren().add(formGrid);

        // OCR result (raw text) - collapsible
        TitledPane ocrPane = new TitledPane();
        ocrPane.setText("📄 Raw OCR Text (click to expand)");
        ocrPane.setExpanded(false);
        
        ocrResultArea = new TextArea();
        ocrResultArea.setPromptText("OCR extracted text will appear here...");
        ocrResultArea.setPrefRowCount(6);
        ocrResultArea.setWrapText(true);
        ocrResultArea.setEditable(true); // Allow manual editing
        ocrResultArea.setStyle("-fx-font-family: monospace; -fx-font-size: 11px;");
        
        ocrPane.setContent(ocrResultArea);
        content.getChildren().add(ocrPane);

        getDialogPane().setContent(content);

        // Add buttons
        ButtonType saveButtonType = new ButtonType("💾 Save Item", ButtonBar.ButtonData.OK_DONE);
        ButtonType manualButtonType = new ButtonType("✏️ Enter Manually", ButtonBar.ButtonData.OTHER);
        getDialogPane().getButtonTypes().addAll(saveButtonType, manualButtonType, ButtonType.CANCEL);

        // Get button references
        Button saveButton = (Button) getDialogPane().lookupButton(saveButtonType);
        Button manualButton = (Button) getDialogPane().lookupButton(manualButtonType);
        
        // Disable save button initially
        saveButton.setDisable(true);
        
        // Manual entry bypasses OCR
        manualButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            event.consume();
            progressIndicator.setVisible(false);
            statusLabel.setText("Manual entry mode - fill in the fields below");
            saveButton.setDisable(false);
            processButton.setDisable(true);
            manualButton.setDisable(true);
        });

        // Validate and save
        saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (!validateInput()) {
                event.consume();
            }
        });

        setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return createItemFromInput();
            }
            return null;
        });

        // Auto-process on show if OCR is available
        setOnShown(e -> {
            if (ocrService != null && ocrService.isInitialized()) {
                processImage();
            } else {
                statusLabel.setText("⚠ OCR not available - Please enter details manually");
                statusLabel.setStyle("-fx-text-fill: orange; -fx-font-size: 12px;");
                saveButton.setDisable(false);
                processButton.setDisable(true);
            }
        });
    }

    /**
     * Process image with OCR in background.
     */
    private void processImage() {
        processButton.setDisable(true);
        progressIndicator.setVisible(true);
        statusLabel.setText("Processing image...");

        Task<String> ocrTask = new Task<String>() {
            @Override
            protected String call() throws Exception {
                return ocrService.doOcr(imageFile);
            }
        };

        ocrTask.setOnSucceeded(event -> {
            extractedText = ocrTask.getValue();
            ocrResultArea.setText(extractedText);
            
            // Parse extracted text
            parseExtractedText(extractedText);
            
            progressIndicator.setVisible(false);
            statusLabel.setText("OCR complete - Please review and edit");
            
            // Enable save button
            Button saveButton = (Button) getDialogPane().lookupButton(
                getDialogPane().getButtonTypes().get(0)
            );
            saveButton.setDisable(false);
        });

        ocrTask.setOnFailed(event -> {
            Throwable error = ocrTask.getException();
            progressIndicator.setVisible(false);
            statusLabel.setText("OCR failed");
            
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("OCR Error");
            alert.setHeaderText("Failed to process image");
            alert.setContentText(error != null ? error.getMessage() : "Unknown error");
            alert.showAndWait();
            
            processButton.setDisable(false);
        });

        // Run OCR in background thread
        Thread ocrThread = new Thread(ocrTask);
        ocrThread.setDaemon(true);
        ocrThread.start();
    }

    /**
     * Parse extracted text and populate fields.
     */
    private void parseExtractedText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return;
        }
        
        // Extract product name
        String productName = DateParser.extractProductName(text);
        if (productName != null && !productName.isEmpty()) {
            nameField.setText(productName);
        }

        // Extract expiry date
        Optional<LocalDate> expiryDate = DateParser.parseDateFromText(text);
        expiryDate.ifPresent(date -> {
            // Check if this looks like a manufacture date
            String textUpper = text.toUpperCase();
            if (textUpper.contains("MFG") || textUpper.contains("MANUFAC") || 
                textUpper.contains("MFD") || textUpper.contains("PRODUCTION")) {
                // Manufacture date detected - add default shelf life
                expiryDatePicker.setValue(date.plusMonths(6));
                if (notesArea.getText().isEmpty()) {
                    notesArea.setText("Manufacture date detected: " + date + 
                                    "\nEstimated expiry (6 months added)");
                }
            } else {
                // Likely an expiry date
                expiryDatePicker.setValue(date);
            }
        });

        // Enhanced category suggestion based on keywords
        suggestCategory(text);
    }
    
    /**
     * Suggest category based on text content.
     */
    private void suggestCategory(String text) {
        String textUpper = text.toUpperCase();
        
        // Dairy & Eggs
        if (textUpper.contains("MILK") || textUpper.contains("DAIRY") || 
            textUpper.contains("CHEESE") || textUpper.contains("YOGURT") ||
            textUpper.contains("BUTTER") || textUpper.contains("CREAM") ||
            textUpper.contains("EGG")) {
            categoryCombo.setValue("Dairy & Eggs");
        }
        // Meat & Poultry
        else if (textUpper.contains("MEAT") || textUpper.contains("CHICKEN") || 
                 textUpper.contains("BEEF") || textUpper.contains("PORK") ||
                 textUpper.contains("LAMB") || textUpper.contains("TURKEY") ||
                 textUpper.contains("SAUSAGE") || textUpper.contains("BACON")) {
            categoryCombo.setValue("Meat & Poultry");
        }
        // Seafood
        else if (textUpper.contains("FISH") || textUpper.contains("SEAFOOD") || 
                 textUpper.contains("SALMON") || textUpper.contains("TUNA") ||
                 textUpper.contains("SHRIMP") || textUpper.contains("PRAWN")) {
            categoryCombo.setValue("Seafood");
        }
        // Vegetables
        else if (textUpper.contains("VEGETABLE") || textUpper.contains("VEGGIE") ||
                 textUpper.contains("LETTUCE") || textUpper.contains("TOMATO") ||
                 textUpper.contains("CARROT") || textUpper.contains("SPINACH")) {
            categoryCombo.setValue("Vegetables");
        }
        // Fruits
        else if (textUpper.contains("FRUIT") || textUpper.contains("APPLE") || 
                 textUpper.contains("ORANGE") || textUpper.contains("BANANA") ||
                 textUpper.contains("BERRY") || textUpper.contains("GRAPE")) {
            categoryCombo.setValue("Fruits");
        }
        // Beverages
        else if (textUpper.contains("JUICE") || textUpper.contains("DRINK") || 
                 textUpper.contains("BEVERAGE") || textUpper.contains("SODA") ||
                 textUpper.contains("WATER") || textUpper.contains("TEA") ||
                 textUpper.contains("COFFEE")) {
            categoryCombo.setValue("Beverages");
        }
        // Bread & Bakery
        else if (textUpper.contains("BREAD") || textUpper.contains("BAKERY") ||
                 textUpper.contains("CAKE") || textUpper.contains("PASTRY") ||
                 textUpper.contains("ROLL") || textUpper.contains("BAGEL")) {
            categoryCombo.setValue("Bread & Bakery");
        }
        // Canned Goods
        else if (textUpper.contains("CANNED") || textUpper.contains("CAN") ||
                 textUpper.contains("TINNED")) {
            categoryCombo.setValue("Canned Goods");
        }
        // Frozen Foods
        else if (textUpper.contains("FROZEN") || textUpper.contains("FREEZE")) {
            categoryCombo.setValue("Frozen Foods");
        }
        // Snacks & Sweets
        else if (textUpper.contains("SNACK") || textUpper.contains("CHIP") ||
                 textUpper.contains("CANDY") || textUpper.contains("CHOCOLATE") ||
                 textUpper.contains("COOKIE") || textUpper.contains("BISCUIT")) {
            categoryCombo.setValue("Snacks & Sweets");
        }
        // Condiments & Sauces
        else if (textUpper.contains("SAUCE") || textUpper.contains("KETCHUP") ||
                 textUpper.contains("MAYO") || textUpper.contains("MUSTARD") ||
                 textUpper.contains("DRESSING") || textUpper.contains("CONDIMENT")) {
            categoryCombo.setValue("Condiments & Sauces");
        }
        // Baby Food
        else if (textUpper.contains("BABY") || textUpper.contains("INFANT") ||
                 textUpper.contains("FORMULA")) {
            categoryCombo.setValue("Baby Food");
        }
        // Pet Food
        else if (textUpper.contains("PET") || textUpper.contains("DOG") ||
                 textUpper.contains("CAT") || textUpper.contains("ANIMAL")) {
            categoryCombo.setValue("Pet Food");
        }
        // Supplements & Vitamins
        else if (textUpper.contains("VITAMIN") || textUpper.contains("SUPPLEMENT") ||
                 textUpper.contains("MINERAL") || textUpper.contains("CAPSULE") ||
                 textUpper.contains("TABLET")) {
            categoryCombo.setValue("Supplements & Vitamins");
        }
        // Default to first category if nothing matches
        else if (categoryCombo.getValue() == null) {
            categoryCombo.setValue("Other");
        }
    }

    /**
     * Validate input fields.
     */
    private boolean validateInput() {
        if (nameField.getText() == null || nameField.getText().trim().isEmpty()) {
            showError("Product name is required");
            return false;
        }

        if (categoryCombo.getValue() == null || categoryCombo.getValue().trim().isEmpty()) {
            showError("Category is required");
            return false;
        }
        
        // Validate custom category if "Other" is selected
        if ("Other".equals(categoryCombo.getValue())) {
            if (customCategoryField.getText() == null || customCategoryField.getText().trim().isEmpty()) {
                showError("Please specify a custom category");
                return false;
            }
        }

        if (expiryDatePicker.getValue() == null) {
            showError("Expiry date is required");
            return false;
        }

        return true;
    }

    /**
     * Create Item from form input.
     */
    private Item createItemFromInput() {
        Item item = new Item();
        item.setName(nameField.getText().trim());
        
        // Use custom category if "Other" is selected
        String category = categoryCombo.getValue();
        if ("Other".equals(category) && customCategoryField.getText() != null 
                && !customCategoryField.getText().trim().isEmpty()) {
            category = customCategoryField.getText().trim();
        }
        item.setCategory(category);
        
        item.setPurchaseDate(purchaseDatePicker.getValue());
        item.setExpiryDate(expiryDatePicker.getValue());
        item.setQuantity(quantitySpinner.getValue());
        
        // Combine notes
        String notes = notesArea.getText() != null ? notesArea.getText().trim() : "";
        if (!notes.contains("Added via image scan")) {
            notes = (notes.isEmpty() ? "" : notes + "\n") + "Added via image scan";
        }
        item.setNotes(notes);
        
        item.setImagePath(imageFile.getAbsolutePath());

        // Save to database
        try {
            if (itemDAO.insert(item)) {
                System.out.println("✓ Item saved successfully: " + item.getName());
                return item;
            } else {
                showError("Failed to save item to database");
                return null;
            }
        } catch (Exception e) {
            showError("Error saving item: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Show error alert.
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Validation Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
