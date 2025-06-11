package com.sabat.deposit.controller;

import com.sabat.deposit.model.User;
import com.sabat.deposit.navigation.NavigationManager;
import com.sabat.deposit.service.UserService;
import com.sabat.deposit.session.Session;
import com.sabat.deposit.util.Logger;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.IOException;

public class LoginController {

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
            Logger.error("Не вдалося завантажити логотип: {}", e.getMessage());
        }
    }

    @FXML
    protected void onLoginClick(ActionEvent event) {
        hideError();

        String email = emailField.getText().trim();
        String password = passwordField.getText().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Logger.info("Спроба входу з порожніми полями.");
            showError("❌ Введіть email і пароль для входу.");
            return;
        }

        User user = UserService.loginUser(email, password);

        if (user != null) {
            Session.setUser(user);
            Logger.info("Успішний вхід: користувач ID=" + user.getId() + ", email=" + user.getEmail());

            try {
                if ("admin".equals(user.getRole())) {
                    NavigationManager.switchScene(event, "/com/sabat/deposit/views/admin-view.fxml");
                    Logger.info("Вхід адміністратора");
                } else {
                    NavigationManager.switchScene(event, "/com/sabat/deposit/views/account-view.fxml");
                }
            } catch (IOException e) {
                showError("⚠️ Помилка завантаження інтерфейсу кабінету.");
            }
        } else {
            Logger.info("Невдала спроба входу з email: " + email);
            showError("❌ Невірний email або пароль.");
        }
    }

    private void showError(String message) {
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setVisible(true);
            errorLabel.getStyleClass().add("error-label");
        } else {
            Logger.error("Помилка: errorLabel є null. Неможливо показати повідомлення: {}", message);
        }
    }

    private void hideError() {
        if (errorLabel != null) {
            errorLabel.setVisible(false);
        }
    }

    @FXML
    private void onRegisterClick(ActionEvent event) {
        Logger.info("Користувач натиснув кнопку 'Реєстрація'");
        try {
            NavigationManager.switchScene(event, "/com/sabat/deposit/views/auth-view.fxml");
        } catch (IOException e) {
            showError("⚠️ Помилка завантаження сторінки реєстрації.");
        }
    }
}
