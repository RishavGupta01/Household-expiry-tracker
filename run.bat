@echo off
REM Quick Start Batch Script for Household Expiry Tracker

echo ========================================================
echo     Household Expiry Tracker - Quick Start
echo ========================================================
echo.

echo Checking prerequisites...
echo.

REM Check Java
echo 1. Checking Java...
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Java not found!
    echo Please install Java 17+ from: https://adoptium.net/
    pause
    exit /b 1
)
echo [OK] Java is installed
echo.

REM Check Maven
echo 2. Checking Maven...
mvn -version >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Maven not found!
    echo Please install Maven from: https://maven.apache.org/download.cgi
    pause
    exit /b 1
)
echo [OK] Maven is installed
echo.

REM Check Tesseract
echo 3. Checking Tesseract-OCR...
if exist "C:\Program Files\Tesseract-OCR\tesseract.exe" (
    echo [OK] Tesseract found
) else (
    echo [WARNING] Tesseract not found at default location
    echo OCR scanning may not work. Install from:
    echo https://github.com/UB-Mannheim/tesseract/wiki
)
echo.

echo ========================================================
echo Building project...
echo ========================================================
echo.

call mvn clean install

if %errorlevel% neq 0 (
    echo.
    echo [ERROR] Build failed!
    pause
    exit /b 1
)

echo.
echo ========================================================
echo Build successful! Starting application...
echo ========================================================
echo.

call mvn javafx:run

pause
