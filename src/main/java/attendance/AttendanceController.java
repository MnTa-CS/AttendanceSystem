package attendance;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.*;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.ResourceBundle;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class AttendanceController implements Initializable {

    @FXML private TextField        nameField;
    @FXML private TextField        idField;
    @FXML private TextField        searchField;
    @FXML private ComboBox<String> subjectCombo;
    @FXML private RadioButton      presentRadio;
    @FXML private RadioButton      absentRadio;
    @FXML private ToggleGroup      statusGroup;
    @FXML private TableView<AttendanceRecord>           attendanceTable;
    @FXML private TableColumn<AttendanceRecord, String> nameColumn;
    @FXML private TableColumn<AttendanceRecord, String> idColumn;
    @FXML private TableColumn<AttendanceRecord, String> statusColumn;
    @FXML private TableColumn<AttendanceRecord, String> subjectColumn;
    @FXML private TableColumn<AttendanceRecord, String> timestampColumn;
    @FXML private Label statusLabel;
    @FXML private Label totalLabel;
    @FXML private Label filePathLabel;

    @FXML private TextField stuNameField;
    @FXML private TextField stuIdField;
    @FXML private TextField stuSubjectField;
    @FXML private TableView<Student>           studentTable;
    @FXML private TableColumn<Student, String> stuIdColumn;
    @FXML private TableColumn<Student, String> stuNameColumn;
    @FXML private TableColumn<Student, String> stuSubjectsColumn;
    @FXML private Label stuStatusLabel;

    @FXML private ImageView        cameraView;
    @FXML private Label            faceCountLabel;
    @FXML private Label            camStatusLabel;
    @FXML private ComboBox<String> enrollStudentCombo;
    @FXML private Button           enrollButton;
    @FXML private ProgressBar      enrollProgress;
    @FXML private Label            enrollStatusLabel;
    @FXML private ComboBox<String> recogSubjectCombo;
    @FXML private Button           startCamButton;
    @FXML private Button           stopCamButton;
    @FXML private Button           recognizeButton;
    @FXML private Label            recognizedLabel;

    private AttendanceLogger   logger;
    private StudentRegistry    registry;
    private PdfExporter        pdfExporter;
    private Camera             camera;
    private FaceDetector       faceDetector;
    private FaceRecognizer     faceRecognizer;

    private ObservableList<AttendanceRecord> allRecords;
    private ObservableList<Student>          studentData;

    private ScheduledExecutorService cameraExecutor;
    private volatile Rect[]  lastFaces = new Rect[0];
    private volatile Mat     lastFrame = null;

    private final AtomicBoolean enrolling       = new AtomicBoolean(false);
    private final AtomicInteger samplesCaptured = new AtomicInteger(0);
    private static final int    TOTAL_SAMPLES   = 10;
    private String              enrollingId     = null;

    private final HashMap<String, Long> lastLoggedTime = new HashMap<>();
    private static final long           COOLDOWN_MS    = 5000;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        logger        = new AttendanceLogger();
        registry      = new StudentRegistry();
        pdfExporter   = new PdfExporter();
        camera        = new Camera();
        faceDetector  = new FaceDetector();
        faceRecognizer = new FaceRecognizer();

        allRecords  = FXCollections.observableArrayList();
        studentData = FXCollections.observableArrayList();

        setupAttendanceTable();
        setupStudentTable();
        setupSearch();

        loadAttendanceRecords();
        loadStudents();

        presentRadio.setSelected(true);
        filePathLabel.setText("Saving to: " + logger.getFilePath());
        stopCamButton.setDisable(true);
        recognizeButton.setDisable(true);
        enrollProgress.setProgress(0);

        if (!faceDetector.isLoaded()) {
            showCamStatus("Haar cascade XML missing. Place it in the data/ folder.", true);
        }
        refreshEnrollCombo();
        refreshRecogSubjectCombo();

        if (faceRecognizer.isTrained()) {
            showCamStatus("Face model loaded. " + faceRecognizer.enrolledCount()
                    + " student(s) enrolled.", false);
        }
    }

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

        attendanceTable.setItems(allRecords);
    }

    private void setupStudentTable() {
        stuIdColumn.setCellValueFactory(new PropertyValueFactory<>("studentId"));
        stuNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        stuSubjectsColumn.setCellValueFactory(new PropertyValueFactory<>("subjectsDisplay"));
        studentTable.setItems(studentData);

        studentTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, sel) -> {
                    if (sel != null) {
                        stuNameField.setText(sel.getName());
                        stuIdField.setText(sel.getStudentId());
                        stuSubjectField.setText(sel.getSubjectsDisplay().replace("-", ""));
                    }
                });
    }

    private void setupSearch() {
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilter());
        subjectCombo.valueProperty().addListener((obs, oldVal, newVal) -> applyFilter());
    }

    private void applyFilter() {
        String search  = searchField.getText() == null ? "" : searchField.getText().toLowerCase();
        String subject = subjectCombo.getValue();

        try {
            ArrayList<AttendanceRecord> loaded = logger.loadAll();
            ArrayList<AttendanceRecord> filtered = new ArrayList<>();

            for (AttendanceRecord r : loaded) {
                boolean matchesSearch = search.isBlank()
                        || r.getStudentName().toLowerCase().contains(search)
                        || r.getStudentId().toLowerCase().contains(search);
                boolean matchesSubject = subject == null || "All Subjects".equals(subject)
                        || subject.equals(r.getSubject());
                if (matchesSearch && matchesSubject) {
                    filtered.add(r);
                }
            }

            allRecords.setAll(filtered);
            updateTotal();
        } catch (IOException e) {
            showStatus("Filter error: " + e.getMessage(), true);
        }
    }

    @FXML
    private void handleLogAttendance() {
        String name    = nameField.getText();
        String id      = idField.getText();
        String subject = subjectCombo.getValue();
        String status  = presentRadio.isSelected() ? "PRESENT" : "ABSENT";

        if (name == null || name.isBlank()) { showStatus("Enter a student name.", true); return; }
        if (id == null || id.isBlank())     { showStatus("Enter a student ID.",   true); return; }
        if (subject == null || "All Subjects".equals(subject)) {
            showStatus("Select a subject.", true); return;
        }

        try {
            AttendanceRecord rec = new AttendanceRecord(name, id, status, subject);
            logger.save(rec);
            allRecords.add(rec);
            updateTotal();
            clearAttendanceFields();
            showStatus("Logged: " + name + " - " + status, false);
        } catch (IOException e) {
            showStatus("Error saving: " + e.getMessage(), true);
        }
    }

    @FXML
    private void handleClearAll() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete all attendance records?", ButtonType.OK, ButtonType.CANCEL);
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
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

    @FXML
    private void handleRefresh() {
        loadAttendanceRecords();
        showStatus("Refreshed.", false);
    }

    @FXML
    private void handleExportReport() {
        if (allRecords.isEmpty()) { showStatus("No records to export.", true); return; }
        String subj  = subjectCombo.getValue();
        String label = (subj == null || "All Subjects".equals(subj)) ? "All Subjects" : subj;
        try {
            ArrayList<AttendanceRecord> list = new ArrayList<>(allRecords);
            Path p = pdfExporter.exportToHtml(list, label);
            showStatus("Report saved: " + p.getFileName(), false);
            if (Desktop.isDesktopSupported()) Desktop.getDesktop().browse(p.toUri());
        } catch (Exception e) {
            showStatus("Export failed: " + e.getMessage(), true);
        }
    }

    @FXML
    private void handleAddStudent() {
        String name  = stuNameField.getText();
        String id    = stuIdField.getText();
        String subjs = stuSubjectField.getText();

        if (name == null || name.isBlank()) { showStuStatus("Enter a name.", true); return; }
        if (id   == null || id.isBlank())   { showStuStatus("Enter an ID.",  true); return; }

        try {
            Student s = registry.exists(id) ? registry.getById(id) : new Student(id, name);
            s.setName(name);
            if (subjs != null && !subjs.isBlank()) {
                for (String sub : subjs.split(",")) {
                    s.addSubject(sub.trim());
                }
            }
            registry.save(s);
            loadStudents();
            refreshAllSubjectCombos();
            refreshEnrollCombo();
            clearStudentFields();
            showStuStatus("Saved: " + s.getName(), false);
        } catch (IOException e) {
            showStuStatus("Error: " + e.getMessage(), true);
        }
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

    @FXML
    private void handleDeleteStudent() {
        Student sel = studentTable.getSelectionModel().getSelectedItem();
        if (sel == null) { showStuStatus("Select a student first.", true); return; }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Remove " + sel.getName() + "?", ButtonType.OK, ButtonType.CANCEL);
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                try {
                    registry.remove(sel.getStudentId());
                    loadStudents();
                    refreshAllSubjectCombos();
                    refreshEnrollCombo();
                    showStuStatus("Removed: " + sel.getName(), false);
                } catch (IOException e) {
                    showStuStatus("Failed: " + e.getMessage(), true);
                }
            }
        });
    }

    @FXML
    private void handleStartCamera() {
        try {
            camera.start();
            startCamButton.setDisable(true);
            stopCamButton.setDisable(false);
            recognizeButton.setDisable(false);
            enrollButton.setDisable(false);
            showCamStatus("Camera ready.", false);

            cameraExecutor = Executors.newSingleThreadScheduledExecutor();
            cameraExecutor.scheduleAtFixedRate(this::processFrame, 0, 50, TimeUnit.MILLISECONDS);
        } catch (RuntimeException e) {
            showCamStatus("Camera error: " + e.getMessage(), true);
        }
    }

    @FXML
    private void handleStopCamera() {
        stopCamera();
        showCamStatus("Camera stopped.", false);
    }

    @FXML
    private void handleEnroll() {
        String selected = enrollStudentCombo.getValue();
        if (selected == null) { setEnrollStatus("Select a student to enroll.", true); return; }
        if (!camera.isOpen()) { setEnrollStatus("Start the camera first.",     true); return; }
        if (lastFaces.length == 0) { setEnrollStatus("No face detected. Look at the camera.", true); return; }

        String studentId = selected.replaceAll(".*\\((.*)\\)", "$1").trim();
        enrollingId = studentId;
        samplesCaptured.set(0);
        enrolling.set(true);
        enrollButton.setDisable(true);
        enrollProgress.setProgress(0);
        setEnrollStatus("Enrolling... hold still.", false);
    }

    @FXML
    private void handleRecognize() {
        String subject = recogSubjectCombo.getValue();
        if (subject == null || subject.isBlank()) {
            showCamStatus("Select a subject first.", true); return;
        }
        if (!faceRecognizer.isTrained()) {
            showCamStatus("No face model. Enroll students first.", true); return;
        }
        if (lastFaces.length == 0 || lastFrame == null) {
            showCamStatus("No face detected.", true); return;
        }

        Mat frame = lastFrame.clone();
        for (Rect face : lastFaces) {
            Mat gray    = new Mat();
            Imgproc.cvtColor(frame, gray, Imgproc.COLOR_BGR2GRAY);
            Mat faceRoi = new Mat(gray, face);

            String studentId = faceRecognizer.predict(faceRoi);
            faceRoi.release();
            gray.release();

            if (studentId == null) {
                showCamStatus("Face not recognized. Try re-enrolling.", true);
                continue;
            }

            long now  = System.currentTimeMillis();
            Long last = lastLoggedTime.get(studentId);
            if (last != null && now - last < COOLDOWN_MS) continue;
            lastLoggedTime.put(studentId, now);

            Student student = registry.getById(studentId);
            String  name    = student != null ? student.getName() : studentId;

            try {
                AttendanceRecord rec = new AttendanceRecord(name, studentId, "PRESENT", subject);
                logger.save(rec);
                Platform.runLater(() -> {
                    allRecords.add(rec);
                    updateTotal();
                    recognizedLabel.setText("Recognized: " + name);
                    recognizedLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                    showCamStatus("Auto-logged: " + name + " PRESENT", false);
                });
            } catch (IOException e) {
                showCamStatus("Log error: " + e.getMessage(), true);
            }
        }
        frame.release();
    }

    private void processFrame() {
        Mat frame = new Mat();
        if (!camera.readFrame(frame) || frame.empty()) return;

        Rect[] faces = faceDetector.detect(frame);
        lastFaces = faces;
        lastFrame = frame.clone();

        if (enrolling.get() && faces.length > 0 && enrollingId != null) {
            int count = samplesCaptured.get();
            if (count < TOTAL_SAMPLES) {
                Mat gray    = new Mat();
                Imgproc.cvtColor(frame, gray, Imgproc.COLOR_BGR2GRAY);
                Mat faceRoi = new Mat(gray, faces[0]);
                try {
                    faceRecognizer.saveSample(faceRoi, enrollingId, count);
                    int newCount = samplesCaptured.incrementAndGet();
                    double progress = (double) newCount / TOTAL_SAMPLES;

                    Platform.runLater(() -> {
                        enrollProgress.setProgress(progress);
                        setEnrollStatus("Capturing... " + newCount + "/" + TOTAL_SAMPLES, false);
                    });

                    if (newCount >= TOTAL_SAMPLES) {
                        enrolling.set(false);
                        Platform.runLater(() -> setEnrollStatus("Training model...", false));
                        new Thread(() -> {
                            faceRecognizer.train();
                            Platform.runLater(() -> {
                                enrollButton.setDisable(false);
                                enrollProgress.setProgress(1.0);
                                setEnrollStatus("Enrolled! " + faceRecognizer.enrolledCount()
                                        + " student(s) in model.", false);
                            });
                        }).start();
                    }
                } catch (IOException e) {
                    enrolling.set(false);
                    Platform.runLater(() -> setEnrollStatus("Save error: " + e.getMessage(), true));
                }
                faceRoi.release();
                gray.release();
            }
        }

        if (faceRecognizer.isTrained() && faces.length > 0) {
            Mat gray = new Mat();
            Imgproc.cvtColor(frame, gray, Imgproc.COLOR_BGR2GRAY);
            for (Rect face : faces) {
                Mat faceRoi = new Mat(gray, face);
                String sid  = faceRecognizer.predict(faceRoi);
                faceRoi.release();
                if (sid != null) {
                    Student s   = registry.getById(sid);
                    String name = s != null ? s.getName() : sid;
                    Imgproc.putText(frame, name, new Point(face.x, face.y - 28),
                            Imgproc.FONT_HERSHEY_SIMPLEX, 0.6, new Scalar(0, 255, 255), 2);
                }
            }
            gray.release();
        }

        Image image     = matToImage(frame);
        int   faceCount = faces.length;
        frame.release();

        Platform.runLater(() -> {
            cameraView.setImage(image);
            faceCountLabel.setText("Faces: " + faceCount);
            faceCountLabel.setStyle(faceCount > 0
                    ? "-fx-text-fill: #27ae60; -fx-font-weight: bold;"
                    : "-fx-text-fill: #718096;");
        });
    }

    private Image matToImage(Mat frame) {
        Mat rgb = new Mat();
        Imgproc.cvtColor(frame, rgb, Imgproc.COLOR_BGR2RGB);
        int    w   = rgb.cols(), h = rgb.rows();
        int    ch  = (int) rgb.elemSize();
        byte[] buf = new byte[w * h * ch];
        rgb.get(0, 0, buf);
        rgb.release();
        WritableImage img = new WritableImage(w, h);
        img.getPixelWriter().setPixels(0, 0, w, h,
                PixelFormat.getByteRgbInstance(), buf, 0, w * ch);
        return img;
    }

    public void shutdown() { stopCamera(); }

    private void stopCamera() {
        if (cameraExecutor != null && !cameraExecutor.isShutdown()) {
            cameraExecutor.shutdown();
            try { cameraExecutor.awaitTermination(500, TimeUnit.MILLISECONDS); }
            catch (InterruptedException ignored) {}
        }
        camera.stop();
        Platform.runLater(() -> {
            startCamButton.setDisable(false);
            stopCamButton.setDisable(true);
            recognizeButton.setDisable(true);
            cameraView.setImage(null);
        });
    }

    private void loadAttendanceRecords() {
        try {
            allRecords.setAll(logger.loadAll());
            refreshAllSubjectCombos();
            updateTotal();
        } catch (IOException e) {
            showStatus("Load error: " + e.getMessage(), true);
        }
    }

    private void loadStudents() {
        studentData.setAll(registry.getAll());
    }

    private void refreshAllSubjectCombos() {
        ArrayList<String> subjects = new ArrayList<>();
        subjects.add("All Subjects");
        for (Student s : registry.getAll()) {
            for (String sub : s.getSubjects()) {
                if (!subjects.contains(sub)) subjects.add(sub);
            }
        }
        String cur = subjectCombo.getValue();
        subjectCombo.setItems(FXCollections.observableArrayList(subjects));
        subjectCombo.setValue(subjects.contains(cur) ? cur : "All Subjects");
        refreshRecogSubjectCombo();
    }

    private void refreshRecogSubjectCombo() {
        ArrayList<String> subjects = new ArrayList<>();
        for (Student s : registry.getAll()) {
            for (String sub : s.getSubjects()) {
                if (!subjects.contains(sub)) subjects.add(sub);
            }
        }
        String cur = recogSubjectCombo.getValue();
        recogSubjectCombo.setItems(FXCollections.observableArrayList(subjects));
        if (subjects.contains(cur)) recogSubjectCombo.setValue(cur);
    }

    private void refreshEnrollCombo() {
        ArrayList<String> items = new ArrayList<>();
        for (Student s : registry.getAll()) {
            boolean enrolled = faceRecognizer.isEnrolled(s.getStudentId());
            items.add(s.getName() + " (" + s.getStudentId() + ")"
                    + (enrolled ? " [enrolled]" : ""));
        }
        enrollStudentCombo.setItems(FXCollections.observableArrayList(items));
    }

    private void clearAttendanceFields() {
        nameField.clear();
        idField.clear();
        presentRadio.setSelected(true);
        nameField.requestFocus();
    }

    private void clearStudentFields() {
        stuNameField.clear();
        stuIdField.clear();
        stuSubjectField.clear();
    }

    private void showStatus(String msg, boolean isError) {
        statusLabel.setText(msg);
        statusLabel.setStyle(isError ? "-fx-text-fill: #e74c3c;" : "-fx-text-fill: #27ae60;");
    }

    private void showStuStatus(String msg, boolean isError) {
        stuStatusLabel.setText(msg);
        stuStatusLabel.setStyle(isError ? "-fx-text-fill: #e74c3c;" : "-fx-text-fill: #27ae60;");
    }

    private void showCamStatus(String msg, boolean isError) {
        Platform.runLater(() -> {
            camStatusLabel.setText(msg);
            camStatusLabel.setStyle(isError ? "-fx-text-fill: #e74c3c;" : "-fx-text-fill: #27ae60;");
        });
    }

    private void setEnrollStatus(String msg, boolean isError) {
        Platform.runLater(() -> {
            enrollStatusLabel.setText(msg);
            enrollStatusLabel.setStyle(isError ? "-fx-text-fill: #e74c3c;" : "-fx-text-fill: #27ae60;");
        });
    }

    private void updateTotal() {
        long present = 0;
        for (AttendanceRecord r : allRecords) {
            if ("PRESENT".equals(r.getStatus())) present++;
        }
        totalLabel.setText("Showing: " + allRecords.size()
                + "  |  Present: " + present
                + "  |  Absent: " + (allRecords.size() - present));
    }
}
