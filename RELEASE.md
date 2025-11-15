# Release Notes - Household Expiry Tracker v1.0.0

## 🎉 First Release

A smart desktop application to track household item expiry dates with OCR-powered image scanning.

## ✨ Features

- **Smart Date Recognition**: Automatically parse 15+ date formats from text/images
- **OCR Image Scanning**: Extract expiry dates from product photos using Tesseract
- **15 Product Categories**: Dairy & Eggs, Meat & Poultry, Seafood, Vegetables, Fruits, and more
- **Expiry Notifications**: Proactive alerts for items expiring soon
- **Color-Coded UI**: Visual indicators (Green: Safe, Yellow: Expiring Soon, Red: Expired)
- **CSV Import/Export**: Backup and transfer your data
- **Offline-First**: Works completely offline with local SQLite database

## 📋 System Requirements

- **Java**: Version 17 or higher
- **Tesseract OCR** (Optional): For automatic image scanning
  - Download: https://github.com/tesseract-ocr/tesseract/releases
  - Windows installer: `tesseract-ocr-setup.exe`
- **Operating System**: Windows, macOS, or Linux

## 🚀 How to Run

### Option 1: Using the JAR file
```bash
java -jar household-expiry-tracker-1.0.0.jar
```

### Option 2: Using the launcher script
**Windows:**
```bash
run.bat
```

**PowerShell/Linux:**
```bash
./run.ps1
```

## 📥 Installation Steps

1. **Install Java 17+** (if not already installed)
   - Download from: https://adoptium.net/

2. **Install Tesseract OCR** (Optional, for image scanning)
   - Windows: Download from https://github.com/UB-Mannheim/tesseract/wiki
   - Set installation path: `C:\Program Files\Tesseract-OCR`

3. **Download the JAR file** from the release assets

4. **Double-click** `household-expiry-tracker-1.0.0.jar` or run from command line

## 🗂️ Data Storage

- Database location: `%USERPROFILE%\.expirytracker\expiry.db` (Windows)
- Stores all item data locally and securely

## 🐛 Known Issues

- Image scanning requires Tesseract OCR installation
- Manual entry mode available if OCR is not installed

## 💡 Tips

- Use the "Scan Image" feature for quick data entry
- The app automatically detects manufacture dates and adds 6 months
- Export data regularly as CSV backup

## 🤝 Contributing

Found a bug or have a feature request? Open an issue on GitHub!

## 📄 License

This project is open source and available under the MIT License.
