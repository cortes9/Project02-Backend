package com.studygroupfinder.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "SUPABASE_DB_URL", matches = ".+")
class DatabaseConnectionTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void testSupabaseDatabaseConnection() throws SQLException {
        // 1. Verify DataSource bean is initialized
        assertThat(dataSource).isNotNull();

        // 2. Test actual JDBC Connection
        try (Connection connection = dataSource.getConnection()) {
            assertThat(connection.isValid(2)).isTrue();
            System.out.println("==================================================");
            System.out.println("Successfully connected to: " + connection.getMetaData().getDatabaseProductName());
            System.out.println("Database URL: " + connection.getMetaData().getURL());
            System.out.println("Driver Name: " + connection.getMetaData().getDriverName());
            System.out.println("==================================================");
        }

        // 3. Execute a query against the Supabase database
        Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        assertThat(result).isEqualTo(1);
    }
}
