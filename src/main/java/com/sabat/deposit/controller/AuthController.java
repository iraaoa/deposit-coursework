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
import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;

public class AuthController {

    @FXML private TextField nameField;
    @FXML private TextField surnameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private ImageView logoImage;
    @FXML private Label errorLabel;

    @FXML
    public void initialize() {
        try {
            Image logo = new Image(getClass().getResource("/com/sabat/deposit/images/reg.png").toExternalForm());
            logoImage.setImage(logo);
        } catch (Exception e) {
            Logger.error("Не вдалося завантажити логотип: {}", e.getMessage());
        }
    }

    @FXML
    protected void onRegisterClick(ActionEvent event) {
        String name = nameField.getText().trim();
        String surname = surnameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText().trim();

        String validationMessage = validateForm(name, surname, email, password);
        if (validationMessage != null) {
            Logger.error("Помилка валідації реєстрації: {}", validationMessage);
            displayError(validationMessage);
            return;
        }

        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
        User user = new User(0, name, surname, email, hashedPassword);

        if (!registerUser(user)) {
            Logger.error("Не вдалося зареєструвати користувача з email: {}", email);
            return;
        }

        displayError("");
        Logger.info("Користувач успішно зареєстрований: " + email);

        try {
            NavigationManager.switchScene(event, "/com/sabat/deposit/views/account-view.fxml");
            Logger.info("Перехід до головної сторінки після реєстрації.");
        } catch (IOException e) {
            Logger.error("Не вдалося завантажити account-view.fxml: {}", e.getMessage());
            displayError("Не вдалося відкрити акаунт.");
        }
    }

    private boolean registerUser(User user) {
        if (UserService.userExists(user.getEmail())) {
            displayError("Користувач з таким email вже існує.");
            return false;
        }

        if (!UserService.registerUser(user)) {
            displayError("Не вдалося зареєструвати користувача.");
            return false;
        }

        int userId = UserService.getUserIdByEmail(user.getEmail());
        if (userId == -1) {
            displayError("Не вдалося отримати ID користувача.");
            return false;
        }

        user.setId(userId);
        Session.setUser(user);
        return true;
    }

    @FXML
    protected void onLoginClick(ActionEvent event) {
        try {
            NavigationManager.switchScene(event, "/com/sabat/deposit/views/login-view.fxml");
            Logger.info("Перехід до login-view.fxml");
        } catch (IOException e) {
            Logger.error("Не вдалося завантажити login-view.fxml: {}", e.getMessage());
        }
    }

    private String validateForm(String name, String surname, String email, String password) {
        if (name.isEmpty() || surname.isEmpty() || email.isEmpty() || password.isEmpty()) {
            return "❌ Будь ласка, заповніть всі поля!";
        }

        if (!email.matches("^[\\w.-]+@[\\w.-]+\\.\\w{2,}$")) {
            Logger.info("Невірний формат email: " + email);
            return "❌ Невірний формат email.";
        }

        return null;
    }

    private void displayError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(!message.isEmpty());
    }
}
