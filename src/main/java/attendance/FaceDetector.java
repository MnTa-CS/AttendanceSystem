package attendance;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.*;
import java.nio.file.*;

/**
 * Detects faces in a frame using OpenCV Haar Cascade classifier.
 * Demonstrates: external library usage, file I/O, OOP single-responsibility.
 */
public class FaceDetector {

    private final CascadeClassifier classifier;
    private boolean loaded = false;

    // The Haar cascade XML is extracted from the OpenCV jar resources
    private static final String CASCADE_RESOURCE = "/haarcascade_frontalface_default.xml";
    private static final String CASCADE_TEMP      = "data/haarcascade_frontalface_default.xml";

    public FaceDetector() {
        classifier = new CascadeClassifier();
        loadCascade();
    }

    /**
     * Extracts the Haar cascade XML from the classpath to a temp file,
     * then loads it into the classifier.
     */
    private void loadCascade() {
        try {
            // Try loading from data/ directory first (user may have placed it there)
            Path localPath = Paths.get(CASCADE_TEMP);
            if (!Files.exists(localPath)) {
                Files.createDirectories(localPath.getParent());
                // Extract from classpath (bundled in resources)
                InputStream in = getClass().getResourceAsStream(CASCADE_RESOURCE);
                if (in == null) {
                    System.err.println("Haar cascade not found in resources. " +
                        "Place haarcascade_frontalface_default.xml in the data/ folder.");
                    return;
                }
                Files.copy(in, localPath);
                in.close();
            }
            loaded = classifier.load(localPath.toAbsolutePath().toString());
            if (!loaded) {
                System.err.println("Failed to load Haar cascade from: " + localPath.toAbsolutePath());
            }
        } catch (IOException e) {
            System.err.println("Error loading cascade: " + e.getMessage());
        }
    }

    /**
     * Detects faces in the given frame.
     * Draws green rectangles around detected faces.
     *
     * @param frame the input/output frame (modified in place with rectangles)
     * @return array of detected face rectangles
     */
    public Rect[] detect(Mat frame) {
        if (!loaded || frame.empty()) return new Rect[0];

        Mat gray = new Mat();
        Imgproc.cvtColor(frame, gray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.equalizeHist(gray, gray);

        MatOfRect faces = new MatOfRect();
        classifier.detectMultiScale(
            gray, faces,
            1.1,   // scaleFactor
            4,     // minNeighbors
            0,
            new Size(80, 80),   // minSize
            new Size()          // maxSize (unlimited)
        );

        Rect[] faceArray = faces.toArray();

        // Draw rectangles on original frame
        for (Rect rect : faceArray) {
            Imgproc.rectangle(frame, rect,
                new Scalar(0, 220, 0), 2); // green
            Imgproc.putText(frame, "Face detected",
                new Point(rect.x, rect.y - 8),
                Imgproc.FONT_HERSHEY_SIMPLEX,
                0.55, new Scalar(0, 220, 0), 2);
        }

        gray.release();
        return faceArray;
    }

    public boolean isLoaded() { return loaded; }
}
