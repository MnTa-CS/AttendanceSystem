package attendance;

import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

/**
 * Wraps OpenCV VideoCapture for webcam access.
 * Demonstrates: OOP encapsulation, external library integration, resource management.
 */
public class Camera {

    private VideoCapture capture;
    private boolean      open;

    public Camera() {
        this.capture = new VideoCapture();
        this.open    = false;
    }

    /**
     * Opens the default webcam (index 0).
     * @throws RuntimeException if the camera cannot be opened
     */
    public void start() {
        capture.open(0);
        if (!capture.isOpened()) {
            throw new RuntimeException(
                "Could not open webcam. Make sure a camera is connected and not in use.");
        }
        capture.set(Videoio.CAP_PROP_FRAME_WIDTH,  640);
        capture.set(Videoio.CAP_PROP_FRAME_HEIGHT, 480);
        open = true;
    }

    /**
     * Reads the next frame from the webcam.
     * @param frame output Mat to write the frame into
     * @return true if a frame was successfully read
     */
    public boolean readFrame(Mat frame) {
        if (!open || !capture.isOpened()) return false;
        return capture.read(frame);
    }

    /**
     * Releases the webcam resource.
     */
    public void stop() {
        if (capture != null && capture.isOpened()) {
            capture.release();
        }
        open = false;
    }

    public boolean isOpen() { return open && capture.isOpened(); }
}
