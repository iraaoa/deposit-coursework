package com.sabat.deposit.service;

import com.sabat.deposit.db.Database;
import com.sabat.deposit.model.User;
import com.sabat.deposit.util.Logger;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;

public class UserService {

    // Перевірка, чи існує користувач з таким email
    public static boolean userExists(String email) {
        String sql = "SELECT 1 FROM users WHERE email = ? LIMIT 1";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                boolean exists = rs.next();
                return exists;
            }

        } catch (SQLException e) {
            Logger.error("Помилка при перевірці існування користувача за email", e.getMessage());
            return false;
        }
    }

    // Реєстрація нового користувача
    public static boolean registerUser(User user) {
        if (userExists(user.getEmail())) {
            Logger.info("Спроба реєстрації користувача з існуючим email '" + user.getEmail() + "'");
            return false;
        }

        String sql = "INSERT INTO users (name, surname, email, password, balance) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, user.getName());
            stmt.setString(2, user.getSurname());
            stmt.setString(3, user.getEmail());
            stmt.setString(4, user.getPassword());
            stmt.setDouble(5, user.getBalance());

            int rows = stmt.executeUpdate();
            if (rows > 0) {
                return true;
            } else {
                Logger.info("Не вдалося зареєструвати користувача з email '" + user.getEmail() + "'");
                return false;
            }

        } catch (SQLException e) {
            Logger.error("Помилка при реєстрації користувача з email '" + user.getEmail() + "'", e.getMessage());
            return false;
        }
    }

    public static User loginUser(String email, String password) {
        try (Connection conn = Database.getConnection()) {
            String sql = "SELECT * FROM users WHERE email = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, email);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String hashedPassword = rs.getString("password");

                if (BCrypt.checkpw(password, hashedPassword)) {
                    int userId = rs.getInt("id");

                    User user = new User(
                            userId,
                            rs.getString("name"),
                            rs.getString("surname"),
                            rs.getString("email"),
                            hashedPassword
                    );
                    user.setBalance(rs.getDouble("balance"));
                    user.setRole(rs.getString("role"));

                    return user;
                } else {
                    Logger.info("Невдала спроба входу: невірний пароль для email '" + email + "'");
                    return null;
                }
            } else {
                Logger.info("Невдала спроба входу: користувача з email '" + email + "' не знайдено");
                return null;
            }
        } catch (SQLException e) {
            Logger.error("Помилка при вході користувача з email '" + email + "'", e.getMessage());
        }

        return null;
    }

    public static boolean updateUserBalance(User user) {
        String query = "UPDATE users SET balance = ? WHERE email = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setDouble(1, user.getBalance());
            stmt.setString(2, user.getEmail());

            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated > 0) {
                Logger.info("Баланс оновлено для користувача email='" + user.getEmail() + "' на суму " + user.getBalance());
                return true;
            } else {
                Logger.info("Не вдалося оновити баланс користувача email='" + user.getEmail() + "'");
                return false;
            }
        } catch (SQLException e) {
            Logger.error("Помилка оновлення балансу для користувача email='" + user.getEmail() + "'", e.getMessage());
            return false;
        }
    }

    public static String topUpBalance(User user, String amountText) {
        if (amountText == null || amountText.trim().isEmpty()) {
            Logger.info("Спроба поповнення балансу без вказаної суми для користувача email='" + user.getEmail() + "'");
            return "❌ Введіть суму для поповнення.";
        }

        double amount;
        try {
            amount = Double.parseDouble(amountText.trim());
            if (amount <= 0) {
                Logger.info("Невірна сума поповнення '" + amountText + "' для користувача email='" + user.getEmail() + "'");
                return "❌ Сума повинна бути додатним числом.";
            }
            if ((user.getBalance() + amount) > 50000) {
                Logger.info("Перевищено ліміт балансу при спробі поповнення на " + amount + " для користувача email='" + user.getEmail() + "'");
                return "❌ Ви перевищили дозволений ліміт на балансі";
            }
        } catch (NumberFormatException e) {
            Logger.info("Невірний формат суми для поповнення '" + amountText + "', користувач email='" + user.getEmail() + "'");
            return "❌ Сума повинна бути числом.";
        }

        user.setBalance(user.getBalance() + amount);
        boolean success = updateUserBalance(user);

        if (success) {
            Logger.info("Баланс успішно поповнено на " + amount + " для користувача email='" + user.getEmail() + "'");
            Logger.info("Новий баланс: " + user.getBalance() + " для користувача email='" + user.getEmail() + "'");

            return "✅ Баланс успішно поповнено на " + amount;
        } else {
            Logger.error("Помилка при поповненні балансу на " + amount + " для користувача email='" + user.getEmail() + "'", "");
            return "❌ Сталася помилка при поповненні балансу.";
        }
    }

    public static double getBalanceByUserId(int userId) {
        String query = "SELECT balance FROM users WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                double balance = rs.getDouble("balance");
                return balance;
            }
        } catch (SQLException e) {
            Logger.error("Помилка отримання балансу для користувача з id=" + userId, e.getMessage());
        }
        return 0.0;
    }

    public static int getUserIdByEmail(String email) {
        String query = "SELECT id FROM users WHERE email = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int id = rs.getInt("id");
                return id;
            }

        } catch (SQLException e) {
            Logger.error("Помилка отримання id користувача за email='" + email + "'", e.getMessage());
        }

        return -1;
    }

}
