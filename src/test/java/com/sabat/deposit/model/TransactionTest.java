package com.sabat.deposit.model;

import org.junit.jupiter.api.Test;

import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.*;

class TransactionTest {

    @Test
    void testTransactionConstructorAndGetters() {
        int id = 1;
        Timestamp timestamp = Timestamp.valueOf("2025-05-30 14:00:00");
        String type = "Deposit";
        String description = "Поповнення рахунку";
        double amount = 1500.00;

        Transaction transaction = new Transaction(id, timestamp, type, description, amount);

        assertEquals(id, transaction.getId());
        assertEquals(timestamp, transaction.getTransactionDate());
        assertEquals(type, transaction.getType());
        assertEquals(description, transaction.getDescription());
        assertEquals(amount, transaction.getAmount());
    }
}
