package com.sabat.deposit.controller;

import com.sabat.deposit.model.Deposit;
import com.sabat.deposit.navigation.NavigationManager;
import com.sabat.deposit.service.UserService;
import com.sabat.deposit.session.Session;
import com.sabat.deposit.service.DepositService;
import com.sabat.deposit.util.Logger;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.List;

public class MyDepositsController {

    @FXML private FlowPane depositFlowPane;

    private final DepositService depositService = new DepositService("jdbc:sqlite:src/main/resources/db/Deposits.db");

    @FXML
    public void initialize() {
        loadUserDeposits();
    }

    private void loadUserDeposits() {
        depositFlowPane.getChildren().clear();
        List<Deposit> userDeposits = depositService.getDepositsByUserId(Session.getUser().getId());

        if (userDeposits.isEmpty()) {
            Label emptyLabel = new Label("У вас немає відкритих депозитів.");
            depositFlowPane.getChildren().add(emptyLabel);
            return;
        }

        Logger.info("Знайдено " + userDeposits.size() + " депозит(ів) для користувача ID=" + Session.getUser().getId());

        for (Deposit deposit : userDeposits) {
            depositFlowPane.getChildren().add(createDepositCard(deposit));
        }
    }

    private Node createDepositCard(Deposit deposit) {
        VBox card = new VBox(5);
        card.getStyleClass().add("deposit-card");
        card.setPrefWidth(300);

        Label nameLabel = new Label(deposit.getName());
        nameLabel.getStyleClass().add("card-title");

        Label bankLabel = new Label("Банк: " + deposit.getBankName());
        bankLabel.getStyleClass().add("card-bank-name");


        Label rateLabel = new Label("Ставка: " + String.format("%.2f%%", deposit.getInterestRate()));
        rateLabel.getStyleClass().add("card-rate");


        Label termLabel = new Label("Термін: " + deposit.getTerm() + " міс.");
        termLabel.getStyleClass().add("card-info");


        Label amountLabel = new Label("Баланс: " + String.format("%,.2f грн", deposit.getCurrentBalance()));
        Label openedAtLabel = new Label("Відкрито: " + deposit.getOpenedAt());
        Label finishDateLabel = new Label("Дійсний до: " + deposit.getFinishDate());

        Button topUpBtn = new Button("Поповнити");
        topUpBtn.getStyleClass().add("open-deposit-button");

        topUpBtn.setOnAction(e -> onTopUp(deposit));

        Button withdrawBtn = new Button("Зняти");
        withdrawBtn.getStyleClass().add("open-deposit-button");

        withdrawBtn.setOnAction(e -> onWithdraw(deposit));

        card.getChildren().addAll(nameLabel, bankLabel, rateLabel, termLabel,
                amountLabel, openedAtLabel, finishDateLabel, topUpBtn, withdrawBtn);

        return card;
    }



    private void onTopUp(Deposit deposit) {
        Logger.info("Користувач ID=" + Session.getUser().getId() + " ініціює поповнення депозиту ID=" + deposit.getId());

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Поповнення депозиту");
        dialog.setHeaderText("Введіть суму для поповнення депозиту:");
        dialog.setContentText("Сума:");

        dialog.showAndWait().ifPresent(input -> {
            try {
                double amount = Double.parseDouble(input.replace(',', '.'));
                if (amount <= 0) throw new IllegalArgumentException("Сума має бути більшою за 0.");

                depositService.topUpDeposit(Session.getUser().getId(), deposit, amount);

                Logger.info("Поповнено депозит ID=" + deposit.getId() + " на суму " + amount + " грн.");
                showAlert(Alert.AlertType.INFORMATION, "Успіх", "Депозит успішно поповнено на " + amount + " грн.");
                loadUserDeposits();

                double updatedBalance = UserService.getBalanceByUserId(Session.getUser().getId());
                Session.getUser().setBalance(updatedBalance);

            } catch (NumberFormatException ex) {
                Logger.error("Некоректний формат суми при поповненні: {}", input);
                showAlert(Alert.AlertType.ERROR, "Помилка", "Некоректний формат суми.");
            } catch (IllegalArgumentException ex) {
                Logger.error("Невалідна сума поповнення: {}", ex.getMessage());
                showAlert(Alert.AlertType.WARNING, "Помилка", ex.getMessage());
            } catch (Exception ex) {
                Logger.error("Не вдалося поповнити депозит: {}", ex.getMessage());
                showAlert(Alert.AlertType.ERROR, "Помилка", "Не вдалося поповнити депозит: " + ex.getMessage());
            }
        });
    }



    private void onWithdraw(Deposit deposit) {
        Logger.info("Користувач ID=" + Session.getUser().getId() + " ініціює зняття з депозиту ID=" + deposit.getId());

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Зняття коштів");
        dialog.setHeaderText("Введіть суму для зняття з депозиту:");
        dialog.setContentText("Сума:");

        dialog.showAndWait().ifPresent(input -> {
            try {
                double amount = Double.parseDouble(input.replace(',', '.'));
                if (amount <= 0) throw new IllegalArgumentException("Сума має бути більшою за 0.");

                depositService.withdrawFromDeposit(Session.getUser().getId(), deposit, amount);

                Logger.info("Знято з депозиту ID=" + deposit.getId() + " суму " + amount + " грн.");
                showAlert(Alert.AlertType.INFORMATION, "Успіх", "З депозиту знято " + amount + " грн.");
                loadUserDeposits();

                double updatedBalance = UserService.getBalanceByUserId(Session.getUser().getId());
                Session.getUser().setBalance(updatedBalance);

            } catch (NumberFormatException ex) {
                Logger.error("Некоректний формат суми при знятті: {}", input);
                showAlert(Alert.AlertType.ERROR, "Помилка", "Некоректний формат суми.");
            } catch (IllegalArgumentException ex) {
                Logger.error("Невалідна сума зняття: {}", ex.getMessage());
                showAlert(Alert.AlertType.WARNING, "Помилка", ex.getMessage());
            } catch (Exception ex) {
                Logger.error("Не вдалося зняти кошти з депозиту: {}", ex.getMessage());
                showAlert(Alert.AlertType.ERROR, "Помилка", "Не вдалося зняти кошти: " + ex.getMessage());
            }
        });
    }



    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML
    protected void OnDepoBackClick(ActionEvent event) {
        try {
            NavigationManager.switchScene(event, "/com/sabat/deposit/views/account-view.fxml");
        } catch (IOException e) {
            Logger.error("Не вдалося повернутися до account-view.fxml: {}", e.getMessage());
        }
    }
}
