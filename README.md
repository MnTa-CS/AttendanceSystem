# Smart Attendance System
### COMP1402 - Object Oriented Programming | Group Project

A Java desktop application for managing student attendance with live webcam face recognition.

---

## Features

- Log student attendance manually with subject tracking
- Manage a student roster with multiple subjects per student
- Live webcam feed with automatic face detection
- Enroll student faces (10 samples captured automatically)
- Auto check-in by recognizing enrolled faces
- Export attendance reports to HTML (printable as PDF)
- All data saved locally to CSV files

---

## Requirements

Before running, install the following (all free):

### 1. Java 24
Download and install the `.msi` installer for Windows x64:
https://adoptium.net/temurin/releases/?version=24

Verify after installing:
```
java -version
```
Should say `24.x.x`.

### 2. JavaFX 26 SDK
Download the **SDK** zip for Windows at:
https://gluonhq.com/products/javafx/

Extract it to exactly: `C:\javafx-sdk-26.0.1`

### 3. OpenCV 4.12
Download the Windows `.exe` self-extractor at:
https://opencv.org/releases/

Run it and extract to exactly: `C:\OpenCV`

---

## Setup (do once)

**1. Clone or download this repository**

**2. Copy the Haar cascade file**

Find this file on your computer:
```
C:\OpenCV\build\etc\haarcascades\haarcascade_frontalface_default.xml
```
Copy it into the project's `data/` folder:
```
AttendanceSystem\data\haarcascade_frontalface_default.xml
```

**3. Create the `out/` folder** in the project root if it doesn't exist.

---

## Running the App

### Option A - Double-click (easiest)
Just double-click `run.bat` in the project folder.

### Option B - From terminal
Open a terminal in the project folder and run:

**Compile:**
```powershell
javac -encoding UTF-8 --module-path "C:\javafx-sdk-26.0.1\lib" --add-modules javafx.controls,javafx.fxml -cp "src/main/java;C:\OpenCV\build\java\opencv-4120.jar" -d out (Get-ChildItem -Recurse -Filter "*.java" src/main/java | % { $_.FullName })
Copy-Item src/main/resources/* out/
```

**Run:**
```powershell
java "-Djava.library.path=C:\OpenCV\build\java\x64" --module-path "C:\javafx-sdk-26.0.1\lib" --add-modules javafx.controls,javafx.fxml -cp "out;C:\OpenCV\build\java\opencv-4120.jar" attendance.MainApp
```

---

## How to Use

### Adding Students
1. Go to the **Students** tab
2. Enter name, ID, and subjects (comma-separated)
3. Click **Save Student**

### Logging Attendance Manually
1. Go to the **Attendance** tab
2. Fill in name, ID, subject, and status
3. Click **Log Attendance**
4. Use the search bar to filter records by name, ID, or subject

### Face Enrollment (do once per student)
1. Go to the **Camera Check-in** tab
2. Click **Start Camera**
3. Select a student from the dropdown
4. Have the student look at the camera
5. Click **Enroll Face** — it captures 10 photos automatically
6. Wait for the progress bar to complete and model to train
7. Repeat for each student

### Auto Check-in via Face Recognition
1. Go to the **Camera Check-in** tab
2. Click **Start Camera**
3. Select the subject from the dropdown
4. Have the student look at the camera
5. Click **Recognize and Log Attendance**
6. If the face matches, attendance is logged automatically

### Exporting Reports
1. Go to the **Attendance** tab
2. Optionally filter by subject first
3. Click **Export Report (HTML to PDF)**
4. The report opens in your browser — press Ctrl+P and save as PDF

---

## Project Structure

```
AttendanceSystem/
├── src/main/java/attendance/
│   ├── MainApp.java               - Entry point, loads OpenCV native library
│   ├── AttendanceRecord.java      - Record model (name, ID, status, subject, timestamp)
│   ├── AttendanceLogger.java      - Reads and writes attendance CSV
│   ├── Student.java               - Student model with list of subjects
│   ├── StudentRegistry.java       - Manages student roster, persists to CSV
│   ├── Camera.java                - Wraps OpenCV VideoCapture for webcam access
│   ├── FaceDetector.java          - Haar cascade face detection
│   ├── FaceRecognizer.java        - Histogram-based face recognition, saves/loads samples
│   ├── PdfExporter.java           - Generates styled HTML attendance reports
│   └── AttendanceController.java  - JavaFX controller for all UI logic
├── src/main/resources/
│   ├── attendance.fxml            - UI layout (3 tabs)
│   └── style.css                  - Dark theme styles
├── data/                          - Auto-created, stores CSV files and face samples
├── run.bat                        - Double-click to compile and run
└── README.md
```

---

## OOP Concepts Demonstrated

| Concept | Where |
|---|---|
| Encapsulation | All model classes (AttendanceRecord, Student, Camera) |
| Separation of concerns | Model / Controller / Logger / Recognizer are separate classes |
| Java Collections | ArrayList, HashMap, LinkedHashSet throughout |
| File I/O | BufferedWriter, Files.readAllLines in Logger and Registry |
| Error handling | Try-catch in all I/O and camera operations + input validation |
| External library | OpenCV for webcam, face detection, and face recognition |
| GUI | JavaFX with FXML layout and CSS dark theme |

---

## Notes

- Face data is stored in `data/faces/` as PNG files — they persist between sessions
- The face model is rebuilt from saved photos each time the app starts
- If recognition is inaccurate, re-enroll the student in better lighting
- The `data/` folder is excluded from git (see `.gitignore`) since it contains personal data
