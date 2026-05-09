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

    private final String        studentName;
    private final String        studentId;
    private final String        status;
    private final String        subject;
    private final LocalDateTime timestamp;

    public AttendanceRecord(String studentName, String studentId, String status, String subject) {
        if (studentName == null || studentName.isBlank())
            throw new IllegalArgumentException("Student name cannot be empty.");
        if (studentId == null || studentId.isBlank())
            throw new IllegalArgumentException("Student ID cannot be empty.");
        this.studentName = studentName.trim();
        this.studentId   = studentId.trim().toUpperCase();
        this.status      = status;
        this.subject     = subject == null ? "" : subject.trim();
        this.timestamp   = LocalDateTime.now();
    }

    // Constructor for loading from CSV
    public AttendanceRecord(String studentName, String studentId,
                            String status, String subject, LocalDateTime timestamp) {
        this.studentName = studentName;
        this.studentId   = studentId;
        this.status      = status;
        this.subject     = subject == null ? "" : subject;
        this.timestamp   = timestamp;
    }

    public String        getStudentName()        { return studentName; }
    public String        getStudentId()          { return studentId; }
    public String        getStatus()             { return status; }
    public String        getSubject()            { return subject; }
    public LocalDateTime getTimestamp()          { return timestamp; }
    public String        getFormattedTimestamp() { return timestamp.format(FORMATTER); }

    /** Serialize to CSV: name,id,status,subject,timestamp */
    public String toCsvLine() {
        return String.join(",", studentName, studentId, status,
                subject.replace(",", ";"), timestamp.format(FORMATTER));
    }

    /** Deserialize from CSV line. */
    public static AttendanceRecord fromCsvLine(String line) {
        String[] parts = line.split(",", 5);
        if (parts.length < 4)
            throw new IllegalArgumentException("Invalid CSV line: " + line);
        String subj = parts.length == 5 ? parts[3].trim() : "";
        String tsStr = parts.length == 5 ? parts[4].trim() : parts[3].trim();
        LocalDateTime ts = LocalDateTime.parse(tsStr, FORMATTER);
        return new AttendanceRecord(parts[0].trim(), parts[1].trim(),
                parts[2].trim(), subj, ts);
    }

    @Override
    public String toString() {
        return String.format("[%s] %s (%s) %s - %s",
                status, studentName, studentId, subject, getFormattedTimestamp());
    }
}
