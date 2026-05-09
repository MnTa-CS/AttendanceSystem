package attendance;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles all file I/O for attendance records.
 * Demonstrates: I/O Handling, Error Handling, modularity.
 */
public class AttendanceLogger {

    private static final String DATA_DIR  = "data";
    private static final String FILE_NAME = "attendance.csv";
    private static final String CSV_HEADER = "Name,Student ID,Status,Timestamp";

    private final Path filePath;

    public AttendanceLogger() {
        this.filePath = Paths.get(DATA_DIR, FILE_NAME);
        initFile();
    }

    /**
     * Creates the data directory and CSV file with header if they don't exist.
     */
    private void initFile() {
        try {
            Files.createDirectories(filePath.getParent());
            if (!Files.exists(filePath)) {
                Files.writeString(filePath, CSV_HEADER + System.lineSeparator());
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize attendance file: " + e.getMessage(), e);
        }
    }

    /**
     * Appends a new attendance record to the CSV file.
     *
     * @param record the record to save
     * @throws IOException if writing fails
     */
    public void save(AttendanceRecord record) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(
                filePath, StandardOpenOption.APPEND)) {
            writer.write(record.toCsvLine());
            writer.newLine();
        }
    }

    /**
     * Loads all attendance records from the CSV file.
     *
     * @return list of all records (skips header and malformed lines)
     * @throws IOException if reading fails
     */
    public List<AttendanceRecord> loadAll() throws IOException {
        List<AttendanceRecord> records = new ArrayList<>();
        List<String> lines = Files.readAllLines(filePath);

        for (int i = 1; i < lines.size(); i++) { // skip header
            String line = lines.get(i).trim();
            if (line.isEmpty()) continue;
            try {
                records.add(AttendanceRecord.fromCsvLine(line));
            } catch (IllegalArgumentException e) {
                System.err.println("Skipping malformed line " + (i + 1) + ": " + e.getMessage());
            }
        }
        return records;
    }

    /**
     * Clears all records (resets file to header only).
     *
     * @throws IOException if writing fails
     */
    public void clearAll() throws IOException {
        Files.writeString(filePath, CSV_HEADER + System.lineSeparator());
    }

    public Path getFilePath() {
        return filePath.toAbsolutePath();
    }
}
