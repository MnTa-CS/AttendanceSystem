package attendance;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;

public class StudentRegistry {

    private static final String FILE_PATH = "data/students.csv";
    private static final String HEADER    = "StudentID,Name,Subjects";

    private ArrayList<Student> students;

    public StudentRegistry() {
        students = new ArrayList<Student>();
        try {
            Files.createDirectories(Paths.get("data"));
            if (!Files.exists(Paths.get(FILE_PATH))) {
                Files.writeString(Paths.get(FILE_PATH), HEADER + System.lineSeparator());
            }
            loadFromFile();
        } catch (IOException e) {
            System.out.println("Could not load students: " + e.getMessage());
        }
    }

    public void save(Student student) throws IOException {

        for (int i = 0; i < students.size(); i++) {
            if (students.get(i).getStudentId().equals(student.getStudentId())) {
                students.remove(i);
                break;
            }
        }
        students.add(student);
        writeToFile();
    }

    public void remove(String studentId) throws IOException {
        for (int i = 0; i < students.size(); i++) {
            if (students.get(i).getStudentId().equals(studentId.toUpperCase())) {
                students.remove(i);
                break;
            }
        }
        writeToFile();
    }

    public Student getById(String studentId) {
        for (Student s : students) {
            if (s.getStudentId().equals(studentId.toUpperCase())) {
                return s;
            }
        }
        return null;
    }

    public boolean exists(String studentId) {
        return getById(studentId) != null;
    }

    public ArrayList<Student> getAll() {
        return students;
    }

    private void loadFromFile() throws IOException {
        java.util.List<String> lines = Files.readAllLines(Paths.get(FILE_PATH));
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (!line.isEmpty()) {
                try {
                    students.add(Student.fromCsvLine(line));
                } catch (Exception e) {
                    System.out.println("Skipping bad student line: " + e.getMessage());
                }
            }
        }
    }

    private void writeToFile() throws IOException {
        BufferedWriter writer = Files.newBufferedWriter(Paths.get(FILE_PATH));
        writer.write(HEADER);
        writer.newLine();
        for (Student s : students) {
            writer.write(s.toCsvLine());
            writer.newLine();
        }
        writer.close();
    }
}
