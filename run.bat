@echo off
javac -encoding UTF-8 --module-path "javafx" --add-modules javafx.controls,javafx.fxml -cp "src/main/java;libs/opencv-4120.jar" -d out (Get-ChildItem -Recurse -Filter "*.java" src/main/java | % { $_.FullName })
Copy-Item src/main/resources/* out/
java "-Djava.library.path=natives" --module-path "javafx" --add-modules javafx.controls,javafx.fxml -cp "out;libs/opencv-4120.jar" attendance.MainApp
pause