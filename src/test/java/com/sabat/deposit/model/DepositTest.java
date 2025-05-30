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
}
