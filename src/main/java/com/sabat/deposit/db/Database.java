package com.sabat.deposit.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {

    private static String testUrl = null;

    public static void setTestConnection(String url) {
        testUrl = url;
    }

    public static void resetTestConnection() {
        testUrl = null;
    }

    public static Connection getConnection() throws SQLException {
        String url = (testUrl != null) ? testUrl : "jdbc:sqlite:src/main/resources/db/Deposits.db";
        Connection conn = DriverManager.getConnection(url);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
        }
        return conn;
    }
}
