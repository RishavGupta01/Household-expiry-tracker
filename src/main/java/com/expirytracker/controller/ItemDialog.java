package com.expirytracker.controller;

import com.expirytracker.model.Item;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Dialog for adding/editing items with enhanced UI.
 */
public class ItemDialog extends Dialog<Item> {
    
    private final TextField nameField;
    private final ComboBox<String> categoryCombo;
    private final TextField customCategoryField;
    private final DatePicker purchaseDatePicker;
    private final DatePicker expiryDatePicker;
    private final Spinner<Integer> quantitySpinner;
    private final TextArea notesArea;
    
    private final Item item;
    private final boolean isEdit;

    public ItemDialog(Item item) {
        this.item = item;
        this.isEdit = (item != null);
        
        setTitle(isEdit ? "Edit Item" : "Add New Item");
        setHeaderText(isEdit ? "Edit item details" : "Enter item details");
        
        // Make dialog resizable and larger
        getDialogPane().setMinWidth(500);

        // Create form fields
        nameField = new TextField();
        nameField.setPromptText("e.g., Fresh Milk, Chicken Breast");
        nameField.setPrefWidth(300);
        
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
        categoryCombo.setPromptText("Select category");
        categoryCombo.setPrefWidth(300);
        
        // Custom category field (shown only when "Other" is selected)
        customCategoryField = new TextField();
        customCategoryField.setPromptText("Enter custom category");
        customCategoryField.setPrefWidth(300);
        customCategoryField.setVisible(false);
        customCategoryField.setManaged(false);
        
        // Show/hide custom category field based on selection
        categoryCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            boolean isOther = "Other".equals(newVal);
            customCategoryField.setVisible(isOther);
            customCategoryField.setManaged(isOther);
        });
        
        purchaseDatePicker = new DatePicker();
        purchaseDatePicker.setPromptText("Select purchase date");
        purchaseDatePicker.setPrefWidth(300);
        
        expiryDatePicker = new DatePicker();
        expiryDatePicker.setPromptText("Select expiry date");
        expiryDatePicker.setPrefWidth(300);
        
        quantitySpinner = new Spinner<>(1, 999, 1);
        quantitySpinner.setEditable(true);
        quantitySpinner.setPrefWidth(300);
        
        notesArea = new TextArea();
        notesArea.setPromptText("e.g., Stored in fridge, Opened on...");
        notesArea.setPrefRowCount(3);
        notesArea.setPrefWidth(300);

        // Populate fields if editing
        if (isEdit) {
            nameField.setText(item.getName());
            
            // Check if category is in the predefined list
            String itemCategory = item.getCategory();
            if (categoryCombo.getItems().contains(itemCategory)) {
                categoryCombo.setValue(itemCategory);
            } else {
                // Custom category - select "Other" and populate custom field
                categoryCombo.setValue("Other");
                customCategoryField.setText(itemCategory);
            }
            
            purchaseDatePicker.setValue(item.getPurchaseDate());
            expiryDatePicker.setValue(item.getExpiryDate());
            quantitySpinner.getValueFactory().setValue(item.getQuantity());
            notesArea.setText(item.getNotes());
        } else {
            // Default values for new item
            purchaseDatePicker.setValue(LocalDate.now());
            categoryCombo.setValue("Dairy & Eggs"); // Default category
        }

        // Create layout with better spacing
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(12);
        grid.setPadding(new Insets(20));

        int row = 0;
        
        grid.add(new Label("Product Name:*"), 0, row);
        grid.add(nameField, 1, row++);
        
        grid.add(new Label("Category:*"), 0, row);
        VBox categoryBox = new VBox(5);
        categoryBox.getChildren().addAll(categoryCombo, customCategoryField);
        grid.add(categoryBox, 1, row++);
        
        grid.add(new Label("Purchase Date:"), 0, row);
        grid.add(purchaseDatePicker, 1, row++);
        
        grid.add(new Label("Expiry Date:*"), 0, row);
        grid.add(expiryDatePicker, 1, row++);
        
        grid.add(new Label("Quantity:"), 0, row);
        grid.add(quantitySpinner, 1, row++);
        
        grid.add(new Label("Notes:"), 0, row);
        grid.add(notesArea, 1, row++);

        getDialogPane().setContent(grid);

        // Add buttons with better styling
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // Validate input
        Button saveButton = (Button) getDialogPane().lookupButton(saveButtonType);
        saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (!validateInput()) {
                event.consume(); // Prevent dialog from closing
            }
        });

        // Convert result to Item
        setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return createItemFromInput();
            }
            return null;
        });
        
        // Focus on name field
        nameField.requestFocus();
    }

    /**
     * Validate user input.
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
        
        // Validate expiry date is not in the past for new items
        if (!isEdit && expiryDatePicker.getValue().isBefore(LocalDate.now())) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Past Expiry Date");
            alert.setHeaderText("The expiry date is in the past");
            alert.setContentText("Do you want to continue?");
            
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isEmpty() || result.get() != ButtonType.OK) {
                return false;
            }
        }

        return true;
    }

    /**
     * Create Item object from form input.
     */
    private Item createItemFromInput() {
        Item result = isEdit ? item : new Item();
        
        result.setName(nameField.getText().trim());
        
        // Use custom category if "Other" is selected
        String category = categoryCombo.getValue();
        if ("Other".equals(category) && customCategoryField.getText() != null 
                && !customCategoryField.getText().trim().isEmpty()) {
            category = customCategoryField.getText().trim();
        }
        result.setCategory(category);
        
        result.setPurchaseDate(purchaseDatePicker.getValue());
        result.setExpiryDate(expiryDatePicker.getValue());
        result.setQuantity(quantitySpinner.getValue());
        result.setNotes(notesArea.getText() != null ? notesArea.getText().trim() : "");
        
        return result;
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
