package com.expirytracker.util;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DateParser utility.
 * Run with: mvn test
 */
public class DateParserTest {

    @Test
    public void testParseDateWithSlashes() {
        String text = "EXP 12/11/2025";
        Optional<LocalDate> result = DateParser.parseDateFromText(text);
        
        assertTrue(result.isPresent(), "Should parse date with slashes");
        assertEquals(2025, result.get().getYear());
    }

    @Test
    public void testParseDateWithMonthName() {
        String text = "BEST BEFORE 12 NOV 2025";
        Optional<LocalDate> result = DateParser.parseDateFromText(text);
        
        assertTrue(result.isPresent(), "Should parse date with month name");
        assertEquals(11, result.get().getMonthValue());
        assertEquals(12, result.get().getDayOfMonth());
    }

    @Test
    public void testParseCompactDate() {
        String text = "12NOV2025";
        Optional<LocalDate> result = DateParser.parseDateFromText(text);
        
        assertTrue(result.isPresent(), "Should parse compact date format");
        assertEquals(LocalDate.of(2025, 11, 12), result.get());
    }

    @Test
    public void testExtractProductName() {
        String text = "FRESH MILK 2L\nEXP 12/11/2025\nLOT 12345";
        String productName = DateParser.extractProductName(text);
        
        assertNotNull(productName);
        assertTrue(productName.contains("MILK"), "Should extract product name");
    }

    @Test
    public void testNoDateFound() {
        String text = "This text has no dates";
        Optional<LocalDate> result = DateParser.parseDateFromText(text);
        
        assertFalse(result.isPresent(), "Should return empty when no date found");
    }

    @Test
    public void testMultipleDateFormats() {
        String[] testTexts = {
            "EXP 12/11/2025",
            "BEST BEFORE 12-11-2025",
            "USE BY 12.11.2025",
            "EXPIRES 12 NOV 2025",
            "BB: 12NOV2025"
        };

        for (String text : testTexts) {
            Optional<LocalDate> result = DateParser.parseDateFromText(text);
            assertTrue(result.isPresent(), "Should parse: " + text);
        }
    }
}
