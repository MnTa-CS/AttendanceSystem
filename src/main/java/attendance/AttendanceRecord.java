package attendance;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Represents a single attendance record.
 * Demonstrates OOP encapsulation and proper use of Java Collections.
 */
public class AttendanceRecord {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String studentName;
    private final String studentId;
    private final LocalDateTime timestamp;
    private final String status; // "PRESENT" or "ABSENT"

    public AttendanceRecord(String studentName, String studentId, String status) {
        if (studentName == null || studentName.isBlank()) {
            throw new IllegalArgumentException("Student name cannot be empty.");
        }
        if (studentId == null || studentId.isBlank()) {
            throw new IllegalArgumentException("Student ID cannot be empty.");
        }
        this.studentName = studentName.trim();
        this.studentId = studentId.trim().toUpperCase();
        this.status = status;
        this.timestamp = LocalDateTime.now();
    }

    // Constructor for loading from CSV
    public AttendanceRecord(String studentName, String studentId, String status, LocalDateTime timestamp) {
        this.studentName = studentName;
        this.studentId = studentId;
        this.status = status;
        this.timestamp = timestamp;
    }

    public String getStudentName() { return studentName; }
    public String getStudentId()   { return studentId; }
    public String getStatus()      { return status; }
    public LocalDateTime getTimestamp() { return timestamp; }

    public String getFormattedTimestamp() {
        return timestamp.format(FORMATTER);
    }

    /**
     * Serialize to CSV line format.
     */
    public String toCsvLine() {
        return String.join(",", studentName, studentId, status, timestamp.format(FORMATTER));
    }

    /**
     * Deserialize from CSV line format.
     */
    public static AttendanceRecord fromCsvLine(String line) {
        String[] parts = line.split(",", 4);
        if (parts.length != 4) {
            throw new IllegalArgumentException("Invalid CSV line: " + line);
        }
        LocalDateTime ts = LocalDateTime.parse(parts[3].trim(), FORMATTER);
        return new AttendanceRecord(parts[0].trim(), parts[1].trim(), parts[2].trim(), ts);
    }

    @Override
    public String toString() {
        return String.format("[%s] %s (%s) - %s", status, studentName, studentId, getFormattedTimestamp());
    }
}
