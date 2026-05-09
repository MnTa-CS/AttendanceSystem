package attendance;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * Main controller — handles attendance logging, student management,
 * search/filter, and PDF report export.
 */
public class AttendanceController implements Initializable {

    // ── Attendance Tab ────────────────────────────────────────────────────────
    @FXML private TextField   nameField;
    @FXML private TextField   idField;
    @FXML private TextField   searchField;
    @FXML private ComboBox<String> subjectCombo;
    @FXML private RadioButton presentRadio;
    @FXML private RadioButton absentRadio;
    @FXML private ToggleGroup statusGroup;

    @FXML private TableView<AttendanceRecord>           attendanceTable;
    @FXML private TableColumn<AttendanceRecord, String> nameColumn;
    @FXML private TableColumn<AttendanceRecord, String> idColumn;
    @FXML private TableColumn<AttendanceRecord, String> statusColumn;
    @FXML private TableColumn<AttendanceRecord, String> subjectColumn;
    @FXML private TableColumn<AttendanceRecord, String> timestampColumn;

    @FXML private Label statusLabel;
    @FXML private Label totalLabel;
    @FXML private Label filePathLabel;

    // ── Student Tab ───────────────────────────────────────────────────────────
    @FXML private TextField stuNameField;
    @FXML private TextField stuIdField;
    @FXML private TextField stuSubjectField;

    @FXML private TableView<Student>           studentTable;
    @FXML private TableColumn<Student, String> stuIdColumn;
    @FXML private TableColumn<Student, String> stuNameColumn;
    @FXML private TableColumn<Student, String> stuSubjectsColumn;

    @FXML private Label stuStatusLabel;

    // ── Data ──────────────────────────────────────────────────────────────────
    private AttendanceLogger                 logger;
    private StudentRegistry                  registry;
    private PdfExporter                      pdfExporter;
    private ObservableList<AttendanceRecord> allRecords;
    private FilteredList<AttendanceRecord>   filteredRecords;
    private ObservableList<Student>          studentData;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        logger      = new AttendanceLogger();
        registry    = new StudentRegistry();
        pdfExporter = new PdfExporter();
        allRecords  = FXCollections.observableArrayList();
        studentData = FXCollections.observableArrayList();

        setupAttendanceTable();
        setupStudentTable();
        loadAttendanceRecords();
        loadStudents();
        setupSearch();

