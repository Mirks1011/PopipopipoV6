package absolutecinema;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.layout.Region;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

public class PaymentController implements Initializable {

    private int bookingId;
    private double amount;
    private String movieTitle;
    private String cinemaNumber;
    private double price;
    

    private String seatNumber;
    private int movieId;
    private String bookingDate;
    private String bookingTime;

    @FXML private ComboBox<String> modPay;
    @FXML private TextField nameF;
    @FXML private TextField emailF;
    @FXML private Button cancelBtn;
    @FXML private Button confirmBtn;
    @FXML private Label movieTitleDisplay;
    @FXML private Label priceDisplay;
    @FXML private Label cinemaNumDisplay;
    @FXML private Label seatNumDisplay;
    @FXML private Label dateDisplay;
    @FXML private Label timeDisplay;

    @Override
    
    
    
    public void initialize(URL url, ResourceBundle rb) {
        modPay.getItems().addAll("Credit Card", "Debit Card", "PayPal", "Gcash");
        modPay.setPromptText("Select a payment method");
        modPay.setValue(null);
    }

    public void setCinemaNumber(String cinemaNumber) {
        this.cinemaNumber = cinemaNumber;
        cinemaNumDisplay.setText("Cinema Number: " + cinemaNumber);
    }

    public void setPrice(double price) {
        this.price = price;
        DecimalFormat formatter = new DecimalFormat("₱: #,###");
        priceDisplay.setText("Price: " + formatter.format(price));
    }

    public void setSeatNumber(String seatNumber) {
        this.seatNumber = seatNumber;
        seatNumDisplay.setText("Seat Number: " + seatNumber);
    }

    public void setMovieId(int movieId) {
        this.movieId = movieId;
    }

    public void setBookingDate(String date) {
        this.bookingDate = date;
        dateDisplay.setText("Date: " + date);
    }

    public void setBookingTime(String time) {
        this.bookingTime = time;
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("HH:mm");
            Date date = inputFormat.parse(time);
            SimpleDateFormat outputFormat = new SimpleDateFormat("hh:mm a");
            String formattedTime = outputFormat.format(date);
            timeDisplay.setText("Time: " + formattedTime);
        } catch (ParseException e) {
            e.printStackTrace();
            timeDisplay.setText("Time: " + time);
        }
    }

    public void setMovieTitle(String movieTitle) {
        this.movieTitle = movieTitle;
        movieTitleDisplay.setText("Movie: " + movieTitle);
    }

    public void setBookingDetails(int bookingId, double amount) {
        this.bookingId = bookingId;
        this.amount = amount;
    }

    public void setAmount(String amountStr) {
        try {
            amountStr = amountStr.replace("Price:", "").replace("₱", "").trim();
            this.amount = Double.parseDouble(amountStr);
            priceDisplay.setText("₱" + amountStr);
        } catch (NumberFormatException e) {
            System.err.println("Invalid amount format: " + amountStr);
        }
    }

    @FXML
    private void handleCancel() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Cancel Payment");
        alert.setHeaderText(null);
        alert.setContentText("Are you sure you want to cancel the payment?");
        alert.getDialogPane().getStylesheets().add(getClass().getResource("payment.css").toExternalForm());
        alert.getDialogPane().getStyleClass().add("custom-alert");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                nameF.clear();
                emailF.clear();
                modPay.setValue(null);
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("Main.fxml"));
                    Parent root = loader.load();
                    Stage stage = (Stage) cancelBtn.getScene().getWindow();
                    stage.setScene(new Scene(root));
                    stage.show();
                } catch (Exception e) {
                    System.err.println("Error loading Main.fxml: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }

   @FXML
private void handleConfirm() {
    String name = nameF.getText();
    String email = emailF.getText();
    String payment_method = modPay.getValue();
    String payment_date = java.time.LocalDate.now().toString();
    String payment_time = java.time.LocalTime.now().toString();

    if (name.isEmpty() || email.isEmpty() || payment_method == null || payment_method.trim().isEmpty()) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Incomplete Form");
        alert.setHeaderText(null);
        alert.setContentText("Please fill in all fields.");
        alert.showAndWait();
        return;
    }

    try (Connection conn = DBConnection.getConnection()) {

        // Insert Booking first to get booking_id
        String insertBookingSql = "INSERT INTO bookings (movie_id, seat_number, booking_date, booking_time) VALUES (?, ?, ?, ?)";
        int generatedBookingId = -1;

        try (PreparedStatement bookingStmt = conn.prepareStatement(insertBookingSql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            bookingStmt.setInt(1, movieId);
            bookingStmt.setString(2, seatNumber);
            bookingStmt.setString(3, bookingDate);
            bookingStmt.setString(4, bookingTime);
            bookingStmt.executeUpdate();

            try (ResultSet generatedKeys = bookingStmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    generatedBookingId = generatedKeys.getInt(1);
                }
            }
        }

        if (generatedBookingId == -1) {
            throw new Exception("Failed to retrieve generated booking ID.");
        }

        // Insert Payment
        String insertPaymentSql = "INSERT INTO payments (name, email, payment_method, payment_date, payment_time, movie_id, booking_id) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement paymentStmt = conn.prepareStatement(insertPaymentSql)) {
            paymentStmt.setString(1, name);
            paymentStmt.setString(2, email);
            paymentStmt.setString(3, payment_method);
            paymentStmt.setString(4, payment_date);
            paymentStmt.setString(5, payment_time);
            paymentStmt.setInt(6, movieId);
            paymentStmt.setInt(7, generatedBookingId);
            paymentStmt.executeUpdate();
        }

        // Format the price properly as ₱ without PHP
        DecimalFormat currencyFormat = new DecimalFormat("₱#,###");
        String formattedPrice = currencyFormat.format(price);

        // Format the time to exclude seconds
        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a");
        String formattedTime = timeFormat.format(new SimpleDateFormat("HH:mm").parse(bookingTime));

        // Success Alert
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Payment Confirmed!");
        alert.setHeaderText("Thank you for your payment!");
        alert.setContentText("Movie: " + movieTitle +
                "\nName: " + name +
                "\nEmail: " + email +
                "\nSeat #: " + seatNumber +
                "\nCinema: " + cinemaNumber +
                "\nPrice: " + formattedPrice +
                "\nPayment Method: " + payment_method +
                "\nDate: " + bookingDate +
                "\nTime: " + formattedTime);
        alert.getDialogPane().getStylesheets().add(getClass().getResource("payment.css").toExternalForm());
        alert.getDialogPane().getStyleClass().add("custom-alert");
        alert.showAndWait();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("Main.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) confirmBtn.getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.show();

    } catch (Exception e) {
        System.err.println("Error occurred while saving payment or booking: " + e.getMessage());
        e.printStackTrace();
    }
}


    public void showCustomAlert() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, "This is a custom alert box!", ButtonType.OK);
        alert.setTitle("Custom Alert");
        alert.setHeaderText("Custom Header");
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        alert.getDialogPane().setMinWidth(400);
        alert.getDialogPane().getStylesheets().add(getClass().getResource("custom.css").toExternalForm());
        alert.getDialogPane().getStyleClass().add("custom-alert");
        alert.showAndWait();
    }
}



 