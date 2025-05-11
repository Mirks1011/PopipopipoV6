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
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Modality;

public class AdminActualBookingController implements Initializable {

    @FXML private ComboBox<String> date;
    @FXML private ComboBox<String> time;
    @FXML private ComboBox<String> cinema;
    @FXML private Button seatno;
    @FXML private Button prodpay;
    @FXML private ImageView movPost;
    @FXML private Button delMov;

    @FXML private TextField titleField;
    @FXML private TextArea synopsisField;
    @FXML private TextField lengthField;
    @FXML private TextField priceField;

    private String imagePath;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadCinemaChoices();
        loadDateChoices();
        loadTimeChoices();
    }

    private void loadDateChoices() {
        date.setItems(FXCollections.observableArrayList("April 12", "April 13", "April 14"));
    }

    private void loadTimeChoices() {
        time.setItems(FXCollections.observableArrayList("10:00 AM", "1:00 PM", "4:00 PM", "7:00 PM"));
    }

    private void loadCinemaChoices() {
        ObservableList<String> cinemaList = FXCollections.observableArrayList();
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT DISTINCT cinema_number FROM movies";
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                cinemaList.add(rs.getString("cinema_number"));
            }
            cinema.setItems(cinemaList);
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Failed to load cinemas from database.", Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void showPopup() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("SeatNumb.fxml"));
        Parent root = loader.load();
        SeatNumbController popupController = loader.getController();

        Stage stage = new Stage();
        stage.setScene(new Scene(root));
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.showAndWait();

        String selectedAttribute = popupController.getSelectedAttribute();
        if (selectedAttribute != null && !selectedAttribute.isEmpty()) {
            seatno.setText(selectedAttribute);
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
            System.err.println("Error loading AdminMain.fxml: " + e.getMessage());
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

    public void fetchMovieData(String movieTitle) {
        String sql = "SELECT * FROM movies WHERE movie_name = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, movieTitle);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Platform.runLater(() -> {
                    try {
                        titleField.setText(rs.getString("movie_name"));
                        synopsisField.setText(rs.getString("synopsis"));
                        lengthField.setText(rs.getString("length"));
                        priceField.setText(rs.getString("price"));
                        
                        byte[] imageBytes = rs.getBytes("image_data");
                        movPost.setImage(imageBytes != null ? new Image(new ByteArrayInputStream(imageBytes))
                                : new Image("placeholder.png"));
                    } catch (SQLException ex) {
                        Logger.getLogger(AdminActualBookingController.class.getName()).log(Level.SEVERE, null, ex);
                    }
                });
            } else {
                showAlert("Movie not found!", Alert.AlertType.WARNING);
            }

        } catch (SQLException e) {
            showAlert("Database error: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    public void handleSaveMovie(ActionEvent event) {
        String title = titleField.getText();
        String synopsis = synopsisField.getText();
        String length = lengthField.getText();
        String priceText = priceField.getText();
        String cinemaValue = cinema.getValue();
        String dateValue = date.getValue();
        String timeValue = time.getValue();

        if (cinemaValue == null || dateValue == null || timeValue == null ||
            title.isEmpty() || synopsis.isEmpty() || length.isEmpty() || 
            priceText.isEmpty() || imagePath == null) {
            showAlert("Please fill in all movie details and upload an image.", Alert.AlertType.ERROR);
            return;
        }

        File imageFile = new File(imagePath);
        byte[] imageBytes;

        try {
            imageBytes = Files.readAllBytes(imageFile.toPath());
        } catch (IOException e) {
            showAlert("Failed to read image: " + e.getMessage(), Alert.AlertType.ERROR);
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            String sql = "INSERT INTO movies (movie_name, synopsis, length, price, image_data, date, time, cinema_number) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);

            stmt.setString(1, title);
            stmt.setString(2, synopsis);
            stmt.setString(3, length);
            stmt.setString(4, priceText);
            stmt.setBytes(5, imageBytes);
            stmt.setString(6, dateValue);
            stmt.setString(7, timeValue);
            stmt.setString(8, cinemaValue);

            int rowsInserted = stmt.executeUpdate();
            if (rowsInserted > 0) {
                showAlert("Movie successfully saved with image (BLOB)!", Alert.AlertType.INFORMATION);
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
        } catch (SQLException e) {
            showAlert("Database error: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void showAlert(String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle("Action Info");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
