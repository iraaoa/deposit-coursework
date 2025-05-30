package com.sabat.deposit.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void testConstructorAndGetters() {
        User user = new User(10, "John", "Doe", "john.doe@example.com", "password123");

        assertEquals(10, user.getId());
        assertEquals("John", user.getName());
        assertEquals("Doe", user.getSurname());
        assertEquals("john.doe@example.com", user.getEmail());
        assertEquals("password123", user.getPassword());
        assertEquals(0.0, user.getBalance(), 0.0001);
    }

    @Test
    void testSetters() {
        User user = new User(0, "Jane", "Smith", "jane.smith@example.com", "pass");

        user.setId(20);
        user.setBalance(150.5);

        assertEquals(20, user.getId());
        assertEquals(150.5, user.getBalance(), 0.0001);
    }

    @Test
    void testRole() {
        User user = new User(1, "Alice", "Brown", "alice.brown@example.com", "securepass");

        assertNull(user.getRole());

        user.setRole("admin");
        assertEquals("admin", user.getRole());
    }
}
