package absolutecinema;

import javafx.geometry.Pos;
import javafx.scene.text.Font;
import javafx.scene.layout.BorderPane;
import javafx.fxml.FXML;
import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.stage.Stage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;
import javafx.scene.text.TextAlignment;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javafx.geometry.Insets;
import javafx.scene.Node; // This import is necessary for referencing the source button for event handling


public class AdminMainController {
    @FXML private Button addButton;
    @FXML private HBox movieContainer;
    @FXML private Button shortcutbutton13;
    @FXML private HBox comingContainer1;
    @FXML private Button AddComMovie;
    


    @FXML
    public void initialize() {
        loadMoviesFromDatabase();
        loadComingSoonFromDatabase();
    }

    @FXML
    public void handleStart(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/absolutecinema/AdminEditMovie.fxml")); 
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
    private void handleLogout(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/absolutecinema/WelcomeScreen.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Button) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
            System.out.println("Successfully logged out and returned to WelcomeScreen.");
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Failed to load WelcomeScreen: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    public void loadMoviesFromDatabase() {
        String sql = "SELECT movie_id, movie_name, length, price, image_data FROM movies";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            movieContainer.getChildren().clear();
            movieContainer.setAlignment(Pos.TOP_LEFT);

            while (rs.next()) {
                int movieId = rs.getInt("movie_id");
                String titleText = rs.getString("movie_name");
                int duration = parseDuration(rs.getString("length"));
                byte[] imageBytes = rs.getBytes("image_data");

                // New: handle price safely
                String priceString = rs.getString("price");
                Integer price = null;
                try {
                    price = Integer.parseInt(priceString);
                } catch (NumberFormatException e) {
                    System.err.println("Invalid price format: " + priceString);
                    price = null;
                }

                VBox card = createMovieCard(movieId, titleText, duration, price, imageBytes);
                movieContainer.getChildren().add(card);
            }

        } catch (SQLException e) {
            showAlert("Database error: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

private int parseDuration(String durationText) {
    try {
        durationText = durationText.toLowerCase().replaceAll("\\s+", ""); // normalize text

        if (durationText.contains(":")) {
            String[] parts = durationText.split(":");
            int hours = Integer.parseInt(parts[0]);
            int minutes = Integer.parseInt(parts[1]);
            return (hours * 60) + minutes;
        } else if (durationText.contains("hr") || durationText.contains("min")) {
            int hours = 0;
            int minutes = 0;

            if (durationText.contains("hr")) {
                String[] hrSplit = durationText.split("hr");
                hours = Integer.parseInt(hrSplit[0]);
                durationText = hrSplit.length > 1 ? hrSplit[1] : "";
            }

            if (durationText.contains("min")) {
                String[] minSplit = durationText.split("min");
                minutes = Integer.parseInt(minSplit[0]);
            }

            return (hours * 60) + minutes;
        }

        return Integer.parseInt(durationText); // fallback: assume it's just minutes
    } catch (Exception e) {
        System.err.println("Invalid duration format: " + durationText);
        return 0;
    }
}

    private String formatDuration(int totalMinutes) {
        int hours = totalMinutes / 60;
        int minutes = totalMinutes % 60;

        String hourPart = (hours > 0) ? hours + " hour" + (hours > 1 ? "s" : "") : "";
        String minutePart = (minutes > 0) ? minutes + " minute" + (minutes > 1 ? "s" : "") : "";

        if (!hourPart.isEmpty() && !minutePart.isEmpty()) {
            return hourPart + " and " + minutePart;
        } else {
            return hourPart + minutePart;
        }
    }

    private VBox createMovieCard(int movieId, String titleText, int duration, Integer price, byte[] imageBytes) {
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
        imageView.setImage(imageBytes != null ? new Image(new ByteArrayInputStream(imageBytes)) 
                                              : new Image("placeholder.png"));

        frame.setCenter(imageView);

        Label titleLabel = createLabel(titleText, 21);
        Label durationLabel = createLabel(formatDuration(duration), 21);
        Label priceLabel = createLabel("Price: ₱" + (price != null ? price : "N/A"), 21);

        Button editButton = createEditButton(movieId);

        card.getChildren().addAll(frame, titleLabel, durationLabel, priceLabel, editButton);
        return card;
    }

    private Label createLabel(String text, int fontSize) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: white;");
        label.setFont(Font.font("Microsoft Sans Serif", fontSize));
        label.setWrapText(true);
        label.setMaxWidth(260);
        label.setAlignment(Pos.CENTER);
        label.setTextAlignment(TextAlignment.CENTER);
        return label;
    }

    private Button createEditButton(int movieId) {
        Button editButton = new Button("EDIT");
        editButton.setStyle("-fx-background-color: gold; -fx-text-fill: black; -fx-font-weight: bold;");
        editButton.setPrefWidth(100);

        VBox.setMargin(editButton, new Insets(20, 0, 0, 95));
        editButton.setOnAction(e -> {
            System.out.println("Edit movie ID: " + movieId);

            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/absolutecinema/AdminEditMovie.fxml"));
                Parent root = loader.load();

                AdminEditMovieController controller = loader.getController();
                controller.setMovieId(movieId);

                Stage stage = (Stage) ((Button) e.getSource()).getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.show();
            } catch (IOException f) {
                showAlert("Failed to load AdminEditMovie.fxml: " + f.getMessage(), Alert.AlertType.ERROR);
            }
        });

        return editButton;
    }

    private void showAlert(String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void handleAddMovie(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/absolutecinema/AdminActualBooking.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Button) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            System.err.println("Error loading AdminActualBooking.fxml: " + e.getMessage());
            showAlert("Failed to load AddMovie screen: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
    /**COMING SOON DATA RETRIEVAL SECTIONZZZZZZZ*/
    
  @FXML
private void handleAddComMovie(ActionEvent event) {
    try {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/absolutecinema/AdminAddComing.fxml"));
        Parent root = loader.load();

        Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();  
        stage.setScene(new Scene(root));
        stage.show();
    } catch (IOException e) {
        System.err.println("Error loading AdminAddComing.fxml: " + e.getMessage());
        showAlert("Failed to load Add Coming Soon screen: " + e.getMessage(), Alert.AlertType.ERROR);
    }
}

    public void loadComingSoonFromDatabase() {
    String sql = "SELECT com_id, movie_name, length, price, image_data FROM coming_soon";

    try (Connection conn = DBConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql);
         ResultSet rs = stmt.executeQuery()) {

        comingContainer1.getChildren().clear();
        comingContainer1.setAlignment(Pos.TOP_LEFT);

        while (rs.next()) {
            int comId = rs.getInt("com_id");
            String titleText = rs.getString("movie_name");
            int duration = parseDuration(rs.getString("length"));
            byte[] imageBytes = rs.getBytes("image_data");

            String priceString = rs.getString("price");
            Integer price = null;
            try {
                price = Integer.parseInt(priceString);
            } catch (NumberFormatException e) {
                System.err.println("Invalid price format: " + priceString);
            }

            VBox card = createComingSoonCard(comId, titleText, duration, price, imageBytes);
            comingContainer1.getChildren().add(card);
        }

    } catch (SQLException e) {
        showAlert("Database error: " + e.getMessage(), Alert.AlertType.ERROR);
    }
}

    private VBox createComingSoonCard(int comId, String titleText, int duration, Integer price, byte[] imageBytes) {
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
    imageView.setImage(imageBytes != null ? new Image(new ByteArrayInputStream(imageBytes)) 
                                          : new Image("placeholder.png"));

    frame.setCenter(imageView);

    Label titleLabel = createLabel(titleText, 21);
    Label durationLabel = createLabel(formatDuration(duration), 21);
    Label priceLabel = createLabel("Price: ₱" + (price != null ? price : "N/A"), 21);

    Button editButton = createComingSoonEditButton(comId);

    card.getChildren().addAll(frame, titleLabel, durationLabel, priceLabel, editButton);
    return card;
}

    
    private Button createComingSoonEditButton(int comId) {
    Button editButton = new Button("EDIT");
    editButton.setStyle("-fx-background-color: gold; -fx-text-fill: black; -fx-font-weight: bold;");
    editButton.setPrefWidth(100);

    VBox.setMargin(editButton, new Insets(0, 0, 0, 95));
    editButton.setOnAction(e -> {
        System.out.println("Edit coming soon movie ID: " + comId);

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/absolutecinema/AdminEditComing.fxml"));
Parent root = loader.load();
AdminEditComingController controller = loader.getController();
controller.setComingSoonId(comId);  // This sets the ID and triggers data loading

Stage stage = new Stage();
stage.setScene(new Scene(root));
stage.show();

        } catch (IOException f) {
            showAlert("Failed to load AdminEditComing.fxml: " + f.getMessage(), Alert.AlertType.ERROR);
        }
    });

    return editButton;
}
    
    /*GO TO HOME*/
    
    @FXML
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
