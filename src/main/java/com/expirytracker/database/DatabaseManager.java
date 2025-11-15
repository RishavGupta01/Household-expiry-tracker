package com.expirytracker.database;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Manages SQLite database connection and initialization.
 */
public class DatabaseManager {
    private static final String DB_DIR = System.getProperty("user.home") + File.separator + ".expirytracker";
    private static final String DB_PATH = DB_DIR + File.separator + "expiry.db";
    private static final String DB_URL = "jdbc:sqlite:" + DB_PATH;
    
    private static DatabaseManager instance;
    private Connection connection;

    private DatabaseManager() {
        initializeDatabase();
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    /**
     * Initialize database: create directory and tables if needed.
     */
    private void initializeDatabase() {
        try {
            // Create database directory if not exists
            File dbDir = new File(DB_DIR);
            if (!dbDir.exists()) {
                dbDir.mkdirs();
                System.out.println("Created database directory: " + DB_DIR);
            }

            // Establish connection
            connection = DriverManager.getConnection(DB_URL);
            System.out.println("Connected to database: " + DB_PATH);

            // Create tables
            createTables();

        } catch (SQLException e) {
            System.err.println("Failed to initialize database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Create database tables if they don't exist.
     */
    private void createTables() throws SQLException {
        String createItemsTable = """
            CREATE TABLE IF NOT EXISTS items (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                category TEXT,
                purchase_date TEXT,
                expiry_date TEXT,
                quantity INTEGER DEFAULT 1,
                notes TEXT,
                image_path TEXT
            )
        """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createItemsTable);
            System.out.println("Database tables initialized successfully");
        }
    }

    /**
     * Get the active database connection.
     */
    public Connection getConnection() {
        try {
            // Check if connection is still valid
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(DB_URL);
            }
        } catch (SQLException e) {
            System.err.println("Failed to get database connection: " + e.getMessage());
            e.printStackTrace();
        }
        return connection;
    }

    /**
     * Close the database connection.
     */
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Database connection closed");
            }
        } catch (SQLException e) {
            System.err.println("Error closing database connection: " + e.getMessage());
        }
    }

    public String getDatabasePath() {
        return DB_PATH;
    }
}
