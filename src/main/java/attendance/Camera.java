package attendance;

import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

public class Camera {

    private VideoCapture capture;
    private boolean      open;

    public Camera() {
        capture = new VideoCapture();
        open    = false;
    }

    public void start() {
        capture.open(0);
        if (!capture.isOpened()) {
            throw new RuntimeException("Could not open webcam. Check that it is connected.");
        }
        capture.set(Videoio.CAP_PROP_FRAME_WIDTH,  640);
        capture.set(Videoio.CAP_PROP_FRAME_HEIGHT, 480);
        open = true;
    }

    public boolean readFrame(Mat frame) {
        if (!open) return false;
        return capture.read(frame);
    }

    public void stop() {
        if (capture != null && capture.isOpened()) {
            capture.release();
        }
        open = false;
    }

    public boolean isOpen() { return open; }
}
