package com.sabat.deposit.controller;

import com.sabat.deposit.model.Deposit;
import com.sabat.deposit.navigation.NavigationManager;
import com.sabat.deposit.service.DepositService;
import com.sabat.deposit.service.UserService;
import com.sabat.deposit.session.Session;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DepositsController {

    private static final Logger logger = LogManager.getLogger(DepositsController.class);

    @FXML
    FlowPane depositFlowPane;
    @FXML
    private ScrollPane depositScrollPane;
    @FXML
    TextField searchField;
    @FXML
    ComboBox<String> sortCriteriaComboBox;

    ObservableList<Deposit> masterData = FXCollections.observableArrayList();
    DepositService depositService;

    public void setDepositService(DepositService service) {
        this.depositService = service;
    }





    @FXML
    public void initialize() {
        if (depositService == null) {
            depositService = new DepositService("jdbc:sqlite:src/main/resources/db/Deposits.db");
        }

        masterData.setAll(depositService.getAllDeposits());
        updateDisplayedDeposits();

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            logger.info("Користувач ввів пошуковий запит: '{}'", newVal);
            updateDisplayedDeposits();
        });

        sortCriteriaComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            logger.info("Користувач обрав сортування: '{}'", newVal);
            updateDisplayedDeposits();
        });
    }






    void updateDisplayedDeposits() {
        String searchTerm = searchField.getText();
        String sortCriteria = sortCriteriaComboBox.getValue();

        List<Deposit> processedList = new ArrayList<>(masterData);
        processedList = depositService.filterDeposits(processedList, searchTerm);
        processedList = depositService.sortDeposits(processedList, sortCriteria);

        renderDeposits(processedList);
    }






    void renderDeposits(List<Deposit> deposits) {
        depositFlowPane.getChildren().clear();
        if (deposits == null || deposits.isEmpty()) {
            Label noResultsLabel = new Label("Депозити не знайдено.");
            noResultsLabel.getStyleClass().add("no-results-label");
            depositFlowPane.getChildren().add(noResultsLabel);
            return;
        }

        for (Deposit deposit : deposits) {
            depositFlowPane.getChildren().add(createDepositCard(deposit));
        }
    }






    private Node createDepositCard(Deposit deposit) {
        VBox card = new VBox();
        card.getStyleClass().add("deposit-card");

        Label nameLabel = new Label(deposit.getName());
        nameLabel.getStyleClass().add("card-title");

        Label bankLabel = new Label("Банк: " + deposit.getBankName());
        bankLabel.getStyleClass().add("card-bank-name");

        Label rateLabel = new Label("Ставка: " + String.format("%.2f%%", deposit.getInterestRate()));
        rateLabel.getStyleClass().add("card-rate");

        Label typeLabel = new Label("Тип: " + deposit.getType());
        typeLabel.getStyleClass().add("card-info");

        Label termLabel = new Label("Термін: " + deposit.getTerm() + " місяців");
        termLabel.getStyleClass().add("card-info");

        Label minAmountLabel = new Label("Мінімальна сума: " + String.format("%,.2f грн", deposit.getMinAmount()));
        minAmountLabel.getStyleClass().add("card-info");

        Label replenishableLabel = new Label("Поповнення: " + deposit.getReplenishableString());
        replenishableLabel.getStyleClass().add("card-info");

        Label withdrawableLabel = new Label("Дострокове зняття: " + deposit.getEarlyWithdrawalString());
        withdrawableLabel.getStyleClass().add("card-info");

        Button openButton = new Button("Відкрити депозит");
        openButton.setOnAction(e -> handleOpenDepositAction(deposit));

        card.getChildren().addAll(
                nameLabel, bankLabel, rateLabel, typeLabel, termLabel,
                minAmountLabel, replenishableLabel, withdrawableLabel,
                openButton
        );
        return card;
    }






    void handleOpenDepositAction(Deposit deposit) {
        if (deposit == null) return;

        if (Session.getUser() == null) {
            logger.warn("Користувач намагався відкрити депозит, не будучи авторизованим.");
            showAlert(Alert.AlertType.ERROR, "Помилка", "Користувач не авторизований. Будь ласка, увійдіть в систему.");
            return;
        }

        logger.info("Користувач обрав депозит: '{}'", deposit.getName());

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Поповнення депозиту");
        dialog.setHeaderText("Введіть суму для поповнення депозиту:");
        dialog.setContentText("Сума (мін. " + String.format("%.2f", deposit.getMinAmount()) + " грн):");

        dialog.showAndWait().ifPresent(input -> {
            try {
                double amount = Double.parseDouble(input.replace(',', '.'));
                logger.info("Користувач вводить суму поповнення: {} грн для депозиту '{}'", amount, deposit.getName());

                depositService.openDepositForUserWithAmount(Session.getUser().getId(), deposit, amount);

                showAlert(Alert.AlertType.INFORMATION, "Успішно", "Депозит успішно відкрито та поповнено на " + String.format("%.2f", amount) + " грн.");
                double updatedBalance = UserService.getBalanceByUserId(Session.getUser().getId());
                Session.getUser().setBalance(updatedBalance);

                logger.info("Депозит відкрито, новий баланс користувача: {} грн", updatedBalance);

            } catch (IllegalArgumentException e) {
                logger.warn("Помилка валідації введеної суми: {}", e.getMessage());
                showAlert(Alert.AlertType.WARNING, "Помилка валідації", e.getMessage());
            } catch (RuntimeException e) {
                logger.error("Помилка при відкритті депозиту: {}", e.getMessage());
                showAlert(Alert.AlertType.ERROR, "Помилка", "Не вдалося відкрити депозит: " + e.getMessage());
            }
        });
    }




    protected void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }



    @FXML
    protected void OnDepoBackClick(ActionEvent event) throws IOException {
        NavigationManager.switchScene(event, "/com/sabat/deposit/views/account-view.fxml");
    }
}
