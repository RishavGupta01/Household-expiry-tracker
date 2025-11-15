package com.expirytracker;

import com.expirytracker.controller.MainController;
import com.expirytracker.database.DatabaseManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 * Main application entry point for Household Expiry Tracker.
 */
public class ExpiryTrackerApp extends Application {

    private MainController mainController;

    @Override
    public void start(Stage primaryStage) {
        try {
            // Load FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/main.fxml"));
            Parent root = loader.load();
            mainController = loader.getController();

            // Setup scene
            Scene scene = new Scene(root, 900, 600);
            
            // Setup stage
            primaryStage.setTitle("Household Expiry Tracker");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(800);
            primaryStage.setMinHeight(500);
            
            // Set application icon (if available)
            try {
                primaryStage.getIcons().add(
                    new Image(getClass().getResourceAsStream("/icon.png"))
                );
            } catch (Exception e) {
                // Icon not found, skip
            }

            // Show welcome message
            System.out.println("╔════════════════════════════════════════════════════════╗");
            System.out.println("║     Household Expiry Tracker - Started Successfully    ║");
            System.out.println("╚════════════════════════════════════════════════════════╝");
            System.out.println();
            System.out.println("Database location: " + DatabaseManager.getInstance().getDatabasePath());
            System.out.println();

            // Handle window close
            primaryStage.setOnCloseRequest(event -> shutdown());

            primaryStage.show();

        } catch (Exception e) {
            System.err.println("Failed to start application:");
            e.printStackTrace();
        }
    }

    /**
     * Cleanup on application shutdown.
     */
    private void shutdown() {
        System.out.println("Shutting down application...");
        
        if (mainController != null) {
            mainController.shutdown();
        }
        
        DatabaseManager.getInstance().closeConnection();
        System.out.println("Application closed successfully");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
