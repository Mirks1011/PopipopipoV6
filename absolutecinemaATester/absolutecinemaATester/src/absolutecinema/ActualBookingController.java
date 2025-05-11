package absolutecinema;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.fxml.Initializable;
import javafx.fxml.FXML;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class ActualBookingController implements Initializable {

    // FXML references to your ComboBoxes
    @FXML private ComboBox<String> date;
    @FXML private ComboBox<String> time;
    @FXML private ComboBox<String> cinema;
    @FXML private Button seatno;
    @FXML private Button prodpay;
    @FXML private ImageView movPost;

    // Added by me
    @FXML private Label bookingTitle;
    @FXML private Label bookingSynopsis;
    @FXML private Label bookingLength;
    @FXML private Label bookingPrice;

    // Variables to hold the movie details passed from MainController
    private String movieTitle;
    private String movieLength;
    private String moviePrice;
    private String synopsis;
    private byte[] movieImageData;
public static String seatNumber;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadCinemaChoices();
        loadDateChoices();
        loadTimeChoices();

        // Debugging output to verify passed data
        System.out.println("Movie Title: " + movieTitle);
        System.out.println("Movie Length: " + movieLength);
        System.out.println("Movie Price: " + moviePrice);
        System.out.println("Synopsis: " + synopsis);
     
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

        String SelectedAttribute = popupController.getSelectedAttribute();
        if (SelectedAttribute != null && !SelectedAttribute.isEmpty()) {
            seatno.setText(SelectedAttribute);
        }
    }

    @FXML
    public void handleHome(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/absolutecinema/Main.fxml")); 
            Parent root = loader.load();
            Stage stage = (Stage) ((Button) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            System.err.println("Error loading Main.fxml: " + e.getMessage());
            e.printStackTrace();
        }
    }

 @FXML
private void handleProceedToPayment(ActionEvent event) {
    boolean anyEmpty = false;
    StringBuilder emptyFields = new StringBuilder();

    if (date.getValue() == null) {
        anyEmpty = true;
        emptyFields.append("Date\n");
    }
    if (cinema.getValue() == null) {
        anyEmpty = true;
        emptyFields.append("Cinema\n");
    }
    if (time.getValue() == null) {
        anyEmpty = true;
        emptyFields.append("Time\n");
    }
    if ("Seat Number".equals(seatno.getText()) || seatno.getText().trim().isEmpty()) {
        anyEmpty = true;
        emptyFields.append("Seat\n");
    }

    if (anyEmpty) {
        showAlert("Please select the following fields:\n" + emptyFields, Alert.AlertType.WARNING);
        return;
    }

    seatNumber = seatno.getText();

    try (Connection conn = DBConnection.getConnection()) {
        // ✅ Retrieve movie_id
        String fetchMovieIdSql = "SELECT movie_id, price, cinema_number FROM movies WHERE movie_name = ? AND date = ? AND time = ? AND cinema_number = ?";
        PreparedStatement fetchStmt = conn.prepareStatement(fetchMovieIdSql);
        fetchStmt.setString(1, movieTitle);
        fetchStmt.setString(2, date.getValue().toString());
        fetchStmt.setString(3, time.getValue());
        fetchStmt.setString(4, cinema.getValue());
        ResultSet rs = fetchStmt.executeQuery();

        if (!rs.next()) {
            showAlert("No matching movie found for booking.", Alert.AlertType.ERROR);
            return;
        }

        int movieId = rs.getInt("movie_id");
        double price = rs.getDouble("price");
        String cinemaNumber = rs.getString("cinema_number");

        // 🔒 Check if seat is already booked
        String checkSeatSql = "SELECT * FROM bookings WHERE movie_id = ? AND seat_number = ?";
        PreparedStatement checkStmt = conn.prepareStatement(checkSeatSql);
        checkStmt.setInt(1, movieId);
        checkStmt.setString(2, seatNumber);
        ResultSet seatCheckResult = checkStmt.executeQuery();

        if (seatCheckResult.next()) {
            showAlert("This seat has already been booked. Please select a different seat.", Alert.AlertType.WARNING);
            return;
        }

        // 🚫 Do not insert booking yet — only proceed to payment
        FXMLLoader loader = new FXMLLoader(getClass().getResource("Payment.fxml"));
        Parent root = loader.load();
        PaymentController paymentController = loader.getController();

        // Pass necessary details to payment
        paymentController.setMovieTitle(movieTitle);
        paymentController.setSeatNumber(seatNumber);
        paymentController.setMovieId(movieId);
        paymentController.setBookingDate(java.time.LocalDate.now().toString());
        paymentController.setBookingTime(java.time.LocalTime.now().toString());
        paymentController.setCinemaNumber(cinemaNumber);
        paymentController.setPrice(price);

        Stage stage = (Stage) prodpay.getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.show();

    } catch (SQLException | IOException e) {
        e.printStackTrace();
        showAlert("Failed to proceed to payment: " + e.getMessage(), Alert.AlertType.ERROR);
    }
}




    private void switchScene(String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Parent root = loader.load();

            PaymentController paymentController = loader.getController();
            paymentController.setAmount(bookingPrice.getText());

            Stage stage = (Stage) prodpay.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            System.err.println("Error switching scene to " + fxmlFile + ": " + e.getMessage());
        }
    }

    private void showAlert(String message, Alert.AlertType alertType) {
        Alert alert = new Alert(alertType);
        alert.setTitle("Action Info");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
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

    private void loadDateChoices() {
        ObservableList<String> dateList = FXCollections.observableArrayList();

        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT DISTINCT date FROM movies";
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                dateList.add(rs.getString("date"));
            }

            date.setItems(dateList);
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Failed to load dates from database.", Alert.AlertType.ERROR);
        }
    }

    private void loadTimeChoices() {
        ObservableList<String> timeList = FXCollections.observableArrayList();

        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT DISTINCT time FROM movies";
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                timeList.add(rs.getString("time"));
            }

            time.setItems(timeList);
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Failed to load times from database.", Alert.AlertType.ERROR);
        }
    }

    public void setMovieDetails(String title, String length, String price, byte[] imageData, String synopsis) {
    // Set the movie details from MainController
    this.movieTitle = title;
    this.movieLength = length;
    this.moviePrice = price;
    this.synopsis = synopsis;  // Correctly store the synopsis
    this.movieImageData = imageData;

    // Update the labels with the movie details
    bookingTitle.setText(title);
    bookingLength.setText("Time: " + length);
    bookingPrice.setText("Price: " + price);
    bookingSynopsis.setText("Synopsis: " + synopsis);

    // Set the movie poster
    if (movieImageData != null) {
        try {
            Image image = new Image(new ByteArrayInputStream(movieImageData));
            movPost.setImage(image);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Failed to load image: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    } else {
        showAlert("No image data found for movie: " + title, Alert.AlertType.WARNING);
    }
}
}
