@echo off
cd /d "%~dp0"

echo Smart Attendance System
echo ========================

java -version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Java not found.
    echo Install Java 24 from https://adoptium.net
    pause
    exit /b
)

if not exist "javafx" (
    echo ERROR: javafx folder not found.
    echo Make sure you downloaded the full project from GitHub.
    pause
    exit /b
)
if not exist "libs" (
    echo ERROR: libs folder not found.
    echo Make sure you downloaded the full project from GitHub.
    pause
    exit /b
)
if not exist "natives" (
    echo ERROR: natives folder not found.
    echo Make sure you downloaded the full project from GitHub.
    pause
    exit /b
)

if not exist "data\haarcascade_frontalface_default.xml" (
    echo WARNING: Haar cascade not found in data\ folder.
    echo Face detection will not work.
    echo Download haarcascade_frontalface_default.xml and place it in the data\ folder.
    echo.
)

if not exist "out" mkdir out
if not exist "data" mkdir data

echo Compiling...
for /r "src\main\java" %%f in (*.java) do (
    javac -encoding UTF-8 --module-path "javafx" --add-modules javafx.controls,javafx.fxml -cp "src/main/java;libs/opencv-4120.jar" -d out "%%f" 2>nul
)

echo Copying resources...
xcopy /s /y "src\main\resources\*" "out\" >nul

echo Starting...
java -Djava.library.path="javafx/bin" --module-path "javafx" --add-modules javafx.controls,javafx.fxml -cp "out;libs/opencv-4120.jar" attendance.MainApp

if errorlevel 1 (
    echo.
    echo ERROR: App failed to start. See error above.
    pause
)
