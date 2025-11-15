# Household Expiry Tracker

A smart Java desktop application for tracking household item expiry dates with **AI-powered OCR image scanning**.

## 🌟 Key Features

- **Image-Based Expiry Detection**: Take a photo of product labels and automatically extract product name and expiry dates
- Smart date parsing with multiple format support
- Color-coded item list (Green/Yellow/Red based on expiry status)
- Daily notification system for items nearing expiry
- CSV export/import for backup and sharing
- Local SQLite database - no internet required
- OCR powered by Tesseract (offline processing)

## 🛠️ Tech Stack

- Java 17+
- JavaFX 20 (UI framework)
- SQLite (local database)
- Tess4J (Tesseract OCR wrapper)
- Maven (dependency management)

## 📋 Prerequisites

1. **Java 17 or higher** - [Download JDK](https://adoptium.net/)
2. **Maven** - [Install Maven](https://maven.apache.org/install.html)
3. **Tesseract-OCR** - [Download Tesseract](https://github.com/tesseract-ocr/tesseract)
   - Windows: Download installer from [UB-Mannheim](https://github.com/UB-Mannheim/tesseract/wiki)
   - Install to default location: `C:\Program Files\Tesseract-OCR`
   - Add to PATH or note installation directory

## 🚀 Quick Start

1. **Clone/Download this project**

2. **Install Tesseract OCR** (if not already installed)
   - Download from: https://github.com/UB-Mannheim/tesseract/wiki
   - During installation, ensure "English" language data is selected
   - Note the installation path (default: `C:\Program Files\Tesseract-OCR`)

3. **Build the project**
   ```powershell
   mvn clean install
   ```

4. **Run the application**
   ```powershell
   mvn javafx:run
   ```

## 📱 Usage Guide

### Adding Items Manually
1. Click **"Add Item"** button
2. Fill in product name, category, expiry date, etc.
3. Click **"Save"**

### Quick Scan Mode (OCR)
1. Click **"Scan Image"** button
2. Select a photo of the product label
3. Wait for OCR processing (a few seconds)
4. Review and edit the detected name and expiry date
5. Click **"Save"** to add the item

### Managing Items
- **Edit**: Double-click any item in the table
- **Delete**: Select item(s) and click **"Delete"**
- **Export**: Click **"Export CSV"** to backup your data
- **Import**: Click **"Import CSV"** to restore data

### Color Coding
- 🟢 **Green**: More than 14 days until expiry
- 🟡 **Yellow**: 1-14 days until expiry
- 🔴 **Red**: Expired or expires today

## 🔧 Configuration

### Tesseract Path Configuration
If Tesseract is not installed in the default location, update the path in:
`src/main/java/com/expirytracker/service/OcrService.java`

```java
private static final String TESS_DATA_PATH = "C:\\Program Files\\Tesseract-OCR\\tessdata";
```

### Notification Settings
Configure notification threshold in:
`src/main/java/com/expirytracker/service/NotificationService.java`

```java
private static final int NOTIFICATION_THRESHOLD_DAYS = 7; // Alert when <= 7 days
```

## 📸 Tips for Better OCR Results

1. **Take clear, well-lit photos**
2. **Focus on the expiry date area** - crop to label region
3. **Avoid shadows and reflections**
4. **Hold camera steady** - avoid blur
5. **High contrast** - use flash if needed
6. **Common date formats detected**:
   - `12/11/2025`, `12-11-2025`
   - `12 NOV 2025`, `NOV 12, 2025`
   - `2025-11-12`
   - `EXP 12NOV2025`, `BEST BEFORE 12/11/25`

## 📂 Database Location

SQLite database is stored at:
```
%USERPROFILE%\.expirytracker\expiry.db
```

## 🐛 Troubleshooting

### "Tesseract not found" error
- Ensure Tesseract is installed
- Check the path in `OcrService.java`
- Restart the application

### OCR not detecting dates
- Ensure image quality is good
- Try cropping to just the date region
- Check if date format is supported (add more patterns if needed)

### JavaFX runtime errors
- Ensure Java 17+ is installed
- Run: `java --version` to verify

## 🔄 Future Enhancements

- Barcode scanning for product lookup
- Android companion app
- Cloud sync (optional)
- Recipe suggestions based on expiring items
- Shopping list generation

## 📄 License

MIT License - Feel free to use and modify!

## 🤝 Contributing

Contributions welcome! Please open an issue or submit a pull request.
