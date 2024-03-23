package org.example.duplicateimagechecker;
import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.DirectoryChooser;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class ImageInputApp extends Application {

    private String duplicateDirectoryPath = "./duplicate";
    private String newDirectoryPath = "./new";
    private Label selectedFileLabel;
    private Label selectedDirectoryLabel;
    private Label searchStatusLabel;
    private Label selectedInputDirectoryLabel;
    private CheckBox searchSubdirectoriesCheckBox;
    private ToggleGroup selectionToggleGroup;
    private RadioButton selectSinglePhotoRadioButton;
    private RadioButton selectDirectoryRadioButton;

    private boolean searchForImage(String selectedPhotoPath, String selectedDirectoryPath, boolean searchSubdirectories) {
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
                } else if (searchSubdirectories && file.isDirectory()) {
                    // If searchSubdirectories is enabled and the current file is a directory, recursively search it
                    boolean imageFoundInSubdirectory = searchForImage(selectedPhotoPath, file.getAbsolutePath(), true);
                    if (imageFoundInSubdirectory) {
                        return true; // Image found in subdirectory
                    }
                }
            }
        }
        return false; // Image not found
    }
    // Method to perform directory search in a separate thread
    private void searchInDirectory(String inputDirPath, String directoryPath, boolean searchSubdirectories) {
        File directory = new File(inputDirPath);
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().endsWith(".jpg")) {
                    // Compare the selected photo with each file in the directory
                    boolean imageFound = searchForImage(file.getPath(), directoryPath, searchSubdirectories);
                    if (imageFound) {
                        // Add photo to dir called duplicate
                        copyFile(file, duplicateDirectoryPath);
                    } else {
                        // Add photo to dir called new
                        copyFile(file, newDirectoryPath);
                    }
                } else if (searchSubdirectories && file.isDirectory()) {
                    // If searchSubdirectories is enabled and the current file is a directory, recursively search it
                    searchInDirectory(file.getAbsolutePath(), directoryPath, true);
                }
            }
            // Display directories after all files have been processed
            displayDirectories();
        } else {
            searchStatusLabel.setText("Input Directory is Empty");
        }
    }

    private void copyFile(File sourceFile, String destinationDirectory) {
        try {
            Path sourcePath = sourceFile.toPath();
            Path destinationPath = Paths.get(destinationDirectory, sourceFile.getName());
            Files.copy(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void createDirectories() {
        try {
            Path duplicateDirectory = Paths.get(duplicateDirectoryPath);
            Path newDirectory = Paths.get(newDirectoryPath);

            if (Files.exists(duplicateDirectory)) {
                // Clear the duplicate directory if it exists
                Files.list(duplicateDirectory).forEach(file -> {
                    try {
                        Files.delete(file);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            } else {
                // Create the duplicate directory if it doesn't exist
                Files.createDirectories(duplicateDirectory);
            }

            if (Files.exists(newDirectory)) {
                // Clear the new directory if it exists
                Files.list(newDirectory).forEach(file -> {
                    try {
                        Files.delete(file);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            } else {
                // Create the new directory if it doesn't exist
                Files.createDirectories(newDirectory);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void displayDirectories() {
        try {
            //searchStatusLabel.setText("Opening directories...");

            // Open duplicate directory
            File duplicateDirectory = new File(duplicateDirectoryPath);
            Desktop.getDesktop().open(duplicateDirectory);

            // Open new directory
            File newDirectory = new File(newDirectoryPath);
            Desktop.getDesktop().open(newDirectory);

            //searchStatusLabel.setText("Directories opened successfully.");
        } catch (IOException e) {
            //searchStatusLabel.setText("Error opening directories: " + e.getMessage());
            e.printStackTrace();
        }
    }


    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Select JPG File and Directory");

        selectedFileLabel = new Label();
        selectedDirectoryLabel = new Label();
        searchStatusLabel = new Label();
        selectedInputDirectoryLabel = new Label();//for dir input
        searchSubdirectoriesCheckBox = new CheckBox("Search Subdirectories");
        selectionToggleGroup = new ToggleGroup();
        selectSinglePhotoRadioButton = new RadioButton("Select 1 Photo");
        selectDirectoryRadioButton = new RadioButton("Select Directory of Photos");
        selectSinglePhotoRadioButton.setToggleGroup(selectionToggleGroup);
        selectSinglePhotoRadioButton.setSelected(true); // Default selection
        selectDirectoryRadioButton.setToggleGroup(selectionToggleGroup);


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

        Button selectInputDirectoryButton = new Button("Select Directory of photo to check");//Dir photos to check
        selectInputDirectoryButton.setOnAction(e -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("Select Directory Containing Images");
            File selectedDirectory = directoryChooser.showDialog(primaryStage);
            if (selectedDirectory != null) {
                selectedInputDirectoryLabel.setText("Selected Directory: " + selectedDirectory.getAbsolutePath());
            } else {
                selectedInputDirectoryLabel.setText("No directory selected.");
            }
        });
        selectInputDirectoryButton.setVisible(false);
        selectedInputDirectoryLabel.setVisible(false);

        Button selectDirectoryButton = new Button("Select Directory of photos to check against");
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

        // Add event handlers for radio buttons
        selectSinglePhotoRadioButton.setOnAction(e -> {
            selectInputDirectoryButton.setVisible(false);
            selectInputDirectoryButton.setDisable(true);
            selectedInputDirectoryLabel.setVisible(false);
            selectedInputDirectoryLabel.setDisable(true);

            selectFileButton.setVisible(true);
            selectFileButton.setDisable(false);
            selectedFileLabel.setVisible(true);
            selectedFileLabel.setDisable(false);
        });

        selectDirectoryRadioButton.setOnAction(e -> {
            selectInputDirectoryButton.setVisible(true);
            selectInputDirectoryButton.setDisable(false);
            selectedInputDirectoryLabel.setVisible(true);
            selectedInputDirectoryLabel.setDisable(false);

            selectFileButton.setVisible(false);
            selectFileButton.setDisable(true);
            selectedFileLabel.setVisible(false);
            selectedFileLabel.setDisable(true);
        });

        Button startSearchButton = new Button("Start Search");
        startSearchButton.setOnAction(e -> {
            // Check which radio button is selected
            if (selectInputDirectoryButton.isVisible()) {
                // Directory of photos selected
                if (selectedInputDirectoryLabel.getText().isEmpty() || selectedDirectoryLabel.getText().isEmpty()) {
                    // If either directory is not selected, display a message
                    searchStatusLabel.setText("Please select both directories.");
                } else {
                    createDirectories();
                    // Both directories are selected, initiate the search
                    String inputDirPath = selectedInputDirectoryLabel.getText().substring(selectedInputDirectoryLabel.getText().indexOf(":") + 2);
                    String directoryPath = selectedDirectoryLabel.getText().substring(selectedDirectoryLabel.getText().indexOf(":") + 2);
                    boolean searchSubdirectories = searchSubdirectoriesCheckBox.isSelected(); // Get checkbox state

                    // Perform directory search in a separate thread to keep UI responsive
                    Thread directorySearchThread = new Thread(() -> {
                        searchInDirectory(inputDirPath, directoryPath, searchSubdirectories);
                    });
                    directorySearchThread.start();
                }
            } else {
                // Single photo selected
                if (selectedFileLabel.getText().isEmpty() || selectedDirectoryLabel.getText().isEmpty()) {
                    // If either the file or directory is not selected, display a message
                    searchStatusLabel.setText("Please select both a file and a directory.");
                } else {
                    // Both file and directory are selected, initiate the search
                    String filePath = selectedFileLabel.getText().substring(selectedFileLabel.getText().indexOf(":") + 2);
                    String directoryPath = selectedDirectoryLabel.getText().substring(selectedDirectoryLabel.getText().indexOf(":") + 2);
                    boolean searchSubdirectories = searchSubdirectoriesCheckBox.isSelected(); // Get checkbox state

                    // Perform single photo search
                    boolean imageFound = searchForImage(filePath, directoryPath, searchSubdirectories);
                    if (imageFound) {
                        searchStatusLabel.setText("Image found.");
                    } else {
                        searchStatusLabel.setText("Image not found.");
                    }
                    // Clear selected file and directory labels
                    selectedFileLabel.setText("");
                    selectedDirectoryLabel.setText("");
                }
            }
        });


        VBox root = new VBox(10);
        root.getChildren().addAll(selectSinglePhotoRadioButton, selectDirectoryRadioButton, selectInputDirectoryButton, selectedInputDirectoryLabel, selectFileButton, selectedFileLabel, selectDirectoryButton, selectedDirectoryLabel, searchSubdirectoriesCheckBox, searchStatusLabel, startSearchButton);
        Scene scene = new Scene(root, 400, 400);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}