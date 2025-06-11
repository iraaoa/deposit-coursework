package com.sabat.deposit.controller;

import com.sabat.deposit.model.Deposit;
import com.sabat.deposit.service.DepositService;
import com.sabat.deposit.util.Logger;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class EditDepositController {

    @FXML private TextField nameField;
    @FXML private TextField interestRateField;
    @FXML private TextField termField;
    @FXML private TextField bankNameField;
    @FXML private TextField minAmountField;

    @FXML private ComboBox<String> typeComboBox;
    @FXML private CheckBox replenishableCheckBox;
    @FXML private CheckBox earlyWithdrawalCheckBox;

    private Deposit deposit;
    private DepositService depositService;

    @FXML
    public void initialize() {
        typeComboBox.getItems().addAll("Універсальний", "Накопичувальний", "Ощадний");
        typeComboBox.setOnAction(event -> updateDepositOptions());

        try {
            depositService = new DepositService("jdbc:sqlite:src/main/resources/db/Deposits.db");
        } catch (Exception e) {
            Logger.error("Помилка ініціалізації DepositService: {}", e.getMessage());
        }
    }

    private void updateDepositOptions() {
        String selectedType = typeComboBox.getValue();
        if (selectedType == null) return;

        switch (selectedType) {
            case "Універсальний":
                replenishableCheckBox.setSelected(true);
                earlyWithdrawalCheckBox.setSelected(true);
                break;
            case "Накопичувальний":
                replenishableCheckBox.setSelected(true);
                earlyWithdrawalCheckBox.setSelected(false);
                break;
            case "Ощадний":
                replenishableCheckBox.setSelected(false);
                earlyWithdrawalCheckBox.setSelected(false);
                break;
            default:
                replenishableCheckBox.setSelected(false);
                earlyWithdrawalCheckBox.setSelected(false);
        }

        replenishableCheckBox.setDisable(true);
        earlyWithdrawalCheckBox.setDisable(true);
    }

    public void setDepositToEdit(Deposit deposit) {
        this.deposit = deposit;

        nameField.setText(deposit.getName());
        typeComboBox.setValue(deposit.getType());
        interestRateField.setText(String.valueOf(deposit.getInterestRate()));
        termField.setText(String.valueOf(deposit.getTerm()));
        bankNameField.setText(deposit.getBankName());
        minAmountField.setText(String.valueOf(deposit.getMinAmount()));

        updateDepositOptions();
    }

    @FXML
    private void onSaveClick() {
        try {
            deposit.setName(nameField.getText().trim());
            deposit.setType(typeComboBox.getValue());
            deposit.setInterestRate(Double.parseDouble(interestRateField.getText().trim()));
            deposit.setTerm(Integer.parseInt(termField.getText().trim()));
            deposit.setBankName(bankNameField.getText().trim());
            deposit.setIsReplenishable(replenishableCheckBox.isSelected() ? 1 : 0);
            deposit.setIsEarlyWithdrawal(earlyWithdrawalCheckBox.isSelected() ? 1 : 0);
            deposit.setMinAmount(Double.parseDouble(minAmountField.getText().trim()));

            boolean updated = depositService.updateDeposit(deposit);
            if (updated) {
                Logger.info("Депозит ID=" + deposit.getId() + " оновлено");
                closeWindow();
            } else {
                showAlert("Помилка", "Не вдалося оновити депозит.");
            }
        } catch (Exception e) {
            Logger.error("Помилка при оновленні: {}", e.getMessage());
            showAlert("Помилка", "Перевірте введені дані.");
        }
    }

    private void closeWindow() {
        ((Stage) nameField.getScene().getWindow()).close();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
