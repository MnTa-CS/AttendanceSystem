package attendance;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashMap;

public class FaceRecognizer {

    private static final String FACES_DIR  = "data/faces";
    private static final double THRESHOLD  = 0.82;

    private HashMap<String, ArrayList<Mat>> faceHistograms;
    private boolean trained;

    public FaceRecognizer() {
        faceHistograms = new HashMap<String, ArrayList<Mat>>();
        trained = false;
        try {
            Files.createDirectories(Paths.get(FACES_DIR));
        } catch (IOException e) {
            System.out.println("Could not create faces folder: " + e.getMessage());
        }
        train();
    }

    public void saveSample(Mat face, String studentId, int sampleIndex) throws IOException {
        Path dir = Paths.get(FACES_DIR, studentId);
        Files.createDirectories(dir);

        Mat resized = new Mat();
        Imgproc.resize(face, resized, new Size(100, 100));
        Imgcodecs.imwrite(dir.resolve("sample_" + sampleIndex + ".png").toString(), resized);
        resized.release();
    }

    public void train() {
        faceHistograms.clear();

        File dataDir = new File(FACES_DIR);
        if (!dataDir.exists()) return;

        File[] studentDirs = dataDir.listFiles(File::isDirectory);
        if (studentDirs == null) return;

        for (File studentDir : studentDirs) {
            String studentId = studentDir.getName();
            ArrayList<Mat> histograms = new ArrayList<Mat>();

            File[] samples = studentDir.listFiles(f -> f.getName().endsWith(".png"));
            if (samples == null) continue;

            for (File sample : samples) {
                Mat img = Imgcodecs.imread(sample.getAbsolutePath(), Imgcodecs.IMREAD_GRAYSCALE);
                if (!img.empty()) {
                    histograms.add(computeHistogram(img));
                    img.release();
                }
            }

            if (!histograms.isEmpty()) {
                faceHistograms.put(studentId, histograms);
            }
        }

        trained = !faceHistograms.isEmpty();
    }

    public String predict(Mat face) {
        if (!trained || face.empty()) return null;

        Mat resized = new Mat();
        Imgproc.resize(face, resized, new Size(100, 100));
        Mat queryHist = computeHistogram(resized);
        resized.release();

        String bestMatch = null;
        double bestScore = -1.0;

        for (String studentId : faceHistograms.keySet()) {
            ArrayList<Mat> histograms = faceHistograms.get(studentId);
            double total = 0;
            for (Mat stored : histograms) {
                total += Imgproc.compareHist(queryHist, stored, Imgproc.HISTCMP_CORREL);
            }
            double avgScore = total / histograms.size();

            if (avgScore > bestScore) {
                bestScore = avgScore;
                bestMatch = studentId;
            }
        }

        queryHist.release();

        if (bestScore >= THRESHOLD) return bestMatch;
        return null;
    }

    private Mat computeHistogram(Mat grayFace) {
        Mat hist      = new Mat();
        MatOfFloat ranges   = new MatOfFloat(0f, 256f);
        MatOfInt   histSize = new MatOfInt(256);
        MatOfInt   channels = new MatOfInt(0);
        java.util.List<Mat> images = java.util.Collections.singletonList(grayFace);
        Imgproc.calcHist(images, channels, new Mat(), hist, histSize, ranges);
        Core.normalize(hist, hist, 0, 1, Core.NORM_MINMAX);
        return hist;
    }

    public boolean isTrained()           { return trained; }
    public boolean isEnrolled(String id) { return faceHistograms.containsKey(id); }
    public int     enrolledCount()       { return faceHistograms.size(); }
}
