package attendance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a student enrolled in one or more subjects.
 * Demonstrates: OOP encapsulation, composition, Java Collections.
 */
public class Student {

    private final String studentId;
    private String name;
    private final List<String> subjects;

    public Student(String studentId, String name) {
        if (studentId == null || studentId.isBlank())
            throw new IllegalArgumentException("Student ID cannot be empty.");
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("Student name cannot be empty.");

        this.studentId = studentId.trim().toUpperCase();
        this.name      = name.trim();
        this.subjects  = new ArrayList<>();
    }

    /** Enroll the student in a subject (no duplicates). */
    public void addSubject(String subject) {
        if (subject == null || subject.isBlank())
            throw new IllegalArgumentException("Subject cannot be empty.");
        String s = subject.trim();
        if (!subjects.contains(s)) {
            subjects.add(s);
        }
    }

    /** Remove a subject. */
    public void removeSubject(String subject) {
        subjects.remove(subject.trim());
    }

    // --- Getters ---
    public String getStudentId() { return studentId; }
    public String getName()      { return name; }
    public void   setName(String name) { this.name = name.trim(); }

    /** Returns an unmodifiable view of the subjects list. */
    public List<String> getSubjects() {
        return Collections.unmodifiableList(subjects);
    }

    /** Subjects as a comma-separated string for display. */
    public String getSubjectsDisplay() {
        return subjects.isEmpty() ? "—" : String.join(", ", subjects);
    }

    /** Serialize to CSV: id,name,subject1;subject2;subject3 */
    public String toCsvLine() {
        return studentId + "," + name + "," + String.join(";", subjects);
    }

    /** Deserialize from CSV line. */
    public static Student fromCsvLine(String line) {
        String[] parts = line.split(",", 3);
        if (parts.length < 2)
            throw new IllegalArgumentException("Invalid student CSV line: " + line);
        Student s = new Student(parts[0].trim(), parts[1].trim());
        if (parts.length == 3 && !parts[2].isBlank()) {
            for (String subj : parts[2].split(";")) {
                if (!subj.isBlank()) s.addSubject(subj.trim());
            }
        }
        return s;
    }

    @Override
    public String toString() {
        return name + " (" + studentId + ")";
    }
}
