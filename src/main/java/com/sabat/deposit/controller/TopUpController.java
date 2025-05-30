package com.sabat.deposit.controller;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.sabat.deposit.model.User;
import com.sabat.deposit.service.UserService;
import com.sabat.deposit.session.Session;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.animation.PauseTransition;
import javafx.util.Duration;

import java.io.IOException;

public class TopUpController {

    private static final Logger logger = LogManager.getLogger(TopUpController.class);

    @FXML private TextField amountField;
    @FXML private Button confirmButton;
    @FXML private Label errorLabel;

    private AccountController accountController;

    public void setAccountController(AccountController controller) {
        this.accountController = controller;
    }



    @FXML
    protected void onConfirmClick(ActionEvent event) {
        User user = Session.getUser();
        String resultMessage = UserService.topUpBalance(user, amountField.getText());

        if (resultMessage.startsWith("✅")) {
            showSuccess(resultMessage);
            accountController.updateBalance();

            PauseTransition pause = new PauseTransition(Duration.seconds(1));
            pause.setOnFinished(e -> ((Node) event.getSource()).getScene().getWindow().hide());
            pause.play();
        } else {
            showError(resultMessage);
        }
    }


    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setStyle("-fx-text-fill: red;");
    }



    private void showSuccess(String message) {
        errorLabel.setText(message);
        errorLabel.setStyle("-fx-text-fill: green;");
    }



}