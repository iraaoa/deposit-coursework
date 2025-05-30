package com.sabat.deposit.service;

import com.sabat.deposit.db.Database;
import com.sabat.deposit.model.Transaction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class TransactionService {

    public void addTransaction(int userId, String type, String description, double amount) {
        String insertSQL = "INSERT INTO transactions (user_id, transaction_date, type, description, amount) " +
                "VALUES (?, CURRENT_TIMESTAMP, ?, ?, ?)";

        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {

            pstmt.setInt(1, userId);
            pstmt.setString(2, type);
            pstmt.setString(3, description);
            pstmt.setDouble(4, amount);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Помилка додавання транзакції: " + e.getMessage(), e);
        }
    }

    public List<Transaction> getAllTransactionsForUser(int userId) {
        List<Transaction> transactions = new ArrayList<>();
        String sql = "SELECT id, transaction_date, type, description, amount FROM transactions WHERE user_id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    Timestamp date = rs.getTimestamp("transaction_date");
                    String type = rs.getString("type");
                    String description = rs.getString("description");
                    double amount = rs.getDouble("amount");

                    transactions.add(new Transaction(id, date, type, description, amount));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Помилка отримання транзакцій для користувача " + userId + ": " + e.getMessage(), e);
        }
        return transactions;
    }
}