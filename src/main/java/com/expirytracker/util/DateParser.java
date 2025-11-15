package com.expirytracker.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.*;

/**
 * Utility class for parsing dates from OCR text with support for multiple formats.
 */
public class DateParser {
    
    // Common date formatters
    private static final List<DateTimeFormatter> FORMATTERS = List.of(
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("dd-MM-yyyy"),
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        DateTimeFormatter.ofPattern("MM/dd/yyyy"),
        DateTimeFormatter.ofPattern("dd.MM.yyyy"),
        DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("MMMM dd, yyyy", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("ddMMMyyyy", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("MM/yy"),
        DateTimeFormatter.ofPattern("MM-yy"),
        DateTimeFormatter.ofPattern("MM/yyyy"),
        DateTimeFormatter.ofPattern("MM-yyyy")
    );

    // Regex patterns for date detection
    private static final Pattern DATE_PATTERN = Pattern.compile(
        "\\b(\\d{1,2}[\\s]*[\\/\\-\\.][\\s]*\\d{1,2}[\\s]*[\\/\\-\\.][\\s]*\\d{2,4})\\b|" +  // dd/mm/yyyy
        "\\b(\\d{4}[\\s]*[\\-][\\s]*\\d{1,2}[\\s]*[\\-][\\s]*\\d{1,2})\\b|" +               // yyyy-mm-dd
        "\\b(\\d{1,2}[\\s]+[A-Za-z]{3,9}[\\s]+\\d{4})\\b|" +                                 // dd MMM yyyy
        "\\b([A-Za-z]{3,9}[\\s]+\\d{1,2}[\\s]*,?[\\s]*\\d{4})\\b|" +                         // MMM dd, yyyy
        "\\b(\\d{1,2}[\\s]*[\\-\\.][A-Za-z]{3,9}[\\s]*[\\-\\.][\\s]*\\d{2,4})\\b|" +         // dd-MMM-yyyy
        "\\b(\\d{2}[A-Za-z]{3}\\d{4})\\b|" +                                                  // 12NOV2025
        "\\b(\\d{1,2}[\\/\\-]\\d{4})\\b"                                                      // MM/yyyy
    );

    // Keywords that often precede dates
    private static final Pattern DATE_KEYWORD_PATTERN = Pattern.compile(
        "(EXP|EXPIRY|EXPIRES?|BEST\\s*BEFORE|BB|USE\\s*BY|MFG|MFD|MANUFACTURED|PRODUCTION)[:\\s]*([0-9A-Za-z\\s\\-\\/.]{4,})",
        Pattern.CASE_INSENSITIVE
    );

    /**
     * Parse date from OCR text with multiple format support.
     * @param text OCR text to parse
     * @return Optional containing the parsed date, or empty if no date found
     */
    public static Optional<LocalDate> parseDateFromText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return Optional.empty();
        }

        // Clean up text: remove extra whitespace, normalize
        text = text.replaceAll("\\s+", " ").trim();

        // First, try to find dates with keywords
        Matcher keywordMatcher = DATE_KEYWORD_PATTERN.matcher(text);
        while (keywordMatcher.find()) {
            String dateCandidate = keywordMatcher.group(2);
            Optional<LocalDate> parsed = tryParseDateString(dateCandidate);
            if (parsed.isPresent()) {
                return parsed;
            }
        }

        // Then try general date pattern matching
        Matcher dateMatcher = DATE_PATTERN.matcher(text);
        while (dateMatcher.find()) {
            String candidate = dateMatcher.group().trim();
            Optional<LocalDate> parsed = tryParseDateString(candidate);
            if (parsed.isPresent()) {
                return parsed;
            }
        }

        return Optional.empty();
    }

    /**
     * Try to parse a date string with all available formatters.
     */
    private static Optional<LocalDate> tryParseDateString(String dateStr) {
        // Keep original and also prepare a normalized variant
        String original = dateStr == null ? "" : dateStr.trim();
        String normalized = original.replaceAll("\\s+", " ")
                                  .replaceAll("([A-Za-z])(\\d)", "$1 $2")  // Add space between letters and digits
                                  .replaceAll("(\\d)([A-Za-z])", "$1 $2"); // Add space between digits and letters

        // Create a title-cased variant to help parse uppercase month names like "NOV"
        String titleCased = titleCaseWords(original);
        String normalizedTitle = titleCaseWords(normalized);

        // Try each formatter on the original first (handles compact patterns like ddMMMyyyy),
        // then on the normalized form.
        for (DateTimeFormatter formatter : FORMATTERS) {
            // try original
            try {
                LocalDate date = LocalDate.parse(original, formatter);
                if (isReasonableDate(date)) return Optional.of(date);
            } catch (DateTimeParseException ignored) {}

            // try normalized
            try {
                LocalDate date = LocalDate.parse(normalized, formatter);
                if (isReasonableDate(date)) return Optional.of(date);
            } catch (DateTimeParseException ignored) {}

            // try title-cased variants to handle all-uppercase month names from OCR
            try {
                LocalDate date = LocalDate.parse(titleCased, formatter);
                if (isReasonableDate(date)) return Optional.of(date);
            } catch (DateTimeParseException ignored) {}

            try {
                LocalDate date = LocalDate.parse(normalizedTitle, formatter);
                if (isReasonableDate(date)) return Optional.of(date);
            } catch (DateTimeParseException ignored) {}
        }

        // Try manual parsing for compact formats like "12NOV2025" using the original
        Optional<LocalDate> compact = tryParseCompactFormat(original);
        if (compact.isPresent()) return compact;

        // As a last resort, try compact on normalized (in case normalization removed punctuation)
        return tryParseCompactFormat(normalized);
    }

