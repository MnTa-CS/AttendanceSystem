package attendance;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.*;
import java.nio.file.*;

public class FaceDetector {

    private CascadeClassifier classifier;
    private boolean loaded;

    private static final String CASCADE_FILE = "data/haarcascade_frontalface_default.xml";

    public FaceDetector() {
        classifier = new CascadeClassifier();
        loaded     = false;
        loadCascade();
    }

    private void loadCascade() {
        try {
            Path path = Paths.get(CASCADE_FILE);

            if (!Files.exists(path)) {
                Files.createDirectories(path.getParent());
                InputStream in = getClass().getResourceAsStream("/haarcascade_frontalface_default.xml");
                if (in == null) {
                    System.out.println("Haar cascade XML not found. Place it in the data/ folder.");
                    return;
                }
                Files.copy(in, path);
                in.close();
            }

            loaded = classifier.load(path.toAbsolutePath().toString());
        } catch (IOException e) {
            System.out.println("Error loading cascade: " + e.getMessage());
        }
    }

    public Rect[] detect(Mat frame) {
        if (!loaded || frame.empty()) return new Rect[0];

        Mat gray = new Mat();
        Imgproc.cvtColor(frame, gray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.equalizeHist(gray, gray);

        MatOfRect faces = new MatOfRect();
        classifier.detectMultiScale(gray, faces, 1.1, 4, 0,
                new Size(80, 80), new Size());

        Rect[] faceArray = faces.toArray();

        for (Rect rect : faceArray) {
            Imgproc.rectangle(frame, rect, new Scalar(0, 220, 0), 2);
            Imgproc.putText(frame, "Face detected",
                    new Point(rect.x, rect.y - 8),
                    Imgproc.FONT_HERSHEY_SIMPLEX, 0.55, new Scalar(0, 220, 0), 2);
        }

        gray.release();
        return faceArray;
    }

    public boolean isLoaded() { return loaded; }
}
