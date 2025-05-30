package com.sabat.deposit.model;

import java.sql.Timestamp;

public class Transaction {
    private int id;
    private Timestamp transactionDate;
    private String type;
    private String description;
    private double amount;

    public Transaction(int id, Timestamp transactionDate, String type, String description, double amount) {
        this.id = id;
        this.transactionDate = transactionDate;
        this.type = type;
        this.description = description;
        this.amount = amount;
    }

    public int getId() {
        return id;
    }

    public Timestamp getTransactionDate() {
        return transactionDate;
    }

    public String getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public double getAmount() {
        return amount;
    }



}