package com.sabat.deposit.service;

import com.sabat.deposit.db.Database;
import com.sabat.deposit.model.User;
import org.junit.jupiter.api.*;
import org.mindrot.jbcrypt.BCrypt;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.MockedStatic;

import java.io.OutputStream;
import java.io.PrintStream;
import java.sql.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceTest {
    private final PrintStream originalErr = System.err;

    @BeforeEach
    void muteSystemErr() {
        System.setErr(new PrintStream(new OutputStream() {
            @Override
            public void write(int b) {

            }
        }));
    }

    @AfterEach
    void restoreSystemErr() {
        System.setErr(originalErr);
    }
    @Mock
    Connection mockConnection;

    @Mock
    PreparedStatement mockCheckStmt;

    @Mock
    PreparedStatement mockInsertStmt;

    @Mock
    ResultSet mockResultSet;

    MockedStatic<Database> mockedDatabase;

    @BeforeEach
    void setUp() throws SQLException {
        MockitoAnnotations.openMocks(this);

        mockedDatabase = mockStatic(Database.class);
        mockedDatabase.when(Database::getConnection).thenReturn(mockConnection);

        when(mockConnection.prepareStatement("SELECT 1 FROM users WHERE email = ? LIMIT 1"))
                .thenReturn(mockCheckStmt);
        when(mockConnection.prepareStatement("INSERT INTO users (name, surname, email, password, balance) VALUES (?, ?, ?, ?, ?)"))
                .thenReturn(mockInsertStmt);

        when(mockCheckStmt.executeQuery()).thenReturn(mockResultSet);

        when(mockResultSet.next()).thenReturn(false);
        when(mockInsertStmt.executeUpdate()).thenReturn(1);
    }

    @AfterEach
    void tearDown() {
        mockedDatabase.close();
    }

    @Test
    void userExists_ShouldReturnTrue_WhenUserFound() throws SQLException {
        when(mockResultSet.next()).thenReturn(true);

        boolean exists = UserService.userExists("existing@example.com");
        assertTrue(exists);

        verify(mockCheckStmt).setString(1, "existing@example.com");
        verify(mockCheckStmt).executeQuery();
    }

    @Test
    void userExists_ShouldReturnFalse_WhenUserNotFound() throws SQLException {
        when(mockResultSet.next()).thenReturn(false);

        boolean exists = UserService.userExists("notfound@example.com");
        assertFalse(exists);

        verify(mockCheckStmt).setString(1, "notfound@example.com");
        verify(mockCheckStmt).executeQuery();
    }

    @Test
    void registerUser_ShouldReturnFalse_WhenUserAlreadyExists() throws SQLException {
        when(mockResultSet.next()).thenReturn(true);

        User user = new User(0, "John", "Doe", "existing@example.com", "pass");

        boolean result = UserService.registerUser(user);
        assertFalse(result);

        verify(mockCheckStmt).setString(1, "existing@example.com");
        verify(mockCheckStmt).executeQuery();

        verify(mockInsertStmt, never()).executeUpdate();
    }

    @Test
    void registerUser_ShouldReturnTrue_WhenNewUser() throws SQLException {
        when(mockResultSet.next()).thenReturn(false);

        User user = new User(0, "John", "Doe", "new@example.com", "pass");
        user.setBalance(100.0);

        boolean result = UserService.registerUser(user);
        assertTrue(result);

        verify(mockCheckStmt).setString(1, "new@example.com");
        verify(mockCheckStmt).executeQuery();

        verify(mockInsertStmt).setString(1, "John");
        verify(mockInsertStmt).setString(2, "Doe");
        verify(mockInsertStmt).setString(3, "new@example.com");
        verify(mockInsertStmt).setString(4, "pass");
        verify(mockInsertStmt).setDouble(5, 100.0);
        verify(mockInsertStmt).executeUpdate();
    }


    @Test
    void loginUser_ShouldReturnNull_WhenUserNotFound() throws SQLException {
        String sql = "SELECT * FROM users WHERE email = ?";

        PreparedStatement mockLoginStmt = mock(PreparedStatement.class);
        ResultSet mockLoginRs = mock(ResultSet.class);

        when(mockConnection.prepareStatement(sql)).thenReturn(mockLoginStmt);

        when(mockLoginStmt.executeQuery()).thenReturn(mockLoginRs);

        when(mockLoginRs.next()).thenReturn(false);

        User user = UserService.loginUser("wrong@example.com", "wrongpass");

        assertNull(user);

        verify(mockLoginStmt).setString(1, "wrong@example.com");

        verify(mockLoginStmt, never()).setString(eq(2), anyString());

        verify(mockLoginStmt).executeQuery();
    }



    @Test
    void loginUser_ShouldReturnUser_WhenCredentialsCorrect() throws SQLException {
        String sql = "SELECT * FROM users WHERE email = ?";

        PreparedStatement mockStmt = mock(PreparedStatement.class);
        ResultSet mockRs = mock(ResultSet.class);

        when(mockConnection.prepareStatement(sql)).thenReturn(mockStmt);
        when(mockStmt.executeQuery()).thenReturn(mockRs);

        when(mockRs.next()).thenReturn(true);
        when(mockRs.getInt("id")).thenReturn(1);
        when(mockRs.getString("name")).thenReturn("John");
        when(mockRs.getString("surname")).thenReturn("Doe");
        when(mockRs.getString("email")).thenReturn("login@example.com");
        // Тут має бути захешований пароль, оскільки BCrypt перевіряє хеш
        String hashedPassword = BCrypt.hashpw("pass", BCrypt.gensalt());
        when(mockRs.getString("password")).thenReturn(hashedPassword);
        when(mockRs.getDouble("balance")).thenReturn(500.0);
        when(mockRs.getString("role")).thenReturn("user");

        User user = UserService.loginUser("login@example.com", "pass");

        assertNotNull(user);
        assertEquals(1, user.getId());
        assertEquals("John", user.getName());
        assertEquals("Doe", user.getSurname());
        assertEquals("login@example.com", user.getEmail());
        assertEquals(hashedPassword, user.getPassword());
        assertEquals(500.0, user.getBalance());

        verify(mockStmt).setString(1, "login@example.com");
        verify(mockStmt).executeQuery();
    }


    @Test
    void updateUserBalance_ShouldReturnTrue_WhenUpdateSuccess() throws SQLException {
        when(mockConnection.prepareStatement("UPDATE users SET balance = ? WHERE email = ?"))
                .thenReturn(mockInsertStmt);

        when(mockInsertStmt.executeUpdate()).thenReturn(1);

        User user = new User(0, "Bob", "Brown", "bob@example.com", "pass");
        user.setBalance(1000);

        boolean updated = UserService.updateUserBalance(user);
        assertTrue(updated);

        verify(mockInsertStmt).setDouble(1, 1000);
        verify(mockInsertStmt).setString(2, "bob@example.com");
        verify(mockInsertStmt).executeUpdate();
    }

    @Test
    void getBalanceByUserId_ShouldReturnBalance_WhenUserExists() throws SQLException {
        when(mockConnection.prepareStatement("SELECT balance FROM users WHERE id = ?"))
                .thenReturn(mockCheckStmt);
        when(mockCheckStmt.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getDouble("balance")).thenReturn(1234.56);

        double balance = UserService.getBalanceByUserId(1);
        assertEquals(1234.56, balance);

        verify(mockCheckStmt).setInt(1, 1);
        verify(mockCheckStmt).executeQuery();
    }

    @Test
    void getBalanceByUserId_ShouldReturnZero_WhenUserNotFound() throws SQLException {
        when(mockConnection.prepareStatement("SELECT balance FROM users WHERE id = ?"))
                .thenReturn(mockCheckStmt);
        when(mockCheckStmt.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false);

        double balance = UserService.getBalanceByUserId(999);
        assertEquals(0.0, balance);

        verify(mockCheckStmt).setInt(1, 999);
        verify(mockCheckStmt).executeQuery();
    }

    @Test
    void getUserIdByEmail_ShouldReturnId_WhenUserExists() throws SQLException {
        try (MockedStatic<DriverManager> mockedDriverManager = mockStatic(DriverManager.class)) {
            PreparedStatement mockStmt = mock(PreparedStatement.class);
            ResultSet mockRs = mock(ResultSet.class);

            mockedDriverManager.when(() -> DriverManager.getConnection(anyString())).thenReturn(mockConnection);
            when(mockConnection.prepareStatement("SELECT id FROM users WHERE email = ?")).thenReturn(mockStmt);
            when(mockStmt.executeQuery()).thenReturn(mockRs);
            when(mockRs.next()).thenReturn(true);
            when(mockRs.getInt("id")).thenReturn(42);

            int id = UserService.getUserIdByEmail("user@example.com");
            assertEquals(42, id);

            verify(mockStmt).setString(1, "user@example.com");
            verify(mockStmt).executeQuery();
        }
    }

    @Test
    void getUserIdByEmail_ShouldReturnMinusOne_WhenUserNotFound() throws SQLException {
        try (MockedStatic<DriverManager> mockedDriverManager = mockStatic(DriverManager.class)) {
            PreparedStatement mockStmt = mock(PreparedStatement.class);
            ResultSet mockRs = mock(ResultSet.class);

            mockedDriverManager.when(() -> DriverManager.getConnection(anyString())).thenReturn(mockConnection);
            when(mockConnection.prepareStatement("SELECT id FROM users WHERE email = ?")).thenReturn(mockStmt);
            when(mockStmt.executeQuery()).thenReturn(mockRs);
            when(mockRs.next()).thenReturn(false);

            int id = UserService.getUserIdByEmail("missing@example.com");
            assertEquals(-1, id);

            verify(mockStmt).setString(1, "missing@example.com");
            verify(mockStmt).executeQuery();
        }
    }

    @Test
    void userExists_ShouldReturnFalse_WhenSQLExceptionThrown() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("DB error"));

        boolean result = UserService.userExists("any@example.com");

        assertFalse(result);
    }

    @Test
    void registerUser_ShouldReturnFalse_WhenSQLExceptionThrown() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("DB error"));

        User user = new User(0, "John", "Doe", "new@example.com", "pass");

        boolean result = UserService.registerUser(user);

        assertFalse(result);
    }

    @Test
    void loginUser_ShouldReturnNull_WhenSQLExceptionThrown() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("DB error"));

        User user = UserService.loginUser("email@example.com", "pass");

        assertNull(user);
    }

    @Test
    void updateUserBalance_ShouldReturnFalse_WhenSQLExceptionThrown() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("DB error"));

        User user = new User(0, "John", "Doe", "email@example.com", "pass");
        user.setBalance(100);

        boolean result = UserService.updateUserBalance(user);

        assertFalse(result);
    }

    @Test
    void getBalanceByUserId_ShouldReturnZero_WhenSQLExceptionThrown() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("DB error"));

        double balance = UserService.getBalanceByUserId(1);

        assertEquals(0.0, balance);
    }




    @Test
    void topUpBalance_ShouldReturnSuccessMessage_WhenValidAmountAndUpdateSucceeds() throws SQLException {
        User user = new User(1, "Jane", "Doe", "jane@example.com", "pass");
        user.setBalance(100);

        when(mockConnection.prepareStatement("UPDATE users SET balance = ? WHERE email = ?"))
                .thenReturn(mockInsertStmt);
        when(mockInsertStmt.executeUpdate()).thenReturn(1);

        String message = UserService.topUpBalance(user, "50");

        assertEquals("✅ Баланс успішно поповнено на 50.0", message);
        assertEquals(150.0, user.getBalance());
    }

    @Test
    void topUpBalance_ShouldReturnError_WhenAmountIsEmpty() {
        User user = new User(1, "Jane", "Doe", "jane@example.com", "pass");

        String message = UserService.topUpBalance(user, "");
        assertEquals("❌ Введіть суму для поповнення.", message);
    }


    @Test
    void topUpBalance_ShouldReturnError_WhenAmountIsEmptyy() {
        User user = new User(1, "Jane", "Doe", "jane@example.com", "pass");

        String message = UserService.topUpBalance(user, "500000");
        assertEquals("❌ Ви перевищили дозволений ліміт на балансі", message);
    }

    @Test
    void topUpBalance_ShouldReturnError_WhenAmountIsNotNumber() {
        User user = new User(1, "Jane", "Doe", "jane@example.com", "pass");

        String message = UserService.topUpBalance(user, "abc");
        assertEquals("❌ Сума повинна бути числом.", message);
    }

    @Test
    void topUpBalance_ShouldReturnError_WhenAmountIsZeroOrNegative() {
        User user = new User(1, "Jane", "Doe", "jane@example.com", "pass");

        String zeroMessage = UserService.topUpBalance(user, "0");
        assertEquals("❌ Сума повинна бути додатним числом.", zeroMessage);

        String negativeMessage = UserService.topUpBalance(user, "-10");
        assertEquals("❌ Сума повинна бути додатним числом.", negativeMessage);
    }




    @Test
    void topUpBalance_ShouldReturnError_WhenBalanceUpdateFails() throws SQLException {
        User user = new User(1, "Jane", "Doe", "jane@example.com", "pass");
        user.setBalance(200);

        when(mockConnection.prepareStatement("UPDATE users SET balance = ? WHERE email = ?"))
                .thenReturn(mockInsertStmt);
        when(mockInsertStmt.executeUpdate()).thenReturn(0);

        String message = UserService.topUpBalance(user, "100");

        assertEquals("❌ Сталася помилка при поповненні балансу.", message);
        assertEquals(300.0, user.getBalance());
    }



    @Test
    void topUpBalance_ShouldReturnError_WhenAmountIsNull() {
        User user = new User(1, "Jane", "Doe", "jane@example.com", "pass");
        String message = UserService.topUpBalance(user, null);
        assertEquals("❌ Введіть суму для поповнення.", message);
    }



    @Test
    void updateUserBalance_ShouldReturnFalse_WhenExecuteUpdateReturnsZero() throws SQLException {
        when(mockConnection.prepareStatement("UPDATE users SET balance = ? WHERE email = ?"))
                .thenReturn(mockInsertStmt);
        when(mockInsertStmt.executeUpdate()).thenReturn(0);

        User user = new User(0, "Charlie", "Brown", "charlie@example.com", "pass");
        user.setBalance(500);

        boolean updated = UserService.updateUserBalance(user);
        assertFalse(updated);

        verify(mockInsertStmt).executeUpdate();
    }



    @Test
    void loginUser_ShouldReturnNull_WhenSQLExceptionThrownDuringExecuteQuery() throws SQLException {
        PreparedStatement mockLoginStmt = mock(PreparedStatement.class);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockLoginStmt);
        when(mockLoginStmt.executeQuery()).thenThrow(new SQLException("Query fail"));

        User user = UserService.loginUser("fail@example.com", "pass");
        assertNull(user);
    }

    @Test
    void loginUser_ShouldReturnNull_WhenSQLExceptionThrownDuringExecuteQuery_Original() throws SQLException {
        PreparedStatement mockLoginStmt = mock(PreparedStatement.class);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockLoginStmt); // More generic
        when(mockLoginStmt.executeQuery()).thenThrow(new SQLException("Query fail"));

        User user = UserService.loginUser("fail@example.com", "pass");
        assertNull(user);
    }



    @Test
    void userExists_ShouldReturnFalse_WhenExecuteQueryThrowsSQLException() throws SQLException {
        when(mockConnection.prepareStatement("SELECT 1 FROM users WHERE email = ? LIMIT 1")).thenReturn(mockCheckStmt);
        when(mockCheckStmt.executeQuery()).thenThrow(new SQLException("Query execution failed"));

        boolean exists = UserService.userExists("any@example.com");
        assertFalse(exists);

        verify(mockConnection).prepareStatement("SELECT 1 FROM users WHERE email = ? LIMIT 1");
        verify(mockCheckStmt).setString(1, "any@example.com");
        verify(mockCheckStmt).executeQuery();
    }

    @Test
    void registerUser_ShouldReturnFalse_WhenPrepareStatementForInsertThrowsSQLException() throws SQLException {
        when(mockCheckStmt.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false);

        when(mockConnection.prepareStatement("INSERT INTO users (name, surname, email, password, balance) VALUES (?, ?, ?, ?, ?)"))
                .thenThrow(new SQLException("DB error on prepareStatement for insert"));

        User user = new User(0, "John", "Doe", "new@example.com", "pass");
        boolean result = UserService.registerUser(user);
        assertFalse(result);

        verify(mockCheckStmt).setString(1, "new@example.com");
        verify(mockCheckStmt).executeQuery();
    }

    @Test
    void registerUser_ShouldReturnFalse_WhenExecuteUpdateThrowsSQLException() throws SQLException {
        when(mockCheckStmt.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false);

        User user = new User(0, "John", "Doe", "new@example.com", "pass");
        user.setBalance(100.0);

        when(mockConnection.prepareStatement("INSERT INTO users (name, surname, email, password, balance) VALUES (?, ?, ?, ?, ?)"))
                .thenReturn(mockInsertStmt);
        when(mockInsertStmt.executeUpdate()).thenThrow(new SQLException("Insert failed"));

        boolean result = UserService.registerUser(user);
        assertFalse(result);

        verify(mockCheckStmt).setString(1, "new@example.com");
        verify(mockCheckStmt).executeQuery();

        verify(mockConnection).prepareStatement("INSERT INTO users (name, surname, email, password, balance) VALUES (?, ?, ?, ?, ?)");
        verify(mockInsertStmt).setString(1, user.getName());
        verify(mockInsertStmt).setString(2, user.getSurname());
        verify(mockInsertStmt).setString(3, user.getEmail());
        verify(mockInsertStmt).setString(4, user.getPassword());
        verify(mockInsertStmt).setDouble(5, user.getBalance());
        verify(mockInsertStmt).executeUpdate();
    }

    @Test
    void loginUser_ShouldReturnNull_WhenExecuteQueryThrowsSQLException() throws SQLException {
        String sql = "SELECT * FROM users WHERE email = ?";

        PreparedStatement mockLoginStmt = mock(PreparedStatement.class);
        when(mockConnection.prepareStatement(sql)).thenReturn(mockLoginStmt);
        when(mockLoginStmt.executeQuery()).thenThrow(new SQLException("Query execution failed for login"));

        User user = UserService.loginUser("fail@example.com", "pass");
        assertNull(user);

        verify(mockLoginStmt).setString(1, "fail@example.com");
        verify(mockLoginStmt).executeQuery();
        verify(mockLoginStmt, never()).setString(eq(2), anyString());
    }



    @Test
    void updateUserBalance_ShouldReturnFalse_WhenExecuteUpdateThrowsSQLException() throws SQLException {
        PreparedStatement mockUpdateStmt = mock(PreparedStatement.class);
        when(mockConnection.prepareStatement("UPDATE users SET balance = ? WHERE email = ?"))
                .thenReturn(mockUpdateStmt);
        when(mockUpdateStmt.executeUpdate()).thenThrow(new SQLException("Update execution failed"));

        User user = new User(0, "Bob", "Brown", "bob@example.com", "pass");
        user.setBalance(1000);

        boolean updated = UserService.updateUserBalance(user);
        assertFalse(updated);

        verify(mockUpdateStmt).setDouble(1, 1000);
        verify(mockUpdateStmt).setString(2, "bob@example.com");
        verify(mockUpdateStmt).executeUpdate();
    }




    @Test
    void getBalanceByUserId_ShouldReturnZero_WhenExecuteQueryThrowsSQLException() throws SQLException {
        PreparedStatement mockSelectBalanceStmt = mock(PreparedStatement.class);
        when(mockConnection.prepareStatement("SELECT balance FROM users WHERE id = ?"))
                .thenReturn(mockSelectBalanceStmt);
        when(mockSelectBalanceStmt.executeQuery()).thenThrow(new SQLException("Query execution failed for getBalance"));

        double balance = UserService.getBalanceByUserId(1);
        assertEquals(0.0, balance);

        verify(mockSelectBalanceStmt).setInt(1, 1);
        verify(mockSelectBalanceStmt).executeQuery();
    }


}
