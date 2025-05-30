package com.sabat.deposit.service;

import com.sabat.deposit.db.Database;
import com.sabat.deposit.model.User;
import org.mindrot.jbcrypt.BCrypt;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;

public class UserService {

    private static final Logger logger = LogManager.getLogger(UserService.class);

    // Перевірка, чи існує користувач з таким email
    public static boolean userExists(String email) {
        String sql = "SELECT 1 FROM users WHERE email = ? LIMIT 1";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                boolean exists = rs.next();
                logger.info("Перевірка існування користувача з email '{}': {}", email, exists);
                return exists;
            }

        } catch (SQLException e) {
            logger.error("Помилка при перевірці існування користувача з email '{}': {}", email, e.getMessage(), e);
            return false;
        }
    }

    // Реєстрація нового користувача
    public static boolean registerUser(User user) {
        if (userExists(user.getEmail())) {
            logger.warn("Спроба реєстрації користувача з існуючим email '{}'", user.getEmail());
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
                logger.info("Користувач успішно зареєстрований: email='{}', name='{} {}'", user.getEmail(), user.getName(), user.getSurname());
                return true;
            } else {
                logger.warn("Не вдалося зареєструвати користувача з email '{}'", user.getEmail());
                return false;
            }

        } catch (SQLException e) {
            logger.error("Помилка при реєстрації користувача з email '{}': {}", user.getEmail(), e.getMessage(), e);
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

                    logger.info("Користувач успішно увійшов: email='{}', id={}", email, userId);
                    return user;
                } else {
                    logger.warn("Невдала спроба входу: невірний пароль для email '{}'", email);
                    return null;
                }
            } else {
                logger.warn("Невдала спроба входу: користувача з email '{}' не знайдено", email);
                return null;
            }
        } catch (SQLException e) {
            logger.error("Помилка при вході користувача з email '{}': {}", email, e.getMessage(), e);
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
                logger.info("Баланс оновлено для користувача email='{}' на суму {}", user.getEmail(), user.getBalance());
                return true;
            } else {
                logger.warn("Не вдалося оновити баланс користувача email='{}'", user.getEmail());
                return false;
            }
        } catch (SQLException e) {
            logger.error("Помилка оновлення балансу для користувача email='{}': {}", user.getEmail(), e.getMessage(), e);
            return false;
        }
    }

    public static String topUpBalance(User user, String amountText) {
        if (amountText == null || amountText.trim().isEmpty()) {
            logger.warn("Спроба поповнення балансу без вказаної суми для користувача email='{}'", user.getEmail());
            return "❌ Введіть суму для поповнення.";
        }

        double amount;
        try {
            amount = Double.parseDouble(amountText.trim());
            if (amount <= 0) {
                logger.warn("Невірна сума поповнення '{}' для користувача email='{}'", amountText, user.getEmail());
                return "❌ Сума повинна бути додатним числом.";
            }
            if ((user.getBalance() + amount) > 50000) {
                logger.warn("Перевищено ліміт балансу при спробі поповнення на {} для користувача email='{}'", amount, user.getEmail());
                return "❌ Ви перевищили дозволений ліміт на балансі";
            }
        } catch (NumberFormatException e) {
            logger.warn("Невірний формат суми для поповнення '{}', користувач email='{}'", amountText, user.getEmail());
            return "❌ Сума повинна бути числом.";
        }

        user.setBalance(user.getBalance() + amount);
        boolean success = updateUserBalance(user);

        if (success) {
            logger.info("Баланс успішно поповнено на {} для користувача email='{}'", amount, user.getEmail());
            return "✅ Баланс успішно поповнено на " + amount;
        } else {
            logger.error("Помилка при поповненні балансу на {} для користувача email='{}'", amount, user.getEmail());
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
                logger.info("Отримано баланс {} для користувача з id={}", balance, userId);
                return balance;
            }
        } catch (SQLException e) {
            logger.error("Помилка отримання балансу для користувача з id={}: {}", userId, e.getMessage(), e);
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
                logger.info("Отримано id={} для користувача з email='{}'", id, email);
                return id;
            }

        } catch (SQLException e) {
            logger.error("Помилка отримання id користувача за email='{}': {}", email, e.getMessage(), e);
        }

        return -1;
    }

}
