package com.sabat.deposit.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DepositTest {

    @Test
    void testConstructorAndGetters() {
        Deposit deposit = new Deposit(1, "Standard Deposit", "Savings", 5.5, 12,
                "MyBank", 1, 0, 1000.0);

        assertEquals(1, deposit.getId());
        assertEquals("Standard Deposit", deposit.getName());
        assertEquals("Savings", deposit.getType());
        assertEquals(5.5, deposit.getInterestRate());
        assertEquals(12, deposit.getTerm());
        assertEquals("MyBank", deposit.getBankName());
        assertEquals(1, deposit.getIsReplenishable());
        assertEquals(0, deposit.getIsEarlyWithdrawal());
        assertEquals(1000.0, deposit.getMinAmount());

        assertNull(deposit.getOpenedAt());
        assertNull(deposit.getFinishDate());

        assertEquals("Так", deposit.getReplenishableString());
        assertEquals("Ні", deposit.getEarlyWithdrawalString());
    }

    @Test
    void testSetters() {
        Deposit deposit = new Deposit(0, "", "", 0, 0, "", 0, 0, 0);

        deposit.setId(2);
        deposit.setBankName("NewBank");
        deposit.setIsReplenishable(0);
        deposit.setIsEarlyWithdrawal(1);
        deposit.setMinAmount(500.0);
        deposit.setCurrentBalance(1500.5);
        deposit.setOpenedAt("2025-01-01");
        deposit.setFinishDate("2025-12-31");

        assertEquals(2, deposit.getId());
        assertEquals("NewBank", deposit.getBankName());
        assertEquals(0, deposit.getIsReplenishable());
        assertEquals(1, deposit.getIsEarlyWithdrawal());
        assertEquals(500.0, deposit.getMinAmount());
        assertEquals(1500.5, deposit.getCurrentBalance());
        assertEquals("2025-01-01", deposit.getOpenedAt());
        assertEquals("2025-12-31", deposit.getFinishDate());

        assertEquals("Ні", deposit.getReplenishableString());
        assertEquals("Так", deposit.getEarlyWithdrawalString());
    }

    @Test
    void testSetName() {
        Deposit deposit = new Deposit(1, "Initial Name", "Savings", 5.0, 12, "BankA", 1, 0, 1000.0);
        deposit.setName("Updated Name");
        assertEquals("Updated Name", deposit.getName());
    }

    @Test
    void testSetType() {
        Deposit deposit = new Deposit(1, "Initial Name", "Savings", 5.0, 12, "BankA", 1, 0, 1000.0);
        deposit.setType("Checking");
        assertEquals("Checking", deposit.getType());
    }

    @Test
    void testSetInterestRate() {
        Deposit deposit = new Deposit(1, "Initial Name", "Savings", 5.0, 12, "BankA", 1, 0, 1000.0);
        deposit.setInterestRate(6.25);
        assertEquals(6.25, deposit.getInterestRate());
    }

    @Test
    void testSetTerm() {
        Deposit deposit = new Deposit(1, "Initial Name", "Savings", 5.0, 12, "BankA", 1, 0, 1000.0);
        deposit.setTerm(24);
        assertEquals(24, deposit.getTerm());
    }

    @Test
    void testReplenishableStringLogic() {
        Deposit deposit1 = new Deposit(1, "Deposit A", "Type A", 1.0, 1, "Bank X", 1, 0, 100.0);
        assertEquals("Так", deposit1.getReplenishableString());

        Deposit deposit2 = new Deposit(2, "Deposit B", "Type B", 2.0, 2, "Bank Y", 0, 1, 200.0);
        assertEquals("Ні", deposit2.getReplenishableString());
    }

    @Test
    void testEarlyWithdrawalStringLogic() {
        Deposit deposit1 = new Deposit(1, "Deposit A", "Type A", 1.0, 1, "Bank X", 0, 1, 100.0);
        assertEquals("Так", deposit1.getEarlyWithdrawalString());

        Deposit deposit2 = new Deposit(2, "Deposit B", "Type B", 2.0, 2, "Bank Y", 1, 0, 200.0);
        assertEquals("Ні", deposit2.getEarlyWithdrawalString());
    }
}