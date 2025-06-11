package com.sabat.deposit.controller;

import com.sabat.deposit.model.Deposit;
import com.sabat.deposit.service.DepositService;
import com.sabat.deposit.util.Logger;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class RegisterDepositController {

    @FXML private TextField nameField;
    @FXML private ComboBox<String> typeComboBox;
    @FXML private TextField interestRateField;
    @FXML private TextField termField;
    @FXML private TextField bankNameField;
    @FXML private CheckBox replenishableCheckBox;
    @FXML private CheckBox earlyWithdrawalCheckBox;
    @FXML private TextField minAmountField;
    @FXML private Label messageLabel;

    @FXML
    public void initialize() {
        typeComboBox.setOnAction(event -> updateDepositOptions());
    }

    private void updateDepositOptions() {
        String selectedType = typeComboBox.getValue();
        if (selectedType == null) return;

        switch (selectedType) {
            case "Універсальний" -> {
                replenishableCheckBox.setSelected(true);
                earlyWithdrawalCheckBox.setSelected(true);
            }
            case "Накопичувальний" -> {
                replenishableCheckBox.setSelected(true);
                earlyWithdrawalCheckBox.setSelected(false);
            }
            case "Ощадний" -> {
                replenishableCheckBox.setSelected(false);
                earlyWithdrawalCheckBox.setSelected(false);
            }
            default -> {
                replenishableCheckBox.setSelected(false);
                earlyWithdrawalCheckBox.setSelected(false);
            }
        }
        replenishableCheckBox.setDisable(true);
        earlyWithdrawalCheckBox.setDisable(true);
    }

    @FXML
    private void onSaveClick() {
        String name = nameField.getText().trim();
        String type = typeComboBox.getValue();
        String interestRateStr = interestRateField.getText().trim();
        String termStr = termField.getText().trim();
        String bankName = bankNameField.getText().trim();
        boolean replenishable = replenishableCheckBox.isSelected();
        boolean earlyWithdrawal = earlyWithdrawalCheckBox.isSelected();
        String minAmountStr = minAmountField.getText().trim();

        if (name.isEmpty() || type == null || interestRateStr.isEmpty() || termStr.isEmpty() || bankName.isEmpty() || minAmountStr.isEmpty()) {
            messageLabel.setStyle("-fx-text-fill: red;");
            messageLabel.setText("Будь ласка, заповніть всі поля.");
            Logger.info("Спроба збереження депозиту з порожніми полями.");
            return;
        }

        double interestRate;
        int term;
        double minAmount;

        try {
            interestRate = Double.parseDouble(interestRateStr);
            term = Integer.parseInt(termStr);
            minAmount = Double.parseDouble(minAmountStr);
        } catch (NumberFormatException e) {
            messageLabel.setStyle("-fx-text-fill: red;");
            messageLabel.setText("Некоректні числові значення.");
            Logger.info("Некоректні числові значення при реєстрації депозиту: " + e.getMessage());
            return;
        }

        Deposit deposit = new Deposit(
                0,
                name,
                type,
                interestRate,
                term,
                bankName,
                replenishable ? 1 : 0,
                earlyWithdrawal ? 1 : 0,
                minAmount
        );

        boolean success = DepositService.saveDeposit(deposit);

        if (success) {
            messageLabel.setStyle("-fx-text-fill: green;");
            messageLabel.setText("Депозит зареєстровано успішно!");
            Logger.info("Депозит '" + name + "' успішно збережено в базі даних.");
        } else {
            messageLabel.setStyle("-fx-text-fill: red;");
            messageLabel.setText("Помилка при збереженні депозиту.");
            Logger.error("Помилка при збереженні депозиту '{}'", name);
        }
    }

    @FXML
    private void onCancelClick() {
        nameField.clear();
        typeComboBox.getSelectionModel().clearSelection();
        interestRateField.clear();
        termField.clear();
        bankNameField.clear();
        replenishableCheckBox.setSelected(false);
        earlyWithdrawalCheckBox.setSelected(false);
        minAmountField.clear();
        messageLabel.setText("");
        replenishableCheckBox.setDisable(false);
        earlyWithdrawalCheckBox.setDisable(false);
        Logger.info("Форма реєстрації депозиту очищена користувачем.");
    }
}
