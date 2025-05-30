package com.sabat.deposit.controller;

import com.sabat.deposit.model.Deposit;
import com.sabat.deposit.navigation.NavigationManager;
import com.sabat.deposit.service.UserService;
import com.sabat.deposit.session.Session;
import com.sabat.deposit.service.DepositService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MyDepositsController {

    private static final Logger logger = LogManager.getLogger(MyDepositsController.class);

    @FXML
    private FlowPane depositFlowPane;

    private final DepositService depositService = new DepositService("jdbc:sqlite:src/main/resources/db/Deposits.db");

    @FXML
    public void initialize() {
        logger.info("Ініціалізація MyDepositsController для користувача ID={}", Session.getUser().getId());
        loadUserDeposits();
    }





    private void loadUserDeposits() {
        depositFlowPane.getChildren().clear();
        List<Deposit> userDeposits = depositService.getDepositsByUserId(Session.getUser().getId());

        if (userDeposits.isEmpty()) {
            logger.info("Користувач ID={} не має відкритих депозитів", Session.getUser().getId());
            Label emptyLabel = new Label("У вас немає відкритих депозитів.");
            depositFlowPane.getChildren().add(emptyLabel);
            return;
        }

        logger.info("Знайдено {} депозит(ів) для користувача ID={}", userDeposits.size(), Session.getUser().getId());

        for (Deposit deposit : userDeposits) {
            depositFlowPane.getChildren().add(createDepositCard(deposit));
        }
    }






    private Node createDepositCard(Deposit deposit) {
        VBox card = new VBox(5);
        card.getStyleClass().add("deposit-card");
        card.setPrefWidth(300);

        Label nameLabel = new Label(deposit.getName());
        Label bankLabel = new Label("Банк: " + deposit.getBankName());
        Label rateLabel = new Label("Ставка: " + String.format("%.2f%%", deposit.getInterestRate()));
        Label termLabel = new Label("Термін: " + deposit.getTerm() + " міс.");
        Label amountLabel = new Label("Баланс: " + String.format("%,.2f грн", deposit.getCurrentBalance()));

        Label openedAtLabel = new Label("Відкрито: " + deposit.getOpenedAt());
        Label finishDateLabel = new Label("Дійсний до: " + deposit.getFinishDate());

        Button topUpBtn = new Button("Поповнити");
        topUpBtn.setOnAction(e -> onTopUp(deposit));

        Button withdrawBtn = new Button("Зняти");
        withdrawBtn.setOnAction(e -> onWithdraw(deposit));

        card.getChildren().addAll(
                nameLabel, bankLabel, rateLabel, termLabel,
                amountLabel, openedAtLabel, finishDateLabel,
                topUpBtn, withdrawBtn
        );

        return card;
    }









    private void onTopUp(Deposit deposit) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Поповнення депозиту");
        dialog.setHeaderText("Введіть суму для поповнення депозиту:");
        dialog.setContentText("Сума:");

        dialog.showAndWait().ifPresent(input -> {
            try {
                double amount = Double.parseDouble(input.replace(',', '.'));
                if (amount <= 0) {
                    throw new IllegalArgumentException("Сума має бути більшою за 0.");
                }

                depositService.topUpDeposit(Session.getUser().getId(), deposit, amount);

                showAlert(Alert.AlertType.INFORMATION, "Успіх", "Депозит успішно поповнено на " + amount + " грн.");
                loadUserDeposits();
                double updatedBalance = UserService.getBalanceByUserId(Session.getUser().getId());
                Session.getUser().setBalance(updatedBalance);

            } catch (NumberFormatException ex) {
                showAlert(Alert.AlertType.ERROR, "Помилка", "Некоректний формат суми.");
            } catch (IllegalArgumentException ex) {
                showAlert(Alert.AlertType.WARNING, "Помилка", ex.getMessage());
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Помилка", "Не вдалося поповнити депозит: " + ex.getMessage());
            }
        });
    }









    private void onWithdraw(Deposit deposit) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Зняття коштів");
        dialog.setHeaderText("Введіть суму для зняття з депозиту:");
        dialog.setContentText("Сума:");

        dialog.showAndWait().ifPresent(input -> {
            try {
                double amount = Double.parseDouble(input.replace(',', '.'));
                if (amount <= 0) {
                    throw new IllegalArgumentException("Сума має бути більшою за 0.");
                }

                depositService.withdrawFromDeposit(Session.getUser().getId(), deposit, amount);

                showAlert(Alert.AlertType.INFORMATION, "Успіх", "З депозиту знято " + amount + " грн.");
                loadUserDeposits();

                double updatedBalance = UserService.getBalanceByUserId(Session.getUser().getId());
                Session.getUser().setBalance(updatedBalance);

            } catch (NumberFormatException ex) {
                showAlert(Alert.AlertType.ERROR, "Помилка", "Некоректний формат суми.");
            } catch (IllegalArgumentException ex) {
                showAlert(Alert.AlertType.WARNING, "Помилка", ex.getMessage());
            } catch (Exception ex) {
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
    protected void OnDepoBackClick(ActionEvent event){
        logger.info("Користувач ID={} натиснув кнопку 'Назад' у MyDeposits", Session.getUser().getId());
        try {
            NavigationManager.switchScene(event, "/com/sabat/deposit/views/account-view.fxml");
        } catch (IOException e) {
            logger.error("Не вдалося повернутися до account-view.fxml: {}", e.getMessage());
        }
    }

}
