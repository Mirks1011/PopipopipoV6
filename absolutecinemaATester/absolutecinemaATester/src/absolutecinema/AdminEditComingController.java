package absolutecinema;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.sql.*;
import java.util.ResourceBundle;

public class AdminEditComingController implements Initializable {

    @FXML private TextField titleField;
    @FXML private TextArea synopsisField;
    @FXML private TextField lengthField;
    @FXML private TextField priceField;
    @FXML private ImageView movPost;
    @FXML private Button updateMovieBtn;

    private String imagePath;
    private int comingSoonId;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (comingSoonId > 0) {
            fetchComingSoonData(comingSoonId);
        }
    }

    public void setComingSoonId(int id) {
        this.comingSoonId = id;
        fetchComingSoonData(id);
    }

    private void fetchComingSoonData(int id) {
        String sql = "SELECT * FROM coming_soon WHERE com_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String title = rs.getString("movie_name");
                String synopsis = rs.getString("synopsis");
                String length = rs.getString("length");
                String price = rs.getString("price");
                byte[] imageBytes = rs.getBytes("image_data");

                if (imageBytes == null || imageBytes.length == 0) {
                    imageBytes = Files.readAllBytes(new File("src/images/placeholder.png").toPath());
                }

                byte[] finalImage = imageBytes;
                Platform.runLater(() -> {
                    titleField.setText(title);
                    synopsisField.setText(synopsis);
                    lengthField.setText(length);
                    priceField.setText(price);
                    movPost.setImage(new Image(new ByteArrayInputStream(finalImage)));
                });

            } else {
                showAlert("Coming Soon movie not found!", Alert.AlertType.WARNING);
            }

        } catch (SQLException | IOException e) {
            showAlert("Error fetching data: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
    
    @FXML
    public void addImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );
        File file = fileChooser.showOpenDialog(null);

        if (file != null) {
            imagePath = file.getAbsolutePath();
            Image image = new Image(file.toURI().toString());
            movPost.setImage(image);
        }
    }

   @FXML
public void handleUpdateComingMovie(ActionEvent event) {
    String title = titleField.getText();
    String synopsis = synopsisField.getText();
    String length = lengthField.getText();
    String price = priceField.getText();

    if (title.isEmpty() || synopsis.isEmpty() || length.isEmpty() || price.isEmpty()) {
        showAlert("Please fill in all fields.", Alert.AlertType.WARNING);
        return;
    }

    byte[] imageBytes;
    try {
        if (imagePath != null) {
            imageBytes = Files.readAllBytes(new File(imagePath).toPath());
        } else {
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement("SELECT image_data FROM coming_soon WHERE com_id = ?")) {
                stmt.setInt(1, comingSoonId);
                ResultSet rs = stmt.executeQuery();
                imageBytes = rs.next() ? rs.getBytes("image_data") : null;
            }
        }

        try (Connection conn = DBConnection.getConnection()) {
            String sql = "UPDATE coming_soon SET movie_name=?, synopsis=?, length=?, price=?, image_data=? WHERE com_id=?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, title);
            stmt.setString(2, synopsis);
            stmt.setString(3, length);
            stmt.setString(4, price);
            stmt.setBytes(5, imageBytes);
            stmt.setInt(6, comingSoonId);

            int rows = stmt.executeUpdate();
            if (rows > 0) {
                showAlert("Coming Soon movie updated successfully!", Alert.AlertType.INFORMATION);
                goToAdminMain(event);
            } else {
                showAlert("Update failed. No changes were made.", Alert.AlertType.WARNING);
            }
        }

    } catch (IOException | SQLException e) {
        showAlert("Error updating movie: " + e.getMessage(), Alert.AlertType.ERROR);
    }
}


    @FXML
    public void handleDeleteComingMovie(ActionEvent event) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to delete this movie?");
        confirm.setTitle("Confirm Delete");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try (Connection conn = DBConnection.getConnection();
                     PreparedStatement stmt = conn.prepareStatement("DELETE FROM coming_soon WHERE com_id = ?")) {
                    stmt.setInt(1, comingSoonId);
                    int rows = stmt.executeUpdate();
                    if (rows > 0) {
                        showAlert("Movie deleted successfully.", Alert.AlertType.INFORMATION);
                        goToComingSoonScreen(event);
                    } else {
                        showAlert("Failed to delete movie.", Alert.AlertType.WARNING);
                    }
                } catch (SQLException e) {
                    showAlert("Database error: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }

    private void goToComingSoonScreen(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/absolutecinema/AdminEditComing.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Button) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            showAlert("Navigation error: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void showAlert(String message, Alert.AlertType type) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle("Notification");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
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
    }
}
    
    private void goToWelcomeScreen(ActionEvent event) {
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
    }
}
     
    
    
    
}
