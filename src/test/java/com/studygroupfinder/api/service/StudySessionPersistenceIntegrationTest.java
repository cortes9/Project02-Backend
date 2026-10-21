
package com.studygroupfinder.api.service;

import com.studygroupfinder.api.error.ResourceNotFoundException;
import com.studygroupfinder.api.dto.CreateStudySessionRequest;
import com.studygroupfinder.api.dto.ReplaceStudySessionRequest;
import com.studygroupfinder.api.dto.StudySessionPageResponse;
import com.studygroupfinder.api.dto.StudySessionResponse;
import com.studygroupfinder.api.dto.UpdateStudySessionRequest;
import com.studygroupfinder.api.entity.StudySession;
import com.studygroupfinder.api.entity.User;
import com.studygroupfinder.api.error.ConflictException;
import com.studygroupfinder.api.repository.SessionMajorRepository;
import com.studygroupfinder.api.repository.SessionMemberRepository;
import com.studygroupfinder.api.repository.StudySessionRepository;
import com.studygroupfinder.api.repository.UserRepository;

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
import org.springframework.security.access.AccessDeniedException;

import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.time.LocalDate;
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
    private SessionMemberRepository sessionMemberRepository;

    @Autowired
    private StudySessionQueryService queryService;

    @Autowired
    private StudySessionManagementService managementService;

    @Autowired
    private UserRepository userRepository;

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
                "INSERT INTO course (course_id, course_code, course_name) VALUES (?, ?, ?)",
                10L, "CS101", "Introduction to Computing");
        jdbcTemplate.update(
                "INSERT INTO course (course_id, course_code, course_name) VALUES (?, ?, ?)",
                11L, "MATH200", "Discrete Mathematics");
        jdbcTemplate.update(
                "INSERT INTO building (building_id, building_name) VALUES (?, ?)",
                20L, "Library");

        jdbcTemplate.update(
                "INSERT INTO major (major_id, major_name) VALUES (?, ?)",
                3L, "Computer Science"
        );

        jdbcTemplate.update(
                "INSERT INTO major (major_id, major_name) VALUES (?, ?)",
                5L, "Mathematics"
        );

        // Add a test user, not a real student.
        jdbcTemplate.update(
                "INSERT INTO \"USER\" (user_id, oauth_subject, name, is_admin) VALUES (?, ?, ?, ?)",
                42L,
                "integration-test-subject",
                "Session Owner",
                false
        );
        jdbcTemplate.update(
                "INSERT INTO \"USER\" (user_id, oauth_subject, name, is_admin) VALUES (?, ?, ?, ?)",
                43L, "integration-test-member", "Session Member", false);
        jdbcTemplate.update(
                "INSERT INTO \"USER\" (user_id, oauth_subject, name, is_admin) VALUES (?, ?, ?, ?)",
                44L, "integration-test-admin", "Administrator", true);
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
                ResourceNotFoundException.class,
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
    void rejectsInvalidCapacityDuringCreation() {
        CreateStudySessionRequest request = new CreateStudySessionRequest(
                10L,
                null,
                "IN_PERSON",
                OffsetDateTime.parse("2026-10-15T14:00:00Z"),
                OffsetDateTime.parse("2026-10-15T16:00:00Z"),
                0,
                "Study group",
                List.of(3L));

        assertThrows(IllegalArgumentException.class,
                () -> persistenceService.saveSession(request, 42L, List.of(3L)));
        assertEquals(0, studySessionRepository.count());
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

    @Test
    void listsFiltersAndReturnsNestedData() {
        StudySession matching = saveSession(
                10L, 20L, "2026-10-15T14:00:00Z", "2026-10-15T15:00:00Z", 4);
        saveSession(11L, null, "2026-10-16T14:00:00Z", "2026-10-16T15:00:00Z", 4);

        StudySessionPageResponse all = queryService.findSessions(null, null, null, 20, 0);
        assertEquals(2, all.totalElements());
        assertEquals(matching.getSessionId(), all.content().getFirst().sessionId());

        StudySessionPageResponse byCourse = queryService.findSessions("cs101", null, null, 20, 0);
        StudySessionPageResponse byBuilding = queryService.findSessions(null, 20L, null, 20, 0);
        StudySessionPageResponse byDate = queryService.findSessions(
                null, null, LocalDate.parse("2026-10-15"), 20, 0);
        StudySessionPageResponse combined = queryService.findSessions(
                "CS101", 20L, LocalDate.parse("2026-10-15"), 20, 0);

        assertEquals(1, byCourse.content().size());
        assertEquals(1, byBuilding.content().size());
        assertEquals(1, byDate.content().size());
        assertEquals(1, combined.content().size());
        StudySessionResponse result = combined.content().getFirst();
        assertEquals("CS101", result.course().code());
        assertEquals("Introduction to Computing", result.course().name());
        assertEquals("Library", result.building().name());
        assertEquals("Session Owner", result.creator().name());
        assertEquals("Computer Science", result.targetedMajors().getFirst().name());
    }

    @Test
    void paginatesDeterministicallyAndValidatesBounds() {
        StudySession first = saveSession(
                10L, null, "2026-10-15T12:00:00Z", "2026-10-15T13:00:00Z", 3);
        StudySession second = saveSession(
                10L, null, "2026-10-15T14:00:00Z", "2026-10-15T15:00:00Z", 3);

        StudySessionPageResponse page = queryService.findSessions(null, null, null, 1, 0);
        assertEquals(List.of(first.getSessionId()), page.content().stream()
                .map(StudySessionResponse::sessionId).toList());
        assertTrue(page.hasNext());
        assertEquals(1, page.nextOffset());
        StudySessionPageResponse next = queryService.findSessions(null, null, null, 1, 1);
        assertEquals(second.getSessionId(), next.content().getFirst().sessionId());
        assertFalse(next.hasNext());
        assertThrows(IllegalArgumentException.class,
                () -> queryService.findSessions(null, null, null, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> queryService.findSessions(null, null, null, 101, 0));
        assertThrows(IllegalArgumentException.class,
                () -> queryService.findSessions(null, null, null, 20, -1));
    }

    @Test
    void returnsDetailAndMissingDetail() {
        StudySession session = saveSession(
                10L, 20L, "2026-10-15T14:00:00Z", "2026-10-15T15:00:00Z", 3);
        StudySessionResponse detail = queryService.findSession(session.getSessionId());
        assertEquals(session.getSessionId(), detail.sessionId());
        assertEquals(3, detail.capacity());
        assertEquals(0, detail.participantCount());
        assertThrows(ResourceNotFoundException.class, () -> queryService.findSession(99999L));
    }

    @Test
    void joinsRejectsDuplicateAndLeavesIdempotently() {
        StudySession session = saveSession(
                10L, null, "2026-10-15T14:00:00Z", "2026-10-15T15:00:00Z", 1);
        User member = user(43L);

        managementService.join(session.getSessionId(), member);
        assertEquals(1, sessionMemberRepository.countByIdSessionId(session.getSessionId()));
        assertEquals("FULL", studySessionRepository.findById(session.getSessionId()).orElseThrow().getStatus());
        assertThrows(ConflictException.class,
                () -> managementService.join(session.getSessionId(), member));

        managementService.leave(session.getSessionId(), member);
        managementService.leave(session.getSessionId(), member);
        assertEquals(0, sessionMemberRepository.countByIdSessionId(session.getSessionId()));
        assertEquals("OPEN", studySessionRepository.findById(session.getSessionId()).orElseThrow().getStatus());
    }

    @Test
    void rejectsFullAndOverlappingSessionsButAllowsNonOverlappingJoin() {
        User member = user(43L);
        User admin = user(44L);
        StudySession full = saveSession(
                10L, null, "2026-10-15T09:00:00Z", "2026-10-15T10:00:00Z", 1);
        managementService.join(full.getSessionId(), member);
        assertThrows(ConflictException.class,
                () -> managementService.join(full.getSessionId(), admin));

        StudySession joined = saveSession(
                10L, null, "2026-10-15T14:00:00Z", "2026-10-15T15:00:00Z", 3);
        managementService.join(joined.getSessionId(), member);
        StudySession overlapping = saveSession(
                11L, null, "2026-10-15T14:30:00Z", "2026-10-15T15:30:00Z", 3);
        assertThrows(ConflictException.class,
                () -> managementService.join(overlapping.getSessionId(), member));
        StudySession adjacent = saveSession(
                11L, null, "2026-10-15T15:00:00Z", "2026-10-15T16:00:00Z", 3);
        managementService.join(adjacent.getSessionId(), member);
        assertEquals(1, sessionMemberRepository.countByIdSessionId(adjacent.getSessionId()));
    }

    @Test
    void replacesAndPatchesOnlyAsCreator() {
        StudySession session = saveSession(
                10L, null, "2026-10-15T14:00:00Z", "2026-10-15T15:00:00Z", 3);
        ReplaceStudySessionRequest replacement = new ReplaceStudySessionRequest(
                11L, 20L, "IN_PERSON",
                OffsetDateTime.parse("2026-10-16T14:00:00Z"),
                OffsetDateTime.parse("2026-10-16T16:00:00Z"),
                5, "Updated", List.of(5L));
        managementService.replace(session.getSessionId(), user(42L), replacement);
        StudySession replaced = studySessionRepository.findById(session.getSessionId()).orElseThrow();
        assertEquals(11L, replaced.getCourseId());
        assertEquals(5, replaced.getCapacity());

        UpdateStudySessionRequest patch = new UpdateStudySessionRequest();
        patch.setDescription("Patched");
        managementService.update(session.getSessionId(), user(42L), patch);
        StudySession patched = studySessionRepository.findById(session.getSessionId()).orElseThrow();
        assertEquals("Patched", patched.getDescription());
        assertEquals(11L, patched.getCourseId());

        assertThrows(AccessDeniedException.class,
                () -> managementService.update(session.getSessionId(), user(43L), patch));
        assertThrows(ResourceNotFoundException.class,
                () -> managementService.update(99999L, user(42L), patch));
    }

    @Test
    void rejectsInvalidUpdateAndCapacityBelowMemberCount() {
        StudySession session = saveSession(
                10L, null, "2026-10-15T14:00:00Z", "2026-10-15T15:00:00Z", 3);
        managementService.join(session.getSessionId(), user(43L));

        UpdateStudySessionRequest zeroCapacity = new UpdateStudySessionRequest();
        zeroCapacity.setCapacity(0);
        assertThrows(IllegalArgumentException.class,
                () -> managementService.update(session.getSessionId(), user(42L), zeroCapacity));

        UpdateStudySessionRequest tooSmall = new UpdateStudySessionRequest();
        tooSmall.setCapacity(0);
        assertThrows(IllegalArgumentException.class,
                () -> managementService.update(session.getSessionId(), user(42L), tooSmall));

        managementService.join(session.getSessionId(), user(44L));
        UpdateStudySessionRequest belowMembers = new UpdateStudySessionRequest();
        belowMembers.setCapacity(1);
        assertThrows(ConflictException.class,
                () -> managementService.update(session.getSessionId(), user(42L), belowMembers));
    }

    @Test
    void creatorAndAdministratorCanDeleteButOthersCannot() {
        StudySession byCreator = saveSession(
                10L, null, "2026-10-15T10:00:00Z", "2026-10-15T11:00:00Z", 3);
        managementService.delete(byCreator.getSessionId(), user(42L));
        assertFalse(studySessionRepository.existsById(byCreator.getSessionId()));

        StudySession byAdmin = saveSession(
                10L, null, "2026-10-15T12:00:00Z", "2026-10-15T13:00:00Z", 3);
        managementService.delete(byAdmin.getSessionId(), user(44L));
        assertFalse(studySessionRepository.existsById(byAdmin.getSessionId()));

        StudySession protectedSession = saveSession(
                10L, null, "2026-10-15T14:00:00Z", "2026-10-15T15:00:00Z", 3);
        assertThrows(AccessDeniedException.class,
                () -> managementService.delete(protectedSession.getSessionId(), user(43L)));
        assertThrows(ResourceNotFoundException.class,
                () -> managementService.delete(99999L, user(42L)));
    }

    // Clear only the tables used by this test.
    private void clearTestData() {

        jdbcTemplate.update("DELETE FROM session_member");
        jdbcTemplate.update("DELETE FROM session_major");
        jdbcTemplate.update("DELETE FROM study_session");
        jdbcTemplate.update("DELETE FROM building");
        jdbcTemplate.update("DELETE FROM major");
        jdbcTemplate.update("DELETE FROM course");

        jdbcTemplate.update(
                "DELETE FROM \"USER\" WHERE oauth_subject LIKE ?",
                "integration-test-%"
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

    private StudySession saveSession(
            Long courseId,
            Long buildingId,
            String start,
            String end,
            int capacity) {
        return persistenceService.saveSession(
                new CreateStudySessionRequest(
                        courseId,
                        buildingId,
                        "IN_PERSON",
                        OffsetDateTime.parse(start),
                        OffsetDateTime.parse(end),
                        capacity,
                        "Study group",
                        List.of(3L)),
                42L,
                List.of(3L));
    }

    private User user(Long userId) {
        return userRepository.findById(userId).orElseThrow();
    }
}
