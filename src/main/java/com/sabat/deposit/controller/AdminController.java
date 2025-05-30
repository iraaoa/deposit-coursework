package com.sabat.deposit.controller;

import com.sabat.deposit.db.Database;
import com.sabat.deposit.model.Deposit;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class AdminController {

    private static final Logger logger = LogManager.getLogger(AdminController.class);

    @FXML
    private TableView<Deposit> depositsTable;

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
            logger.error("Помилка створення DepositService: {}", e.getMessage(), e);
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
            logger.error("Помилка завантаження register-deposit-view.fxml: {}", e.getMessage(), e);
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
            logger.error("DepositService не ініціалізовано. Неможливо видалити депозит.");
            showAlert("Помилка Сервісу", "Не вдалося видалити депозит. Сервіс недоступний.");
            return;
        }

        boolean success = false;
        try {
            success = depositService.deleteDeposit(selectedDeposit); // Використання екземпляра сервісу
        } catch (Exception e) {
            logger.error("Помилка під час виклику сервісу для видалення депозиту ID {}: {}", selectedDeposit.getId(), e.getMessage(), e);
            showAlert("Помилка Видалення", "Сталася помилка під час видалення депозиту.");
            return;
        }


        if (success) {
            logger.info("Депозит ID={}, Name={} успішно видалено.", selectedDeposit.getId(), selectedDeposit.getName());
            showAlert("Успіх", "Депозит '" + selectedDeposit.getName() + "' видалено.");
            loadDepositsIntoTable();
        } else {
            logger.warn("Не вдалося видалити депозит ID={}, Name={}. Метод сервісу повернув false.", selectedDeposit.getId(), selectedDeposit.getName());
            showAlert("Помилка Видалення", "Не вдалося видалити депозит '" + selectedDeposit.getName() + "'.");
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