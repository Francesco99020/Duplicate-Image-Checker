package org.example.duplicateimagechecker;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImageInputApp extends Application {
    private String duplicateDirectoryPath = "./duplicate";
    private String newDirectoryPath = "./new";
    private Label selectedFileLabel;
    private Label selectedDirectoryLabel;
    private Label searchStatusLabel;
    private Label selectedInputDirectoryLabel;
    private CheckBox searchSubdirectoriesCheckBox;

    private Label selectedDuplicateDirectoryLabel;
    private Label selectedNewDirectoryLabel;

    private boolean searchForImage(String selectedPhotoPath, Map<Long, String> directoryImageHashes) {
        // Calculate hash of the selected photo
        long selectedPhotoHash = calculateImageHash(selectedPhotoPath);

        // Check if the calculated hash exists in the directoryImageHashes map
        return directoryImageHashes.containsKey(selectedPhotoHash);
    }

    private long calculateImageHash(String imagePath) {
        long hash = 0;
        try {
            BufferedImage image = ImageIO.read(new File(imagePath));
            if (image != null) {
                // Convert image to grayscale and resize
                image = resizeImage(image);

                // Calculate hash value
                int averagePixelValue = calculateAveragePixelValue(image);
                for (int y = 0; y < image.getHeight(); y++) {
                    for (int x = 0; x < image.getWidth(); x++) {
                        hash <<= 1;
                        if ((image.getRGB(x, y) & 0xFF) >= averagePixelValue) {
                            hash |= 1;
                        }
                    }
                }
            }
        } catch (IOException e) {
            // Handle IOException if unable to read the image file
            e.printStackTrace();
        }
        return hash;
    }


    private boolean searchForImage(String selectedPhotoPath, String selectedDirectoryPath, boolean searchSubdirectories) {
        File directory = new File(selectedDirectoryPath);
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if ((file.isFile() && file.getName().endsWith(".jpg")) || (file.isFile() && file.getName().endsWith(".png"))) {
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
            // Calculate hashes for images in directoryPath
            File checkDirectory = new File(directoryPath);
            File[] checkFiles = checkDirectory.listFiles();
            assert checkFiles != null;
            Map<Long, String> directoryImageHashes = calculateImageHashes(checkFiles, searchSubdirectories);

            // Create ExecutorService with a fixed thread pool
            ExecutorService executor = Executors.newFixedThreadPool((int)((double)Runtime.getRuntime().availableProcessors() * 0.8));

            for (File file : files) {
                if (file.isFile() && (file.getName().endsWith(".jpg") || file.getName().endsWith(".png"))) {
                    // Submit image comparison task to ExecutorService
                    executor.submit(() -> {
                        boolean imageFound = searchForImage(file.getPath(), directoryImageHashes);
                        if (imageFound) {
                            // Add photo to dir called duplicate
                            copyFile(file, duplicateDirectoryPath);
                        } else {
                            // Add photo to dir called new
                            copyFile(file, newDirectoryPath);
                        }
                    });
                } else if (searchSubdirectories && file.isDirectory()) {
                    // If searchSubdirectories is enabled and the current file is a directory, recursively search it
                    searchInDirectory(file.getAbsolutePath(), directoryPath, true);
                }
            }
            // Shutdown ExecutorService after submitting all tasks
            executor.shutdown();
        }
    }

    public Map<Long, String> calculateImageHashes(File[] imageFiles, boolean searchSubdirectories) {
        Map<Long, String> imageHashes = new ConcurrentHashMap<>();
        ExecutorService executor = Executors.newFixedThreadPool((int)((double)Runtime.getRuntime().availableProcessors() * 0.4));
        for (File file : imageFiles) {
            executor.execute(() -> processFile(file, searchSubdirectories, imageHashes));
        }
        executor.shutdown();
        return imageHashes;
    }

    private void processFile(File file, boolean searchSubdirectories, Map<Long, String> imageHashes) {
        try {
            ImageInputStream imageInputStream = ImageIO.createImageInputStream(file);
            if (imageInputStream != null) {
                BufferedImage image = ImageIO.read(imageInputStream);
                if (image != null) {
                    long hash = calculatePerceptualHash(image);
                    imageHashes.put(hash, file.getAbsolutePath());
                }
            } else if (searchSubdirectories && file.isDirectory()) {
                processDirectory(file, imageHashes);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void processDirectory(File directory, Map<Long, String> imageHashes) {
        ExecutorService directoryExecutor = Executors.newFixedThreadPool((int)((double)Runtime.getRuntime().availableProcessors() * 0.4));

        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                directoryExecutor.execute(() -> processFile(file, true, imageHashes));
            }
        }
        directoryExecutor.shutdown();
    }
    private long calculatePerceptualHash(BufferedImage image) {
        // Convert image to grayscale and resize
        image = resizeImage(image);

        // Calculate hash value
        long hash = 0;
        int averagePixelValue = calculateAveragePixelValue(image);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                hash <<= 1;
                if ((image.getRGB(x, y) & 0xFF) >= averagePixelValue) {
                    hash |= 1;
                }
            }
        }
        return hash;
    }

    private BufferedImage resizeImage(BufferedImage originalImage) {
        BufferedImage resizedImage = new BufferedImage(8, 8, BufferedImage.TYPE_BYTE_GRAY);
        resizedImage.getGraphics().drawImage(originalImage.getScaledInstance(8, 8, java.awt.Image.SCALE_SMOOTH), 0, 0, null);
        return resizedImage;
    }

    private static int calculateAveragePixelValue(BufferedImage image) {
        long totalPixelValue = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                totalPixelValue += image.getRGB(x, y) & 0xFF;
            }
        }
        return (int) (totalPixelValue / (image.getWidth() * image.getHeight()));
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
        primaryStage.setTitle("Duplicate Image Checker App");

        selectedFileLabel = new Label();
        selectedDirectoryLabel = new Label();
        searchStatusLabel = new Label();
        selectedInputDirectoryLabel = new Label();//for dir input
        searchSubdirectoriesCheckBox = new CheckBox("Search Subdirectories");
        ToggleGroup selectionToggleGroup = new ToggleGroup();
        RadioButton selectSinglePhotoRadioButton = new RadioButton("Select 1 Photo");
        RadioButton selectDirectoryRadioButton = new RadioButton("Select Directory of Photos");
        selectSinglePhotoRadioButton.setToggleGroup(selectionToggleGroup);
        selectSinglePhotoRadioButton.setSelected(true); // Default selection
        selectDirectoryRadioButton.setToggleGroup(selectionToggleGroup);
        selectedDuplicateDirectoryLabel = new Label("");
        selectedNewDirectoryLabel = new Label("");

        Button setOutputDirectoriesButton = getButton(primaryStage);


        Button selectFileButton = new Button("Select Image File");
        selectFileButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("JPEG Files", "*.jpg"),
                    new FileChooser.ExtensionFilter("PNG Files", "*.png")
            );
            File selectedFile = fileChooser.showOpenDialog(primaryStage);
            if (selectedFile != null) {
                selectedFileLabel.setText("Selected File: " + selectedFile.getAbsolutePath());
            } else {
                selectedFileLabel.setText("No file selected.");
            }
        });

        Button selectInputDirectoryButton = getSelectDirectoryButton("Select Directory of photo to check", primaryStage, selectedInputDirectoryLabel);
        selectInputDirectoryButton.setVisible(false);
        selectedInputDirectoryLabel.setVisible(false);

        Button selectDirectoryButton = getSelectDirectoryButton("Select Directory of photos to check against", primaryStage, selectedDirectoryLabel);

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

        Button startSearchButton = getButton(selectInputDirectoryButton);


        VBox root = new VBox(10);
        root.getChildren().addAll(selectSinglePhotoRadioButton, selectDirectoryRadioButton, selectInputDirectoryButton, selectedInputDirectoryLabel, selectFileButton, selectedFileLabel, selectDirectoryButton, selectedDirectoryLabel, searchSubdirectoriesCheckBox, setOutputDirectoriesButton, selectedDuplicateDirectoryLabel, selectedNewDirectoryLabel, searchStatusLabel, startSearchButton);
        Scene scene = new Scene(root, 400, 450);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private Button getButton(Button selectInputDirectoryButton) {
        Button startSearchButton = new Button("Start Search");
        startSearchButton.setOnAction(e -> {
            // Check which radio button is selected
            if (selectInputDirectoryButton.isVisible()) {
                // Directory of photos selected
                if (selectedInputDirectoryLabel.getText().isEmpty() || selectedDirectoryLabel.getText().isEmpty()) {
                    // If either directory is not selected, display a message
                    searchStatusLabel.setText("Please select both directories.");
                } else if(selectedDuplicateDirectoryLabel.getText().isEmpty() || selectedNewDirectoryLabel.getText().isEmpty()){
                    searchStatusLabel.setText("Please select both Output directories new and duplicate.");
                }
                else {
                    searchStatusLabel.setText("Searching for duplicates now. This might take a few minutes...");
                    createDirectories();
                    // Both directories are selected, initiate the search
                    Thread directorySearchThread = getThread();
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
        return startSearchButton;
    }

    private Thread getThread() {
        String inputDirPath = selectedInputDirectoryLabel.getText().substring(selectedInputDirectoryLabel.getText().indexOf(":") + 2);
        String directoryPath = selectedDirectoryLabel.getText().substring(selectedDirectoryLabel.getText().indexOf(":") + 2);
        boolean searchSubdirectories = searchSubdirectoriesCheckBox.isSelected(); // Get checkbox state

        // Perform directory search in a separate thread to keep UI responsive
        return new Thread(() -> {
            searchInDirectory(inputDirPath, directoryPath, searchSubdirectories);
            // After the search is complete, update the UI
            Platform.runLater(() -> {
                searchStatusLabel.setText("Search Complete!");
                displayDirectories();
            });
        });
    }

    private Button getSelectDirectoryButton(String s, Stage primaryStage, Label selectedDirectoryLabel) {
        Button selectDirectoryButton = new Button(s);
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
        return selectDirectoryButton;
    }

    private Button getButton(Stage primaryStage) {
        Button setOutputDirectoriesButton = new Button("Set Output Directories");
        setOutputDirectoriesButton.setOnAction(e -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("Select Output Directory");
            File selectedDirectory = directoryChooser.showDialog(primaryStage);
            if (selectedDirectory != null) {
                // Set the duplicate and new directory paths
                duplicateDirectoryPath = selectedDirectory.getAbsolutePath() + "\\duplicate";
                newDirectoryPath = selectedDirectory.getAbsolutePath() + "\\new";

                // Update UI to display the selected directory paths
                selectedDuplicateDirectoryLabel.setText("Selected Duplicate Directory: " + duplicateDirectoryPath);
                selectedNewDirectoryLabel.setText("Selected New Directory: " + newDirectoryPath);
            } else {
                selectedDuplicateDirectoryLabel.setText("No directory selected.");
                selectedNewDirectoryLabel.setText("No directory selected.");
            }
        });
        return setOutputDirectoriesButton;
    }
    public static void main(String[] args) {
        launch(args);
    }
}