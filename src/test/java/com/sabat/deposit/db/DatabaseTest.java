package com.sabat.deposit.db;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.*;

public class DatabaseTest {

    private static final String TEST_DB_URL = "jdbc:sqlite::memory:";  // In-memory БД для тестів

    @AfterEach
    void tearDown() {
        Database.resetTestConnection();
    }

    @Test
    void testGetConnection_returnsConnectionForProdDB() throws SQLException {
        Database.resetTestConnection();

        try (Connection connection = Database.getConnection()) {
            assertThat(connection).isNotNull();
            assertThat(connection.isClosed()).isFalse();
            assertThat(connection.getMetaData().getURL())
                    .contains("Deposits.db");
        }
    }

    @Test
    void testGetConnection_returnsConnectionForTestDB() throws SQLException {
        Database.setTestConnection(TEST_DB_URL);

        try (Connection connection = Database.getConnection()) {
            assertThat(connection).isNotNull();
            assertThat(connection.isClosed()).isFalse();
            assertThat(connection.getMetaData().getURL())
                    .isEqualTo(TEST_DB_URL);
        }
    }

    @Test
    void testResetTestConnection_clearsTestURL() {
        Database.setTestConnection(TEST_DB_URL);
        Database.resetTestConnection();

        try (Connection connection = Database.getConnection()) {
            assertThat(connection.getMetaData().getURL())
                    .doesNotContain(":memory:");
        } catch (SQLException e) {
            fail("Exception thrown while getting connection after resetTestConnection", e);
        }
    }
}
