module org.example.duplicateimagechecker {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;


    opens org.example.duplicateimagechecker to javafx.fxml;
    exports org.example.duplicateimagechecker;
}