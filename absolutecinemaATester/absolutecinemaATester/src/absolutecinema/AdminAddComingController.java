package absolutecinema;


import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.control.Label;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

public class AdminAddComingController {
    
     @FXML 
     private Label home;

     @FXML 
     private Button back;
    
    @FXML
    private ComboBox<String> date;;

    @FXML
    private ComboBox<String> cinema;

    @FXML
    private ComboBox<String> time;

    @FXML
    private TextField lengthField;

    @FXML
    private ImageView movPost;

    @FXML
    private TextField priceField;

    @FXML
    private TextArea synopsisField;

    @FXML
    private TextField titleField;

    private File selectedImageFile;

    
    @FXML
public void goToWelcomeScreen(ActionEvent event) {
    try {
        // Load the WelcomeScreen.fxml
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/absolutecinema/WelcomeScreen.fxml"));
        Parent root = loader.load();

        // Get the current stage (window)
        Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();

        // Set the new scene
        stage.setScene(new Scene(root));

        // Show the new scene (WelcomeScreen.fxml)
        stage.show();

    } catch (IOException e) {
        System.err.println("Error loading WelcomeScreen.fxml: " + e.getMessage());
        showAlert("Error", "Failed to load WelcomeScreen.", Alert.AlertType.ERROR);
    }
}

    
    @FXML
public void goToAdminMain(ActionEvent event) {
    try {
        // Load the AdminMain.fxml
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/absolutecinema/AdminMain.fxml"));
        Parent root = loader.load();

        // Get the current stage (window)
        Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();

        // Set the new scene
        stage.setScene(new Scene(root));

        // Show the new scene (AdminMain.fxml)
        stage.show();

    } catch (IOException e) {
        System.err.println("Error loading AdminMain.fxml: " + e.getMessage());
        showAlert("Error", "Failed to load AdminMain screen.", Alert.AlertType.ERROR);
    }
}

    
    
    @FXML
    void addImage(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Movie Poster");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.jpg", "*.png", "*.jpeg", "*.gif"));
        selectedImageFile = fileChooser.showOpenDialog(null);
        if (selectedImageFile != null) {
            Image image = new Image(selectedImageFile.toURI().toString());
            movPost.setImage(image);
        }
    }

    @FXML
    void saveComingMovie(ActionEvent event) {
        String sql = "INSERT INTO coming_soon (movie_name, synopsis, length, price, availability, time, image_data) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, titleField.getText());
            stmt.setString(2, synopsisField.getText());
            stmt.setString(3, lengthField.getText());
            stmt.setString(4, priceField.getText());
            stmt.setString(5, date.getValue());
            stmt.setString(6, time.getValue());

            if (selectedImageFile != null) {
                FileInputStream fis = new FileInputStream(selectedImageFile);
                stmt.setBinaryStream(7, fis, (int) selectedImageFile.length());
            } else {
                stmt.setNull(7, Types.BLOB);
            }

            int result = stmt.executeUpdate();
            if (result > 0) {
                showAlert("Success", "Coming Soon movie added successfully.", AlertType.INFORMATION);
                clearForm();
            } else {
                showAlert("Failure", "Movie could not be added.", AlertType.WARNING);
            }

        } catch (Exception e) {
            showAlert("Error", "Error adding movie: " + e.getMessage(), AlertType.ERROR);
        }
    }

    private void clearForm() {
        titleField.clear();
        synopsisField.clear();
        lengthField.clear();
        priceField.clear();
        date.setValue(null);
        time.setValue(null);
        movPost.setImage(null);
        selectedImageFile = null;
    }

    private void showAlert(String title, String content, AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
    
    
    
}
