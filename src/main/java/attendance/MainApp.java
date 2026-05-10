package attendance;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Load OpenCV native library from the bin directory
        // Pass -Djava.library.path="C:\OpenCV\build\bin" when running
        System.loadLibrary("opencv_java4120");

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/attendance.fxml"));
        Parent root = loader.load();

        // Get controller reference so we can shut down the camera cleanly
        AttendanceController controller = loader.getController();

        Scene scene = new Scene(root, 960, 640);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        primaryStage.setTitle("Smart Attendance System");
        primaryStage.setScene(scene);
        primaryStage.setResizable(true);

        // Release camera when window is closed
        primaryStage.setOnCloseRequest(e -> controller.shutdown());

        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
