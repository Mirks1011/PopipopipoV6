package absolutecinema;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class AdminEditMovieController implements Initializable {

    @FXML private TextField titleField;
    @FXML private TextArea synopsisField;
    @FXML private TextField lengthField;
    @FXML private TextField priceField;
    @FXML private ComboBox<String> cinema;
@FXML private ComboBox<String> date;
@FXML private ComboBox<String> time;


    
    @FXML private ImageView movPost;
    @FXML private Button updateMovieBtn;

    private String imagePath;
    private int movieId; // Store the movie ID

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Fetch movie data only if a movie ID has been set
        if (movieId > 0) {
            fetchMovieData(movieId);
        }
    }
    
        @FXML
    public void handleHome(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/absolutecinema/AdminMain.fxml")); 
            Parent root = loader.load();
            Stage stage = (Stage) ((Button) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            System.err.println("Error loading AdminActualBooking.fxml: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @FXML
    public void handleHome2() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/absolutecinema/AdminMain.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) updateMovieBtn.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            System.err.println("Error loading AdminMain.fxml: " + e.getMessage());
        }
    }

    /** Set the selected movie ID and fetch its data */
    public void setMovieId(int id) {
        this.movieId = id;
        fetchMovieData(id);
    }

    /** Fetch movie details from the database using movie ID */
    public void fetchMovieData(int movieId) {
        System.out.println("Fetching movie data for ID: " + movieId);

        String sql = "SELECT * FROM movies WHERE movie_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, movieId);
            System.out.println("Executing SQL query: " + stmt);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    System.out.println("Movie found with ID: " + movieId);

                    // Store values BEFORE closing the ResultSet
                    String movieName = rs.getString("movie_name");
                    String synopsis = rs.getString("synopsis");
                    String length = rs.getString("length");
                    String price = rs.getString("price");
                    byte[] imageBytes = rs.getBytes("image_data");

                    if (imageBytes == null || imageBytes.length == 0) {
                        System.out.println("No image found, using placeholder.");
                        imageBytes = Files.readAllBytes(new File("src/images/placeholder.png").toPath());
                    }

                    // Update UI safely
                    byte[] finalImageBytes = imageBytes;
                    Platform.runLater(() -> {
                        titleField.setText(movieName);
                        synopsisField.setText(synopsis);
                        lengthField.setText(length);
                        priceField.setText(price);
                        movPost.setImage(new Image(new ByteArrayInputStream(finalImageBytes)));
                    });
                } else {
                    System.out.println("No matching movie found for ID: " + movieId);
                    showAlert("Movie not found!", Alert.AlertType.WARNING);
                }
            }
        } catch (SQLException | IOException e) {
            System.err.println("Database query error: " + e.getMessage());
            showAlert("Database error: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    /** Update movie details in the database */
 @FXML
public void handleUpdateMovie(ActionEvent event) {
    String title = titleField.getText();
    String synopsis = synopsisField.getText();
    String length = lengthField.getText();
    String priceText = priceField.getText();

    String selectedCinema = cinema.getValue();
    String selectedDate = date.getValue();
    String selectedTime = time.getValue();

    // Debug check to ensure fields are populated
    System.out.println("Updating movie ID: " + movieId);
    System.out.println("Synopsis: " + synopsis);
    System.out.println("Length: " + length);
    System.out.println("Price: " + priceText);
    System.out.println("Cinema: " + selectedCinema);
    System.out.println("Date: " + selectedDate);
    System.out.println("Time: " + selectedTime);

    if (title.isEmpty() || synopsis.isEmpty() || length.isEmpty() || priceText.isEmpty()
            || selectedCinema == null || selectedDate == null || selectedTime == null) {
        showAlert("Please fill in all movie details, including cinema, date, and time.", Alert.AlertType.ERROR);
        return;
    }

    File imageFile = imagePath != null ? new File(imagePath) : null;
    byte[] imageBytes = null;

    try {
        if (imageFile != null) {
            imageBytes = Files.readAllBytes(imageFile.toPath());
        } else {
            System.out.println("No new image selected, using existing database image.");
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement("SELECT image_data FROM movies WHERE movie_id = ?")) {
                stmt.setInt(1, movieId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        imageBytes = rs.getBytes("image_data");
                    }
                }
            }
        }
    } catch (IOException | SQLException e) {
        showAlert("Error handling movie image: " + e.getMessage(), Alert.AlertType.ERROR);
        return;
    }

    try (Connection conn = DBConnection.getConnection()) {
        String sql = "UPDATE movies SET movie_name=?, synopsis=?, length=?, price=?, image_data=?, cinema_number=?, date=?, time=? WHERE movie_id=?";
        PreparedStatement stmt = conn.prepareStatement(sql);

        stmt.setString(1, title);
        stmt.setString(2, synopsis);
        stmt.setString(3, length);
        stmt.setString(4, priceText);
        stmt.setBytes(5, imageBytes);
        stmt.setString(6, selectedCinema);
        stmt.setString(7, selectedDate);
        stmt.setString(8, selectedTime);
        stmt.setInt(9, movieId); // Now the 9th parameter

        System.out.println("Executing Update SQL: " + stmt);

        int rowsUpdated = stmt.executeUpdate();
        System.out.println("Rows affected: " + rowsUpdated);

        if (rowsUpdated > 0) {
            System.out.println("Movie updated successfully!");
            showAlert("Movie updated successfully!", Alert.AlertType.INFORMATION);

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/absolutecinema/AdminMain.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Button) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } else {
            System.err.println("Update failed! No rows affected.");
            showAlert("Update failed! No changes were made.", Alert.AlertType.WARNING);
        }
    } catch (SQLException | IOException e) {
        System.err.println("Database update error: " + e.getMessage());
        showAlert("Database error: " + e.getMessage(), Alert.AlertType.ERROR);
    }
}



    /** Allow users to change the movie poster image */
    @FXML
    public void addImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );
        File file = fileChooser.showOpenDialog(null);

        if (file != null) {
            imagePath = file.getAbsolutePath();
            movPost.setImage(new Image(file.toURI().toString()));
        }
    }

    /** Show alerts */
    private void showAlert(String message, Alert.AlertType type) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle("Action Info");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
    
    @FXML
public void handleDeleteMovie(ActionEvent event) {
    // Confirmation Dialog
    Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
    confirmAlert.setTitle("Delete Movie");
    confirmAlert.setHeaderText(null);
    confirmAlert.setContentText("Are you sure you want to delete this movie?");
    
    if (confirmAlert.showAndWait().get() == javafx.scene.control.ButtonType.OK) {
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "DELETE FROM movies WHERE movie_id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);

            stmt.setInt(1, movieId);

            System.out.println("Executing Delete SQL: " + stmt);
            int rowsDeleted = stmt.executeUpdate();
            System.out.println("Rows affected: " + rowsDeleted);

            if (rowsDeleted > 0) {
                showAlert("Movie deleted successfully!", Alert.AlertType.INFORMATION);

                // Redirect to the main screen after deletion
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/absolutecinema/AdminMain.fxml"));
                Parent root = loader.load();
                Stage stage = (Stage) ((Button) event.getSource()).getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.show();
            } else {
                showAlert("Failed to delete the movie.", Alert.AlertType.ERROR);
            }
        } catch (SQLException | IOException e) {
            System.err.println("Database delete error: " + e.getMessage());
            showAlert("Database error: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
}

}


