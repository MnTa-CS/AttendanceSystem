# Smart Attendance System
### COMP1402 — Object Oriented Programming | Group Project (Low-Risk Version)

---

## What It Does
A desktop GUI application that lets you log student attendance manually.
Records are saved to a local CSV file and displayed in a sortable table.

---

## Project Structure

```
AttendanceSystem/
├── pom.xml                                      ← Maven build file
├── data/
│   └── attendance.csv                           ← Auto-created on first run
└── src/main/
    ├── java/attendance/
    │   ├── MainApp.java                          ← Entry point (JavaFX Application)
    │   ├── AttendanceRecord.java                 ← Model class (OOP - encapsulation)
    │   ├── AttendanceLogger.java                 ← File I/O handler
    │   └── AttendanceController.java             ← GUI controller (JavaFX)
    └── resources/
        ├── attendance.fxml                       ← UI layout
        └── style.css                             ← Dark theme styles
```

---

## How to Run

### Prerequisites
- Java 17+
- Maven 3.6+

### Steps
```bash
# 1. Clone / open the project folder
cd AttendanceSystem

# 2. Build the project
mvn clean package -q

# 3. Run the app
mvn javafx:run
```

Or run the fat JAR directly:
```bash
java --module-path /path/to/javafx-sdk/lib \
     --add-modules javafx.controls,javafx.fxml \
     -jar target/AttendanceSystem-1.0.jar
```

---

## OOP Concepts Used (for grading)

| Concept | Where |
|---|---|
| Encapsulation | `AttendanceRecord` — private fields, public getters |
| Separation of concerns | Model / Controller / Logger are separate classes |
| Error handling | Try-catch in all I/O operations + input validation |
| Java Collections | `ArrayList`, `ObservableList` for records |
| File I/O | `BufferedWriter`, `Files.readAllLines` in `AttendanceLogger` |
| JavaFX GUI | FXML layout + CSS styling + Controller bindings |

---

## Assessment Criteria Coverage

- ✅ Class design and structure
- ✅ Code quality and documentation (Javadoc comments)
- ✅ Code reusability and modularity
- ✅ Correct Java Collections and Datatypes
- ✅ I/O Handling (CSV read/write)
- ✅ Error Handling (validation + IOException)
- ✅ Solves a real-world problem (attendance tracking)
- ✅ GUI (JavaFX with FXML + CSS)

---

## Upgrading to Medium Risk (next step)
- Add a `Student` class with a list of subjects → demonstrates more OOP
- Add search/filter functionality to the table
- Export to PDF report

## Upgrading to High Risk (final step)
- Integrate OpenCV webcam feed for automatic face-based check-in
- Add a `Camera` class using `VideoCapture` from OpenCV
