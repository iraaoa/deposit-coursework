package com.sabat.deposit.controller;

import com.sabat.deposit.db.Database;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseSetup {

    public void createTransactionsTable() {
        String createTableSQL = """
    CREATE TABLE IF NOT EXISTS transactions (
        id SERIAL PRIMARY KEY,
        user_id INTEGER NOT NULL,
        transaction_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        type TEXT NOT NULL,        
        description TEXT,
        amount DECIMAL(15,2) NOT NULL,
        FOREIGN KEY(user_id) REFERENCES users(id)
    )
    """;


        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute(createTableSQL);
            System.out.println("Таблиця transactions створена або вже існує.");

        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Не вдалося створити таблицю transactions: " + e.getMessage());
        }
    }
}
