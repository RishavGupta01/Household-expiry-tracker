package com.expirytracker.controller;

import com.expirytracker.database.ItemDAO;
import com.expirytracker.model.Item;
import com.expirytracker.service.NotificationService;
import com.expirytracker.service.OcrService;
import com.expirytracker.util.CsvUtil;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Main controller for the Expiry Tracker application.
 */
public class MainController {

    @FXML private TableView<Item> itemsTable;
    @FXML private TableColumn<Item, String> nameColumn;
    @FXML private TableColumn<Item, String> categoryColumn;
    @FXML private TableColumn<Item, LocalDate> expiryDateColumn;
    @FXML private TableColumn<Item, Integer> quantityColumn;
    @FXML private TableColumn<Item, String> statusColumn;
    
    @FXML private TextField searchField;
    @FXML private Button addButton;
    @FXML private Button editButton;
    @FXML private Button deleteButton;
    @FXML private Button scanButton;
    @FXML private Button exportButton;
    @FXML private Button importButton;
    @FXML private Label statusLabel;

    private final ItemDAO itemDAO;
    private final OcrService ocrService;
    private final NotificationService notificationService;
    private final ObservableList<Item> itemsList;

    public MainController() {
        this.itemDAO = new ItemDAO();
        this.ocrService = new OcrService();
        this.notificationService = new NotificationService(itemDAO);
        this.itemsList = FXCollections.observableArrayList();
    }

