package com.expirytracker.service;

import net.sourceforge.tess4j.*;
import java.io.File;

/**
 * OCR Service using Tesseract to extract text from images.
 */
public class OcrService {
    
    // Default Tesseract installation paths
    private static final String[] POSSIBLE_TESS_PATHS = {
        "C:\\Program Files\\Tesseract-OCR\\tessdata",
        "C:\\Program Files (x86)\\Tesseract-OCR\\tessdata",
        "C:\\Tesseract-OCR\\tessdata",
        "/usr/share/tesseract-ocr/4.00/tessdata",
        "/usr/share/tesseract-ocr/tessdata",
        "/usr/local/share/tessdata"
    };

    private final Tesseract tesseract;
    private boolean initialized = false;
    private String tessDataPathField = null;

    public OcrService() {
        this.tesseract = new Tesseract();
        initializeTesseract();
    }

    /**
     * Initialize Tesseract with appropriate data path.
     */
    private void initializeTesseract() {
        // Try to find tessdata directory
        String tessDataPath = findTessDataPath();
        
        if (tessDataPath != null) {
            tesseract.setDatapath(tessDataPath);
            this.tessDataPathField = tessDataPath;
            tesseract.setLanguage("eng");
            tesseract.setPageSegMode(1); // Automatic page segmentation with OSD
            tesseract.setOcrEngineMode(1); // Neural nets LSTM engine
            
            // Optional: improve accuracy
            tesseract.setTessVariable("user_defined_dpi", "300");
            
            initialized = true;
            System.out.println("Tesseract initialized with data path: " + tessDataPath);
        } else {
            System.err.println("WARNING: Could not find Tesseract tessdata directory!");
            System.err.println("Please install Tesseract-OCR from: https://github.com/UB-Mannheim/tesseract/wiki");
            System.err.println("Searched paths:");
            for (String path : POSSIBLE_TESS_PATHS) {
                System.err.println("  - " + path);
            }
        }
    }

    /**
     * Find tessdata directory by checking common installation paths.
     */
    private String findTessDataPath() {
        // First, check if user specified TESSDATA_PREFIX environment variable
        String tessDataPrefix = System.getenv("TESSDATA_PREFIX");
        if (tessDataPrefix != null) {
            File tessDataDir = new File(tessDataPrefix);
            if (tessDataDir.exists() && tessDataDir.isDirectory()) {
                return tessDataPrefix;
            }
        }

        // Check common paths
        for (String path : POSSIBLE_TESS_PATHS) {
            File tessDataDir = new File(path);
            if (tessDataDir.exists() && tessDataDir.isDirectory()) {
                // Check if eng.traineddata exists
                File engData = new File(tessDataDir, "eng.traineddata");
                if (engData.exists()) {
                    return path;
                }
            }
        }

        // Try to get parent directory and look for tessdata
        String programPath = System.getenv("ProgramFiles");
        if (programPath != null) {
            File tessDir = new File(programPath, "Tesseract-OCR\\tessdata");
            if (tessDir.exists() && tessDir.isDirectory()) {
                return tessDir.getAbsolutePath();
            }
        }

        return null;
    }

    /**
     * Perform OCR on an image file.
     * @param imageFile Image file to process
     * @return Extracted text from the image
     * @throws TesseractException if OCR fails
     */
    public String doOcr(File imageFile) throws TesseractException {
        if (!initialized) {
            throw new TesseractException("Tesseract not properly initialized. Please install Tesseract-OCR.");
        }

        if (imageFile == null || !imageFile.exists()) {
            throw new TesseractException("Image file does not exist: " + imageFile);
        }

        try {
            System.out.println("Processing image: " + imageFile.getAbsolutePath());
            String result = tesseract.doOCR(imageFile);
            System.out.println("OCR completed. Extracted " + result.length() + " characters.");
            return result;
        } catch (TesseractException e) {
            System.err.println("OCR failed: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Check if Tesseract is properly initialized.
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Get the tessdata path being used.
     */
    public String getTessDataPath() {
        return tessDataPathField;
    }

    /**
     * Set custom tessdata path (for advanced users).
     */
    public void setTessDataPath(String path) {
        if (path != null && new File(path).exists()) {
            tesseract.setDatapath(path);
            this.tessDataPathField = path;
            initialized = true;
            System.out.println("Custom tessdata path set: " + path);
        }
    }
}
