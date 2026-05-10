package attendance;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Manages the student roster.
 * Demonstrates: Java Collections (HashMap), File I/O, modularity.
 */
public class StudentRegistry {

    private static final String DATA_DIR  = "data";
    private static final String FILE_NAME = "students.csv";
    private static final String CSV_HEADER = "StudentID,Name,Subjects";

    private final Path filePath;
    private final Map<String, Student> students; // key = studentId

    public StudentRegistry() {
        this.filePath = Paths.get(DATA_DIR, FILE_NAME);
        this.students = new LinkedHashMap<>();
        initFile();
        loadAll();
    }

    private void initFile() {
        try {
            Files.createDirectories(filePath.getParent());
            if (!Files.exists(filePath)) {
                Files.writeString(filePath, CSV_HEADER + System.lineSeparator());
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize students file: " + e.getMessage(), e);
        }
    }

    private void loadAll() {
        try {
            List<String> lines = Files.readAllLines(filePath);
            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.isEmpty()) continue;
                try {
                    Student s = Student.fromCsvLine(line);
                    students.put(s.getStudentId(), s);
                } catch (IllegalArgumentException e) {
                    System.err.println("Skipping malformed student line " + (i + 1) + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Could not load students: " + e.getMessage());
        }
    }

    /** Add or update a student, then persist. */
    public void save(Student student) throws IOException {
        students.put(student.getStudentId(), student);
        persist();
    }

    /** Remove a student by ID, then persist. */
    public void remove(String studentId) throws IOException {
        students.remove(studentId.toUpperCase());
        persist();
    }

    /** Rewrite the entire file from the in-memory map. */
    private void persist() throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(filePath)) {
            w.write(CSV_HEADER);
            w.newLine();
            for (Student s : students.values()) {
                w.write(s.toCsvLine());
                w.newLine();
            }
        }
    }

    public Student getById(String studentId) {
        return students.get(studentId == null ? null : studentId.toUpperCase());
    }

    public boolean exists(String studentId) {
        return students.containsKey(studentId == null ? null : studentId.toUpperCase());
    }

    public Collection<Student> getAll() {
        return Collections.unmodifiableCollection(students.values());
    }

    public int size() { return students.size(); }
}
