package absolutecinema;

import static absolutecinema.ActualBookingController.seatNumber;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.geometry.Pos;

public class MainController {
    @FXML private Label exit;
    @FXML private ComboBox<String> schedule;
    @FXML private ComboBox<String> loc;
    @FXML private ComboBox<String> mov;
    @FXML private ComboBox<String> date;
    @FXML private ComboBox<String> time;
    @FXML private Button seatno;
    @FXML private Button logoutBtn;
    @FXML private Button shortcutbutton;
    @FXML private Button start;
    @FXML private Button addButton;
    @FXML private HBox movieContainer;
    @FXML private Button prodpay;
    @FXML private HBox comingContainer1;

    @FXML
    public void initialize() {
        loadComboBoxData();
        loadMoviesFromDatabase();
        loadComingSoonFromDatabase();
    }

    @FXML
    public void handleStart(ActionEvent event) {
        switchToScene(event, "/absolutecinema/Payment.fxml");
    }

    @FXML
private void handleExitClick() {
    try {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/absolutecinema/WelcomeScreen.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) exit.getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.show();
        System.out.println("Returned to WelcomeScreen.");
    } catch (IOException e) {
        e.printStackTrace();
        showAlert("Failed to load WelcomeScreen: " + e.getMessage(), Alert.AlertType.ERROR);
    }
}

    
    
    private void loadComboBoxData() {
        try (Connection conn = DBConnection.getConnection()) {
            if (conn == null) return;

            Statement stmt = conn.createStatement();
            ResultSet rs;

            // Load movie names
            rs = stmt.executeQuery("SELECT DISTINCT movie_name FROM movies");
            while (rs.next()) mov.getItems().add(rs.getString("movie_name"));

            // Load locations (cinema_number)
            rs = stmt.executeQuery("SELECT DISTINCT cinema_number FROM movies");
            while (rs.next()) loc.getItems().add(rs.getString("cinema_number"));

            // Load schedules (date)
            rs = stmt.executeQuery("SELECT DISTINCT date FROM movies");
            while (rs.next()) schedule.getItems().add(rs.getString("date"));

            // Load times
            rs = stmt.executeQuery("SELECT DISTINCT time FROM movies");
            while (rs.next()) time.getItems().add(rs.getString("time"));

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error loading combo box data: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

@FXML
private void handleProceedToPayment(ActionEvent event) {
    boolean anyEmpty = false;
    StringBuilder emptyFields = new StringBuilder();

    String selectedMovie = mov.getValue();
    String selectedLocation = loc.getValue();
    String selectedDate = schedule.getValue();
    String selectedTime = time.getValue();
    String selectedSeat = seatno.getText();

    if (selectedMovie == null) {
        anyEmpty = true;
        emptyFields.append("Movie\n");
    }
    if (selectedLocation == null) {
        anyEmpty = true;
        emptyFields.append("Location\n");
    }
    if (selectedDate == null) {
        anyEmpty = true;
        emptyFields.append("Date\n");
    }
    if (selectedTime == null) {
        anyEmpty = true;
        emptyFields.append("Time\n");
    }
    if ("Seat".equals(selectedSeat) || selectedSeat.trim().isEmpty()) {
        anyEmpty = true;
        emptyFields.append("Seat\n");
    }

    if (anyEmpty) {
        showAlert("Please select the following fields:\n" + emptyFields, Alert.AlertType.WARNING);
        return;
    }

    try (Connection conn = DBConnection.getConnection()) {
        if (conn == null) {
            showAlert("Database connection failed.", Alert.AlertType.ERROR);
            return;
        }

        // 🔎 Get movie_id, price, cinema_number
        String fetchMovieIdSql = "SELECT movie_id, price, cinema_number FROM movies WHERE movie_name = ? AND date = ? AND time = ?";
        PreparedStatement fetchStmt = conn.prepareStatement(fetchMovieIdSql);
        fetchStmt.setString(1, selectedMovie);
        fetchStmt.setString(2, selectedDate);
        fetchStmt.setString(3, selectedTime);

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
        checkStmt.setString(2, selectedSeat);
        ResultSet seatCheck = checkStmt.executeQuery();

        if (seatCheck.next()) {
            showAlert("This seat has already been booked. Please select a different seat.", Alert.AlertType.WARNING);
            return;
        }

        // 👉 Proceed to Payment.fxml with collected data
        FXMLLoader loader = new FXMLLoader(getClass().getResource("Payment.fxml"));
        Parent root = loader.load();
        PaymentController controller = loader.getController();

        // Pass all needed data to PaymentController
        controller.setMovieId(movieId);
        controller.setMovieTitle(selectedMovie);
        controller.setSeatNumber(selectedSeat);
        controller.setBookingDate(java.time.LocalDate.now().toString());
        controller.setBookingTime(java.time.LocalTime.now().toString());
        controller.setCinemaNumber(cinemaNumber);
        controller.setPrice(price);

        Stage stage = (Stage) prodpay.getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.show();

    } catch (SQLException | IOException e) {
        e.printStackTrace();
        showAlert("Failed to proceed to payment: " + e.getMessage(), Alert.AlertType.ERROR);
    }
}


    private void switchToScene(ActionEvent event, String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) ((Button) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            System.err.println("Error loading scene: " + e.getMessage());
            e.printStackTrace();
        }
    }

    

    private void showAlert(String message, Alert.AlertType alertType) {
        Alert alert = new Alert(alertType);
        alert.setTitle("Action Info");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
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

        String selectedSeat = popupController.getSelectedAttribute();
        if (selectedSeat != null && !selectedSeat.isEmpty()) {
            seatno.setText(selectedSeat);
        }
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/absolutecinema/LoginPage.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Button) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            System.err.println("Error loading LoginPage.fxml: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadMoviesFromDatabase() {
    try (Connection conn = DBConnection.getConnection()) {
        String sql = "SELECT movie_name, length, image_data, price, synopsis FROM movies";
        PreparedStatement stmt = conn.prepareStatement(sql);
        ResultSet rs = stmt.executeQuery();

        movieContainer.getChildren().clear();
        movieContainer.setAlignment(Pos.TOP_LEFT);

        while (rs.next()) {
            String titleText = rs.getString("movie_name");
            String durationText = rs.getString("length");
             String priceString = rs.getString("price");
                Integer price = null;
                try {
                    price = Integer.parseInt(priceString);
                } catch (NumberFormatException e) {
                    System.err.println("Invalid price format: " + priceString);
                    price = null;
                }
            String synopsis = rs.getString("synopsis");  // Added synopsis retrieval
            byte[] imageBytes = rs.getBytes("image_data");

            VBox card = new VBox(10);
            card.setPrefWidth(300);
            card.setStyle("-fx-background-color: black; -fx-padding: 10;");
            card.setAlignment(Pos.TOP_LEFT);

            BorderPane frame = new BorderPane();
            frame.setStyle("-fx-border-color: gold; -fx-border-width: 10; -fx-background-color: gold;");
            frame.setPrefSize(276, 338);
            frame.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

            ImageView imageView = new ImageView();
            imageView.setFitWidth(242);
            imageView.setFitHeight(313);
            imageView.setPreserveRatio(true);
            if (imageBytes != null) {
                imageView.setImage(new Image(new ByteArrayInputStream(imageBytes)));
            }
            frame.setCenter(imageView);

            Label titleLabel = new Label(titleText);
            titleLabel.setStyle("-fx-text-fill: white;");
            titleLabel.setFont(Font.font("Microsoft Sans Serif", 21));
            titleLabel.setWrapText(true);
            titleLabel.setMaxWidth(260);
            titleLabel.setAlignment(Pos.CENTER);
            titleLabel.setTextAlignment(TextAlignment.CENTER);

           Label durationLabel = new Label(formatDurationText(durationText));
durationLabel.setStyle("-fx-text-fill: white;");
durationLabel.setFont(Font.font("Microsoft Sans Serif", 21));
durationLabel.setWrapText(true);  // Allow multiline if needed
durationLabel.setMaxWidth(260);   // Increase width to allow longer text
durationLabel.setAlignment(Pos.CENTER);
durationLabel.setTextAlignment(TextAlignment.CENTER);
VBox.setMargin(durationLabel, new Insets(0, 15, 0, 15)); // Reduce left inset


           String formattedPrice = (price != null) ? String.format("₱%,d", price) : "₱N/A";
Label priceLabel = new Label("Price: " + formattedPrice);
priceLabel.setStyle("-fx-text-fill: white;");
priceLabel.setFont(Font.font("Microsoft Sans Serif", 21));
VBox.setMargin(priceLabel, new Insets(0, 15, 0, 60));
priceLabel.setAlignment(Pos.CENTER);
priceLabel.setTextAlignment(TextAlignment.CENTER);


            Button editButton = new Button("BUY TICKETS");
            editButton.setStyle("-fx-background-color: gold; -fx-text-fill: black; -fx-font-weight: bold;");
            VBox.setMargin(editButton, new Insets(10, 0, 0, 70));
            editButton.setPrefWidth(150);
            editButton.setOnAction(e -> {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/absolutecinema/ActualBooking.fxml"));
                    Parent root = loader.load();

                    // Pass movie data to controller
                    ActualBookingController bookingController = loader.getController();
                   bookingController.setMovieDetails(titleText, durationText, priceString, imageBytes, synopsis);


                    // Show the booking scene
                    Stage stage = (Stage) ((Button) e.getSource()).getScene().getWindow();
                    stage.setScene(new Scene(root));
                    stage.show();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            });

            card.getChildren().addAll(frame, titleLabel, durationLabel, priceLabel, editButton);
            movieContainer.getChildren().add(card);
        }

    } catch (SQLException e) {
        e.printStackTrace();
    }
}
    private String formatDurationText(String rawDuration) {
    try {
        int totalMinutes;

        if (rawDuration.contains(":")) {
            String[] parts = rawDuration.split(":");
            int hours = Integer.parseInt(parts[0]);
            int minutes = Integer.parseInt(parts[1]);
            totalMinutes = hours * 60 + minutes;
        } else {
            totalMinutes = Integer.parseInt(rawDuration);
        }

        int hours = totalMinutes / 60;
        int minutes = totalMinutes % 60;

        StringBuilder formatted = new StringBuilder();
        if (hours > 0) {
            formatted.append(hours).append(" hour").append(hours > 1 ? "s" : "");
        }
        if (minutes > 0) {
            if (formatted.length() > 0) {
                formatted.append(" and ");
            }
            formatted.append(minutes).append(" minute").append(minutes > 1 ? "s" : "");
        }
        return formatted.toString();

    } catch (NumberFormatException e) {
        System.err.println("Invalid duration format: " + rawDuration);
        return rawDuration; // Fallback
    }
}
    
    /* COMING SOON PART*////
      private void loadComingSoonFromDatabase() {
    String sql = "SELECT com_id, movie_name, image_data FROM coming_soon";

    try (Connection conn = DBConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql);
         ResultSet rs = stmt.executeQuery()) {

        comingContainer1.getChildren().clear();
        comingContainer1.setAlignment(Pos.TOP_LEFT);

        while (rs.next()) {
            int comId = rs.getInt("com_id");
            String titleText = rs.getString("movie_name");
            byte[] imageBytes = rs.getBytes("image_data");

            VBox card = createComingSoonCard(comId, titleText, imageBytes);
            comingContainer1.getChildren().add(card);
        }

    } catch (SQLException e) {
        showAlert("Database error: " + e.getMessage(), Alert.AlertType.ERROR);
    }
}


   private VBox createComingSoonCard(int comId, String titleText, byte[] imageBytes) {
    VBox card = new VBox(10);
    card.setPrefWidth(300);
    card.setStyle("-fx-background-color: black; -fx-padding: 10;");
    card.setAlignment(Pos.TOP_LEFT);

    BorderPane frame = new BorderPane();
    frame.setStyle("-fx-border-color: gold; -fx-border-width: 10; -fx-background-color: gold;");
    frame.setPrefSize(276, 338);
    frame.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

    ImageView imageView = new ImageView();
    imageView.setFitWidth(242);
    imageView.setFitHeight(313);
    imageView.setPreserveRatio(true);
    imageView.setImage(imageBytes != null 
        ? new Image(new ByteArrayInputStream(imageBytes)) 
        : new Image("placeholder.png"));

    frame.setCenter(imageView);

    Label titleLabel = createLabel(titleText, 21);
    card.getChildren().addAll(frame, titleLabel);

    return card;
}


// Add this helper method inside the same class (outside other methods)
private Label createLabel(String text, int fontSize) {
    Label label = new Label(text);
    label.setFont(Font.font("System", FontWeight.BOLD, fontSize));
    label.setTextFill(Color.WHITE); // Optional: to ensure text is visible on black background
    return label;
}

    
}


