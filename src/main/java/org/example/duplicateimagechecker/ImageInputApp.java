package org.example.duplicateimagechecker;
import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.DirectoryChooser;
import java.io.File;

public class ImageInputApp extends Application {

    private Label selectedFileLabel;
    private Label selectedDirectoryLabel;
    private Label searchStatusLabel;


    private boolean searchForImage(String selectedPhotoPath, String selectedDirectoryPath) {
        File directory = new File(selectedDirectoryPath);
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().endsWith(".jpg")) {
                    // Compare the selected photo with each file in the directory
                    ImageComparison imageComparison = new ImageComparison(selectedPhotoPath, file.getAbsolutePath());
                    if (imageComparison.compareImages()) {
                        return true; // Image found
                    }
                }
            }
        }
        return false; // Image not found
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Select JPG File and Directory");

        selectedFileLabel = new Label();
        selectedDirectoryLabel = new Label();
        searchStatusLabel = new Label();

        Button selectFileButton = new Button("Select JPG File");
        selectFileButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JPEG Files", "*.jpg"));
            File selectedFile = fileChooser.showOpenDialog(primaryStage);
            if (selectedFile != null) {
                selectedFileLabel.setText("Selected File: " + selectedFile.getAbsolutePath());
            } else {
                selectedFileLabel.setText("No file selected.");
            }
        });

        Button selectDirectoryButton = new Button("Select Directory");
        selectDirectoryButton.setOnAction(e -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("Select Directory Containing Images");
            File selectedDirectory = directoryChooser.showDialog(primaryStage);
            if (selectedDirectory != null) {
                selectedDirectoryLabel.setText("Selected Directory: " + selectedDirectory.getAbsolutePath());
            } else {
                selectedDirectoryLabel.setText("No directory selected.");
            }
        });

        Button startSearchButton = new Button("Start Search");
        startSearchButton.setOnAction(e -> {
            if (selectedFileLabel.getText().isEmpty() || selectedDirectoryLabel.getText().isEmpty()) {
                // If either the file or directory is not selected, display a message
                searchStatusLabel.setText("Please select both a file and a directory.");
            } else {
                // Both file and directory are selected, initiate the search
                int startIndex = selectedFileLabel.getText().indexOf(":") + 2; // Start index after the ": "
                String filePath = selectedFileLabel.getText().substring(startIndex);
                startIndex = selectedDirectoryLabel.getText().indexOf(":") + 2; // Start index after the ": "
                String directoryPath = selectedDirectoryLabel.getText().substring(startIndex);
                boolean imageFound = searchForImage(filePath, directoryPath);
                if (imageFound) {
                    searchStatusLabel.setText("Image found in directory.");
                } else {
                    searchStatusLabel.setText("Image not found in directory.");
                }
                // Clear selected file and directory labels
                selectedFileLabel.setText("");
                selectedDirectoryLabel.setText("");
            }
        });

        VBox root = new VBox(10);
        root.getChildren().addAll(selectFileButton, selectedFileLabel, selectDirectoryButton, selectedDirectoryLabel, searchStatusLabel, startSearchButton);
        Scene scene = new Scene(root, 400, 250);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
