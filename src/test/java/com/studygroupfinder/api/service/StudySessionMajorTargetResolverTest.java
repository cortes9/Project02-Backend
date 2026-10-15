
package com.studygroupfinder.api.service;

import com.studygroupfinder.api.dto.CreateStudySessionRequest;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StudySessionMajorTargetResolverTest {

    // Test the actual resolver without requiring Docker or a database.
    private final StudySessionMajorTargetResolver resolver =
            new StudySessionMajorTargetResolver();

    @Test
    void acceptsSelectedMajors() {

        // A student can select multiple majors for the session.
        CreateStudySessionRequest request =
                validRequest(List.of(3L, 5L));

        List<Long> result =
                resolver.resolveMajorIds(request, 42L);

        assertEquals(List.of(3L, 5L), result);
    }

    @Test
    void rejectsEmptyMajorSelection() {

        // A study session must target at least one major.
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> resolver.resolveMajorIds(
                        validRequest(List.of()),
                        42L
                )
        );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                exception.getStatusCode()
        );
    }

    @Test
    void rejectsDuplicateMajors() {

        // Duplicates would refer to the same join-table relationship.
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> resolver.resolveMajorIds(
                        validRequest(List.of(3L, 3L)),
                        42L
                )
        );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                exception.getStatusCode()
        );
    }

    @Test
    void rejectsNullMajorId() {

        // A selected major must have a valid identifier.
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> resolver.resolveMajorIds(
                        validRequest(Arrays.asList(3L, null)),
                        42L
                )
        );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                exception.getStatusCode()
        );
    }

    @Test
    void rejectsMissingCreator() {

        // Session creation requires an authenticated creator.
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> resolver.resolveMajorIds(
                        validRequest(List.of(3L)),
                        null
                )
        );

        assertEquals(
                HttpStatus.UNAUTHORIZED,
                exception.getStatusCode()
        );
    }

    // Builds a valid session request with the specified major selection.
    private CreateStudySessionRequest validRequest(List<Long> majorIds) {
        return new CreateStudySessionRequest(
                1L,
                null,
                "IN_PERSON",
                OffsetDateTime.parse("2026-10-15T14:00:00Z"),
                OffsetDateTime.parse("2026-10-15T16:00:00Z"),
                10,
                "Study session",
                majorIds
        );
    }
}
