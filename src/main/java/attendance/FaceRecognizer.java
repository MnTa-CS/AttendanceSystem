package attendance;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Face recognizer using histogram comparison (core OpenCV only).
 * Saves grayscale face samples per student, compares using correlation.
 *
 * Demonstrates: file I/O, Java Collections (HashMap, ArrayList),
 * external library usage, OOP encapsulation.
 */
public class FaceRecognizer {

    private static final String DATA_DIR   = "data/faces";
    private static final String LABEL_FILE = "data/face_labels.csv";

    // Correlation threshold: 0.0 = no match, 1.0 = perfect match
    // Values above this are considered a match
    private static final double MATCH_THRESHOLD = 0.82;

    // Maps studentId -> list of stored histogram Mats
    private final Map<String, List<Mat>> faceHistograms;
    private boolean trained = false;

    public FaceRecognizer() {
        faceHistograms = new HashMap<>();
        try {
            Files.createDirectories(Paths.get(DATA_DIR));
            Files.createDirectories(Paths.get("data"));
        } catch (IOException e) {
            System.err.println("Could not create face directories: " + e.getMessage());
        }
        loadSamples();
    }

    // ── Enrollment ────────────────────────────────────────────────────────────

    /**
     * Saves a grayscale face sample for a student.
     * Call multiple times (e.g. 10x) to collect training samples.
     */
    public void saveSample(Mat face, String studentId, int sampleIndex) throws IOException {
        Path dir = Paths.get(DATA_DIR, studentId);
        Files.createDirectories(dir);

        Mat resized = new Mat();
        Imgproc.resize(face, resized, new Size(100, 100));

        Path out = dir.resolve("sample_" + sampleIndex + ".png");
        Imgcodecs.imwrite(out.toString(), resized);
        resized.release();
    }

    /**
     * Loads all saved samples from disk and builds histograms.
     * Call after finishing enrollment.
     */
    public void train() {
        faceHistograms.clear();

        File dataDir = new File(DATA_DIR);
        if (!dataDir.exists()) return;

        File[] studentDirs = dataDir.listFiles(File::isDirectory);
        if (studentDirs == null) return;

        for (File studentDir : studentDirs) {
            String studentId = studentDir.getName();
            List<Mat> histograms = new ArrayList<>();

            File[] samples = studentDir.listFiles(f -> f.getName().endsWith(".png"));
            if (samples == null) continue;

            for (File sample : samples) {
                Mat img = Imgcodecs.imread(sample.getAbsolutePath(), Imgcodecs.IMREAD_GRAYSCALE);
                if (img.empty()) continue;
                Mat hist = computeHistogram(img);
                histograms.add(hist);
                img.release();
            }

            if (!histograms.isEmpty()) {
                faceHistograms.put(studentId, histograms);
            }
        }

        trained = !faceHistograms.isEmpty();
        saveLabelFile();
    }

    // ── Recognition ──────────────────────────────────────────────────────────

    /**
     * Predicts who the face belongs to by comparing histograms.
     *
     * @param face grayscale face Mat
     * @return studentId if recognized, null if unknown
     */
    public String predict(Mat face) {
        if (!trained || face.empty()) return null;

        Mat resized = new Mat();
        Imgproc.resize(face, resized, new Size(100, 100));
        Mat queryHist = computeHistogram(resized);
        resized.release();

        String bestMatch    = null;
        double bestScore    = -1.0;

        for (Map.Entry<String, List<Mat>> entry : faceHistograms.entrySet()) {
            String studentId = entry.getKey();
            double avgScore  = 0.0;

            for (Mat storedHist : entry.getValue()) {
                // HISTCMP_CORREL: higher = better match (range -1 to 1)
                avgScore += Imgproc.compareHist(queryHist, storedHist, Imgproc.HISTCMP_CORREL);
            }
            avgScore /= entry.getValue().size();

            if (avgScore > bestScore) {
                bestScore = avgScore;
                bestMatch = studentId;
            }
        }

        queryHist.release();

        if (bestScore >= MATCH_THRESHOLD) return bestMatch;
        return null; // not recognized
    }

    // ── Histogram Helper ─────────────────────────────────────────────────────

    private Mat computeHistogram(Mat grayFace) {
        Mat hist     = new Mat();
        MatOfFloat ranges    = new MatOfFloat(0f, 256f);
        MatOfInt   histSize  = new MatOfInt(256);
        MatOfInt   channels  = new MatOfInt(0);

        List<Mat> images = Collections.singletonList(grayFace);
        Imgproc.calcHist(images, channels, new Mat(), hist, histSize, ranges);
        Core.normalize(hist, hist, 0, 1, Core.NORM_MINMAX);
        return hist;
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    /** Reloads all face samples from disk (called on startup). */
    private void loadSamples() {
        train(); // train() already reads from disk
    }

    private void saveLabelFile() {
        try (BufferedWriter w = Files.newBufferedWriter(Paths.get(LABEL_FILE))) {
            for (String id : faceHistograms.keySet()) {
                w.write(id);
                w.newLine();
            }
        } catch (IOException e) {
            System.err.println("Could not save label file: " + e.getMessage());
        }
    }

    /** Deletes all saved samples for a student so they can re-enroll. */
    public void deleteSamples(String studentId) throws IOException {
        Path dir = Paths.get(DATA_DIR, studentId);
        if (Files.exists(dir)) {
            Files.walk(dir)
                 .sorted(Comparator.reverseOrder())
                 .map(Path::toFile)
                 .forEach(File::delete);
        }
        faceHistograms.remove(studentId);
    }

    public boolean isTrained()           { return trained; }
    public boolean isEnrolled(String id) { return faceHistograms.containsKey(id); }
    public int     enrolledCount()       { return faceHistograms.size(); }
}
