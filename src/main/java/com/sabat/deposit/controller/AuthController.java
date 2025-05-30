package com.sabat.deposit.controller;

import com.sabat.deposit.model.User;
import com.sabat.deposit.navigation.NavigationManager;
import com.sabat.deposit.service.UserService;
import com.sabat.deposit.session.Session;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;

public class AuthController {

    private static final Logger logger = LogManager.getLogger(AuthController.class);

    @FXML private TextField nameField;
    @FXML private TextField surnameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private ImageView logoImage;
    @FXML private Label errorLabel;

    @FXML
    public void initialize() {
        try {
            logoImage.setImage(new Image(getClass().getResource("/com/sabat/deposit/images/reg.png").toExternalForm()));
        } catch (Exception e) {
            logger.error("Не вдалося завантажити логотип: {}", e.getMessage());
        }
    }








    @FXML
    protected void onRegisterClick(ActionEvent event) {
        String name = nameField.getText().trim();
        String surname = surnameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText().trim();

        String validationError = validateRegistrationForm(name, surname, email, password);
        if (validationError != null) {
            logger.warn("Помилка валідації форми: {}", validationError);
            displayError(validationError);
            return;
        }

        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
        User user = new User(0, name, surname, email, hashedPassword);

        if (!performRegistration(user)) {
            logger.warn("Реєстрація не вдалася для email: {}", email);
            return;
        }

        logger.info("Користувача зареєстровано успішно: {}", email);
        displayError("");

        try {
            NavigationManager.switchScene(event, "/com/sabat/deposit/views/account-view.fxml");
            logger.info("Перехід до account-view.fxml після реєстрації.");
        } catch (IOException e) {
            logger.error("Не вдалося завантажити account-view.fxml: {}", e.getMessage());
            displayError("Не вдалося відкрити акаунт.");
        }
    }

    private boolean performRegistration(User user) {
        if (UserService.userExists(user.getEmail())) {
            displayError("Користувач з таким email вже існує.");
            return false;
        }

        boolean success = UserService.registerUser(user);
        if (!success) {
            displayError("Помилка при реєстрації користувача.");
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
            logger.info("Перехід до login-view.fxml");
        } catch (IOException e) {
            logger.error("Помилка при завантаженні login-view.fxml: {}", e.getMessage());
        }
    }




    private String validateRegistrationForm(String name, String surname, String email, String password) {
        if (name.isEmpty() || surname.isEmpty() || email.isEmpty() || password.isEmpty()) {
            return "❌ Будь ласка, заповніть всі поля!";
        }

        if (!email.matches("^[\\w.-]+@[\\w.-]+\\.\\w{2,}$")) {
            logger.info("Спроба входу з неправильним форматом email: {}", email);
            return("❌ Невірний формат email.");
        }
        return null;
    }







    private void displayError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(!message.isEmpty());
    }



}
