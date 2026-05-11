package attendance;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.File;

public class MainApp extends Application {

    static {

        try {
            File nativesDir = new File(System.getProperty("user.dir"), "natives");
            File[] dlls = nativesDir.listFiles(
                f -> f.getName().startsWith("opencv_java") && f.getName().endsWith(".dll")
            );
            if (dlls != null && dlls.length > 0) {
                System.load(dlls[0].getAbsolutePath());
                System.out.println("Loaded OpenCV: " + dlls[0].getName());
            } else {
                System.out.println("Warning: No OpenCV DLL found in natives/ folder.");
            }
        } catch (Exception e) {
            System.out.println("Warning: Could not load OpenCV: " + e.getMessage());
        }
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/attendance.fxml"));
        Parent root = loader.load();

        AttendanceController controller = loader.getController();

        Scene scene = new Scene(root, 960, 640);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        primaryStage.setTitle("Smart Attendance System");
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(e -> controller.shutdown());
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
