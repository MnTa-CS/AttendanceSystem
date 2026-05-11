package attendance;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AttendanceRecord {

    private static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private String        studentName;
    private String        studentId;
    private String        status;
    private String        subject;
    private LocalDateTime timestamp;

    public AttendanceRecord(String studentName, String studentId,
                            String status, String subject) {
        this.studentName = studentName;
        this.studentId   = studentId.toUpperCase();
        this.status      = status;
        this.subject     = subject;
        this.timestamp   = LocalDateTime.now();
    }

    public AttendanceRecord(String studentName, String studentId,
                            String status, String subject, LocalDateTime timestamp) {
        this.studentName = studentName;
        this.studentId   = studentId;
        this.status      = status;
        this.subject     = subject;
        this.timestamp   = timestamp;
    }

    public String getStudentName()        { return studentName; }
    public String getStudentId()          { return studentId; }
    public String getStatus()             { return status; }
    public String getSubject()            { return subject; }
    public String getFormattedTimestamp() { return timestamp.format(FORMAT); }

    public String toCsvLine() {
        return studentName + "," + studentId + "," + status + ","
                + subject + "," + timestamp.format(FORMAT);
    }

    public static AttendanceRecord fromCsvLine(String line) {
        String[] p = line.split(",", 5);
        LocalDateTime ts = LocalDateTime.parse(p[4].trim(), FORMAT);
        return new AttendanceRecord(p[0].trim(), p[1].trim(), p[2].trim(), p[3].trim(), ts);
    }
}
