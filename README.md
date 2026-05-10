# Smart Attendance System
### COMP1402 - Object Oriented Programming | Group Project

A Java desktop application for managing student attendance with live webcam face recognition.

---

## Project Risk Levels

This project was built in three progressive stages, each adding more complexity.

### Low Risk - Manual Attendance Logging
The foundation of the system. A JavaFX desktop app that lets you manually log student attendance and saves records to a CSV file.

**Features:**
- Enter student name, ID, and status (Present/Absent)
- Records saved automatically to `data/attendance.csv`
- View all records in a sortable table
- Clear all records with confirmation dialog

**OOP concepts:** Encapsulation, File I/O, Error Handling, JavaFX GUI

**Classes:** `MainApp`, `AttendanceRecord`, `AttendanceLogger`, `AttendanceController`

---

### Medium Risk - Student Roster, Search, and PDF Export
Builds on the low risk version by adding a student management system, live search/filter, subject tracking, and report generation.

**Features:**
- Student roster with multiple subjects per student
- Subject dropdown that filters attendance records
- Live search bar filtering by name or ID
- Export attendance to a styled HTML report (printable as PDF)
- Clicking a student in the roster auto-fills the attendance form

**OOP concepts:** Java Collections (HashMap, ArrayList), composition (Student has a list of subjects), separation of concerns

**New classes:** `Student`, `StudentRegistry`, `PdfExporter`

---

### High Risk - Live Webcam Face Recognition
Builds on the medium risk version by integrating OpenCV for live webcam access, face detection, face enrollment, and automatic face-based attendance logging.

**Features:**
- Live webcam feed displayed in the app
- Haar cascade face detection with green bounding boxes
- Enroll a student's face by capturing 10 photos automatically
- Face samples saved to `data/faces/` and persist between sessions
- Recognize enrolled faces using histogram comparison and auto-log attendance
- Recognized student's name displayed above their face in real time
- Cooldown system prevents logging the same student twice within 5 seconds

**OOP concepts:** External library integration (OpenCV), resource management, multithreading (background camera thread with JavaFX Platform.runLater)

**New classes:** `Camera`, `FaceDetector`, `FaceRecognizer`

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
├── javafx/                        - Bundled JavaFX 26 SDK jars
├── natives/                       - Bundled OpenCV native DLL
├── libs/                          - Bundled OpenCV Java jar
├── data/                          - Auto-created, stores CSV files and face samples
├── run.bat                        - Double-click to compile and run
└── README.md
```

---

## OOP Concepts Demonstrated

| Concept | Where |
|---|---|
| Encapsulation | All model classes (AttendanceRecord, Student, Camera) |
| Composition | Student has a List of subjects; StudentRegistry has a Map of Students |
| Separation of concerns | Model / Controller / Logger / Recognizer are separate classes |
| Java Collections | ArrayList, HashMap, LinkedHashSet throughout |
| File I/O | BufferedWriter, Files.readAllLines in Logger and Registry |
| Error handling | Try-catch in all I/O and camera operations plus input validation |
| External library | OpenCV for webcam, face detection, and face recognition |
| Multithreading | Background camera thread updates JavaFX UI via Platform.runLater |
| GUI | JavaFX with FXML layout and CSS dark theme |

---

## Requirements

Only **Java 24** needs to be installed. JavaFX and OpenCV are already bundled in the repo.

Download Java 24 here: https://adoptium.net/temurin/releases/?version=24

Verify after installing:
```
java -version
```
Should say `24.x.x`.

---

## Setup (do once)

**1. Clone or download this repository**

**2. Copy the Haar cascade file**

Find this file:
```
C:\OpenCV\build\etc\haarcascades\haarcascade_frontalface_default.xml
```
Copy it into the project's `data/` folder:
```
AttendanceSystem\data\haarcascade_frontalface_default.xml
```
If you don't have OpenCV installed, download it from https://opencv.org/releases/ just to get this file.

**3. Create the `out/` folder** in the project root if it doesn't exist.

---

## Running the App

### Option A - Double-click (easiest)
Just double-click `run.bat` in the project folder.

### Option B - From terminal
Open a terminal in the project folder and run:

**Compile:**
```powershell
javac -encoding UTF-8 --module-path "javafx" --add-modules javafx.controls,javafx.fxml -cp "src/main/java;libs/opencv-4120.jar" -d out (Get-ChildItem -Recurse -Filter "*.java" src/main/java | % { $_.FullName })
Copy-Item src/main/resources/* out/
```

**Run:**
```powershell
java "-Djava.library.path=natives" --module-path "javafx" --add-modules javafx.controls,javafx.fxml -cp "out;libs/opencv-4120.jar" attendance.MainApp
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
4. Use the search bar to filter by name, ID, or subject

### Face Enrollment (do once per student)
1. Go to the **Camera Check-in** tab
2. Click **Start Camera**
3. Select a student from the dropdown
4. Have the student look at the camera
5. Click **Enroll Face** - it captures 10 photos automatically
6. Wait for the progress bar to complete
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
4. The report opens in your browser - press Ctrl+P and save as PDF

---

## Notes

- Face data is stored in `data/faces/` as PNG files and persist between sessions
- The face model is rebuilt from saved photos each time the app starts
- If recognition is inaccurate, re-enroll the student in better lighting
- The `data/` folder is excluded from git since it contains personal data
