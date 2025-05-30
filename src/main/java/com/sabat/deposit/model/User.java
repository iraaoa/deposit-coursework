package com.sabat.deposit.model;

public class User {
    private int id;
    private String name;
    private String surname;
    private String email;
    private String password;
    private double balance;
    private String role;

    public User(int id, String name, String surname, String email, String password) {
        this.id = id;
        this.name = name;
        this.surname = surname;
        this.email = email;
        this.password = password;
        this.balance = 0.0;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getSurname() { return surname; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public double getBalance() { return balance; }


    public String getRole() {
        return role;
    }


    public void setRole(String role) {
        this.role = role;
    }


    public void setBalance(double balance) { this.balance = balance; }


    public void setId(int id) {
        this.id = id;
    }
}
