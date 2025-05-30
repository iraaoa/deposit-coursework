package com.sabat.deposit.service;

import com.sabat.deposit.db.Database;
import com.sabat.deposit.model.Transaction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private Connection mockConnection;

    @Mock
    private PreparedStatement mockPreparedStatement;

    @Mock
    private ResultSet mockResultSet;

    @InjectMocks
    private TransactionService transactionService;

    private MockedStatic<Database> mockedDatabase;

    @BeforeEach
    void setUp() throws SQLException {
        mockedDatabase = Mockito.mockStatic(Database.class);
        mockedDatabase.when(Database::getConnection).thenReturn(mockConnection);


        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
    }

    @AfterEach
    void tearDown() {
        mockedDatabase.close();
    }

    @Test
    void addTransaction_success() throws SQLException {
        int userId = 1;
        String type = "DEPOSIT";
        String description = "Test deposit";
        double amount = 100.0;

        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        transactionService.addTransaction(userId, type, description, amount);


        mockedDatabase.verify(Database::getConnection);
        verify(mockConnection).prepareStatement(
                "INSERT INTO transactions (user_id, transaction_date, type, description, amount) " +
                        "VALUES (?, CURRENT_TIMESTAMP, ?, ?, ?)");
        verify(mockPreparedStatement).setInt(1, userId);
        verify(mockPreparedStatement).setString(2, type);
        verify(mockPreparedStatement).setString(3, description);
        verify(mockPreparedStatement).setDouble(4, amount);
        verify(mockPreparedStatement).executeUpdate();
        verify(mockPreparedStatement).close();
        verify(mockConnection).close();
    }

    @Test
    void addTransaction_throwsRuntimeException_onSqlException() throws SQLException {
        int userId = 1;
        String type = "DEPOSIT";
        String description = "Test deposit";
        double amount = 100.0;

        when(mockPreparedStatement.executeUpdate()).thenThrow(new SQLException("DB error"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            transactionService.addTransaction(userId, type, description, amount);
        });

        assertEquals("Помилка додавання транзакції: DB error", exception.getMessage());
        verify(mockPreparedStatement).close();
        verify(mockConnection).close();
    }

    @Test
    void getAllTransactionsForUser_success_returnsTransactions() throws SQLException {
        int userId = 1;
        Timestamp now = Timestamp.from(Instant.now());

        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true).thenReturn(true).thenReturn(false); // Два рядки
        when(mockResultSet.getInt("id")).thenReturn(101, 102);
        when(mockResultSet.getTimestamp("transaction_date")).thenReturn(now, now);
        when(mockResultSet.getString("type")).thenReturn("DEPOSIT", "WITHDRAWAL");
        when(mockResultSet.getString("description")).thenReturn("First transaction", "Second transaction");
        when(mockResultSet.getDouble("amount")).thenReturn(100.0, 50.0);

        // Act
        List<Transaction> transactions = transactionService.getAllTransactionsForUser(userId);

        // Assert
        assertNotNull(transactions);
        assertEquals(2, transactions.size());

        Transaction t1 = transactions.get(0);
        assertEquals(101, t1.getId());
        assertEquals(now, t1.getTransactionDate()); // Або getDate()
        assertEquals("DEPOSIT", t1.getType());
        assertEquals("First transaction", t1.getDescription());
        assertEquals(100.0, t1.getAmount());

        Transaction t2 = transactions.get(1);
        assertEquals(102, t2.getId());
        assertEquals(now, t2.getTransactionDate());
        assertEquals("WITHDRAWAL", t2.getType());
        assertEquals("Second transaction", t2.getDescription());
        assertEquals(50.0, t2.getAmount());

        mockedDatabase.verify(Database::getConnection);
        verify(mockConnection).prepareStatement("SELECT id, transaction_date, type, description, amount FROM transactions WHERE user_id = ?");
        verify(mockPreparedStatement).setInt(1, userId);
        verify(mockPreparedStatement).executeQuery();
        verify(mockResultSet, times(3)).next();
        verify(mockResultSet, times(2)).getInt("id");

        verify(mockResultSet).close();
        verify(mockPreparedStatement).close();
        verify(mockConnection).close();
    }

    @Test
    void getAllTransactionsForUser_success_returnsEmptyList_whenNoTransactions() throws SQLException {
        // Arrange
        int userId = 1;
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false); // Немає рядків

        List<Transaction> transactions = transactionService.getAllTransactionsForUser(userId);

        assertNotNull(transactions);
        assertTrue(transactions.isEmpty());

        verify(mockPreparedStatement).setInt(1, userId);
        verify(mockPreparedStatement).executeQuery();
        verify(mockResultSet).close();
        verify(mockPreparedStatement).close();
        verify(mockConnection).close();
    }

    @Test
    void getAllTransactionsForUser_throwsRuntimeException_onSqlException() throws SQLException {
        int userId = 1;
        when(mockPreparedStatement.executeQuery()).thenThrow(new SQLException("DB query error"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            transactionService.getAllTransactionsForUser(userId);
        });

        assertEquals("Помилка отримання транзакцій для користувача " + userId + ": DB query error", exception.getMessage());

        verify(mockPreparedStatement).close();
        verify(mockConnection).close();
    }
}