        presentRadio.setSelected(true);
        filePathLabel.setText("Saving to: " + logger.getFilePath());
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    private void setupAttendanceTable() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("studentName"));
        idColumn.setCellValueFactory(new PropertyValueFactory<>("studentId"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        subjectColumn.setCellValueFactory(new PropertyValueFactory<>("subject"));
        timestampColumn.setCellValueFactory(new PropertyValueFactory<>("formattedTimestamp"));

        statusColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); setStyle(""); return; }
                setText(s);
                setStyle("PRESENT".equals(s)
                        ? "-fx-text-fill: #27ae60; -fx-font-weight: bold;"
                        : "-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
            }
        });

        filteredRecords = new FilteredList<>(allRecords, r -> true);
        attendanceTable.setItems(filteredRecords);
    }

    private void setupStudentTable() {
        stuIdColumn.setCellValueFactory(new PropertyValueFactory<>("studentId"));
        stuNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        stuSubjectsColumn.setCellValueFactory(new PropertyValueFactory<>("subjectsDisplay"));
        studentTable.setItems(studentData);

        studentTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, selected) -> {
                    if (selected != null) {
                        nameField.setText(selected.getName());
                        idField.setText(selected.getStudentId());
                        refreshSubjectComboForStudent(selected);
                    }
                });
    }

    private void setupSearch() {
        searchField.textProperty().addListener((obs, old, text) -> applyFilter());
        subjectCombo.valueProperty().addListener((obs, old, val) -> applyFilter());
    }

    private void applyFilter() {
        String search  = searchField.getText() == null ? "" : searchField.getText().toLowerCase();
        String subject = subjectCombo.getValue();

        filteredRecords.setPredicate(r -> {
            boolean matchSearch = search.isBlank()
                    || r.getStudentName().toLowerCase().contains(search)
                    || r.getStudentId().toLowerCase().contains(search);
            boolean matchSubject = subject == null || "All Subjects".equals(subject)
                    || subject.equals(r.getSubject());
            return matchSearch && matchSubject;
        });
        updateTotal();
    }

    // ── Attendance Handlers ───────────────────────────────────────────────────

    @FXML
    private void handleLogAttendance() {
        String name    = nameField.getText();
        String id      = idField.getText();
        String subject = subjectCombo.getValue();
        String status  = presentRadio.isSelected() ? "PRESENT" : "ABSENT";

        if (name == null || name.isBlank())  { showStatus("Enter a student name.", true); return; }
        if (id   == null || id.isBlank())    { showStatus("Enter a student ID.",   true); return; }
        if (subject == null || subject.isBlank() || "All Subjects".equals(subject)) {
            showStatus("Select a subject.", true); return;
        }

        try {
            AttendanceRecord record = new AttendanceRecord(name, id, status, subject);
            logger.save(record);
            allRecords.add(record);
            updateTotal();
            clearAttendanceInputs();
            showStatus("✓ Logged: " + record.getStudentName() + " — " + status, false);
        } catch (IllegalArgumentException e) {
            showStatus("Input error: " + e.getMessage(), true);
        } catch (IOException e) {
            showStatus("File error: " + e.getMessage(), true);
        }
    }

    @FXML
    private void handleClearAll() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete all attendance records?", ButtonType.OK, ButtonType.CANCEL);
        confirm.setTitle("Clear All Records");
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                try {
                    logger.clearAll();
                    allRecords.clear();
                    updateTotal();
                    showStatus("All records cleared.", false);
                } catch (IOException e) {
                    showStatus("Failed: " + e.getMessage(), true);
                }
            }
        });
    }

    @FXML private void handleRefresh() {
        loadAttendanceRecords();
        showStatus("Records refreshed.", false);
    }

    @FXML
    private void handleExportReport() {
        List<AttendanceRecord> records = filteredRecords;
        if (records.isEmpty()) { showStatus("No records to export.", true); return; }

        String subject = subjectCombo.getValue();
        String label   = (subject == null || "All Subjects".equals(subject)) ? "All Subjects" : subject;

        try {
            Path reportPath = pdfExporter.exportToHtml(records, label);
            showStatus("✓ Report saved: " + reportPath.getFileName(), false);
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(reportPath.toUri());
            }
        } catch (IOException e) {
            showStatus("Export failed: " + e.getMessage(), true);
        } catch (Exception e) {
            showStatus("Could not open browser: report saved to data/reports/", false);
        }
    }

    // ── Student Handlers ──────────────────────────────────────────────────────

    @FXML
    private void handleAddStudent() {
        String name = stuNameField.getText();
        String id   = stuIdField.getText();

        if (name == null || name.isBlank()) { showStuStatus("Enter a name.", true); return; }
        if (id   == null || id.isBlank())   { showStuStatus("Enter an ID.",  true); return; }

        try {
            Student student = registry.exists(id) ? registry.getById(id) : new Student(id, name);
            student.setName(name);

            String subj = stuSubjectField.getText();
            if (subj != null && !subj.isBlank()) {
                for (String s : subj.split(",")) student.addSubject(s.trim());
            }

            registry.save(student);
            loadStudents();
            refreshAllSubjectCombos();
            clearStudentInputs();
            showStuStatus("✓ Student saved: " + student.getName(), false);
        } catch (IllegalArgumentException e) {
            showStuStatus("Input error: " + e.getMessage(), true);
        } catch (IOException e) {
            showStuStatus("File error: " + e.getMessage(), true);
        }
    }

    @FXML
    private void handleDeleteStudent() {
        Student selected = studentTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showStuStatus("Select a student to delete.", true); return; }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Remove " + selected.getName() + "?", ButtonType.OK, ButtonType.CANCEL);
        confirm.setTitle("Delete Student");
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                try {
                    registry.remove(selected.getStudentId());
                    loadStudents();
                    refreshAllSubjectCombos();
                    showStuStatus("Removed: " + selected.getName(), false);
                } catch (IOException e) {
                    showStuStatus("Failed: " + e.getMessage(), true);
                }
            }
        });
    }

    @FXML
    private void handleStudentTableClick() {
        Student s = studentTable.getSelectionModel().getSelectedItem();
        if (s != null) {
            stuNameField.setText(s.getName());
            stuIdField.setText(s.getStudentId());
            stuSubjectField.setText(String.join(", ", s.getSubjects()));
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void loadAttendanceRecords() {
        try {
            allRecords.setAll(logger.loadAll());
            refreshAllSubjectCombos();
            updateTotal();
        } catch (IOException e) {
            showStatus("Failed to load records: " + e.getMessage(), true);
        }
    }

    private void loadStudents() {
        studentData.setAll(registry.getAll());
    }

    private void refreshAllSubjectCombos() {
        Set<String> subjects = new LinkedHashSet<>();
        subjects.add("All Subjects");
        for (Student s : registry.getAll()) subjects.addAll(s.getSubjects());

        String current = subjectCombo.getValue();
        subjectCombo.setItems(FXCollections.observableArrayList(subjects));
        subjectCombo.setValue(subjects.contains(current) ? current : "All Subjects");
    }

    private void refreshSubjectComboForStudent(Student student) {
        Set<String> subjects = new LinkedHashSet<>(student.getSubjects());
        subjectCombo.setItems(FXCollections.observableArrayList(subjects));
        if (!subjects.isEmpty()) subjectCombo.setValue(subjects.iterator().next());
    }

    private void clearAttendanceInputs() {
        nameField.clear(); idField.clear();
        presentRadio.setSelected(true);
        nameField.requestFocus();
    }

    private void clearStudentInputs() {
        stuNameField.clear(); stuIdField.clear(); stuSubjectField.clear();
    }

    private void showStatus(String msg, boolean error) {
        statusLabel.setText(msg);
        statusLabel.setStyle(error ? "-fx-text-fill: #e74c3c;" : "-fx-text-fill: #27ae60;");
    }

    private void showStuStatus(String msg, boolean error) {
        stuStatusLabel.setText(msg);
        stuStatusLabel.setStyle(error ? "-fx-text-fill: #e74c3c;" : "-fx-text-fill: #27ae60;");
    }

    private void updateTotal() {
        long present = filteredRecords.stream().filter(r -> "PRESENT".equals(r.getStatus())).count();
        long absent  = filteredRecords.size() - present;
        totalLabel.setText("Showing: " + filteredRecords.size()
                + "  |  Present: " + present + "  |  Absent: " + absent);
    }
}
