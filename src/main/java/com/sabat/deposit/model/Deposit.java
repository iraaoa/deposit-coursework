package com.sabat.deposit.model;

public class Deposit {
    private int id;
    private String name;
    private String type;
    private double interestRate;
    private int term;
    private String bankName;
    private int isReplenishable;
    private int isEarlyWithdrawal;
    private double minAmount;
    private double currentBalance;


    private String openedAt;
    private String finishDate;



    public Deposit(int id, String name, String type, double interestRate, int term, String bankName,
                   int isReplenishable, int isEarlyWithdrawal, double minAmount) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.interestRate = interestRate;
        this.term = term;
        this.bankName = bankName;
        this.isReplenishable = isReplenishable;
        this.isEarlyWithdrawal = isEarlyWithdrawal;
        this.minAmount = minAmount;
    }


    public int getId() { return id; }
    public String getName() { return name; }
    public String getType() { return type; }
    public double getInterestRate() { return interestRate; }
    public int getTerm() { return term; }
    public String getBankName() { return bankName; }
    public int getIsReplenishable() { return isReplenishable; }
    public int getIsEarlyWithdrawal() { return isEarlyWithdrawal; }
    public double getMinAmount() { return minAmount; }
    public void setMinAmount(double minAmount) { this.minAmount = minAmount; }
    public double getCurrentBalance() { return currentBalance; }
    public void setCurrentBalance(double currentBalance) { this.currentBalance = currentBalance; }
    public void setBankName(String bankName) { this.bankName = bankName; }
    public void setIsReplenishable(int i) { this.isReplenishable = i; }
    public void setIsEarlyWithdrawal(int i) { this.isEarlyWithdrawal = i; }
    public void setId(int i) { this.id = i; }

    public String getOpenedAt() {
        return openedAt;
    }

    public void setOpenedAt(String openedAt) {
        this.openedAt = openedAt;
    }

    public String getFinishDate() {
        return finishDate;
    }

    public void setFinishDate(String finishDate) {
        this.finishDate = finishDate;
    }

    public String getReplenishableString() {
        return isReplenishable == 1 ? "Так" : "Ні";
    }

    public String getEarlyWithdrawalString() {
        return isEarlyWithdrawal == 1 ? "Так" : "Ні";
    }
}
