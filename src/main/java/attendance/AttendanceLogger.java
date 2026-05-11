package attendance;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;

public class AttendanceLogger {

    private static final String FILE_PATH = "data/attendance.csv";
    private static final String HEADER    = "Name,StudentID,Status,Subject,Timestamp";

    public AttendanceLogger() {

        try {
            Files.createDirectories(Paths.get("data"));
            if (!Files.exists(Paths.get(FILE_PATH))) {
                Files.writeString(Paths.get(FILE_PATH), HEADER + System.lineSeparator());
            }
        } catch (IOException e) {
            System.out.println("Could not create attendance file: " + e.getMessage());
        }
    }

    public void save(AttendanceRecord record) throws IOException {
        BufferedWriter writer = Files.newBufferedWriter(
                Paths.get(FILE_PATH), StandardOpenOption.APPEND);
        writer.write(record.toCsvLine());
        writer.newLine();
        writer.close();
    }

    public ArrayList<AttendanceRecord> loadAll() throws IOException {
        ArrayList<AttendanceRecord> records = new ArrayList<AttendanceRecord>();
        java.util.List<String> lines = Files.readAllLines(Paths.get(FILE_PATH));

        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (!line.isEmpty()) {
                try {
                    records.add(AttendanceRecord.fromCsvLine(line));
                } catch (Exception e) {
                    System.out.println("Skipping bad line " + (i + 1) + ": " + e.getMessage());
                }
            }
        }
        return records;
    }

    public void clearAll() throws IOException {
        Files.writeString(Paths.get(FILE_PATH), HEADER + System.lineSeparator());
    }

    public String getFilePath() {
        return Paths.get(FILE_PATH).toAbsolutePath().toString();
    }
}