    @FXML
    public void initialize() {
        setupTableColumns();
        setupTableRowFactory();
        loadItems();
        
        // Start notification service
        notificationService.start();
        
        // Setup search functionality
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null || newValue.trim().isEmpty()) {
                loadItems();
            } else {
                searchItems(newValue);
            }
        });
        
        updateStatus("Ready");
    }

    /**
     * Setup table columns.
     */
    private void setupTableColumns() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        expiryDateColumn.setCellValueFactory(new PropertyValueFactory<>("expiryDate"));
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        
        // Status column shows days to expiry
        statusColumn.setCellValueFactory(cellData -> {
            Item item = cellData.getValue();
            long days = item.daysToExpiry();
            String status;
            if (days == Long.MAX_VALUE) {
                status = "No expiry";
            } else if (days < 0) {
                status = "EXPIRED";
            } else if (days == 0) {
                status = "Expires today";
            } else {
                status = days + " day" + (days == 1 ? "" : "s");
            }
            return new javafx.beans.property.SimpleStringProperty(status);
        });
    }

    /**
     * Setup row color coding based on expiry status.
     */
    private void setupTableRowFactory() {
        itemsTable.setRowFactory(tv -> new TableRow<Item>() {
            @Override
            protected void updateItem(Item item, boolean empty) {
                super.updateItem(item, empty);
                
                if (item == null || empty) {
                    setStyle("");
                } else {
                    long days = item.daysToExpiry();
                    if (days < 0) {
                        // Red: Expired
                        setStyle("-fx-background-color: rgba(255, 100, 100, 0.3);");
                    } else if (days <= 14) {
                        // Yellow: Expiring soon (1-14 days)
                        setStyle("-fx-background-color: rgba(255, 230, 120, 0.3);");
                    } else {
                        // Green: Fresh (>14 days)
                        setStyle("-fx-background-color: rgba(200, 255, 200, 0.3);");
                    }
                }
            }
        });
    }

    /**
     * Load all items from database.
     */
    private void loadItems() {
        List<Item> items = itemDAO.findAll();
        itemsList.clear();
        itemsList.addAll(items);
        itemsTable.setItems(itemsList);
        updateStatus(items.size() + " item(s) loaded");
    }

    /**
     * Search items by keyword.
     */
    private void searchItems(String keyword) {
        List<Item> items = itemDAO.search(keyword);
        itemsList.clear();
        itemsList.addAll(items);
        itemsTable.setItems(itemsList);
        updateStatus(items.size() + " item(s) found");
    }

    /**
     * Handle Add Item button.
     */
    @FXML
    private void handleAddItem() {
        try {
            ItemDialog dialog = new ItemDialog(null);
            Optional<Item> result = dialog.showAndWait();
            
            result.ifPresent(item -> {
                try {
                    boolean success = itemDAO.insert(item);
                    if (success) {
                        loadItems();
                        showInfo("Success", "Item added successfully: " + item.getName());
                        updateStatus("Item added: " + item.getName());
                    } else {
                        showError("Failed to add item to database. Please try again.");
                    }
                } catch (Exception e) {
                    showError("Error adding item: " + e.getMessage());
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            showError("Error opening add item dialog: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Handle Edit Item button.
     */
    @FXML
    private void handleEditItem() {
        Item selectedItem = itemsTable.getSelectionModel().getSelectedItem();
        
        if (selectedItem == null) {
            showWarning("Please select an item to edit");
            return;
        }

        ItemDialog dialog = new ItemDialog(selectedItem);
        Optional<Item> result = dialog.showAndWait();
        
        result.ifPresent(item -> {
            if (itemDAO.update(item)) {
                loadItems();
                updateStatus("Item updated: " + item.getName());
            } else {
                showError("Failed to update item");
            }
        });
    }

    /**
     * Handle Delete Item button.
     */
    @FXML
    private void handleDeleteItem() {
        Item selectedItem = itemsTable.getSelectionModel().getSelectedItem();
        
        if (selectedItem == null) {
            showWarning("Please select an item to delete");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Delete");
        alert.setHeaderText("Delete item: " + selectedItem.getName() + "?");
        alert.setContentText("This action cannot be undone.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (itemDAO.delete(selectedItem.getId())) {
                loadItems();
                updateStatus("Item deleted: " + selectedItem.getName());
            } else {
                showError("Failed to delete item");
            }
        }
    }

    /**
     * Handle Scan Image button.
     */
    @FXML
    private void handleScanImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Image to Scan");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.bmp", "*.gif", "*.tiff"),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        Stage stage = (Stage) scanButton.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            // Show dialog even if OCR is not available - user can enter manually
            if (!ocrService.isInitialized()) {
                Alert info = new Alert(Alert.AlertType.INFORMATION);
                info.setTitle("OCR Not Available");
                info.setHeaderText("Tesseract OCR is not installed");
                info.setContentText("You can still view the image and enter details manually.\n\n" +
                                  "To enable automatic OCR, install Tesseract from:\n" +
                                  "https://github.com/UB-Mannheim/tesseract/wiki");
                info.showAndWait();
            }
            
            ScanImageDialog dialog = new ScanImageDialog(selectedFile, ocrService, itemDAO);
            dialog.showAndWait();
            loadItems(); // Refresh table in case item was added
        }
    }

    /**
     * Handle Export CSV button.
     */
    @FXML
    private void handleExportCsv() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export to CSV");
        fileChooser.setInitialFileName("expiry_items_" + LocalDate.now() + ".csv");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("CSV Files", "*.csv")
        );

        Stage stage = (Stage) exportButton.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);

        if (file != null) {
            List<Item> items = itemDAO.findAll();
            if (CsvUtil.exportToCsv(items, file)) {
                updateStatus("Exported " + items.size() + " items to CSV");
                showInfo("Export successful", items.size() + " items exported to:\n" + file.getAbsolutePath());
            } else {
                showError("Failed to export CSV");
            }
        }
    }

    /**
     * Handle Import CSV button.
     */
    @FXML
    private void handleImportCsv() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import from CSV");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("CSV Files", "*.csv")
        );

        Stage stage = (Stage) importButton.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            List<Item> items = CsvUtil.importFromCsv(file);
            
            if (!items.isEmpty()) {
                int count = 0;
                for (Item item : items) {
                    if (itemDAO.insert(item)) {
                        count++;
                    }
                }
                loadItems();
                updateStatus("Imported " + count + " items from CSV");
                showInfo("Import successful", count + " items imported from:\n" + file.getAbsolutePath());
            } else {
                showWarning("No valid items found in CSV file");
            }
        }
    }

    /**
     * Update status label.
     */
    private void updateStatus(String message) {
        Platform.runLater(() -> statusLabel.setText(message));
    }

    /**
     * Show error alert.
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Show warning alert.
     */
    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Warning");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Show info alert.
     */
    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Shutdown - cleanup resources.
     */
    public void shutdown() {
        notificationService.stop();
    }
}
