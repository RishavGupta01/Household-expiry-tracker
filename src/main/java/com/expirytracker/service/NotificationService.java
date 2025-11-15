package com.expirytracker.service;

import com.expirytracker.database.ItemDAO;
import com.expirytracker.model.Item;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Service for managing notifications about expiring items.
 */
public class NotificationService {
    
    private static final int NOTIFICATION_THRESHOLD_DAYS = 7; // Alert when <= 7 days
    private static final int CHECK_INTERVAL_HOURS = 24; // Check every 24 hours
    
    private final ItemDAO itemDAO;
    private final ScheduledExecutorService scheduler;
    private boolean running = false;

    public NotificationService(ItemDAO itemDAO) {
        this.itemDAO = itemDAO;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r);
            thread.setDaemon(true); // Daemon thread so it doesn't prevent app shutdown
            thread.setName("NotificationService");
            return thread;
        });
    }

    /**
     * Start the notification service with daily checks.
     */
    public void start() {
        if (running) {
            System.out.println("NotificationService already running");
            return;
        }

        running = true;
        
        // Run initial check after 10 seconds
        long initialDelaySeconds = 10;
        
        scheduler.scheduleAtFixedRate(() -> {
            try {
                checkAndNotify();
            } catch (Exception e) {
                System.err.println("Error in notification check: " + e.getMessage());
                e.printStackTrace();
            }
        }, initialDelaySeconds, CHECK_INTERVAL_HOURS * 3600, TimeUnit.SECONDS);

        System.out.println("NotificationService started - checking every " + CHECK_INTERVAL_HOURS + " hours");
    }

    /**
     * Check for expiring items and show notification.
     */
    private void checkAndNotify() {
        System.out.println("Running expiry check...");
        
        List<Item> expiringItems = itemDAO.findExpiringWithinDays(NOTIFICATION_THRESHOLD_DAYS);
        
        if (!expiringItems.isEmpty()) {
            System.out.println("Found " + expiringItems.size() + " items expiring soon");
            
            // Show notification on JavaFX UI thread
            Platform.runLater(() -> showNotification(expiringItems));
        } else {
            System.out.println("No items expiring soon");
        }
    }

    /**
     * Show notification dialog with expiring items.
     */
    private void showNotification(List<Item> items) {
        Alert alert = new Alert(AlertType.WARNING);
        alert.setTitle("Items Expiring Soon!");
        alert.setHeaderText(items.size() + " item(s) expiring within " + NOTIFICATION_THRESHOLD_DAYS + " days");
        
        StringBuilder content = new StringBuilder();
        for (Item item : items) {
            long days = item.daysToExpiry();
            String status = days < 0 ? "EXPIRED" : days == 0 ? "EXPIRES TODAY" : "Expires in " + days + " day(s)";
            content.append("• ").append(item.getName())
                   .append(" - ").append(status)
                   .append("\n");
        }
        
        alert.setContentText(content.toString());
        alert.show();
    }

    /**
     * Manually trigger a notification check (for testing).
     */
    public void checkNow() {
        checkAndNotify();
    }

    /**
     * Stop the notification service.
     */
    public void stop() {
        if (running) {
            scheduler.shutdownNow();
            running = false;
            System.out.println("NotificationService stopped");
        }
    }

    public boolean isRunning() {
        return running;
    }
}
