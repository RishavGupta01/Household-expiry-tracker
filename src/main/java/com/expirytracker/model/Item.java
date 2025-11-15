package com.expirytracker.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Represents a household item with expiry tracking information.
 */
public class Item {
    private int id;
    private String name;
    private String category;
    private LocalDate purchaseDate;
    private LocalDate expiryDate;
    private int quantity;
    private String notes;
    private String imagePath;

    // Constructors
    public Item() {
        this.quantity = 1;
    }

    public Item(String name, String category, LocalDate purchaseDate, 
                LocalDate expiryDate, int quantity, String notes, String imagePath) {
        this.name = name;
        this.category = category;
        this.purchaseDate = purchaseDate;
        this.expiryDate = expiryDate;
        this.quantity = quantity;
        this.notes = notes;
        this.imagePath = imagePath;
    }

    /**
     * Calculate days remaining until expiry.
     * @return days to expiry (negative if expired, MAX_VALUE if no expiry set)
     */
    public long daysToExpiry() {
        if (expiryDate == null) return Long.MAX_VALUE;
        return ChronoUnit.DAYS.between(LocalDate.now(), expiryDate);
    }

    /**
     * Check if item is expired.
     */
    public boolean isExpired() {
        return expiryDate != null && expiryDate.isBefore(LocalDate.now());
    }

    /**
     * Get status color based on days to expiry.
     * @return "red", "yellow", or "green"
     */
    public String getStatusColor() {
        long days = daysToExpiry();
        if (days < 0) return "red";
        if (days <= 14) return "yellow";
        return "green";
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public LocalDate getPurchaseDate() {
        return purchaseDate;
    }

    public void setPurchaseDate(LocalDate purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    @Override
    public String toString() {
        return String.format("Item{id=%d, name='%s', category='%s', expiryDate=%s, daysToExpiry=%d}",
                id, name, category, expiryDate, daysToExpiry());
    }
}