    /**
     * Parse compact date formats like "12NOV2025" or "12NOV25".
     */
    private static Optional<LocalDate> tryParseCompactFormat(String dateStr) {
        Pattern compactPattern = Pattern.compile("(\\d{1,2})([A-Za-z]{3})(\\d{2,4})", Pattern.CASE_INSENSITIVE);
        Matcher matcher = compactPattern.matcher(dateStr);
        
        if (matcher.find()) {
            try {
                int day = Integer.parseInt(matcher.group(1));
                String monthStr = matcher.group(2).toUpperCase();
                int year = Integer.parseInt(matcher.group(3));
                
                // Handle 2-digit year
                if (year < 100) {
                    year += (year < 50) ? 2000 : 1900;
                }
                
                // Parse month name
                Map<String, Integer> months = Map.ofEntries(
                    Map.entry("JAN", 1), Map.entry("FEB", 2), Map.entry("MAR", 3),
                    Map.entry("APR", 4), Map.entry("MAY", 5), Map.entry("JUN", 6),
                    Map.entry("JUL", 7), Map.entry("AUG", 8), Map.entry("SEP", 9),
                    Map.entry("OCT", 10), Map.entry("NOV", 11), Map.entry("DEC", 12)
                );
                
                Integer month = months.get(monthStr);
                if (month != null) {
                    LocalDate date = LocalDate.of(year, month, day);
                    if (isReasonableDate(date)) {
                        return Optional.of(date);
                    }
                }
            } catch (Exception ignored) {
                // Parsing failed
            }
        }
        
        return Optional.empty();
    }

    /**
     * Title-case long alphabetic words (length >=3) to help parsing month names.
     */
    private static String titleCaseWords(String input) {
        if (input == null || input.isEmpty()) return input == null ? "" : input;
        Pattern p = Pattern.compile("\\b([A-Za-z]{3,})\\b");
        Matcher m = p.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String w = m.group(1);
            String rep = w.substring(0,1).toUpperCase() + w.substring(1).toLowerCase();
            m.appendReplacement(sb, rep);
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /**
     * Check if a date is reasonable (not too far in past or future).
     */
    private static boolean isReasonableDate(LocalDate date) {
        LocalDate now = LocalDate.now();
        LocalDate minDate = now.minusYears(5);  // Not more than 5 years old
        LocalDate maxDate = now.plusYears(10);  // Not more than 10 years in future
        
        return !date.isBefore(minDate) && !date.isAfter(maxDate);
    }

    /**
     * Extract potential product name from OCR text (before date keywords).
     */
    public static String extractProductName(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }

        // Split by lines
        String[] lines = text.split("\\r?\\n");
        
        // Look for keywords that typically come before product names
        Pattern keywordPattern = Pattern.compile(
            ".*(EXP|EXPIRY|EXPIRES|BEST\\s*BEFORE|BB|USE\\s*BY|MFG|LOT|BATCH).*",
            Pattern.CASE_INSENSITIVE
        );

        StringBuilder productName = new StringBuilder();
        
        // Take first few lines that don't contain date keywords
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            
            // Stop if we hit a date keyword line
            if (keywordPattern.matcher(line).matches()) {
                break;
            }
            
            // Stop if line looks like a date
            if (DATE_PATTERN.matcher(line).find()) {
                break;
            }
            
            // Add this line to product name
            if (productName.length() > 0) {
                productName.append(" ");
            }
            productName.append(line);
            
            // Limit to first 2-3 lines
            if (productName.length() > 100) {
                break;
            }
        }

        String result = productName.toString().trim();
        
        // If empty, just take the first non-empty line
        if (result.isEmpty() && lines.length > 0) {
            for (String line : lines) {
                line = line.trim();
                if (!line.isEmpty()) {
                    result = line;
                    break;
                }
            }
        }

        return result;
    }
}
