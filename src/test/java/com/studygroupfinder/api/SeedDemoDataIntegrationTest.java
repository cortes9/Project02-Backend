package com.studygroupfinder.api;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Testcontainers
class SeedDemoDataIntegrationTest {

    private static final Path SEED_SCRIPT =
            Path.of("scripts", "seed-demo-data.sql").toAbsolutePath();

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:16.15");

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void exactSeedScriptIsValidAndIdempotent() throws Exception {
        assertTrue(Files.isRegularFile(SEED_SCRIPT),
                "The demo seed script must exist at scripts/seed-demo-data.sql");

        executeSeedScript();
        SeedCounts firstRun = seededCounts();
        assertEquals(new SeedCounts(5, 10, 5, 10, 15, 23, 16), firstRun);

        executeSeedScript();
        assertEquals(firstRun, seededCounts());

        verifySeedRelationships();
        verifyGeneratedConstraints();
        verifySessionConsistency();
        verifyDistinctNaturalIdentifiers();
        verifyDemoScenarios();
    }

    private void executeSeedScript() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(
                    connection,
                    new FileSystemResource(SEED_SCRIPT));
        }
    }

    private SeedCounts seededCounts() {
        return new SeedCounts(
                count("major", "major_id", -13005, -13001),
                count("course", "course_id", -13110, -13101),
                count("building", "building_id", -13205, -13201),
                count("\"USER\"", "user_id", -13310, -13301),
                count("study_session", "session_id", -13415, -13401),
                jdbcTemplate.queryForObject("""
                        SELECT COUNT(*) FROM session_major
                        WHERE session_id BETWEEN -13415 AND -13401
                        """, Integer.class),
                jdbcTemplate.queryForObject("""
                        SELECT COUNT(*) FROM session_member
                        WHERE session_id BETWEEN -13415 AND -13401
                        """, Integer.class));
    }

    private int count(String table, String idColumn, long firstId, long lastId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + table
                        + " WHERE " + idColumn + " BETWEEN ? AND ?",
                Integer.class,
                firstId,
                lastId);
    }

    private void verifySeedRelationships() {
        assertEquals(0, scalar("""
                SELECT COUNT(*)
                FROM "USER" demo_user
                LEFT JOIN major m ON m.major_id = demo_user.major_id
                WHERE demo_user.user_id BETWEEN -13310 AND -13301
                  AND m.major_id IS NULL
                """));

        assertEquals(0, scalar("""
                SELECT COUNT(*)
                FROM study_session s
                LEFT JOIN course c ON c.course_id = s.course_id
                LEFT JOIN "USER" creator ON creator.user_id = s.creator_id
                LEFT JOIN building b ON b.building_id = s.building_id
                WHERE s.session_id BETWEEN -13415 AND -13401
                  AND (c.course_id IS NULL
                    OR creator.user_id IS NULL
                    OR (s.building_id IS NOT NULL AND b.building_id IS NULL))
                """));

        assertEquals(0, scalar("""
                SELECT COUNT(*)
                FROM session_major sm
                LEFT JOIN study_session s ON s.session_id = sm.session_id
                LEFT JOIN major m ON m.major_id = sm.major_id
                WHERE sm.session_id BETWEEN -13415 AND -13401
                  AND (s.session_id IS NULL OR m.major_id IS NULL)
                """));

        assertEquals(0, scalar("""
                SELECT COUNT(*)
                FROM session_member sm
                LEFT JOIN study_session s ON s.session_id = sm.session_id
                LEFT JOIN "USER" member_user ON member_user.user_id = sm.user_id
                WHERE sm.session_id BETWEEN -13415 AND -13401
                  AND (s.session_id IS NULL OR member_user.user_id IS NULL)
                """));
    }

    private void verifyGeneratedConstraints() {
        List<String> primaryKeyTables = jdbcTemplate.queryForList("""
                SELECT table_name
                FROM information_schema.table_constraints
                WHERE constraint_schema = 'public'
                  AND constraint_type = 'PRIMARY KEY'
                  AND table_name IN (
                    'major', 'course', 'building', 'USER',
                    'study_session', 'session_major', 'session_member')
                """, String.class);
        assertEquals(7, primaryKeyTables.size());

        assertTrue(scalar("""
                SELECT COUNT(*)
                FROM information_schema.table_constraints
                WHERE constraint_schema = 'public'
                  AND table_name = 'USER'
                  AND constraint_type = 'UNIQUE'
                """) >= 1, "oauth_subject must have a generated unique constraint");

        assertTrue(scalar("""
                SELECT COUNT(*)
                FROM information_schema.table_constraints
                WHERE constraint_schema = 'public'
                  AND table_name IN ('study_session', 'session_member')
                  AND constraint_type = 'FOREIGN KEY'
                """) >= 5, "JPA must generate the mapped session foreign keys");
    }

    private void verifySessionConsistency() {
        assertEquals(0, scalar("""
                SELECT COUNT(*) FROM study_session
                WHERE session_id BETWEEN -13415 AND -13401
                  AND (status NOT IN ('OPEN', 'FULL')
                    OR delivery_mode NOT IN ('IN_PERSON', 'REMOTE', 'ASYNC')
                    OR start_time >= end_time
                    OR capacity <= 0
                    OR (delivery_mode = 'IN_PERSON' AND building_id IS NULL))
                """));

        assertEquals(0, scalar("""
                SELECT COUNT(*)
                FROM (
                    SELECT s.session_id
                    FROM study_session s
                    LEFT JOIN session_member sm ON sm.session_id = s.session_id
                    WHERE s.session_id BETWEEN -13415 AND -13401
                    GROUP BY s.session_id, s.capacity
                    HAVING COUNT(sm.user_id) > s.capacity
                ) over_capacity_sessions
                """));

        assertEquals(0, scalar("""
                SELECT COUNT(*)
                FROM (
                    SELECT s.session_id
                    FROM study_session s
                    LEFT JOIN session_member sm ON sm.session_id = s.session_id
                    WHERE s.session_id BETWEEN -13415 AND -13401
                      AND s.status = 'FULL'
                    GROUP BY s.session_id, s.capacity
                    HAVING COUNT(sm.user_id) <> s.capacity
                ) invalid_full_sessions
                """));

        assertEquals(0, scalar("""
                SELECT COUNT(*)
                FROM (
                    SELECT s.session_id
                    FROM study_session s
                    LEFT JOIN session_member sm ON sm.session_id = s.session_id
                    WHERE s.session_id BETWEEN -13415 AND -13401
                      AND s.status = 'OPEN'
                    GROUP BY s.session_id, s.capacity
                    HAVING COUNT(sm.user_id) >= s.capacity
                ) invalid_open_sessions
                """));
    }

    private void verifyDistinctNaturalIdentifiers() {
        assertEquals(10, scalar("""
                SELECT COUNT(DISTINCT email) FROM "USER"
                WHERE user_id BETWEEN -13310 AND -13301
                """));
        assertEquals(10, scalar("""
                SELECT COUNT(DISTINCT oauth_subject) FROM "USER"
                WHERE user_id BETWEEN -13310 AND -13301
                """));
        assertEquals(10, scalar("""
                SELECT COUNT(DISTINCT course_code) FROM course
                WHERE course_id BETWEEN -13110 AND -13101
                """));
        assertTrue(scalar("""
                SELECT COUNT(*) FROM "USER"
                WHERE user_id BETWEEN -13310 AND -13301
                  AND is_admin = TRUE
                """) >= 1);
    }

    private void verifyDemoScenarios() {
        assertTrue(scalar("""
                SELECT COUNT(*)
                FROM session_major
                WHERE session_id BETWEEN -13415 AND -13401
                GROUP BY session_id
                HAVING COUNT(*) > 1
                LIMIT 1
                """) > 1, "At least one session must target multiple majors");

        assertEquals(2, scalar("""
                SELECT COUNT(*)
                FROM session_member
                WHERE user_id = -13302
                  AND session_id IN (-13401, -13407)
                """));

        assertEquals(2, scalar("""
                SELECT COUNT(*)
                FROM session_member membership
                JOIN study_session joined_session
                  ON joined_session.session_id = membership.session_id
                JOIN study_session candidate
                  ON candidate.session_id = -13406
                WHERE membership.user_id = -13302
                  AND candidate.start_time < joined_session.end_time
                  AND candidate.end_time > joined_session.start_time
                """));

        assertTrue(scalar("""
                SELECT COUNT(*) FROM study_session
                WHERE session_id BETWEEN -13415 AND -13401
                  AND status = 'OPEN'
                """) > 0);
        assertTrue(scalar("""
                SELECT COUNT(*) FROM study_session
                WHERE session_id BETWEEN -13415 AND -13401
                  AND status = 'FULL'
                """) > 0);
    }

    private int scalar(String sql) {
        return jdbcTemplate.queryForObject(sql, Integer.class);
    }

    private record SeedCounts(
            int majors,
            int courses,
            int buildings,
            int users,
            int sessions,
            int sessionMajorLinks,
            int sessionMemberships) {
    }
}
