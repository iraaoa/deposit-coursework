package com.sabat.deposit.controller;

import com.sabat.deposit.model.User;
import com.sabat.deposit.navigation.NavigationManager;
import com.sabat.deposit.session.Session;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import com.sabat.deposit.util.Logger;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.Locale;

import javafx.animation.FadeTransition;
import javafx.util.Duration;
import javafx.scene.layout.StackPane;

public class AccountController {

    @FXML private Label balanceLabel;
    @FXML private Label nameLabel;
    @FXML private Label cardNameLabel;
    @FXML private StackPane cardPane;
    @FXML private ImageView logoImage;

    @FXML
    public void initialize() {
        User user = Session.getUser();
        if (user != null) {
            nameLabel.setText(user.getName());
            NumberFormat nf = NumberFormat.getNumberInstance(new Locale("uk", "UA"));
            nf.setMinimumFractionDigits(2);
            nf.setMaximumFractionDigits(2);
            String formattedBalance = nf.format(user.getBalance());
            balanceLabel.setText("₴ " + formattedBalance);
            String fullNameUpper = (user.getName() + " " + user.getSurname()).toUpperCase();
            cardNameLabel.setText(fullNameUpper);
        } else {
            showAlert("Помилка", "Користувача не знайдено. Будь ласка, увійдіть повторно.");
        }

        logoImage.setImage(new Image(getClass().getResource("/com/sabat/deposit/images/reg.png").toExternalForm()));
        animateCardFadeIn();
    }

    private void animateCardFadeIn() {
        if (cardPane != null) {
            cardPane.setOpacity(0);
            FadeTransition fade = new FadeTransition(Duration.millis(2000), cardPane);
            fade.setFromValue(0);
            fade.setToValue(1);
            fade.play();
        }
    }

    public void updateBalance() {
        User user = Session.getUser();
        if (user != null) {
            NumberFormat nf = NumberFormat.getNumberInstance(new Locale("uk", "UA"));
            nf.setMinimumFractionDigits(2);
            nf.setMaximumFractionDigits(2);
            String formattedBalance = nf.format(user.getBalance());
            balanceLabel.setText("₴ " + formattedBalance);
        } else {
        }
    }

    @FXML
    protected void onTopUpClick(ActionEvent event) {
        User user = Session.getUser();
        String userId = (user != null) ? String.valueOf(user.getId()) : "невідомий";
        Logger.info("Користувач " + userId + " натиснув кнопку 'Поповнити'.");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/sabat/deposit/views/topup-view.fxml"));
            Parent topUpRoot = loader.load();
            TopUpController topUpController = loader.getController();
            topUpController.setAccountController(this);
            Scene topUpScene = new Scene(topUpRoot);
            Stage stage = new Stage();
            stage.setScene(topUpScene);
            stage.setTitle("Поповнення балансу");
            stage.showAndWait();
        } catch (IOException e) {
            showAlert("Помилка", "Не вдалося відкрити вікно поповнення.");
        }
    }

    @FXML
    public void onDepositsClick(ActionEvent event) {
        User user = Session.getUser();
        String userId = (user != null) ? String.valueOf(user.getId()) : "невідомий";
        Logger.info("Користувач " + userId + " натиснув кнопку 'Депозити', перехід до списку депозитів.");
        try {
            NavigationManager.switchScene(event, "/com/sabat/deposit/views/deposits-view.fxml");
        } catch (IOException e) {
            Logger.error("Не вдалося відкрити deposits-view.fxml для користувача " + userId + ": " + e.getMessage(), "");
            showAlert("Помилка", "Не вдалося відкрити сторінку депозитів.");
        }
    }

    @FXML
    protected void onMyDepositsClick(ActionEvent event) {
        User user = Session.getUser();
        String userId = (user != null) ? String.valueOf(user.getId()) : "невідомий";
        Logger.info("Користувач " + userId + " натиснув кнопку 'Мої депозити'.");
        try {
            NavigationManager.switchScene(event, "/com/sabat/deposit/views/my-deposits-view.fxml");
        } catch (IOException e) {
            Logger.error("Помилка відкриття my-deposits-view.fxml для користувача " + userId + ": " + e.getMessage(), "");
            showAlert("Помилка", "Не вдалося відкрити сторінку 'Мої депозити'.");
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void onHistoryClick(ActionEvent event) {
        User currentUser = Session.getUser();
        String userIdLog = (currentUser != null) ? String.valueOf(currentUser.getId()) : "невідомий";
        Logger.info("Користувач " + userIdLog + " натиснув кнопку 'Історія'.");

        if (currentUser == null) {
            showAlert("Помилка", "Користувача не знайдено. Будь ласка, увійдіть повторно.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/sabat/deposit/views/history.fxml"));
            Parent root = loader.load();

            HistoryController controller = loader.getController();
            int currentUserId = currentUser.getId();
            controller.setUserId(currentUserId);

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Історія транзакцій");
            stage.show();
        } catch (IOException e) {
            showAlert("Помилка", "Не вдалося відкрити історію транзакцій.");
        }
    }

    @FXML
    private void onLogoutClick(ActionEvent event) {
        User user = Session.getUser();
        String userId = (user != null) ? String.valueOf(user.getId()) : "невідомий";
        Session.clear();
        try {
            NavigationManager.switchScene(event, "/com/sabat/deposit/views/login-view.fxml");
            Logger.info("Користувач " + userId + " успішно вийшов, перенаправлено на сторінку входу.");
        } catch (IOException e) {
            showAlert("Помилка", "Не вдалося повернутися на сторінку входу.");
        }
    }
}
