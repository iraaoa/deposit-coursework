package com.sabat.deposit.controller;

import com.sabat.deposit.model.User;
import com.sabat.deposit.navigation.NavigationManager;
import com.sabat.deposit.service.UserService;
import com.sabat.deposit.session.Session;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LoginController {

    private static final Logger logger = LogManager.getLogger(LoginController.class);


    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private ImageView logoImage;




    @FXML
    public void initialize() {
        hideError();

        try {
            logoImage.setImage(new Image(getClass().getResource("/com/sabat/deposit/images/reg.png").toExternalForm()));
        } catch (Exception e) {
            logger.warn("Не вдалося завантажити логотип: {}", e.getMessage());
        }
    }





    @FXML
    protected void onLoginClick(ActionEvent event) {
        hideError();

        String email = emailField.getText().trim();
        String password = passwordField.getText().trim();

        if (email.isEmpty() || password.isEmpty()) {
            logger.info("Спроба входу з порожніми полями.");
            showError("❌ Введіть email і пароль для входу.");
            return;
        }

        logger.info("Спроба входу користувача з email: {}", email);

        User user = UserService.loginUser(email, password);

        if (user != null) {
            Session.setUser(user);
            logger.info("Успішний вхід: користувач ID={}, email={}", user.getId(), user.getEmail());

            try {

                if (user.getRole().equals("admin")) {
                    NavigationManager.switchScene(event, "/com/sabat/deposit/views/admin-view.fxml");
                    logger.info("Вхід адміністратора");

                }  else {
                    NavigationManager.switchScene(event, "/com/sabat/deposit/views/account-view.fxml");
                }
            } catch (IOException e) {
                logger.error("Помилка при завантаженні account-view.fxml: {}", e.getMessage());
                showError("⚠️ Помилка завантаження інтерфейсу кабінету.");
            }

        } else {
            logger.warn("Невдала спроба входу з email: {}", email);
            showError("❌ Невірний email або пароль.");
        }
    }



    private void showError(String message) {
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setVisible(true);
            errorLabel.getStyleClass().add("error-label");

        } else {
            logger.error("Помилка: errorLabel є null. Неможливо показати повідомлення: {}", message);
        }
    }






    private void hideError() {
        if (errorLabel != null) {
            errorLabel.setVisible(false);
        }
    }





    @FXML
    private void onRegisterClick(ActionEvent event) {
        logger.info("Користувач натиснув кнопку 'Реєстрація'");

        try {
            NavigationManager.switchScene(event, "/com/sabat/deposit/views/auth-view.fxml");
        } catch (IOException e) {
            logger.error("Помилка при завантаженні auth-view.fxml: {}", e.getMessage());
            showError("⚠️ Помилка завантаження сторінки реєстрації.");
        }
    }

}
