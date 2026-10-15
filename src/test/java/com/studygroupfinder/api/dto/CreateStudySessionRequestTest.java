
package com.studygroupfinder.api.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CreateStudySessionRequestTest {

    // Runs the same validation rules used by Spring.
    private static final ValidatorFactory FACTORY =
            Validation.buildDefaultValidatorFactory();

    private static final Validator VALIDATOR =
            FACTORY.getValidator();

    @AfterAll
    static void closeValidatorFactory() {
        FACTORY.close();
    }

    @Test
    void acceptsValidRequest() {

        Set<ConstraintViolation<CreateStudySessionRequest>> violations =
                VALIDATOR.validate(
                        validRequest(List.of(3L, 5L))
                );

        assertTrue(violations.isEmpty());
    }

    @Test
    void rejectsEqualStartAndEndTime() {

        OffsetDateTime start =
                OffsetDateTime.parse("2026-10-15T14:00:00Z");

        CreateStudySessionRequest request =
                new CreateStudySessionRequest(
                        1L, null, "IN_PERSON",
                        start, start, 10,
                        "Study group", List.of(3L)
                );

        assertFalse(VALIDATOR.validate(request).isEmpty());
    }

    @Test
    void rejectsEndTimeBeforeStartTime() {

        CreateStudySessionRequest request =
                new CreateStudySessionRequest(
                        1L, null, "IN_PERSON",
                        OffsetDateTime.parse("2026-10-15T16:00:00Z"),
                        OffsetDateTime.parse("2026-10-15T14:00:00Z"),
                        10, "Study group", List.of(3L)
                );

        assertFalse(VALIDATOR.validate(request).isEmpty());
    }

    @Test
    void rejectsInvalidDeliveryMode() {

        CreateStudySessionRequest request =
                new CreateStudySessionRequest(
                        1L, null, "HYBRID",
                        OffsetDateTime.parse("2026-10-15T14:00:00Z"),
                        OffsetDateTime.parse("2026-10-15T16:00:00Z"),
                        10, "Study group", List.of(3L)
                );

        assertFalse(VALIDATOR.validate(request).isEmpty());
    }

    @Test
    void rejectsMissingMajorSelection() {

        // A student must provide majors.
        assertFalse(
                VALIDATOR.validate(validRequest(null)).isEmpty()
        );
    }

    @Test
    void rejectsEmptyMajorSelection() {

        // At least one major must be selected.
        assertFalse(
                VALIDATOR.validate(validRequest(List.of())).isEmpty()
        );
    }

    @Test
    void rejectsNullMajorId() {

        // Selected majors cannot contain null IDs.
        assertFalse(
                VALIDATOR.validate(
                        validRequest(Arrays.asList(3L, null))
                ).isEmpty()
        );
    }

    @Test
    void rejectsNonPositiveMajorId() {

        // Major IDs must be positive.
        assertFalse(
                VALIDATOR.validate(
                        validRequest(List.of(0L, -2L))
                ).isEmpty()
        );
    }

    @Test
    void rejectsCapacityBelowOne() {

        CreateStudySessionRequest request =
                new CreateStudySessionRequest(
                        1L, null, "IN_PERSON",
                        OffsetDateTime.parse("2026-10-15T14:00:00Z"),
                        OffsetDateTime.parse("2026-10-15T16:00:00Z"),
                        0, "Study group", List.of(3L)
                );

        assertFalse(VALIDATOR.validate(request).isEmpty());
    }

    @Test
    void rejectsMissingCourse() {

        CreateStudySessionRequest request =
                new CreateStudySessionRequest(
                        null, null, "IN_PERSON",
                        OffsetDateTime.parse("2026-10-15T14:00:00Z"),
                        OffsetDateTime.parse("2026-10-15T16:00:00Z"),
                        10, "Study group", List.of(3L)
                );

        assertFalse(VALIDATOR.validate(request).isEmpty());
    }

    // Creates a valid request for the tests.
    private CreateStudySessionRequest validRequest(List<Long> majorIds) {
        return new CreateStudySessionRequest(
                1L,
                null,
                "IN_PERSON",
                OffsetDateTime.parse("2026-10-15T14:00:00Z"),
                OffsetDateTime.parse("2026-10-15T16:00:00Z"),
                10,
                "Study group",
                majorIds
        );
    }
}
