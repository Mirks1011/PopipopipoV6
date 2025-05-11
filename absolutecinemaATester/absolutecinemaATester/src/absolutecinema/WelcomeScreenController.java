package absolutecinema;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

/**
 * FXML Controller class
 *
 * @author jaycee
 */
public class WelcomeScreenController implements Initializable {

    private boolean isEPressed = false; // Flag to track key press

    @FXML
    private Button start; // Ensure this fx:id matches your FXML

    // Initialize method for tracking SHIFT key and preparing the scene
    @Override
public void initialize(URL location, ResourceBundle resources) {
    // Delay initialization to ensure the scene is available
    Platform.runLater(() -> {
        if (start != null && start.getScene() != null) {
            Scene scene = start.getScene();

            // Listen for key press
            scene.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.E) {
                    isEPressed = true;
                }
            });

            // Listen for key release
            scene.setOnKeyReleased(event -> {
                if (event.getCode() == KeyCode.E) {
                    isEPressed = false;
                }
            });
        } else {
            System.err.println("Scene or button is not properly initialized.");
        }
    });
}

    @FXML
    private void handleStart(ActionEvent event) {
        if (isEPressed) {
            System.out.println("Button clicked while holding E!");
            
            
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/absolutecinema/LoginPage.fxml"));
                Parent root = loader.load();

                // Get the current stage
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

                // Set the new scene
                Scene scene = new Scene(root);
                stage.setScene(scene);
                stage.show();

            } catch (IOException e) {
                System.err.println("Error loading Main.fxml: " + e.getMessage());
                e.printStackTrace();
            }

        } else {
            System.out.println("Button clicked normally.");
            
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/absolutecinema/Main.fxml"));
                Parent root = loader.load();

                // Get the current stage
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

                // Set the new scene
                Scene scene = new Scene(root);
                stage.setScene(scene);
                stage.show();

            } catch (IOException e) {
                System.err.println("Error loading Main.fxml: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}