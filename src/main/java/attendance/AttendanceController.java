package attendance;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.paint.Color;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * JavaFX Controller for the Attendance System UI.
 * Demonstrates: GUI handling, error handling, OOP usage.
 */
public class AttendanceController implements Initializable {

    // --- Input fields ---
    @FXML private TextField nameField;
    @FXML private TextField idField;
    @FXML private ToggleGroup statusGroup;
    @FXML private RadioButton presentRadio;
    @FXML private RadioButton absentRadio;

    // --- Buttons ---
    @FXML private Button logButton;
    @FXML private Button clearButton;
    @FXML private Button refreshButton;

    // --- Table ---
    @FXML private TableView<AttendanceRecord> attendanceTable;
    @FXML private TableColumn<AttendanceRecord, String> nameColumn;
    @FXML private TableColumn<AttendanceRecord, String> idColumn;
    @FXML private TableColumn<AttendanceRecord, String> statusColumn;
    @FXML private TableColumn<AttendanceRecord, String> timestampColumn;

    // --- Status label ---
    @FXML private Label statusLabel;
    @FXML private Label filePathLabel;
    @FXML private Label totalLabel;

    // --- Data ---
    private AttendanceLogger logger;
    private ObservableList<AttendanceRecord> tableData;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        logger    = new AttendanceLogger();
        tableData = FXCollections.observableArrayList();

        setupTable();
        loadRecords();

        filePathLabel.setText("Saving to: " + logger.getFilePath());
        presentRadio.setSelected(true);
    }

    // -------------------------------------------------------------------------
    // Table Setup
    // -------------------------------------------------------------------------

    private void setupTable() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("studentName"));
        idColumn.setCellValueFactory(new PropertyValueFactory<>("studentId"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        timestampColumn.setCellValueFactory(new PropertyValueFactory<>("formattedTimestamp"));

        // Color-code status column
        statusColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status);
                    if ("PRESENT".equals(status)) {
                        setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    }
                }
            }
        });

        attendanceTable.setItems(tableData);
    }

    // -------------------------------------------------------------------------
    // Button Handlers
    // -------------------------------------------------------------------------

    @FXML
    private void handleLogAttendance() {
        String name = nameField.getText();
        String id   = idField.getText();
        String status = presentRadio.isSelected() ? "PRESENT" : "ABSENT";

        // Input validation
        if (name == null || name.isBlank()) {
            showStatus("Please enter a student name.", true);
            nameField.requestFocus();
            return;
        }
        if (id == null || id.isBlank()) {
            showStatus("Please enter a student ID.", true);
            idField.requestFocus();
            return;
        }

        try {
            AttendanceRecord record = new AttendanceRecord(name, id, status);
            logger.save(record);
            tableData.add(record);
            updateTotal();
            clearInputs();
            showStatus("✓ Logged: " + record.getStudentName() + " — " + status, false);
        } catch (IllegalArgumentException e) {
            showStatus("Input error: " + e.getMessage(), true);
        } catch (IOException e) {
            showStatus("File error: " + e.getMessage(), true);
        }
    }

    @FXML
    private void handleClear() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Clear All Records");
        confirm.setHeaderText("This will delete all attendance records.");
        confirm.setContentText("Are you sure?");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    logger.clearAll();
                    tableData.clear();
                    updateTotal();
                    showStatus("All records cleared.", false);
                } catch (IOException e) {
                    showStatus("Failed to clear records: " + e.getMessage(), true);
                }
            }
        });
    }

    @FXML
    private void handleRefresh() {
        loadRecords();
        showStatus("Records refreshed.", false);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void loadRecords() {
        try {
            List<AttendanceRecord> records = logger.loadAll();
            tableData.setAll(records);
            updateTotal();
        } catch (IOException e) {
            showStatus("Failed to load records: " + e.getMessage(), true);
        }
    }

    private void clearInputs() {
        nameField.clear();
        idField.clear();
        presentRadio.setSelected(true);
        nameField.requestFocus();
    }

    private void showStatus(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.setStyle(isError
                ? "-fx-text-fill: #e74c3c;"
                : "-fx-text-fill: #27ae60;");
    }

    private void updateTotal() {
        long present = tableData.stream().filter(r -> "PRESENT".equals(r.getStatus())).count();
        long absent  = tableData.stream().filter(r -> "ABSENT".equals(r.getStatus())).count();
        totalLabel.setText("Total: " + tableData.size() + "  |  Present: " + present + "  |  Absent: " + absent);
    }
}
