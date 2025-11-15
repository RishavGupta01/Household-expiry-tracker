package com.expirytracker.util;

import com.expirytracker.model.Item;

import java.io.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for CSV import/export operations.
 */
public class CsvUtil {
    
    private static final String CSV_SEPARATOR = ",";
    private static final String CSV_HEADER = "Name,Category,Purchase Date,Expiry Date,Quantity,Notes,Image Path";

    /**
     * Export items to CSV file.
     */
    public static boolean exportToCsv(List<Item> items, File file) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            // Write header
            writer.println(CSV_HEADER);

            // Write items
            for (Item item : items) {
                writer.println(itemToCsvLine(item));
            }

            System.out.println("Exported " + items.size() + " items to: " + file.getAbsolutePath());
            return true;

        } catch (IOException e) {
            System.err.println("Error exporting to CSV: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Import items from CSV file.
     */
    public static List<Item> importFromCsv(File file) {
        List<Item> items = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            boolean firstLine = true;

            while ((line = reader.readLine()) != null) {
                // Skip header
                if (firstLine) {
                    firstLine = false;
                    continue;
                }

                // Parse line
                Item item = csvLineToItem(line);
                if (item != null) {
                    items.add(item);
                }
            }

            System.out.println("Imported " + items.size() + " items from: " + file.getAbsolutePath());

        } catch (IOException e) {
            System.err.println("Error importing from CSV: " + e.getMessage());
            e.printStackTrace();
        }

        return items;
    }

    /**
     * Convert Item to CSV line.
     */
    private static String itemToCsvLine(Item item) {
        return String.format("\"%s\",\"%s\",\"%s\",\"%s\",%d,\"%s\",\"%s\"",
                escapeQuotes(item.getName()),
                escapeQuotes(item.getCategory()),
                item.getPurchaseDate() != null ? item.getPurchaseDate().toString() : "",
                item.getExpiryDate() != null ? item.getExpiryDate().toString() : "",
                item.getQuantity(),
                escapeQuotes(item.getNotes()),
                escapeQuotes(item.getImagePath())
        );
    }

    /**
     * Convert CSV line to Item.
     */
    private static Item csvLineToItem(String line) {
        try {
            // Simple CSV parsing (handles quoted fields)
            List<String> fields = parseCsvLine(line);
            
            if (fields.size() < 5) {
                System.err.println("Invalid CSV line (too few fields): " + line);
                return null;
            }

            Item item = new Item();
            item.setName(fields.get(0));
            item.setCategory(fields.get(1));

            // Parse purchase date
            if (!fields.get(2).isEmpty()) {
                try {
                    item.setPurchaseDate(LocalDate.parse(fields.get(2)));
                } catch (Exception e) {
                    System.err.println("Invalid purchase date: " + fields.get(2));
                }
            }

            // Parse expiry date
            if (!fields.get(3).isEmpty()) {
                try {
                    item.setExpiryDate(LocalDate.parse(fields.get(3)));
                } catch (Exception e) {
                    System.err.println("Invalid expiry date: " + fields.get(3));
                }
            }

            // Parse quantity
            try {
                item.setQuantity(Integer.parseInt(fields.get(4)));
            } catch (NumberFormatException e) {
                item.setQuantity(1);
            }

            // Optional fields
            if (fields.size() > 5) {
                item.setNotes(fields.get(5));
            }
            if (fields.size() > 6) {
                item.setImagePath(fields.get(6));
            }

            return item;

        } catch (Exception e) {
            System.err.println("Error parsing CSV line: " + line);
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Parse CSV line handling quoted fields.
     */
    private static List<String> parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                fields.add(currentField.toString());
                currentField = new StringBuilder();
            } else {
                currentField.append(c);
            }
        }

        fields.add(currentField.toString());
        return fields;
    }

    /**
     * Escape quotes in CSV fields.
     */
    private static String escapeQuotes(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\"", "\"\"");
    }
}
