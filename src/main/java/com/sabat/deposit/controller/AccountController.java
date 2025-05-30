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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class AccountController {

    private static final Logger logger = LogManager.getLogger(AccountController.class);

    @FXML private Label balanceLabel;
    @FXML private Label nameLabel;
    @FXML
    private ImageView cardImageView;



    @FXML
    public void initialize() {
        logger.info("Ініціалізація AccountController.");
        User user = Session.getUser();
        if (user != null) {
            nameLabel.setText(user.getName());
            balanceLabel.setText(String.format("%.2f грн", user.getBalance()));
            logger.info("Дані користувача {} завантажено в AccountView.", user.getName());
        } else {
            logger.error("Користувача не знайдено в сесії під час ініціалізації AccountView.");
            showAlert("Помилка", "Користувача не знайдено. Будь ласка, увійдіть повторно.");
        }



        try {
            cardImageView.setImage(new Image(getClass().getResource("/com/sabat/deposit/images/card.png").toExternalForm()));
            logger.debug("Зображення картки успішно завантажено.");
        } catch (Exception e) {
            logger.error("Не вдалося завантажити зображення картки: {}", e.getMessage(), e);
        }

    }






    public void updateBalance() {
        User user = Session.getUser();
        if (user != null) {
            balanceLabel.setText(String.format("%.2f грн", user.getBalance()));
            logger.info("Баланс оновлено на AccountView: {} грн для користувача {}", user.getBalance(), user.getId());
        } else {
            logger.warn("Спроба оновити баланс на AccountView без користувача в сесії.");
        }
    }





    @FXML
    protected void onTopUpClick(ActionEvent event) {
        User user = Session.getUser();
        String userId = (user != null) ? String.valueOf(user.getId()) : "невідомий";
        logger.info("Користувач {} натиснув кнопку 'Поповнити'.", userId);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/sabat/deposit/views/topup-view.fxml"));
            Parent topUpRoot = loader.load();
            TopUpController topUpController = loader.getController();
            topUpController.setAccountController(this);
            Scene topUpScene = new Scene(topUpRoot);
            Stage stage = new Stage();
            stage.setScene(topUpScene);
            stage.setTitle("Поповнення балансу");
            logger.debug("Вікно поповнення балансу відкрито для користувача {}.", userId);
            stage.showAndWait();
        } catch (IOException e) {
            logger.error("Помилка завантаження topup-view.fxml для користувача {}: {}", userId, e.getMessage(), e);
            showAlert("Помилка", "Не вдалося відкрити вікно поповнення.");
        }
    }







    @FXML
    public void onDepositsClick(ActionEvent event) {
        User user = Session.getUser();
        String userId = (user != null) ? String.valueOf(user.getId()) : "невідомий";
        logger.info("Користувач {} натиснув кнопку 'Депозити', перехід до списку депозитів.", userId);
        try {
            NavigationManager.switchScene(event, "/com/sabat/deposit/views/deposits-view.fxml");
            logger.debug("Успішно перейшли на deposits-view.fxml для користувача {}.", userId);
        } catch (IOException e) {
            logger.error("Не вдалося відкрити deposits-view.fxml для користувача {}: {}", userId, e.getMessage(), e);
            showAlert("Помилка", "Не вдалося відкрити сторінку депозитів.");
        }
    }







    @FXML
    protected void onMyDepositsClick(ActionEvent event) {
        User user = Session.getUser();
        String userId = (user != null) ? String.valueOf(user.getId()) : "невідомий";
        logger.info("Користувач {} натиснув кнопку 'Мої депозити'.", userId);
        try {
            NavigationManager.switchScene(event, "/com/sabat/deposit/views/my-deposits-view.fxml");
            logger.debug("Успішно перейшли на my-deposits-view.fxml для користувача {}.", userId);
        } catch (IOException e) {
            logger.error("Помилка відкриття my-deposits-view.fxml для користувача {}: {}", userId, e.getMessage(), e);
            showAlert("Помилка", "Не вдалося відкрити сторінку 'Мої депозити'.");
        }
    }





    private void showAlert(String title, String message) {
        logger.warn("Показ сповіщення: Title='{}', Message='{}'", title, message);
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
        logger.info("Користувач {} натиснув кнопку 'Історія'.", userIdLog);

        if (currentUser == null) {
            logger.error("Неможливо відкрити історію: користувач не знайдений в сесії.");
            showAlert("Помилка", "Користувача не знайдено. Будь ласка, увійдіть повторно.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/sabat/deposit/views/history.fxml"));
            Parent root = loader.load();
            logger.debug("history.fxml завантажено успішно для користувача {}.", currentUser.getId());

            HistoryController controller = loader.getController();
            int currentUserId = currentUser.getId();
            controller.setUserId(currentUserId);
            logger.debug("UserId {} встановлено для HistoryController.", currentUserId);

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Історія транзакцій");
            stage.show();
            logger.info("Вікно історії транзакцій відкрито для користувача {}.", currentUserId);
        } catch (IOException e) {
            logger.error("Помилка завантаження або відображення history.fxml для користувача {}: {}", currentUser.getId(), e.getMessage(), e);
            showAlert("Помилка", "Не вдалося відкрити історію транзакцій.");
        }
    }




    @FXML
    private void onLogoutClick(ActionEvent event) {
        User user = Session.getUser();
        String userId = (user != null) ? String.valueOf(user.getId()) : "невідомий";
        logger.info("Користувач {} натиснув кнопку виходу.", userId);
        Session.clear();
        logger.info("Сесію користувача {} очищено.", userId);
        try {
            NavigationManager.switchScene(event, "/com/sabat/deposit/views/login-view.fxml");
            logger.info("Користувач {} успішно вийшов, перенаправлено на сторінку входу.", userId);
        } catch (IOException e) {
            logger.error("Помилка перенаправлення на сторінку входу після виходу користувача {}: {}", userId, e.getMessage(), e);
            showAlert("Помилка", "Не вдалося повернутися на сторінку входу.");
        }
    }
}