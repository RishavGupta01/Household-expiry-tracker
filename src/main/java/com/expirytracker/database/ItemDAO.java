package com.expirytracker.database;

import com.expirytracker.model.Item;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Item entity - handles all database operations.
 */
public class ItemDAO {
    private final DatabaseManager dbManager;

    public ItemDAO() {
        this.dbManager = DatabaseManager.getInstance();
    }

    /**
     * Insert a new item into the database.
     */
    public boolean insert(Item item) {
        String sql = """
            INSERT INTO items (name, category, purchase_date, expiry_date, quantity, notes, image_path)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;

        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            
            pstmt.setString(1, item.getName());
            pstmt.setString(2, item.getCategory());
            pstmt.setString(3, item.getPurchaseDate() != null ? item.getPurchaseDate().toString() : null);
            pstmt.setString(4, item.getExpiryDate() != null ? item.getExpiryDate().toString() : null);
            pstmt.setInt(5, item.getQuantity());
            pstmt.setString(6, item.getNotes());
            pstmt.setString(7, item.getImagePath());

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                // Get the last inserted ID using SQLite's last_insert_rowid()
                try (Statement stmt = dbManager.getConnection().createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {
                    if (rs.next()) {
                        item.setId(rs.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error inserting item: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Update an existing item in the database.
     */
    public boolean update(Item item) {
        String sql = """
            UPDATE items 
            SET name = ?, category = ?, purchase_date = ?, expiry_date = ?, 
                quantity = ?, notes = ?, image_path = ?
            WHERE id = ?
        """;

        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setString(1, item.getName());
            pstmt.setString(2, item.getCategory());
            pstmt.setString(3, item.getPurchaseDate() != null ? item.getPurchaseDate().toString() : null);
            pstmt.setString(4, item.getExpiryDate() != null ? item.getExpiryDate().toString() : null);
            pstmt.setInt(5, item.getQuantity());
            pstmt.setString(6, item.getNotes());
            pstmt.setString(7, item.getImagePath());
            pstmt.setInt(8, item.getId());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating item: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Delete an item from the database.
     */
    public boolean delete(int id) {
        String sql = "DELETE FROM items WHERE id = ?";

        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting item: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Get all items from the database.
     */
    public List<Item> findAll() {
        List<Item> items = new ArrayList<>();
        String sql = "SELECT * FROM items ORDER BY expiry_date ASC";

        try (Statement stmt = dbManager.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                items.add(mapResultSetToItem(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving items: " + e.getMessage());
            e.printStackTrace();
        }
        return items;
    }

    /**
     * Get an item by ID.
     */
    public Item findById(int id) {
        String sql = "SELECT * FROM items WHERE id = ?";

        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToItem(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding item by ID: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Find items expiring within specified days.
     */
    public List<Item> findExpiringWithinDays(int days) {
        List<Item> items = new ArrayList<>();
        LocalDate targetDate = LocalDate.now().plusDays(days);
        
        String sql = """
            SELECT * FROM items 
            WHERE expiry_date IS NOT NULL 
            AND expiry_date <= ? 
            AND expiry_date >= ?
            ORDER BY expiry_date ASC
        """;

        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setString(1, targetDate.toString());
            pstmt.setString(2, LocalDate.now().toString());
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    items.add(mapResultSetToItem(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding expiring items: " + e.getMessage());
            e.printStackTrace();
        }
        return items;
    }

    /**
     * Search items by name or category.
     */
    public List<Item> search(String keyword) {
        List<Item> items = new ArrayList<>();
        String sql = """
            SELECT * FROM items 
            WHERE name LIKE ? OR category LIKE ?
            ORDER BY expiry_date ASC
        """;

        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            String searchPattern = "%" + keyword + "%";
            pstmt.setString(1, searchPattern);
            pstmt.setString(2, searchPattern);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    items.add(mapResultSetToItem(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error searching items: " + e.getMessage());
            e.printStackTrace();
        }
        return items;
    }

    /**
     * Map ResultSet row to Item object.
     */
    private Item mapResultSetToItem(ResultSet rs) throws SQLException {
        Item item = new Item();
        item.setId(rs.getInt("id"));
        item.setName(rs.getString("name"));
        item.setCategory(rs.getString("category"));
        
        String purchaseDate = rs.getString("purchase_date");
        if (purchaseDate != null) {
            item.setPurchaseDate(LocalDate.parse(purchaseDate));
        }
        
        String expiryDate = rs.getString("expiry_date");
        if (expiryDate != null) {
            item.setExpiryDate(LocalDate.parse(expiryDate));
        }
        
        item.setQuantity(rs.getInt("quantity"));
        item.setNotes(rs.getString("notes"));
        item.setImagePath(rs.getString("image_path"));
        
        return item;
    }
}
