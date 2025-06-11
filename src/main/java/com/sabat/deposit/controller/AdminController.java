package com.sabat.deposit.controller;

import com.sabat.deposit.model.Deposit;
import com.sabat.deposit.model.User;
import com.sabat.deposit.navigation.NavigationManager;
import com.sabat.deposit.session.Session;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import com.sabat.deposit.service.DepositService;
import com.sabat.deposit.util.Logger;

import java.io.IOException;
import java.util.List;

public class AdminController {

    @FXML private TableView<Deposit> depositsTable;
    @FXML private TableColumn<Deposit, Integer> idColumn;
    @FXML private TableColumn<Deposit, String> nameColumn;
    @FXML private TableColumn<Deposit, String> typeColumn;
    @FXML private TableColumn<Deposit, Double> interestRateColumn;
    @FXML private TableColumn<Deposit, Integer> termColumn;
    @FXML private TableColumn<Deposit, String> bankNameColumn;
    @FXML private TableColumn<Deposit, Integer> isReplenishableColumn;
    @FXML private TableColumn<Deposit, Integer> isEarlyWithdrawalColumn;
    @FXML private TableColumn<Deposit, Double> minAmountColumn;

    private DepositService depositService;

    public AdminController() {
        try {
            depositService = new DepositService("jdbc:sqlite:src/main/resources/db/Deposits.db");
        } catch (Exception e) {
            Logger.error("Помилка створення DepositService: " + e.getMessage(), "");
        }
    }

    @FXML
    public void initialize() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        interestRateColumn.setCellValueFactory(new PropertyValueFactory<>("interestRate"));
        termColumn.setCellValueFactory(new PropertyValueFactory<>("term"));
        bankNameColumn.setCellValueFactory(new PropertyValueFactory<>("bankName"));
        isReplenishableColumn.setCellValueFactory(new PropertyValueFactory<>("isReplenishable"));
        isEarlyWithdrawalColumn.setCellValueFactory(new PropertyValueFactory<>("isEarlyWithdrawal"));
        minAmountColumn.setCellValueFactory(new PropertyValueFactory<>("minAmount"));
        loadDepositsIntoTable();
    }

    private void loadDepositsIntoTable() {
        if (depositService == null) {
            showAlert("Помилка Сервісу", "Не вдалося завантажити дані депозитів. Сервіс недоступний.");
            return;
        }
        try {
            List<Deposit> deposits = depositService.getAllDeposits();
            ObservableList<Deposit> observableDeposits = FXCollections.observableArrayList(deposits);
            depositsTable.setItems(observableDeposits);
        } catch (Exception e) {
            Logger.error("Помилка завантаження депозитів: " + e.getMessage(), "");
            showAlert("Помилка Завантаження", "Не вдалося завантажити список депозитів.");
        }
    }

    @FXML
    private void onOpenRegisterDepositClick() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/sabat/deposit/views/register-deposit-view.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Реєстрація нового депозиту");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();

            loadDepositsIntoTable();
        } catch (IOException e) {
            Logger.error("Помилка завантаження register-deposit-view.fxml: " + e.getMessage(), "");
            showAlert("Помилка Завантаження", "Не вдалося відкрити вікно реєстрації депозиту.");
        }
    }

    @FXML
    private void onDeleteDepositClick() {
        Deposit selectedDeposit = depositsTable.getSelectionModel().getSelectedItem();

        if (selectedDeposit == null) {
            showAlert("Помилка Видалення", "Будь ласка, виберіть депозит для видалення.");
            return;
        }

        if (depositService == null) {
            showAlert("Помилка Сервісу", "Не вдалося видалити депозит. Сервіс недоступний.");
            return;
        }

        boolean success;
        try {
            success = depositService.deleteDeposit(selectedDeposit);
        } catch (Exception e) {
            Logger.error("Помилка під час видалення депозиту ID " + selectedDeposit.getId() + ": " + e.getMessage(), "");
            showAlert("Помилка Видалення", "Сталася помилка під час видалення депозиту.");
            return;
        }

        if (success) {
            Logger.info("Депозит ID=" + selectedDeposit.getId() + ", Name=" + selectedDeposit.getName() + " успішно видалено.");
            showAlert("Успіх", "Депозит '" + selectedDeposit.getName() + "' видалено.");
            loadDepositsIntoTable();
        } else {
            Logger.info("Не вдалося видалити депозит ID=" + selectedDeposit.getId() + ", Name=" + selectedDeposit.getName() + ". Метод сервісу повернув false.");
            showAlert("Помилка Видалення", "Не вдалося видалити депозит '" + selectedDeposit.getName() + "'.");
        }
    }

    @FXML
    private void onEditDepositClick() {
        Deposit selectedDeposit = depositsTable.getSelectionModel().getSelectedItem();

        if (selectedDeposit == null) {
            showAlert("Помилка Редагування", "Будь ласка, виберіть депозит для редагування.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/sabat/deposit/views/edit-deposit-view.fxml"));
            Parent root = loader.load();

            EditDepositController controller = loader.getController();
            controller.setDepositToEdit(selectedDeposit);

            Stage stage = new Stage();
            stage.setTitle("Редагування депозиту");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();

            loadDepositsIntoTable();
        } catch (IOException e) {
            Logger.error("Помилка відкриття вікна редагування депозиту: " + e.getMessage(), "");
            showAlert("Помилка Завантаження", "Не вдалося відкрити вікно редагування депозиту.");
        }
    }

    @FXML
    private void onLogoutClick(ActionEvent event) {
        User user = Session.getUser();
        String userId = (user != null) ? String.valueOf(user.getId()) : "невідомий";
        Logger.info("Адміністратор " + userId + " натиснув кнопку виходу.");
        Session.clear();
        Logger.info("Сесію адміністратора " + userId + " очищено.");
        try {
            NavigationManager.switchScene(event, "/com/sabat/deposit/views/login-view.fxml");
            Logger.info("Адміністартор " + userId + " успішно вийшов, перенаправлено на сторінку входу.");
        } catch (IOException e) {
            Logger.error("Помилка перенаправлення на сторінку входу після виходу адміністартора " + userId + ": " + e.getMessage(), "");
            showAlert("Помилка", "Не вдалося повернутися на сторінку входу.");
        }
    }

    private void showAlert(String title, String message) {
        Alert.AlertType type = title.toLowerCase().contains("помилка") ? Alert.AlertType.ERROR : Alert.AlertType.INFORMATION;
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
