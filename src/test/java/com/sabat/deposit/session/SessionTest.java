package com.sabat.deposit.session;

import com.sabat.deposit.model.User;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

class SessionTest {

    @BeforeEach
    void resetSession() {
        Session.setUser(null);  // Очищаємо сесію перед кожним тестом
    }

    @Test
    void testGetUserInitiallyNull() {
        assertNull(Session.getUser(), "Поточний користувач повинен бути null за замовчуванням");
    }

    @Test
    void testSetUserAndGetUser() {
        User user = new User(1, "John", "Doe", "john@example.com", "pass123");
        Session.setUser(user);

        User current = Session.getUser();
        assertNotNull(current);
        assertEquals(user, current);
        assertEquals(1, current.getId());
        assertEquals("John", current.getName());
    }

    @Test
    void testSetUserToNull() {
        User user = new User(1, "John", "Doe", "john@example.com", "pass123");
        Session.setUser(user);
        assertNotNull(Session.getUser());

        Session.setUser(null);
        assertNull(Session.getUser());
    }

    @Test
    void testClearSession() {
        User user = new User(2, "John", "Doe", "john2@example.com", "pass123");
        Session.setUser(user);
        Session.clear();

        assertNull(Session.getUser());
    }
}
