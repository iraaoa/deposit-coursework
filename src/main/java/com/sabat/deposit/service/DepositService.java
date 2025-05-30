package com.sabat.deposit.service;

import com.sabat.deposit.controller.RegisterDepositController;
import com.sabat.deposit.db.Database;
import com.sabat.deposit.model.Deposit;
import com.sabat.deposit.session.Session;

import java.sql.*;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DepositService {

    private static final org.apache.log4j.Logger logger = LogManager.getLogger(DepositService.class);


    private final String dbUrl;

    public DepositService(String dbUrl) {
        this.dbUrl = dbUrl;
    }

    TransactionService transactionService = new TransactionService();

    public double roundToTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }


    public Connection getConnectionWithForeignKeysEnabled() throws SQLException {
        Connection conn = DriverManager.getConnection(dbUrl);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
        }
        return conn;
    }

    public List<Deposit> getAllDeposits() {
        List<Deposit> deposits = new ArrayList<>();
        String query = "SELECT * FROM deposits";

        try (Connection conn = getConnectionWithForeignKeysEnabled();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                deposits.add(new Deposit(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("type"),
                        rs.getDouble("interest_rate"),
                        rs.getInt("term"),
                        rs.getString("bank_name"),
                        rs.getInt("is_replenishable"),
                        rs.getInt("is_early_withdrawal"),
                        rs.getDouble("min_amount")
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Помилка при читанні депозитів з БД: " + e.getMessage(), e);
        }
        return deposits;
    }

    public List<Deposit> filterDeposits(List<Deposit> deposits, String keyword) {
        if (deposits == null) return new ArrayList<>();
        if (keyword == null || keyword.trim().isEmpty()) {
            return new ArrayList<>(deposits);
        }
        String lowerKeyword = keyword.toLowerCase().trim();
        return deposits.stream()
                .filter(d -> d.getBankName().toLowerCase().contains(lowerKeyword) ||
                        d.getName().toLowerCase().contains(lowerKeyword))
                .collect(Collectors.toList());
    }

    public List<Deposit> sortDeposits(List<Deposit> deposits, String criteria) {
        if (deposits == null) return new ArrayList<>();
        if (criteria == null) return new ArrayList<>(deposits);

        switch (criteria) {
            case "Ставка (зрост.)":
                return deposits.stream()
                        .sorted(Comparator.comparingDouble(Deposit::getInterestRate))
                        .collect(Collectors.toList());
            case "Ставка (спад.)":
                return deposits.stream()
                        .sorted(Comparator.comparingDouble(Deposit::getInterestRate).reversed())
                        .collect(Collectors.toList());
            case "Термін (зрост.)":
                return deposits.stream()
                        .sorted(Comparator.comparingInt(Deposit::getTerm))
                        .collect(Collectors.toList());
            case "Термін (спад.)":
                return deposits.stream()
                        .sorted(Comparator.comparingInt(Deposit::getTerm).reversed())
                        .collect(Collectors.toList());
            case "Мін. сума (зрост.)":
                return deposits.stream()
                        .sorted(Comparator.comparingDouble(Deposit::getMinAmount))
                        .collect(Collectors.toList());
            case "Мін. сума (спад.)":
                return deposits.stream()
                        .sorted(Comparator.comparingDouble(Deposit::getMinAmount).reversed())
                        .collect(Collectors.toList());
            default:
                return new ArrayList<>(deposits);
        }
    }


    public static boolean deleteDeposit(Deposit deposit) {
        String deleteSql = "DELETE FROM deposits WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(deleteSql)) {

            stmt.setInt(1, deposit.getId());
            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }



    public void openDepositForUserWithAmount(int userId, Deposit deposit, double amount) {
        logger.info(String.format("Користувач %d намагається відкрити депозит '%s' на суму %.2f", userId, deposit.getName(), amount));
        if (amount < deposit.getMinAmount()) {
            logger.warn(String.format("Користувач %d: сума %.2f менша за мінімально дозволену %.2f", userId, amount, deposit.getMinAmount()));
            throw new IllegalArgumentException("Сума менша за мінімально дозволену для цього депозиту.");
        }
        if (userHasDeposit(userId, deposit.getId())) {
            logger.warn(String.format("Користувач %d вже має відкритий депозит ID %d", userId, deposit.getId()));
            throw new IllegalArgumentException("Ви вже відкрили цей депозит раніше.");
        }

        try (Connection conn = getConnectionWithForeignKeysEnabled()) {
            conn.setAutoCommit(false);

            double currentBalance = getUserBalanceById(userId, conn);
            if (currentBalance < amount) {
                logger.warn(String.format("Користувач %d: недостатньо коштів (потрібно %.2f, є %.2f)", userId, amount, currentBalance));
                throw new IllegalArgumentException("Недостатньо коштів на балансі для відкриття депозиту.");
            }

            updateUserBalanceById(userId, currentBalance - amount, conn);

            LocalDateTime finishDate = LocalDateTime.now().plusMonths(deposit.getTerm());
            String formattedFinishDate = finishDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            String insertQuery = "INSERT INTO user_deposits (user_id, deposit_id, opened_at, balance, last_interest_accrued, finish_date) " +
                    "VALUES (?, ?, CURRENT_TIMESTAMP, ?, CURRENT_TIMESTAMP, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(insertQuery)) {
                pstmt.setInt(1, userId);
                pstmt.setInt(2, deposit.getId());
                pstmt.setDouble(3, amount);
                pstmt.setString(4, formattedFinishDate);
                pstmt.executeUpdate();
            }

            conn.commit();
            logger.info(String.format("Користувач %d успішно відкрив депозит '%s' на суму %.2f", userId, deposit.getName(), amount));

            String description = "Ви відкрили депозит: " + deposit.getName();
            transactionService.addTransaction(userId, "DEPOSIT_OPEN", description, amount);

        } catch (SQLException e) {
            logger.error("Помилка бази даних при відкритті депозиту: " + e.getMessage(), e);
            throw new RuntimeException("Помилка бази даних при відкритті депозиту: " + e.getMessage(), e);
        }
    }



    private double getUserBalanceById(int userId, Connection conn) throws SQLException {
        String sql = "SELECT balance FROM users WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getDouble("balance");
            } else {
                throw new SQLException("Користувача не знайдено.");
            }
        }
    }


    private void updateUserBalanceById(int userId, double newBalance, Connection conn) throws SQLException {
        String sql = "UPDATE users SET balance = ? WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, newBalance);
            stmt.setInt(2, userId);
            int updated = stmt.executeUpdate();
            if (updated == 0) {
                throw new SQLException("Не вдалося оновити баланс користувача.");
            }
        }
    }

    public List<Deposit> getDepositsByUserId(int userId) {
        List<Deposit> deposits = new ArrayList<>();
        String query = "SELECT ud.id as user_deposit_id, d.*, ud.balance, ud.opened_at, ud.finish_date " +
                "FROM user_deposits ud " +
                "JOIN deposits d ON ud.deposit_id = d.id " +
                "WHERE ud.user_id = ?";

        try (Connection conn = getConnectionWithForeignKeysEnabled();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, userId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Deposit deposit = new Deposit(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("type"),
                            rs.getDouble("interest_rate"),
                            rs.getInt("term"),
                            rs.getString("bank_name"),
                            rs.getInt("is_replenishable"),
                            rs.getInt("is_early_withdrawal"),
                            rs.getDouble("min_amount")
                    );

                    deposit.setCurrentBalance(rs.getDouble("balance"));
                    deposit.setOpenedAt(rs.getString("opened_at"));
                    deposit.setFinishDate(rs.getString("finish_date"));

                    deposits.add(deposit);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Помилка при отриманні депозитів користувача: " + e.getMessage(), e);
        }

        for (Deposit deposit : deposits) {
            accrueInterest(userId, deposit);
        }

        return deposits;
    }


    public void topUpDeposit(int userId, Deposit deposit, double amount) {
        if (deposit.getIsReplenishable() == 0) {
            throw new IllegalStateException("Цей депозит не підтримує поповнення.");
        }
        if (amount <= 0) throw new IllegalArgumentException("Сума має бути більшою за 0.");

        final double BONUS_THRESHOLD = 100.0;   // поріг для бонусу
        final double BONUS_RATE = 0.05;          // 1% бонус

        String selectBalanceSql = "SELECT balance FROM users WHERE id = ?";
        String updateUserBalanceSql = "UPDATE users SET balance = balance - ? WHERE id = ?";
        String updateDepositSql = "UPDATE user_deposits SET balance = balance + ? WHERE user_id = ? AND deposit_id = ?";

        Connection conn = null;
        try {
            conn = getConnectionWithForeignKeysEnabled();
            conn.setAutoCommit(false);

            try (PreparedStatement selectStmt = conn.prepareStatement(selectBalanceSql)) {
                selectStmt.setInt(1, userId);
                try (ResultSet rs = selectStmt.executeQuery()) {
                    if (!rs.next()) {
                        throw new RuntimeException("Користувача не знайдено.");
                    }
                    double userBalance = rs.getDouble("balance");
                    if (userBalance < amount) {
                        logger.warn(String.format("Користувач %d має недостатньо коштів: %.2f, потрібно: %.2f", userId, userBalance, amount));
                        throw new IllegalArgumentException("Недостатньо коштів на балансі користувача.");
                    }
                }
            }

            try (PreparedStatement updateUserStmt = conn.prepareStatement(updateUserBalanceSql)) {
                updateUserStmt.setDouble(1, amount);
                updateUserStmt.setInt(2, userId);
                updateUserStmt.executeUpdate();
            }


            double bonus = 0;
            if (amount >= BONUS_THRESHOLD) {
                bonus = amount * BONUS_RATE;
            }


            double totalDepositAmount = amount + bonus;


            System.out.println("Bonus: " + bonus);
            System.out.println("Total deposit with bonus: " + totalDepositAmount);

            try (PreparedStatement updateDepositStmt = conn.prepareStatement(updateDepositSql)) {
                updateDepositStmt.setDouble(1, roundToTwoDecimals(totalDepositAmount));
                updateDepositStmt.setInt(2, userId);
                updateDepositStmt.setInt(3, deposit.getId());
                int rowsUpdated = updateDepositStmt.executeUpdate();
                if (rowsUpdated == 0) {
                    throw new RuntimeException("Депозит не знайдено для користувача.");
                }
            }
            logger.info(String.format("Користувач %d поповнив депозит '%s' на суму %.2f (бонус %.2f)", userId, deposit.getName(), amount, bonus));
            conn.commit();
            String description = "Поповнення депозиту \"" + deposit.getName() + "\" на суму " + roundToTwoDecimals(totalDepositAmount);
            transactionService.addTransaction(userId, "DEPOSIT_TOP_UP", description, totalDepositAmount);

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }

            logger.error("Помилка при поповненні депозиту: " + e.getMessage(), e);
            throw new RuntimeException("Помилка при поповненні депозиту: " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }


    public static boolean saveDeposit(Deposit deposit) {
        String sql = "INSERT INTO deposits (name, type, interest_rate, term, bank_name, is_replenishable, is_early_withdrawal, min_amount) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, deposit.getName());
            stmt.setString(2, deposit.getType());
            stmt.setDouble(3, deposit.getInterestRate());
            stmt.setInt(4, deposit.getTerm());
            stmt.setString(5, deposit.getBankName());
            stmt.setInt(6, deposit.getIsReplenishable());
            stmt.setInt(7, deposit.getIsEarlyWithdrawal());
            stmt.setDouble(8, deposit.getMinAmount());

            int rowsInserted = stmt.executeUpdate();
            return rowsInserted > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }




    public void withdrawFromDeposit(int userId, Deposit deposit, double amount) {
        if (deposit.getIsEarlyWithdrawal() == 0) {
            logger.warn(String.format("Користувач %d спробував дострокове зняття з депозиту '%s', але це заборонено", userId, deposit.getName()));

            throw new IllegalStateException("Дострокове зняття коштів з цього депозиту заборонено.");
        }
        if (amount <= 0) throw new IllegalArgumentException("Сума має бути більшою за 0.");

        final double penaltyRate = 0.05; // 5% штраф

        String selectSql = "SELECT balance FROM user_deposits WHERE user_id = ? AND deposit_id = ?";

        try (Connection conn = getConnectionWithForeignKeysEnabled()) {
            conn.setAutoCommit(false);

            double currentBalance;
            try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                selectStmt.setInt(1, userId);
                selectStmt.setInt(2, deposit.getId());

                try (ResultSet rs = selectStmt.executeQuery()) {
                    if (!rs.next()) {
                        throw new RuntimeException("Депозит не знайдено для користувача.");
                    }
                    currentBalance = rs.getDouble("balance");
                }
            }

            if (amount > currentBalance) {
                logger.warn(String.format("Користувач %d намагається зняти %.2f, а на депозиті є лише %.2f", userId, amount, currentBalance));

                throw new IllegalArgumentException("Недостатньо коштів на депозиті.");
            }

            double newBalance = currentBalance - amount;

            if (newBalance < 0) {
                logger.warn(String.format("Користувач %d намагається зняти більше, ніж є на депозиті", userId));

                throw new IllegalArgumentException("Неможливо зняти суму більшу за баланс.");
            }

            if (newBalance == 0) {
                String deleteSql = "DELETE FROM user_deposits WHERE user_id = ? AND deposit_id = ?";
                try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
                    deleteStmt.setInt(1, userId);
                    deleteStmt.setInt(2, deposit.getId());
                    deleteStmt.executeUpdate();
                }
            } else {
                String updateSql = "UPDATE user_deposits SET balance = ? WHERE user_id = ? AND deposit_id = ?";
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setDouble(1, roundToTwoDecimals(newBalance));
                    updateStmt.setInt(2, userId);
                    updateStmt.setInt(3, deposit.getId());
                    updateStmt.executeUpdate();
                }
            }

            double penalty = amount * penaltyRate;
            double amountAfterPenalty = amount - penalty;

            String getUserBalanceSql = "SELECT balance FROM users WHERE id = ?";
            double userBalance;
            try (PreparedStatement getUserBalanceStmt = conn.prepareStatement(getUserBalanceSql)) {
                getUserBalanceStmt.setInt(1, userId);
                try (ResultSet userRs = getUserBalanceStmt.executeQuery()) {
                    if (!userRs.next()) {
                        throw new RuntimeException("Користувача не знайдено.");
                    }
                    userBalance = userRs.getDouble("balance");
                }
            }

            double newUserBalance = userBalance + amountAfterPenalty;

            String updateUserBalanceSql = "UPDATE users SET balance = ? WHERE id = ?";
            try (PreparedStatement updateUserBalanceStmt = conn.prepareStatement(updateUserBalanceSql)) {
                updateUserBalanceStmt.setDouble(1, roundToTwoDecimals(newUserBalance));
                updateUserBalanceStmt.setInt(2, userId);
                updateUserBalanceStmt.executeUpdate();
                logger.info("Оновлено баланс користувача після зняття з депозиту. Новий баланс: " + newUserBalance);

            }

            conn.commit();
            logger.info(String.format("Користувач %d успішно зняв %.2f з депозиту '%s' (штраф %.2f), на баланс додано %.2f",
                    userId, amount, deposit.getName(), penalty, amountAfterPenalty));

            String description = "Дострокове зняття з депозиту \"" + deposit.getName() + "\" сума: " + roundToTwoDecimals(amount) + " (штраф: " + roundToTwoDecimals(penalty) + ")";
            transactionService.addTransaction(userId, "DEPOSIT_OPEN", description, amountAfterPenalty);

        } catch (SQLException e) {
            logger.error("Помилка при знятті коштів з депозиту: " + e.getMessage(), e);
            throw new RuntimeException("Помилка при знятті коштів з депозиту: " + e.getMessage(), e);
        }
    }


    public void accrueInterest(int userId, Deposit deposit) {
        String selectSql = "SELECT balance, last_interest_accrued, opened_at FROM user_deposits WHERE user_id = ? AND deposit_id = ?";
        String updateDepositSql = "UPDATE user_deposits SET balance = ?, last_interest_accrued = ? WHERE user_id = ? AND deposit_id = ?";
        String deleteDepositSql = "DELETE FROM user_deposits WHERE user_id = ? AND deposit_id = ?";
        String updateUserBalanceSql = "UPDATE users SET balance = balance + ? WHERE id = ?";

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        try (Connection conn = getConnectionWithForeignKeysEnabled();
             PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {

            conn.setAutoCommit(false);

            selectStmt.setInt(1, userId);
            selectStmt.setInt(2, deposit.getId());

            try (ResultSet rs = selectStmt.executeQuery()) {
                if (!rs.next()) return;

                double balance = rs.getDouble("balance");
                String lastAccruedStr = rs.getString("last_interest_accrued");
                Timestamp openedAt = rs.getTimestamp("opened_at");

                LocalDateTime lastAccrued = LocalDateTime.parse(lastAccruedStr, formatter);
                LocalDateTime now = LocalDateTime.now();

                LocalDate openedDate = openedAt.toLocalDateTime().toLocalDate();
                LocalDate nowDate = now.toLocalDate();
                LocalDate endDate = openedDate.plusMonths(deposit.getTerm());
                boolean isTermFinished = !nowDate.isBefore(endDate);

                long daysPassed = Duration.between(lastAccrued, now).toDays();
                if (daysPassed <= 0) return;

                double ratePerDay = deposit.getInterestRate() / 100.0 / 365;
                double interest = balance * ratePerDay * daysPassed;
                double newBalance = balance + interest;
                String nowStr = now.format(formatter);

                if (isTermFinished) {

                    try (PreparedStatement updateUserBalanceStmt = conn.prepareStatement(updateUserBalanceSql);
                         PreparedStatement deleteDepositStmt = conn.prepareStatement(deleteDepositSql)) {

                        updateUserBalanceStmt.setDouble(1, roundToTwoDecimals(newBalance));
                        updateUserBalanceStmt.setInt(2, userId);
                        int rowsAffected = updateUserBalanceStmt.executeUpdate();

                        if (rowsAffected == 0) {
                            conn.rollback();
                            System.err.println("❌ Не знайдено користувача з ID: " + userId);
                            return;
                        }

                        deleteDepositStmt.setInt(1, userId);
                        deleteDepositStmt.setInt(2, deposit.getId());
                        deleteDepositStmt.executeUpdate();

                        conn.commit();
                        logger.info(String.format("Депозит завершено. Нараховано %.2f, загальна сума %.2f повернута на баланс користувача.", interest, newBalance));

                    }

                    double updatedBalance = UserService.getBalanceByUserId(userId);
                    Session.getUser().setBalance(updatedBalance);

                } else {
                    try (PreparedStatement updateDepositStmt = conn.prepareStatement(updateDepositSql)) {
                        updateDepositStmt.setDouble(1, roundToTwoDecimals(newBalance));
                        updateDepositStmt.setString(2, nowStr);
                        updateDepositStmt.setInt(3, userId);
                        updateDepositStmt.setInt(4, deposit.getId());
                        updateDepositStmt.executeUpdate();

                        conn.commit();
                        logger.info(String.format("Нараховано %.2f відсотків за %d днів. Новий баланс: %.2f", interest, daysPassed, newBalance));

                    }

                    double updatedBalance = UserService.getBalanceByUserId(userId);
                    Session.getUser().setBalance(updatedBalance);
                }
            }

        } catch (SQLException e) {
            logger.error("Помилка при нарахуванні відсотків: " + e.getMessage(), e);
            throw new RuntimeException("Помилка нарахування відсотків", e);
        }
    }





    public boolean userHasDeposit(int userId, int depositId) {
        String sql = "SELECT 1 FROM user_deposits WHERE user_id = ? AND deposit_id = ? LIMIT 1";
        try (Connection conn = getConnectionWithForeignKeysEnabled();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, depositId);

            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            throw new RuntimeException("Помилка при перевірці депозиту користувача: " + e.getMessage(), e);
        }
    }


}