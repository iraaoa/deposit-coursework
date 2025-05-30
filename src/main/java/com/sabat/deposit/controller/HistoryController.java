package com.sabat.deposit.controller;

import com.sabat.deposit.model.Transaction;
import com.sabat.deposit.service.TransactionService; // ВИПРАВЛЕНО: правильний сервіс
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
// Імпорт для роботи з датою та форматуванням
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.List;

public class HistoryController {

    @FXML
    private TableView<Transaction> historyTable;

    @FXML private TableColumn<Transaction, Timestamp> dateColumn;
    @FXML private TableColumn<Transaction, String> typeColumn;
    @FXML private TableColumn<Transaction, String> descriptionColumn;
    @FXML private TableColumn<Transaction, Double> amountColumn;

    private ObservableList<Transaction> transactionList = FXCollections.observableArrayList();

    private int userId;
    private TransactionService transactionService;

    public void setUserId(int userId) {
        this.userId = userId;
        loadTransactions();
    }

    @FXML
    private void initialize() {
        this.transactionService = new TransactionService(); // Ініціалізація сервісу


        dateColumn.setCellValueFactory(new PropertyValueFactory<>("transactionDate"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));

        if (descriptionColumn != null) {
            descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        }
        amountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        dateColumn.setCellFactory(column -> new TableCell<Transaction, Timestamp>() {

            @Override
            protected void updateItem(Timestamp item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setText(null);
                } else {
                    setText(dateFormat.format(item));
                }
            }
        });

        DecimalFormat amountFormat = new DecimalFormat("#,##0.00");
        amountColumn.setCellFactory(column -> new TableCell<Transaction, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setText(null);
                } else {
                    setText(amountFormat.format(item) /* + " грн" */);
                }
            }
        });

        historyTable.setItems(transactionList);
    }

    private void loadTransactions() {
        List<Transaction> list = transactionService.getAllTransactionsForUser(userId);
        transactionList.setAll(list);
    }

    @FXML
    private void onCloseClick() {
        Stage stage = (Stage) historyTable.getScene().getWindow();
        stage.close();
        System.out.println("Кнопка 'Закрити' натиснута - реалізуйте дію.");
    }
}
