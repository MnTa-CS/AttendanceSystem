package attendance;

import java.util.ArrayList;

public class Student {

    private String studentId;
    private String name;
    private ArrayList<String> subjects;

    public Student(String studentId, String name) {
        this.studentId = studentId.trim().toUpperCase();
        this.name = name.trim();
        this.subjects = new ArrayList<String>();
    }

    public String getStudentId() { return studentId; }
    public String getName()      { return name; }
    public void   setName(String name) { this.name = name.trim(); }

    public ArrayList<String> getSubjects() { return subjects; }

    public void addSubject(String subject) {
        if (!subjects.contains(subject.trim())) {
            subjects.add(subject.trim());
        }
    }

    public String getSubjectsDisplay() {
        if (subjects.isEmpty()) return "-";
        return String.join(", ", subjects);
    }

    public String toCsvLine() {
        return studentId + "," + name + "," + String.join(";", subjects);
    }

    public static Student fromCsvLine(String line) {
        String[] parts = line.split(",", 3);
        Student s = new Student(parts[0], parts[1]);
        if (parts.length == 3 && !parts[2].isBlank()) {
            for (String sub : parts[2].split(";")) {
                s.addSubject(sub);
            }
        }
        return s;
    }

    @Override
    public String toString() {
        return name + " (" + studentId + ")";
    }
}
