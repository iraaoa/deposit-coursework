package com.sabat.deposit.service;

import com.sabat.deposit.db.Database;
import com.sabat.deposit.model.Deposit;


import com.sabat.deposit.model.User;
import com.sabat.deposit.session.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DepositServiceTest {

    private static final String DB_URL = "jdbc:h2:mem:testdb";

    @Mock private Connection mockConnection;
    @Mock private Statement mockPragmaStatement;
    @Mock private Statement mockGetAllDepositsStatement;
    @Mock private ResultSet mockResultSet;
    @Mock private PreparedStatement mockPreparedStatement;

    @Mock private Connection mockStaticDbConnection;
    @Mock private PreparedStatement mockSaveDepositPStmt;


    private DepositService depositService;
    private MockedStatic<DriverManager> mockedDriverManager;
    private MockedStatic<Database> mockedStaticDatabaseUtil; // Для мокування Database.getConnection()

    @BeforeEach
    void setUp() throws SQLException {
        mockedDriverManager = Mockito.mockStatic(DriverManager.class);
        lenient().when(DriverManager.getConnection(anyString())).thenReturn(mockConnection);

        lenient().when(mockConnection.createStatement())
                .thenReturn(mockPragmaStatement)
                .thenReturn(mockGetAllDepositsStatement);
        lenient().when(mockPragmaStatement.execute(startsWith("PRAGMA foreign_keys"))).thenReturn(true);

        mockedStaticDatabaseUtil = Mockito.mockStatic(Database.class);
        mockedStaticDatabaseUtil.when(Database::getConnection).thenReturn(mockStaticDbConnection);
        lenient().when(mockStaticDbConnection.prepareStatement(anyString())).thenReturn(mockSaveDepositPStmt);

        depositService = new DepositService(DB_URL);
    }

    @AfterEach
    void tearDown() {
        mockedDriverManager.close();
        if (mockedStaticDatabaseUtil != null) {
            mockedStaticDatabaseUtil.close();
        }
        Mockito.validateMockitoUsage();
    }

    private Deposit createTestDeposit(int id, String name, double rate, int term, double minAmount) {
        return new Deposit(id, name, "Term", rate, term, "TestBank", 1, 1, minAmount);
    }


    @Test
    void getAllDeposits_shouldReturnListOfDeposits() throws SQLException {
        when(mockGetAllDepositsStatement.executeQuery(eq("SELECT * FROM deposits"))).thenReturn(mockResultSet);

        when(mockResultSet.next()).thenReturn(true, true, false);
        when(mockResultSet.getInt("id")).thenReturn(1, 2);
        when(mockResultSet.getString("name")).thenReturn("Deposit A", "Deposit B");
        when(mockResultSet.getString("type")).thenReturn("Term", "Savings");
        when(mockResultSet.getDouble("interest_rate")).thenReturn(5.0, 3.0);
        when(mockResultSet.getInt("term")).thenReturn(365, 180);
        when(mockResultSet.getString("bank_name")).thenReturn("BankX", "BankY");
        when(mockResultSet.getInt("is_replenishable")).thenReturn(1, 0);
        when(mockResultSet.getInt("is_early_withdrawal")).thenReturn(1, 0);
        when(mockResultSet.getDouble("min_amount")).thenReturn(1000.0, 500.0);

        List<Deposit> deposits = depositService.getAllDeposits();

        assertEquals(2, deposits.size());
        assertEquals("Deposit A", deposits.get(0).getName());
        assertEquals("Deposit B", deposits.get(1).getName());

        verify(mockPragmaStatement).execute(startsWith("PRAGMA foreign_keys"));
        verify(mockGetAllDepositsStatement).executeQuery("SELECT * FROM deposits");
        verify(mockConnection, times(2)).createStatement();
    }

    @Test
    void getAllDeposits_shouldThrowRuntimeExceptionOnSqlError() throws SQLException {
        SQLException dbError = new SQLException("DB error");
        doThrow(dbError)
                .when(mockGetAllDepositsStatement).executeQuery(eq("SELECT * FROM deposits"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> depositService.getAllDeposits());
        assertTrue(exception.getMessage().contains("Помилка при читанні депозитів з БД"));
        assertSame(dbError, exception.getCause());

        verify(mockPragmaStatement).execute(startsWith("PRAGMA foreign_keys"));
        verify(mockGetAllDepositsStatement).executeQuery(eq("SELECT * FROM deposits"));
    }

    @Test
    void filterDeposits_byBankName() {
        Deposit d1 = createTestDeposit(1, "Alpha", 5.0, 365, 1000);
        d1.setBankName("PrivatBank");
        Deposit d2 = createTestDeposit(2, "Beta", 6.0, 180, 500);
        d2.setBankName("MonoBank");
        List<Deposit> deposits = Arrays.asList(d1, d2);

        List<Deposit> filtered = depositService.filterDeposits(deposits, "Privat");
        assertEquals(1, filtered.size());
        assertEquals("PrivatBank", filtered.get(0).getBankName());
    }

    @Test
    void filterDeposits_byDepositName() {
        Deposit d1 = createTestDeposit(1, "Alpha Plus", 5.0, 365, 1000);
        Deposit d2 = createTestDeposit(2, "Beta Max", 6.0, 180, 500);
        List<Deposit> deposits = Arrays.asList(d1, d2);

        List<Deposit> filtered = depositService.filterDeposits(deposits, "plus");
        assertEquals(1, filtered.size());
        assertEquals("Alpha Plus", filtered.get(0).getName());
    }

    @Test
    void filterDeposits_nullKeyword_returnsAll() {
        Deposit d1 = createTestDeposit(1, "Alpha", 5.0, 365, 1000);
        Deposit d2 = createTestDeposit(2, "Beta", 6.0, 180, 500);
        List<Deposit> deposits = Arrays.asList(d1, d2);

        List<Deposit> filtered = depositService.filterDeposits(deposits, null);
        assertEquals(2, filtered.size());
    }

    @Test
    void filterDeposits_emptyKeyword_returnsAll() {
        Deposit d1 = createTestDeposit(1, "Alpha", 5.0, 365, 1000);
        Deposit d2 = createTestDeposit(2, "Beta", 6.0, 180, 500);
        List<Deposit> deposits = Arrays.asList(d1, d2);

        List<Deposit> filtered = depositService.filterDeposits(deposits, "  ");
        assertEquals(2, filtered.size());
    }

    @Test
    void filterDeposits_nullDepositsList_returnsEmptyList() {
        List<Deposit> filtered = depositService.filterDeposits(null, "keyword");
        assertTrue(filtered.isEmpty());
    }


    @Test
    void sortDeposits_byInterestRateAsc() {
        Deposit d1 = createTestDeposit(1, "LowRate", 3.0, 365, 1000);
        Deposit d2 = createTestDeposit(2, "HighRate", 5.0, 180, 500);
        List<Deposit> deposits = Arrays.asList(d1, d2);

        List<Deposit> sorted = depositService.sortDeposits(deposits, "Ставка (зрост.)");
        assertEquals(3.0, sorted.get(0).getInterestRate());
        assertEquals(5.0, sorted.get(1).getInterestRate());
    }



    @Test
    void sortDeposits_byInterestRateDesc() {
        Deposit d1 = createTestDeposit(1, "LowRate", 3.0, 365, 1000);
        Deposit d2 = createTestDeposit(2, "HighRate", 5.0, 180, 500);
        List<Deposit> deposits = Arrays.asList(d1, d2);

        List<Deposit> sorted = depositService.sortDeposits(deposits, "Ставка (спад.)");
        assertEquals(5.0, sorted.get(0).getInterestRate());
        assertEquals(3.0, sorted.get(1).getInterestRate());
    }


    @Test
    void sortDeposits_byTermAsc() {
        Deposit d1 = createTestDeposit(1, "LowTerm", 3.0, 365, 1000);
        Deposit d2 = createTestDeposit(2, "HighTerm", 5.0, 180, 500);
        List<Deposit> deposits = Arrays.asList(d1, d2);

        List<Deposit> sorted = depositService.sortDeposits(deposits, "Термін (зрост.)");
        assertEquals(180, sorted.get(0).getTerm());
        assertEquals(365, sorted.get(1).getTerm());
    }


    @Test
    void sortDeposits_byTermDesc() {
        Deposit d1 = createTestDeposit(1, "LowTerm", 3.0, 365, 1000);
        Deposit d2 = createTestDeposit(2, "HighTerm", 5.0, 180, 500);
        List<Deposit> deposits = Arrays.asList(d1, d2);

        List<Deposit> sorted = depositService.sortDeposits(deposits, "Термін (спад.)");
        assertEquals(365, sorted.get(0).getTerm());
        assertEquals(180, sorted.get(1).getTerm());
    }



    @Test
    void sortDeposits_bySumaAsc() {
        Deposit d1 = createTestDeposit(1, "LowSuma", 3.0, 365, 100);
        Deposit d2 = createTestDeposit(2, "HighSuma", 5.0, 180, 500);
        List<Deposit> deposits = Arrays.asList(d1, d2);

        List<Deposit> sorted = depositService.sortDeposits(deposits, "Мін. сума (зрост.)");
        assertEquals(100, sorted.get(0).getMinAmount());
        assertEquals(500, sorted.get(1).getMinAmount());
    }



    @Test
    void sortDeposits_bySumaDesc() {
        Deposit d1 = createTestDeposit(1, "LowSuma", 3.0, 365, 100);
        Deposit d2 = createTestDeposit(2, "HighSuma", 5.0, 180, 500);
        List<Deposit> deposits = Arrays.asList(d1, d2);

        List<Deposit> sorted = depositService.sortDeposits(deposits, "Мін. сума (спад.)");
        assertEquals(500, sorted.get(0).getMinAmount());
        assertEquals(100, sorted.get(1).getMinAmount());
    }


    @Test
    void sortDeposits_nullCriteria_returnsOriginalOrderCopy() {
        Deposit d1 = createTestDeposit(1, "A", 3.0, 365, 1000);
        Deposit d2 = createTestDeposit(2, "B", 5.0, 180, 500);
        List<Deposit> originalDeposits = Arrays.asList(d1, d2);

        List<Deposit> sorted = depositService.sortDeposits(new ArrayList<>(originalDeposits), null);
        assertEquals(2, sorted.size());
        assertEquals("A", sorted.get(0).getName());
        assertEquals("B", sorted.get(1).getName());
        assertNotSame(originalDeposits, sorted);
    }



    @Test
    public void testSortDepositsWithUnknownCriteria_returnsCopyUnchanged() {
        Deposit deposit1 = new Deposit(1, "Deposit A", "Type1", 3.5, 12, "Bank A", 1, 1, 1000);
        Deposit deposit2 = new Deposit(2, "Deposit B", "Type2", 4.0, 6, "Bank B", 0, 1, 500);
        List<Deposit> deposits = Arrays.asList(deposit1, deposit2);

        List<Deposit> result = depositService.sortDeposits(deposits, "Невідомий критерій");

        assertNotNull(result);
        assertNotSame(deposits, result);
        assertEquals(deposits, result);
    }

    @Test
    void openDepositForUserWithAmount_success() throws SQLException {
        Deposit depositToOpen = createTestDeposit(1, "TestDeposit", 5.0, 12, 100.0);
        int userId = 1;
        double amount = 200.0;

        // Мок для перевірки чи вже є депозит у користувача
        PreparedStatement mockUserHasDepositPStmt = mock(PreparedStatement.class);
        ResultSet mockUserHasDepositRs = mock(ResultSet.class);
        when(mockConnection.prepareStatement(startsWith("SELECT 1 FROM user_deposits"))).thenReturn(mockUserHasDepositPStmt);
        when(mockUserHasDepositPStmt.executeQuery()).thenReturn(mockUserHasDepositRs);
        when(mockUserHasDepositRs.next()).thenReturn(false); // немає відкритого депозиту

        // Мок для отримання балансу користувача
        PreparedStatement mockGetUserBalancePStmt = mock(PreparedStatement.class);
        ResultSet mockGetUserBalanceRs = mock(ResultSet.class);
        when(mockConnection.prepareStatement(eq("SELECT balance FROM users WHERE id = ?"))).thenReturn(mockGetUserBalancePStmt);
        when(mockGetUserBalancePStmt.executeQuery()).thenReturn(mockGetUserBalanceRs);
        when(mockGetUserBalanceRs.next()).thenReturn(true);
        when(mockGetUserBalanceRs.getDouble("balance")).thenReturn(500.0);

        // Мок для оновлення балансу користувача
        PreparedStatement mockUpdateUserBalancePStmt = mock(PreparedStatement.class);
        when(mockConnection.prepareStatement(eq("UPDATE users SET balance = ? WHERE id = ?"))).thenReturn(mockUpdateUserBalancePStmt);
        when(mockUpdateUserBalancePStmt.executeUpdate()).thenReturn(1);

        // Мок для вставки нового депозиту
        String insertSql = "INSERT INTO user_deposits (user_id, deposit_id, opened_at, balance, last_interest_accrued, finish_date) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        PreparedStatement mockInsertDepositPStmt = mock(PreparedStatement.class);
        when(mockConnection.prepareStatement(eq(insertSql))).thenReturn(mockInsertDepositPStmt);
        when(mockInsertDepositPStmt.executeUpdate()).thenReturn(1);

        // Виклик
        assertDoesNotThrow(() -> depositService.openDepositForUserWithAmount(userId, depositToOpen, amount));

        // Верифікація
        verify(mockConnection).setAutoCommit(false);
        verify(mockUserHasDepositPStmt).setInt(1, userId);
        verify(mockUserHasDepositPStmt).setInt(2, depositToOpen.getId());

        verify(mockGetUserBalancePStmt).setInt(1, userId);

        verify(mockUpdateUserBalancePStmt).setDouble(1, 300.0); // 500 - 200
        verify(mockUpdateUserBalancePStmt).setInt(2, userId);

        verify(mockInsertDepositPStmt).setInt(1, userId);
        verify(mockInsertDepositPStmt).setInt(2, depositToOpen.getId());
        verify(mockInsertDepositPStmt).setString(eq(3), anyString()); // opened_at
        verify(mockInsertDepositPStmt).setDouble(4, amount);          // balance
        verify(mockInsertDepositPStmt).setString(eq(5), anyString()); // last_interest_accrued
        verify(mockInsertDepositPStmt).setString(eq(6), anyString()); // finish_date

        verify(mockInsertDepositPStmt).executeUpdate();

        verify(mockConnection).commit();
        verify(mockConnection, never()).rollback();
    }


    @Test
    void openDepositForUserWithAmount_amountLessThanMin() throws SQLException {
        Deposit depositToOpen = createTestDeposit(1, "TestDeposit", 5.0, 365, 100.0);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> depositService.openDepositForUserWithAmount(1, depositToOpen, 50.0));
        assertEquals("Сума менша за мінімально дозволену для цього депозиту.", ex.getMessage());
        verify(mockConnection, never()).setAutoCommit(anyBoolean());
    }

    @Test
    void openDepositForUserWithAmount_userAlreadyHasDeposit() throws SQLException {
        Deposit depositToOpen = createTestDeposit(1, "TestDeposit", 5.0, 365, 100.0);

        PreparedStatement mockUserHasDepositPStmt = mock(PreparedStatement.class);
        ResultSet mockUserHasDepositRs = mock(ResultSet.class);
        when(mockConnection.prepareStatement(startsWith("SELECT 1 FROM user_deposits"))).thenReturn(mockUserHasDepositPStmt);
        when(mockUserHasDepositPStmt.executeQuery()).thenReturn(mockUserHasDepositRs);
        when(mockUserHasDepositRs.next()).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> depositService.openDepositForUserWithAmount(1, depositToOpen, 150.0));
        assertEquals("Ви вже відкрили цей депозит раніше.", ex.getMessage());
    }

    @Test
    void openDepositForUserWithAmount_insufficientFunds() throws SQLException {
        Deposit depositToOpen = createTestDeposit(1, "TestDeposit", 5.0, 365, 100.0);
        int userId = 1;
        double amount = 200.0;

        PreparedStatement mockUserHasDepositPStmt = mock(PreparedStatement.class);
        ResultSet mockUserHasDepositRs = mock(ResultSet.class);
        when(mockConnection.prepareStatement(startsWith("SELECT 1 FROM user_deposits"))).thenReturn(mockUserHasDepositPStmt);
        when(mockUserHasDepositPStmt.executeQuery()).thenReturn(mockUserHasDepositRs);
        when(mockUserHasDepositRs.next()).thenReturn(false);

        PreparedStatement mockGetUserBalancePStmt = mock(PreparedStatement.class);
        ResultSet mockGetUserBalanceRs = mock(ResultSet.class);
        when(mockConnection.prepareStatement(eq("SELECT balance FROM users WHERE id = ?"))).thenReturn(mockGetUserBalancePStmt);
        when(mockGetUserBalancePStmt.executeQuery()).thenReturn(mockGetUserBalanceRs);
        when(mockGetUserBalanceRs.next()).thenReturn(true);
        when(mockGetUserBalanceRs.getDouble("balance")).thenReturn(150.0); // Insufficient

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> depositService.openDepositForUserWithAmount(userId, depositToOpen, amount));
        assertEquals("Недостатньо коштів на балансі для відкриття депозиту.", ex.getMessage());
        verify(mockConnection).setAutoCommit(false);
        verify(mockConnection, never()).commit();
        verify(mockConnection, never()).rollback();
    }


    @Test
    void topUpDeposit_success() throws SQLException {
        Deposit depositToTopUp = createTestDeposit(1, "TestDeposit", 5.0, 365, 100.0);
        depositToTopUp.setIsReplenishable(1);
        int userId = 1;
        double topUpAmount = 100.0; // Для бонусу
        double expectedBonus = topUpAmount * 0.05;
        double expectedTotalTopUp = topUpAmount + expectedBonus;


        PreparedStatement mockSelectUserBalancePStmtLocal = mock(PreparedStatement.class);
        ResultSet mockUserBalanceRs = mock(ResultSet.class);
        when(mockConnection.prepareStatement(eq("SELECT balance FROM users WHERE id = ?"))).thenReturn(mockSelectUserBalancePStmtLocal);
        when(mockSelectUserBalancePStmtLocal.executeQuery()).thenReturn(mockUserBalanceRs);
        when(mockUserBalanceRs.next()).thenReturn(true);
        when(mockUserBalanceRs.getDouble("balance")).thenReturn(200.0);

        PreparedStatement mockUpdateUserBalancePStmtLocal = mock(PreparedStatement.class);
        when(mockConnection.prepareStatement(eq("UPDATE users SET balance = balance - ? WHERE id = ?"))).thenReturn(mockUpdateUserBalancePStmtLocal);
        when(mockUpdateUserBalancePStmtLocal.executeUpdate()).thenReturn(1);

        PreparedStatement mockUpdateDepositPStmtLocal = mock(PreparedStatement.class);
        when(mockConnection.prepareStatement(eq("UPDATE user_deposits SET balance = balance + ? WHERE user_id = ? AND deposit_id = ?"))).thenReturn(mockUpdateDepositPStmtLocal);
        when(mockUpdateDepositPStmtLocal.executeUpdate()).thenReturn(1);

        assertDoesNotThrow(() -> depositService.topUpDeposit(userId, depositToTopUp, topUpAmount));

        verify(mockConnection).setAutoCommit(false);
        verify(mockSelectUserBalancePStmtLocal).setInt(1, userId);
        verify(mockUpdateUserBalancePStmtLocal).setDouble(1, topUpAmount);
        verify(mockUpdateUserBalancePStmtLocal).setInt(2, userId);

        ArgumentCaptor<Double> captor = ArgumentCaptor.forClass(Double.class);
        verify(mockUpdateDepositPStmtLocal).setDouble(eq(1), captor.capture());
        assertEquals(expectedTotalTopUp, captor.getValue(), 0.001); // Перевірка суми з бонусом

        verify(mockUpdateDepositPStmtLocal).setInt(2, userId);
        verify(mockUpdateDepositPStmtLocal).setInt(3, depositToTopUp.getId());
        verify(mockConnection).commit();
        verify(mockConnection, never()).rollback();
        verify(mockConnection).setAutoCommit(true);
        verify(mockConnection).close();

        verify(mockPragmaStatement).close();
        verify(mockSelectUserBalancePStmtLocal).close();
        verify(mockUserBalanceRs).close();
        verify(mockUpdateUserBalancePStmtLocal).close();
        verify(mockUpdateDepositPStmtLocal).close();
    }

    @Test
    void topUpDeposit_notReplenishable() throws SQLException {
        Deposit deposit = createTestDeposit(1, "NonReplen", 5.0, 365, 100);
        deposit.setIsReplenishable(0);
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> depositService.topUpDeposit(1, deposit, 50.0));
        assertEquals("Цей депозит не підтримує поповнення.", ex.getMessage());

    }

    @Test
    void topUpDeposit_insufficientUserFunds() throws SQLException {
        Deposit depositToTopUp = createTestDeposit(1, "TestDeposit", 5.0, 365, 100.0);
        depositToTopUp.setIsReplenishable(1);
        int userId = 1;
        double topUpAmount = 150.0;

        PreparedStatement mockSelectUserBalancePStmtLocal = mock(PreparedStatement.class);
        ResultSet mockUserBalanceRs = mock(ResultSet.class);
        when(mockConnection.prepareStatement(eq("SELECT balance FROM users WHERE id = ?"))).thenReturn(mockSelectUserBalancePStmtLocal);
        when(mockSelectUserBalancePStmtLocal.executeQuery()).thenReturn(mockUserBalanceRs);
        when(mockUserBalanceRs.next()).thenReturn(true);
        when(mockUserBalanceRs.getDouble("balance")).thenReturn(100.0);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> depositService.topUpDeposit(userId, depositToTopUp, topUpAmount));
        assertEquals("Недостатньо коштів на балансі користувача.", ex.getMessage());

        verify(mockConnection).setAutoCommit(false);
        verify(mockConnection, never()).commit();
        verify(mockConnection, never()).rollback();
        verify(mockConnection).setAutoCommit(true);
        verify(mockConnection).close();

        verify(mockPragmaStatement).close();
        verify(mockSelectUserBalancePStmtLocal).close();
        verify(mockUserBalanceRs).close();
    }


    @Test
    void topUpDeposit_sqlErrorDuringUpdate_shouldRollback() throws SQLException {
        Deposit depositToTopUp = createTestDeposit(1, "TestDeposit", 5.0, 365, 100.0);
        depositToTopUp.setIsReplenishable(1);
        int userId = 1;
        double topUpAmount = 50.0;
        SQLException dbUpdateFailed = new SQLException("DB update failed");


        PreparedStatement mockSelectUserBalancePStmtLocal = mock(PreparedStatement.class);
        ResultSet mockUserBalanceRs = mock(ResultSet.class);
        when(mockConnection.prepareStatement(eq("SELECT balance FROM users WHERE id = ?"))).thenReturn(mockSelectUserBalancePStmtLocal);
        when(mockSelectUserBalancePStmtLocal.executeQuery()).thenReturn(mockUserBalanceRs);
        when(mockUserBalanceRs.next()).thenReturn(true);
        when(mockUserBalanceRs.getDouble("balance")).thenReturn(200.0);

        PreparedStatement mockUpdateUserBalancePStmtLocal = mock(PreparedStatement.class);
        when(mockConnection.prepareStatement(eq("UPDATE users SET balance = balance - ? WHERE id = ?"))).thenReturn(mockUpdateUserBalancePStmtLocal);
        doThrow(dbUpdateFailed).when(mockUpdateUserBalancePStmtLocal).executeUpdate();

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> depositService.topUpDeposit(userId, depositToTopUp, topUpAmount));
        assertTrue(ex.getMessage().contains("Помилка при поповненні депозиту"));
        assertSame(dbUpdateFailed, ex.getCause());

        verify(mockConnection).setAutoCommit(false);
        verify(mockSelectUserBalancePStmtLocal).executeQuery();
        verify(mockUpdateUserBalancePStmtLocal).executeUpdate();
        verify(mockConnection).rollback();
        verify(mockConnection, never()).commit();
        verify(mockConnection).setAutoCommit(true);
        verify(mockConnection).close();

        verify(mockPragmaStatement).close();
        verify(mockSelectUserBalancePStmtLocal).close();
        verify(mockUserBalanceRs).close();
        verify(mockUpdateUserBalancePStmtLocal).close();
    }


    @Test
    void withdrawFromDeposit_successPartialWithdrawal() throws SQLException {
        Deposit deposit = createTestDeposit(1, "WithdrawTest", 5.0, 365, 100.0);
        deposit.setIsEarlyWithdrawal(1);
        int userId = 1;
        double withdrawAmount = 50.0;
        double initialDepositBalance = 200.0;
        double initialUserBalance = 100.0;

        double penalty = withdrawAmount * 0.05; // 2.5
        double amountAfterPenalty = withdrawAmount - penalty; // 47.5
        double expectedNewDepositBalance = initialDepositBalance - withdrawAmount; // 150.0
        double expectedNewUserBalance = initialUserBalance + amountAfterPenalty; // 147.5

        PreparedStatement mockSelectDepositBalancePStmt = mock(PreparedStatement.class);
        ResultSet mockDepositBalanceRs = mock(ResultSet.class);
        when(mockConnection.prepareStatement(eq("SELECT balance FROM user_deposits WHERE user_id = ? AND deposit_id = ?"))).thenReturn(mockSelectDepositBalancePStmt);
        when(mockSelectDepositBalancePStmt.executeQuery()).thenReturn(mockDepositBalanceRs);
        when(mockDepositBalanceRs.next()).thenReturn(true);
        when(mockDepositBalanceRs.getDouble("balance")).thenReturn(initialDepositBalance);

        PreparedStatement mockUpdateDepositBalancePStmt = mock(PreparedStatement.class);
        when(mockConnection.prepareStatement(eq("UPDATE user_deposits SET balance = ? WHERE user_id = ? AND deposit_id = ?"))).thenReturn(mockUpdateDepositBalancePStmt);
        when(mockUpdateDepositBalancePStmt.executeUpdate()).thenReturn(1);

        PreparedStatement mockGetUserBalancePStmt = mock(PreparedStatement.class);
        ResultSet mockUserBalanceRs = mock(ResultSet.class);
        when(mockConnection.prepareStatement(eq("SELECT balance FROM users WHERE id = ?"))).thenReturn(mockGetUserBalancePStmt);
        when(mockGetUserBalancePStmt.executeQuery()).thenReturn(mockUserBalanceRs);
        when(mockUserBalanceRs.next()).thenReturn(true);
        when(mockUserBalanceRs.getDouble("balance")).thenReturn(initialUserBalance);

        PreparedStatement mockUpdateUserBalancePStmt = mock(PreparedStatement.class);
        when(mockConnection.prepareStatement(eq("UPDATE users SET balance = ? WHERE id = ?"))).thenReturn(mockUpdateUserBalancePStmt);
        when(mockUpdateUserBalancePStmt.executeUpdate()).thenReturn(1);

        assertDoesNotThrow(() -> depositService.withdrawFromDeposit(userId, deposit, withdrawAmount));

        verify(mockUpdateDepositBalancePStmt).setDouble(1, expectedNewDepositBalance);
        verify(mockUpdateDepositBalancePStmt).setInt(2, userId);
        verify(mockUpdateDepositBalancePStmt).setInt(3, deposit.getId());

        verify(mockUpdateUserBalancePStmt).setDouble(1, expectedNewUserBalance);
        verify(mockUpdateUserBalancePStmt).setInt(2, userId);

        verify(mockConnection, never()).prepareStatement(startsWith("DELETE FROM user_deposits"));
    }

    @Test
    void withdrawFromDeposit_successFullWithdrawalDeletesDeposit() throws SQLException {
        Deposit deposit = createTestDeposit(1, "WithdrawFullTest", 5.0, 365, 100.0);
        deposit.setIsEarlyWithdrawal(1);
        int userId = 1;
        double withdrawAmount = 200.0;
        double initialDepositBalance = 200.0;
        double initialUserBalance = 100.0;

        PreparedStatement mockSelectDepositBalancePStmt = mock(PreparedStatement.class);
        ResultSet mockDepositBalanceRs = mock(ResultSet.class);
        when(mockConnection.prepareStatement(eq("SELECT balance FROM user_deposits WHERE user_id = ? AND deposit_id = ?"))).thenReturn(mockSelectDepositBalancePStmt);
        when(mockSelectDepositBalancePStmt.executeQuery()).thenReturn(mockDepositBalanceRs);
        when(mockDepositBalanceRs.next()).thenReturn(true);
        when(mockDepositBalanceRs.getDouble("balance")).thenReturn(initialDepositBalance);

        PreparedStatement mockDeletePStmt = mock(PreparedStatement.class);
        when(mockConnection.prepareStatement(eq("DELETE FROM user_deposits WHERE user_id = ? AND deposit_id = ?"))).thenReturn(mockDeletePStmt);
        when(mockDeletePStmt.executeUpdate()).thenReturn(1);

        PreparedStatement mockGetUserBalancePStmt = mock(PreparedStatement.class);
        ResultSet mockUserBalanceRs = mock(ResultSet.class);
        when(mockConnection.prepareStatement(eq("SELECT balance FROM users WHERE id = ?"))).thenReturn(mockGetUserBalancePStmt);
        when(mockGetUserBalancePStmt.executeQuery()).thenReturn(mockUserBalanceRs);
        when(mockUserBalanceRs.next()).thenReturn(true);
        when(mockUserBalanceRs.getDouble("balance")).thenReturn(initialUserBalance);

        PreparedStatement mockUpdateUserBalancePStmt = mock(PreparedStatement.class);
        when(mockConnection.prepareStatement(eq("UPDATE users SET balance = ? WHERE id = ?"))).thenReturn(mockUpdateUserBalancePStmt);
        when(mockUpdateUserBalancePStmt.executeUpdate()).thenReturn(1);

        assertDoesNotThrow(() -> depositService.withdrawFromDeposit(userId, deposit, withdrawAmount));

        verify(mockDeletePStmt).setInt(1, userId);
        verify(mockDeletePStmt).setInt(2, deposit.getId());
        double penalty = withdrawAmount * 0.05;
        double amountAfterPenalty = withdrawAmount - penalty;

        verify(mockUpdateUserBalancePStmt).setDouble(1, initialUserBalance + amountAfterPenalty);

        verify(mockConnection, never()).prepareStatement(eq("UPDATE user_deposits SET balance = ? WHERE user_id = ? AND deposit_id = ?"));
    }
    @Test
    void withdrawFromDeposit_notEarlyWithdrawable() {
        Deposit deposit = createTestDeposit(1, "NoWithdraw", 5.0, 365, 100);
        deposit.setIsEarlyWithdrawal(0);
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> depositService.withdrawFromDeposit(1, deposit, 50.0));
        assertEquals("Дострокове зняття коштів з цього депозиту заборонено.", ex.getMessage());
    }

    @Test
    void withdrawFromDeposit_insufficientDepositFunds() throws SQLException {
        Deposit deposit = createTestDeposit(1, "WithdrawTest", 5.0, 365, 100.0);
        deposit.setIsEarlyWithdrawal(1);
        int userId = 1;
        double withdrawAmount = 250.0;
        double initialDepositBalance = 200.0;

        PreparedStatement mockSelectDepositBalancePStmt = mock(PreparedStatement.class);
        ResultSet mockDepositBalanceRs = mock(ResultSet.class);
        when(mockConnection.prepareStatement(eq("SELECT balance FROM user_deposits WHERE user_id = ? AND deposit_id = ?"))).thenReturn(mockSelectDepositBalancePStmt);
        when(mockSelectDepositBalancePStmt.executeQuery()).thenReturn(mockDepositBalanceRs);
        when(mockDepositBalanceRs.next()).thenReturn(true);
        when(mockDepositBalanceRs.getDouble("balance")).thenReturn(initialDepositBalance);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> depositService.withdrawFromDeposit(userId, deposit, withdrawAmount));
        assertEquals("Недостатньо коштів на депозиті.", ex.getMessage());
    }



    @Test
    void userHasDeposit_true() throws SQLException {
        PreparedStatement mockPStmt = mock(PreparedStatement.class);
        ResultSet localMockResultSet = mock(ResultSet.class);
        when(mockConnection.prepareStatement(startsWith("SELECT 1 FROM user_deposits WHERE user_id = ? AND deposit_id = ?"))).thenReturn(mockPStmt);
        when(mockPStmt.executeQuery()).thenReturn(localMockResultSet);
        when(localMockResultSet.next()).thenReturn(true);

        assertTrue(depositService.userHasDeposit(1, 10));
        verify(mockPStmt).setInt(1, 1);
        verify(mockPStmt).setInt(2, 10);
    }

    @Test
    void userHasDeposit_false() throws SQLException {
        PreparedStatement mockPStmt = mock(PreparedStatement.class);
        ResultSet localMockResultSet = mock(ResultSet.class);
        when(mockConnection.prepareStatement(startsWith("SELECT 1 FROM user_deposits WHERE user_id = ? AND deposit_id = ?"))).thenReturn(mockPStmt);
        when(mockPStmt.executeQuery()).thenReturn(localMockResultSet);
        when(localMockResultSet.next()).thenReturn(false);

        assertFalse(depositService.userHasDeposit(1, 10));
        verify(mockPStmt).setInt(1, 1);
        verify(mockPStmt).setInt(2, 10);
    }


    @Test
    void topUpDeposit_userNotFoundForBalanceCheck_shouldThrowRuntimeException() throws SQLException {
        Deposit depositToTopUp = createTestDeposit(1, "TestDeposit", 5.0, 365, 100.0);
        depositToTopUp.setIsReplenishable(1);
        int userId = 99;
        double topUpAmount = 50.0;

        PreparedStatement mockSelectUserBalancePStmtLocal = mock(PreparedStatement.class);
        ResultSet mockUserBalanceRs = mock(ResultSet.class);
        when(mockConnection.prepareStatement(eq("SELECT balance FROM users WHERE id = ?"))).thenReturn(mockSelectUserBalancePStmtLocal);
        when(mockSelectUserBalancePStmtLocal.executeQuery()).thenReturn(mockUserBalanceRs);
        when(mockUserBalanceRs.next()).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> depositService.topUpDeposit(userId, depositToTopUp, topUpAmount));
        assertEquals("Користувача не знайдено.", ex.getMessage());

        verify(mockConnection).setAutoCommit(false);
        verify(mockConnection, never()).commit();
        verify(mockConnection, never()).rollback();
        verify(mockConnection).setAutoCommit(true);
        verify(mockConnection).close();

        verify(mockPragmaStatement).close();
        verify(mockSelectUserBalancePStmtLocal).close();
        verify(mockUserBalanceRs).close();
    }

    @Test
    void topUpDeposit_depositNotFoundForUpdate_shouldThrowRuntimeException() throws SQLException {
        Deposit depositToTopUp = createTestDeposit(99, "NonExistentDeposit", 5.0, 365, 100.0);
        depositToTopUp.setIsReplenishable(1);
        int userId = 1;
        double topUpAmount = 100.0; // Для бонусу
        double expectedBonus = topUpAmount * 0.05;


        PreparedStatement mockSelectUserBalancePStmtLocal = mock(PreparedStatement.class);
        ResultSet mockUserBalanceRs = mock(ResultSet.class);
        when(mockConnection.prepareStatement(eq("SELECT balance FROM users WHERE id = ?"))).thenReturn(mockSelectUserBalancePStmtLocal);
        when(mockSelectUserBalancePStmtLocal.executeQuery()).thenReturn(mockUserBalanceRs);
        when(mockUserBalanceRs.next()).thenReturn(true);
        when(mockUserBalanceRs.getDouble("balance")).thenReturn(200.0);

        PreparedStatement mockUpdateUserBalancePStmtLocal = mock(PreparedStatement.class);
        when(mockConnection.prepareStatement(eq("UPDATE users SET balance = balance - ? WHERE id = ?"))).thenReturn(mockUpdateUserBalancePStmtLocal);
        when(mockUpdateUserBalancePStmtLocal.executeUpdate()).thenReturn(1);

        PreparedStatement mockUpdateDepositPStmtLocal = mock(PreparedStatement.class);
        when(mockConnection.prepareStatement(eq("UPDATE user_deposits SET balance = balance + ? WHERE user_id = ? AND deposit_id = ?"))).thenReturn(mockUpdateDepositPStmtLocal);
        when(mockUpdateDepositPStmtLocal.executeUpdate()).thenReturn(0); // Депозит не знайдено

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> depositService.topUpDeposit(userId, depositToTopUp, topUpAmount));
        assertEquals("Депозит не знайдено для користувача.", ex.getMessage());

        verify(mockConnection).setAutoCommit(false);
        verify(mockConnection, never()).commit();
        verify(mockConnection, never()).rollback();
        verify(mockConnection).setAutoCommit(true);
        verify(mockConnection).close();

        verify(mockPragmaStatement).close();
        verify(mockSelectUserBalancePStmtLocal).close();
        verify(mockUserBalanceRs).close();
        verify(mockUpdateUserBalancePStmtLocal).close();
        verify(mockUpdateDepositPStmtLocal).close();
    }


    @Test
    void withdrawFromDeposit_sqlExceptionDuringOperation_shouldThrowRuntimeException() throws SQLException {
        Deposit deposit = createTestDeposit(1, "WithdrawTest", 5.0, 365, 100.0);
        deposit.setIsEarlyWithdrawal(1);
        int userId = 1;
        double withdrawAmount = 50.0;
        SQLException dbSelectError = new SQLException("DB select error");

        PreparedStatement mockSelectPStmtLocal = mock(PreparedStatement.class);
        when(mockConnection.prepareStatement(startsWith("SELECT balance FROM user_deposits")))
                .thenReturn(mockSelectPStmtLocal);
        doThrow(dbSelectError).when(mockSelectPStmtLocal).executeQuery();

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> depositService.withdrawFromDeposit(userId, deposit, withdrawAmount));
        assertTrue(ex.getMessage().contains("Помилка при знятті коштів з депозиту"));
        assertSame(dbSelectError, ex.getCause());

        verify(mockPragmaStatement, atLeastOnce()).execute(startsWith("PRAGMA foreign_keys"));
        verify(mockPragmaStatement, atLeastOnce()).close();
        verify(mockSelectPStmtLocal, atLeastOnce()).close();
        verify(mockConnection, atLeastOnce()).close();
    }


    @Test
    void accrueInterest_depositNotFound_shouldPrintAndReturn() throws SQLException {
        int userId = 1;
        Deposit deposit = createTestDeposit(101, "NonExistentForAccrual", 5.0, 30, 1000);

        PreparedStatement mockSelectPStmtLocal = mock(PreparedStatement.class);
        ResultSet mockRs = mock(ResultSet.class);
        when(mockConnection.prepareStatement(startsWith("SELECT balance, last_interest_accrued, opened_at FROM user_deposits")))
                .thenReturn(mockSelectPStmtLocal);
        when(mockSelectPStmtLocal.executeQuery()).thenReturn(mockRs);
        when(mockRs.next()).thenReturn(false);

        assertDoesNotThrow(() -> depositService.accrueInterest(userId, deposit));

        verify(mockPragmaStatement).execute(startsWith("PRAGMA foreign_keys"));
        verify(mockSelectPStmtLocal).setInt(1, userId);
        verify(mockSelectPStmtLocal).setInt(2, deposit.getId());
        verify(mockConnection, never()).prepareStatement(startsWith("UPDATE user_deposits"));
        verify(mockConnection, never()).prepareStatement(startsWith("UPDATE users"));
        verify(mockConnection, never()).prepareStatement(startsWith("DELETE FROM user_deposits"));

        verify(mockPragmaStatement).close();
        verify(mockSelectPStmtLocal).close();
        verify(mockRs).close();
        verify(mockConnection).close();
    }

        private Deposit createSampleDepositForStaticSave() {
        return new Deposit(0, "Test Save Static", "Savings", 3.5, 180,
                "StaticBank", 0, 1, 500.0);
    }

    @Test
    void saveDeposit_static_success_returnsTrue() throws SQLException {
        Deposit depositToSave = createSampleDepositForStaticSave();
        when(mockSaveDepositPStmt.executeUpdate()).thenReturn(1);

        boolean result = DepositService.saveDeposit(depositToSave); // Виклик статичного методу

        assertTrue(result);
        mockedStaticDatabaseUtil.verify(Database::getConnection, times(1));
        verify(mockStaticDbConnection).prepareStatement(startsWith("INSERT INTO deposits"));
        verify(mockSaveDepositPStmt).setString(1, depositToSave.getName());
        verify(mockSaveDepositPStmt).setString(2, depositToSave.getType());
        verify(mockSaveDepositPStmt).setDouble(3, depositToSave.getInterestRate());
        verify(mockSaveDepositPStmt).setInt(4, depositToSave.getTerm());
        verify(mockSaveDepositPStmt).setString(5, depositToSave.getBankName());
        verify(mockSaveDepositPStmt).setInt(6, depositToSave.getIsReplenishable());
        verify(mockSaveDepositPStmt).setInt(7, depositToSave.getIsEarlyWithdrawal());
        verify(mockSaveDepositPStmt).setDouble(8, depositToSave.getMinAmount());
        verify(mockSaveDepositPStmt).executeUpdate();
        verify(mockSaveDepositPStmt).close();
        verify(mockStaticDbConnection).close();
    }

    @Test
    void saveDeposit_static_failure_executeUpdateReturnsZero_returnsFalse() throws SQLException {
        Deposit depositToSave = createSampleDepositForStaticSave();
        when(mockSaveDepositPStmt.executeUpdate()).thenReturn(0);

        boolean result = DepositService.saveDeposit(depositToSave);

        assertFalse(result);
        verify(mockSaveDepositPStmt).executeUpdate();
        verify(mockSaveDepositPStmt).close();
        verify(mockStaticDbConnection).close();
    }

    @Test
    void saveDeposit_static_failure_sqlExceptionOnExecuteUpdate_returnsFalse() throws SQLException {
        Deposit depositToSave = createSampleDepositForStaticSave();
        SQLException executeFailed = new SQLException("Execute failed");
        when(mockSaveDepositPStmt.executeUpdate()).thenThrow(executeFailed);

        boolean result = DepositService.saveDeposit(depositToSave);

        assertFalse(result);
        verify(mockSaveDepositPStmt).executeUpdate();
        verify(mockSaveDepositPStmt).close();
        verify(mockStaticDbConnection).close();
    }

    @Test
    void saveDeposit_static_failure_sqlExceptionOnPrepareStatement_returnsFalse() throws SQLException {
        Deposit depositToSave = createSampleDepositForStaticSave();
        SQLException prepareFailed = new SQLException("Prepare failed");

        when(mockStaticDbConnection.prepareStatement(anyString()))
                .thenThrow(prepareFailed);

        boolean result = DepositService.saveDeposit(depositToSave);

        assertFalse(result);
        mockedStaticDatabaseUtil.verify(Database::getConnection);
        verify(mockStaticDbConnection).prepareStatement(anyString());
        verify(mockSaveDepositPStmt, never()).executeUpdate();
        verify(mockSaveDepositPStmt, never()).close();
        verify(mockStaticDbConnection).close();
    }

    @Test
    void saveDeposit_static_failure_sqlExceptionOnGetConnection_returnsFalse() throws SQLException {
        SQLException getConnectionFailed = new SQLException("GetConnection failed");

        mockedStaticDatabaseUtil.reset();
        mockedStaticDatabaseUtil.when(Database::getConnection)
                .thenThrow(getConnectionFailed);

        boolean result = DepositService.saveDeposit(createSampleDepositForStaticSave());

        assertFalse(result);
        mockedStaticDatabaseUtil.verify(Database::getConnection);
        verify(mockStaticDbConnection, never()).prepareStatement(anyString());
        verify(mockSaveDepositPStmt, never()).executeUpdate();
    }



    @Mock private PreparedStatement mockGetDepositsForUserPStmtLocal;
    @Mock private ResultSet mockUserDepositsResultSetLocal;

    @Test
    void getDepositsByUserId_returnsDepositsAndCallsAccrueInterest() throws SQLException {
        int userId = 1;

        int depositId1 = 101;
        String depositName1 = "Active Saver";
        double currentBalance1 = 1500.75;
        int depositId2 = 102;
        String depositName2 = "Long Term Plus";
        double currentBalance2 = 3200.50;

        String expectedSql = "SELECT ud.id as user_deposit_id, d.*, ud.balance, ud.opened_at, ud.finish_date " +
                "FROM user_deposits ud " +
                "JOIN deposits d ON ud.deposit_id = d.id " +
                "WHERE ud.user_id = ?";

        when(mockConnection.prepareStatement(eq(expectedSql))).thenReturn(mockGetDepositsForUserPStmtLocal);
        when(mockGetDepositsForUserPStmtLocal.executeQuery()).thenReturn(mockUserDepositsResultSetLocal);

        when(mockUserDepositsResultSetLocal.next()).thenReturn(true, true, false);
        when(mockUserDepositsResultSetLocal.getInt("id")).thenReturn(depositId1, depositId2);  // id з deposits
        when(mockUserDepositsResultSetLocal.getString("name")).thenReturn(depositName1, depositName2);
        when(mockUserDepositsResultSetLocal.getString("type")).thenReturn("Term", "Savings");
        when(mockUserDepositsResultSetLocal.getDouble("interest_rate")).thenReturn(5.0, 4.5);
        when(mockUserDepositsResultSetLocal.getInt("term")).thenReturn(365, 180);
        when(mockUserDepositsResultSetLocal.getString("bank_name")).thenReturn("BankA", "BankB");
        when(mockUserDepositsResultSetLocal.getInt("is_replenishable")).thenReturn(1, 0);
        when(mockUserDepositsResultSetLocal.getInt("is_early_withdrawal")).thenReturn(1, 1);
        when(mockUserDepositsResultSetLocal.getDouble("min_amount")).thenReturn(100.0, 500.0);
        when(mockUserDepositsResultSetLocal.getDouble("balance")).thenReturn(currentBalance1, currentBalance2);
        when(mockUserDepositsResultSetLocal.getString("opened_at")).thenReturn("2023-01-01", "2023-06-01");
        when(mockUserDepositsResultSetLocal.getString("finish_date")).thenReturn("2024-01-01", "2024-06-01");


        DepositService spyDepositService = Mockito.spy(depositService);
        doNothing().when(spyDepositService).accrueInterest(anyInt(), any(Deposit.class));

        List<Deposit> resultDeposits = spyDepositService.getDepositsByUserId(userId);

        verify(mockConnection).prepareStatement(eq(expectedSql));
        verify(mockGetDepositsForUserPStmtLocal).setInt(1, userId);
        verify(mockGetDepositsForUserPStmtLocal).executeQuery();
        assertEquals(2, resultDeposits.size());
        assertEquals(depositId1, resultDeposits.get(0).getId());
        assertEquals(currentBalance1, resultDeposits.get(0).getCurrentBalance(), 0.001);
        assertEquals(depositId2, resultDeposits.get(1).getId());
        assertEquals(currentBalance2, resultDeposits.get(1).getCurrentBalance(), 0.001);

        ArgumentCaptor<Deposit> depositCaptor = ArgumentCaptor.forClass(Deposit.class);
        verify(spyDepositService, times(2)).accrueInterest(eq(userId), depositCaptor.capture());
        assertEquals(depositId1, depositCaptor.getAllValues().get(0).getId());
        assertEquals(depositId2, depositCaptor.getAllValues().get(1).getId());

        verify(mockUserDepositsResultSetLocal).close();
        verify(mockGetDepositsForUserPStmtLocal).close();
        verify(mockPragmaStatement).close();
        verify(mockConnection).close();
    }

    @Test
    void getDepositsByUserId_handlesSQLException_andThrowsRuntimeException() throws SQLException {
        int userId = 1;
        String expectedSql = "SELECT ud.id as user_deposit_id, d.*, ud.balance, ud.opened_at, ud.finish_date " +
                "FROM user_deposits ud " +
                "JOIN deposits d ON ud.deposit_id = d.id " +
                "WHERE ud.user_id = ?";

        SQLException dbQueryFailed = new SQLException("DB query failed");

        when(mockConnection.prepareStatement(eq(expectedSql))).thenReturn(mockGetDepositsForUserPStmtLocal);
        when(mockGetDepositsForUserPStmtLocal.executeQuery()).thenThrow(dbQueryFailed);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            depositService.getDepositsByUserId(userId);
        });

        assertTrue(exception.getMessage().contains("Помилка при отриманні депозитів користувача"));
        assertSame(dbQueryFailed, exception.getCause());

        verify(mockConnection).prepareStatement(eq(expectedSql));
        verify(mockGetDepositsForUserPStmtLocal).executeQuery();
        verify(mockGetDepositsForUserPStmtLocal, atLeastOnce()).close();
        verify(mockPragmaStatement, atLeastOnce()).close();
        verify(mockConnection, atLeastOnce()).close();
    }




    @Test
    void testDeleteDeposit_success() throws SQLException {
        Deposit deposit = createTestDeposit(123, "Test Deposit", 5.0, 12, 1000.0);

        when(mockStaticDbConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1); // 1 рядок видалено

        boolean result = DepositService.deleteDeposit(deposit);

        assertTrue(result);

        verify(mockPreparedStatement).setInt(1, deposit.getId());
        verify(mockPreparedStatement).executeUpdate();
    }

    @Test
    void testDeleteDeposit_fail() throws SQLException {
        Deposit deposit = createTestDeposit(123, "Test Deposit", 5.0, 12, 1000.0);

        when(mockStaticDbConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(0); // 0 рядків видалено

        boolean result = DepositService.deleteDeposit(deposit);

        assertFalse(result);

        verify(mockPreparedStatement).setInt(1, deposit.getId());
        verify(mockPreparedStatement).executeUpdate();
    }


    @Test
    void testAccrueInterest_termFinished() throws Exception {
        int userId = 1;
        double interestRate = 5.0;
        int termInMonths = 1;
        double initialBalance = 1000.0;

        Deposit deposit = new Deposit(
                101,                       // id
                "Test Deposit",           // name
                "standard",               // type
                interestRate,             // interestRate
                termInMonths,             // term
                "TestBank",               // bankName
                0,                        // isReplenishable
                0,                        // isEarlyWithdrawal
                500.0                     // minAmount
        );
        deposit.setCurrentBalance(initialBalance);

        LocalDateTime lastAccrued = LocalDateTime.now().minusMonths(4);
        LocalDateTime openedAtDate = LocalDateTime.now().minusMonths(4);


        // Моки
        Connection mockConn = mock(Connection.class);
        PreparedStatement mockSelectStmt = mock(PreparedStatement.class);
        PreparedStatement mockUpdateUserBalanceStmt = mock(PreparedStatement.class);
        PreparedStatement mockDeleteDepositStmt = mock(PreparedStatement.class);
        ResultSet mockResultSet = mock(ResultSet.class);

        DepositService depositServiceSpy = Mockito.spy(depositService);
        when(depositServiceSpy.getConnectionWithForeignKeysEnabled()).thenReturn(mockConn);

        when(mockConn.prepareStatement(startsWith("SELECT balance"))).thenReturn(mockSelectStmt);
        when(mockConn.prepareStatement(startsWith("UPDATE users"))).thenReturn(mockUpdateUserBalanceStmt);
        when(mockConn.prepareStatement(startsWith("DELETE FROM user_deposits"))).thenReturn(mockDeleteDepositStmt);

        when(mockSelectStmt.executeQuery()).thenReturn(mockResultSet);

        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getDouble("balance")).thenReturn(initialBalance);
        when(mockResultSet.getString("last_interest_accrued"))
                .thenReturn(lastAccrued.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        when(mockResultSet.getTimestamp("opened_at")).thenReturn(Timestamp.valueOf(openedAtDate));

        when(mockUpdateUserBalanceStmt.executeUpdate()).thenReturn(1);
        when(mockDeleteDepositStmt.executeUpdate()).thenReturn(1);

        doNothing().when(mockConn).commit();
        doNothing().when(mockConn).setAutoCommit(anyBoolean());
        doNothing().when(mockConn).close();

        try (MockedStatic<UserService> userServiceMock = Mockito.mockStatic(UserService.class);
             MockedStatic<Session> sessionMock = Mockito.mockStatic(Session.class)) {

            User mockUser = mock(User.class);
            sessionMock.when(Session::getUser).thenReturn(mockUser);

            double updatedUserBalance = 2000.0;
            userServiceMock.when(() -> UserService.getBalanceByUserId(userId)).thenReturn(updatedUserBalance);

            depositServiceSpy.accrueInterest(userId, deposit);

            double expectedInterest = initialBalance * (interestRate / 100.0) * 1 / 12;
            double expectedNewBalance = initialBalance + expectedInterest;
            double expectedRoundedBalance = roundToTwoDecimals(expectedNewBalance);

            verify(mockUpdateUserBalanceStmt).setDouble(eq(1), eq(expectedRoundedBalance));
            verify(mockUpdateUserBalanceStmt).setInt(eq(2), eq(userId));
            verify(mockUpdateUserBalanceStmt).executeUpdate();

            verify(mockDeleteDepositStmt).setInt(eq(1), eq(userId));
            verify(mockDeleteDepositStmt).setInt(eq(2), eq(deposit.getId()));
            verify(mockDeleteDepositStmt).executeUpdate();

            verify(mockConn).commit();

            verify(mockUser).setBalance(updatedUserBalance);
        }
    }

    private static double roundToTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }





    @Test
    void testAccrueInterest_notTermFinished() throws Exception {
        int userId = 1;

        // Створюємо депозит із конструктором відповідно до твоєї моделі
        Deposit deposit = new Deposit(
                101,
                "Test Deposit",
                "TypeA",
                5.0,       // interestRate 5%
                6,         // term = 6 місяців (ще не закінчився)
                "Test Bank",
                0,         // isReplenishable
                0,         // isEarlyWithdrawal
                1000.0     // minAmount
        );

        // Дати для тесту
        LocalDateTime lastAccrued = LocalDateTime.now().minusMonths(2);  // останнє нарахування 2 місяці тому
        LocalDateTime openedAtDate = LocalDateTime.now().minusMonths(4); // відкриття депозиту 4 місяці тому

        // Форматтер для дати у форматі yyyy-MM-dd HH:mm:ss
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        // Моки JDBC
        Connection mockConn = mock(Connection.class);
        PreparedStatement mockSelectStmt = mock(PreparedStatement.class);
        PreparedStatement mockUpdateDepositStmt = mock(PreparedStatement.class);
        ResultSet mockResultSet = mock(ResultSet.class);

        // Створюємо spy на сервісі (якщо потрібно)
        DepositService depositServiceSpy = Mockito.spy(depositService);
        when(depositServiceSpy.getConnectionWithForeignKeysEnabled()).thenReturn(mockConn);

        // Мок на підготовку запитів
        when(mockConn.prepareStatement(startsWith("SELECT balance"))).thenReturn(mockSelectStmt);
        when(mockConn.prepareStatement(startsWith("UPDATE user_deposits"))).thenReturn(mockUpdateDepositStmt);

        when(mockSelectStmt.executeQuery()).thenReturn(mockResultSet);

        // Налаштування моків для ResultSet
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getDouble("balance")).thenReturn(1000.0);
        when(mockResultSet.getString("last_interest_accrued")).thenReturn(lastAccrued.format(formatter));
        when(mockResultSet.getTimestamp("opened_at")).thenReturn(Timestamp.valueOf(openedAtDate));

        when(mockUpdateDepositStmt.executeUpdate()).thenReturn(1);

        // Моки для commit, setAutoCommit, close
        doNothing().when(mockConn).commit();
        doNothing().when(mockConn).setAutoCommit(anyBoolean());
        doNothing().when(mockConn).close();

        // Моки статичних класів UserService і Session
        try (MockedStatic<UserService> userServiceMock = Mockito.mockStatic(UserService.class);
             MockedStatic<Session> sessionMock = Mockito.mockStatic(Session.class)) {

            User mockUser = mock(User.class);
            sessionMock.when(Session::getUser).thenReturn(mockUser);

            // Припустимо, що баланс після нарахування 1004.17
            userServiceMock.when(() -> UserService.getBalanceByUserId(userId)).thenReturn(1004.17);

            // Викликаємо метод для тестування
            depositServiceSpy.accrueInterest(userId, deposit);

            // Перевірка, що оновлення депозиту виконується з очікуваними параметрами
            verify(mockUpdateDepositStmt).setDouble(eq(1), anyDouble());
            verify(mockUpdateDepositStmt).setString(eq(2), anyString());
            verify(mockUpdateDepositStmt).setInt(eq(3), eq(userId));
            verify(mockUpdateDepositStmt).setInt(eq(4), eq(deposit.getId()));
            verify(mockUpdateDepositStmt).executeUpdate();

            verify(mockConn).commit();

            // Перевірка, що баланс користувача оновлюється у сесії
            verify(mockUser).setBalance(1004.17);
        }
    }




}