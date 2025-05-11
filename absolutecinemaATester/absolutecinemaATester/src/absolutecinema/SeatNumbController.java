package absolutecinema;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;

/**
 * FXML Controller class
 *
 * @author jaycee
 */
public class SeatNumbController implements Initializable {

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Add any necessary initialization code here
    }  
    
    @FXML
    private Button C1;
    
    @FXML
    private Button returnBtn;
    
    @FXML
    public void handleReturn(ActionEvent event) {
        // Get the current stage (pop-up window) and close it
        Stage stage = (Stage) ((Button) event.getSource()).getScene().getWindow();
        stage.close();
    }
    private String selectedAttribute = "";

    public String getSelectedAttribute() {
        return selectedAttribute;
    }

    @FXML
    private void selectOption(javafx.event.ActionEvent event) {
        // Get the text of the clicked button
        Button clickedButton = (Button) event.getSource();
        selectedAttribute = clickedButton.getText();
        
        System.out.println("Button clicked: " + selectedAttribute);

        // Close the pop-up
        Stage stage = (Stage) clickedButton.getScene().getWindow();
        stage.close();
    }

    
}