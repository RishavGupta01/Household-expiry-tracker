# Quick Start Script for Household Expiry Tracker
# Run this in PowerShell

Write-Host "╔════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║     Household Expiry Tracker - Quick Start Script      ║" -ForegroundColor Cyan
Write-Host "╚════════════════════════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host ""

# Check Java
Write-Host "Checking prerequisites..." -ForegroundColor Yellow
Write-Host ""

Write-Host "1. Checking Java..." -NoNewline
try {
    $javaVersion = java -version 2>&1 | Select-Object -First 1
    Write-Host " ✓ Found: $javaVersion" -ForegroundColor Green
} catch {
    Write-Host " ✗ Java not found!" -ForegroundColor Red
    Write-Host "   Please install Java 17+ from: https://adoptium.net/" -ForegroundColor Yellow
    exit 1
}

# Check Maven
Write-Host "2. Checking Maven..." -NoNewline
try {
    $mavenVersion = mvn -version 2>&1 | Select-Object -First 1
    Write-Host " ✓ Found: $mavenVersion" -ForegroundColor Green
} catch {
    Write-Host " ✗ Maven not found!" -ForegroundColor Red
    Write-Host "   Please install Maven from: https://maven.apache.org/download.cgi" -ForegroundColor Yellow
    exit 1
}

# Check Tesseract
Write-Host "3. Checking Tesseract-OCR..." -NoNewline
$tessPath = "C:\Program Files\Tesseract-OCR\tesseract.exe"
if (Test-Path $tessPath) {
    Write-Host " ✓ Found at $tessPath" -ForegroundColor Green
} else {
    Write-Host " ⚠ Not found at default location" -ForegroundColor Yellow
    Write-Host "   OCR scanning may not work. Install from: https://github.com/UB-Mannheim/tesseract/wiki" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "All prerequisites checked!" -ForegroundColor Green
Write-Host ""

# Build project
Write-Host "Building project..." -ForegroundColor Yellow
mvn clean install

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "✓ Build successful!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Starting application..." -ForegroundColor Yellow
    Write-Host ""
    
    # Run application
    mvn javafx:run
} else {
    Write-Host ""
    Write-Host "✗ Build failed!" -ForegroundColor Red
    Write-Host "Check the error messages above for details." -ForegroundColor Yellow
    exit 1
}
