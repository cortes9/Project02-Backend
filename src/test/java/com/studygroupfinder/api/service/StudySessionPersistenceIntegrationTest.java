
package com.studygroupfinder.api.service;

import com.studygroupfinder.api.dto.CreateStudySessionRequest;
import com.studygroupfinder.api.entity.StudySession;
import com.studygroupfinder.api.repository.SessionMajorRepository;
import com.studygroupfinder.api.repository.StudySessionRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class StudySessionPersistenceIntegrationTest {

    // Use Docker unless local mode is selected.
    private static final String DB_MODE =
            System.getenv().getOrDefault("ISSUE9_DB_MODE", "docker");

    // Only this local test database is allowed.
    private static final String LOCAL_DB_URL =
            "jdbc:postgresql://127.0.0.1:5432/studygroupfinder_issue9_test";

    private static final String LOCAL_DB_NAME =
            "studygroupfinder_issue9_test";

    private static final String LOCAL_DB_USER = "postgres";

    // Docker provides a separate temporary database.
    private static final PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:16.15");

    // Set the database connection before Spring starts Hibernate.
    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {

        if ("local".equalsIgnoreCase(DB_MODE)) {

            String password =
                    System.getenv("ISSUE9_LOCAL_DB_PASSWORD");

            if (password == null || password.isBlank()) {
                throw new IllegalStateException(
                        "Set ISSUE9_LOCAL_DB_PASSWORD before local testing"
                );
            }

            // Check the database before any tables are changed.
            verifyLocalDatabase(password);

            registry.add(
                    "spring.datasource.url",
                    () -> LOCAL_DB_URL
            );

            registry.add(
                    "spring.datasource.username",
                    () -> LOCAL_DB_USER
            );

            registry.add(
                    "spring.datasource.password",
                    () -> password
            );

        } else if ("docker".equalsIgnoreCase(DB_MODE)) {

            // Start PostgreSQL in Docker for this test.
            postgres.start();

            registry.add(
                    "spring.datasource.url",
                    postgres::getJdbcUrl
            );

            registry.add(
                    "spring.datasource.username",
                    postgres::getUsername
            );

            registry.add(
                    "spring.datasource.password",
                    postgres::getPassword
            );

        } else {
            throw new IllegalStateException(
                    "ISSUE9_DB_MODE must be 'docker' or 'local'"
            );
        }
    }

    // Prevent local mode from connecting to the wrong database.
    private static void verifyLocalDatabase(String password) {

        try (
                Connection connection = DriverManager.getConnection(
                        LOCAL_DB_URL,
                        LOCAL_DB_USER,
                        password
                );
                Statement statement = connection.createStatement();
                ResultSet results = statement.executeQuery(
                        "SELECT current_database()"
                )
        ) {

            if (!results.next()
                    || !LOCAL_DB_NAME.equals(results.getString(1))) {

                throw new IllegalStateException(
                        "Wrong database. Local integration test stopped."
                );
            }

        } catch (SQLException exception) {
            throw new IllegalStateException(
                    "Cannot connect to the local Issue #9 test database",
                    exception
            );
        }
    }

    @Autowired
    private StudySessionPersistenceService persistenceService;

    @Autowired
    private StudySessionRepository studySessionRepository;

    @Autowired
    private SessionMajorRepository sessionMajorRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void setUp() {

        // Start each test with clean tables.
        clearTestData();

        // Add the course and majors needed for testing.
        jdbcTemplate.update(
                "INSERT INTO course (course_id) VALUES (?)",
                10L
        );

        jdbcTemplate.update(
                "INSERT INTO major (major_id) VALUES (?)",
                3L
        );

        jdbcTemplate.update(
                "INSERT INTO major (major_id) VALUES (?)",
                5L
        );

        // Add a test user, not a real student.
        jdbcTemplate.update(
                "INSERT INTO \"USER\" (user_id, oauth_subject, is_admin) VALUES (?, ?, ?)",
                42L,
                "integration-test-subject",
                false
        );
    }

    @AfterEach
    void tearDown() {

        // Remove records created by this test.
        clearTestData();
    }

    @Test
    void savesSessionAndSelectedMajors() {

        StudySession saved = persistenceService.saveSession(
                validRequest(),
                42L,
                List.of(3L, 5L)
        );

        // PostgreSQL should create the session ID.
        assertNotNull(saved.getSessionId());

        StudySession persisted = studySessionRepository.findById(
                saved.getSessionId()
        ).orElseThrow();

        assertEquals(42L, persisted.getCreatorId());
        assertEquals(10L, persisted.getCourseId());
        assertEquals("IN_PERSON", persisted.getDeliveryMode());
        assertEquals(12, persisted.getCapacity());

        // Check that both major relationships were saved.
        Integer relationshipCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM session_major WHERE session_id = ?",
                Integer.class,
                saved.getSessionId()
        );

        assertEquals(2, relationshipCount);

        List<Long> majorIds = jdbcTemplate.queryForList(
                "SELECT major_id FROM session_major "
                        + "WHERE session_id = ? ORDER BY major_id",
                Long.class,
                saved.getSessionId()
        );

        assertEquals(List.of(3L, 5L), majorIds);
    }

    @Test
    void rejectsNonexistentCourseWithoutInsertingSession() {

        CreateStudySessionRequest request =
                new CreateStudySessionRequest(
                        999L,
                        null,
                        "IN_PERSON",
                        OffsetDateTime.parse("2026-10-15T14:00:00Z"),
                        OffsetDateTime.parse("2026-10-15T16:00:00Z"),
                        12,
                        "Study group",
                        List.of(3L)
                );

        // An unknown course must stop creation.
        assertThrows(
                IllegalArgumentException.class,
                () -> persistenceService.saveSession(
                        request,
                        42L,
                        List.of(3L)
                )
        );

        assertEquals(0, studySessionRepository.count());
        assertEquals(0, sessionMajorRepository.count());
    }

    @Test
    void rollsBackSessionAndMajorsWhenTransactionFails() {

        TransactionTemplate transaction =
                new TransactionTemplate(transactionManager);

        // Force a failure after saving both record types.
        assertThrows(
                IllegalStateException.class,
                () -> transaction.execute(status -> {

                    persistenceService.saveSession(
                            validRequest(),
                            42L,
                            List.of(3L, 5L)
                    );

                    // Send pending inserts to PostgreSQL.
                    studySessionRepository.flush();
                    sessionMajorRepository.flush();

                    throw new IllegalStateException(
                            "Simulated transaction failure"
                    );
                })
        );

        // Neither record type should remain after rollback.
        assertEquals(0, studySessionRepository.count());
        assertEquals(0, sessionMajorRepository.count());
    }

    // Clear only the tables used by this test.
    private void clearTestData() {

        jdbcTemplate.update("DELETE FROM session_major");
        jdbcTemplate.update("DELETE FROM study_session");
        jdbcTemplate.update("DELETE FROM major");
        jdbcTemplate.update("DELETE FROM course");

        jdbcTemplate.update(
                "DELETE FROM \"USER\" WHERE oauth_subject = ?",
                "integration-test-subject"
        );
    }

    // Create a valid request with two selected majors.
    private CreateStudySessionRequest validRequest() {

        return new CreateStudySessionRequest(
                10L,
                null,
                "IN_PERSON",
                OffsetDateTime.parse("2026-10-15T14:00:00Z"),
                OffsetDateTime.parse("2026-10-15T16:00:00Z"),
                12,
                "Study group",
                List.of(3L, 5L)
        );
    }
}
