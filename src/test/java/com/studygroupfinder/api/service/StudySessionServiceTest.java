
package com.studygroupfinder.api.service;

import com.studygroupfinder.api.dto.CreateStudySessionRequest;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StudySessionServiceTest {

    // Create the service directly because time validation
    // does not require Spring Boot or a database connection.
    private final StudySessionService service =
            new StudySessionService();

    @Test
    void validSessionTimeIsAccepted() {

        // A session starting at 2 PM and ending at 4 PM is valid.
        CreateStudySessionRequest request =
                createRequest(
                        "2026-10-15T14:00:00Z",
                        "2026-10-15T16:00:00Z"
                );

        // Valid times should not produce an exception.
        assertDoesNotThrow(
                () -> service.validateSessionTime(request)
        );
    }

    @Test
    void equalStartAndEndTimesAreRejected() {

        // A session cannot start and end at the same time.
        CreateStudySessionRequest request =
                createRequest(
                        "2026-10-15T14:00:00Z",
                        "2026-10-15T14:00:00Z"
                );

        // The service must reject an invalid time range.
        assertThrows(
                IllegalArgumentException.class,
                () -> service.validateSessionTime(request)
        );
    }

    @Test
    void startTimeAfterEndTimeIsRejected() {

        // A session cannot start after its end time.
        CreateStudySessionRequest request =
                createRequest(
                        "2026-10-15T16:00:00Z",
                        "2026-10-15T14:00:00Z"
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.validateSessionTime(request)
        );
    }

    // Helper method used to create sample study-session requests.
    // This avoids repeating the same setup in every test.
    private CreateStudySessionRequest createRequest(
            String start,
            String end
    ) {
        return new CreateStudySessionRequest(
                1L,
                null,
                "IN_PERSON",
                OffsetDateTime.parse(start),
                OffsetDateTime.parse(end),
                10,
                "Study session"
        );
    }
}
