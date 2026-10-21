
package com.studygroupfinder.api.controller;

import com.studygroupfinder.api.dto.CreateStudySessionRequest;
import com.studygroupfinder.api.dto.StudySessionResponse;
import com.studygroupfinder.api.entity.StudySession;
import com.studygroupfinder.api.service.AuthenticatedCreatorResolver;
import com.studygroupfinder.api.service.StudySessionMajorTargetResolver;
import com.studygroupfinder.api.service.StudySessionPersistenceService;
import com.studygroupfinder.api.service.StudySessionQueryService;
import com.studygroupfinder.api.service.StudySessionManagementService;
import com.studygroupfinder.api.service.StudySessionService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class StudySessionControllerTest {

    private AuthenticatedCreatorResolver creatorResolver;
    private StudySessionMajorTargetResolver majorResolver;
    private StudySessionPersistenceService persistenceService;
    private StudySessionQueryService queryService;
    private StudySessionController controller;

    @BeforeEach
    void setUp() {

        // Mock the services used by the controller.
        creatorResolver = mock(AuthenticatedCreatorResolver.class);
        majorResolver = mock(StudySessionMajorTargetResolver.class);
        persistenceService = mock(StudySessionPersistenceService.class);
        queryService = mock(StudySessionQueryService.class);

        controller = new StudySessionController(
                new StudySessionService(),
                creatorResolver,
                majorResolver,
                persistenceService,
                queryService,
                mock(StudySessionManagementService.class)
        );
    }

    @Test
    void rejectsInvalidSessionTimes() {

        CreateStudySessionRequest request =
                new CreateStudySessionRequest(
                        1L,
                        null,
                        "IN_PERSON",
                        OffsetDateTime.parse("2026-10-15T16:00:00Z"),
                        OffsetDateTime.parse("2026-10-15T14:00:00Z"),
                        10,
                        "Study group",
                        List.of(3L)
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> controller.createStudySession(
                        request, authentication()
                )
        );

        // Nothing is saved when times are invalid.
        verifyNoInteractions(
                creatorResolver,
                majorResolver,
                persistenceService
        );
    }

    @Test
    void rejectsMissingAuthentication() {

        // Reject requests without an authenticated user.
        when(creatorResolver.resolveCreatorId(null))
                .thenThrow(new IllegalStateException(
                        "Authenticated user is required"
                ));

        assertThrows(
                IllegalStateException.class,
                () -> controller.createStudySession(
                        validRequest(), null
                )
        );

        verifyNoInteractions(majorResolver, persistenceService);
    }

    @Test
    void rejectsUserWithoutExistingProfile() {

        JwtAuthenticationToken auth = authentication();

        // The student must have a matching user profile.
        when(creatorResolver.resolveCreatorId(auth))
                .thenThrow(new IllegalStateException(
                        "Authenticated user has no existing profile"
                ));

        assertThrows(
                IllegalStateException.class,
                () -> controller.createStudySession(
                        validRequest(), auth
                )
        );

        verifyNoInteractions(majorResolver, persistenceService);
    }

    @Test
    void rejectsInvalidMajorSelectionBeforeSaving() {

        JwtAuthenticationToken auth = authentication();

        when(creatorResolver.resolveCreatorId(auth))
                .thenReturn(42L);

        // Reject invalid major selections.
        when(majorResolver.resolveMajorIds(
                any(CreateStudySessionRequest.class),
                eq(42L)
        )).thenThrow(new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Invalid major selection"
        ));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> controller.createStudySession(
                        validRequest(), auth
                )
        );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                exception.getStatusCode()
        );

        verifyNoInteractions(persistenceService);
    }

    @Test
    void returns201AndGeneratedSessionId() {

        JwtAuthenticationToken auth = authentication();

        when(creatorResolver.resolveCreatorId(auth))
                .thenReturn(42L);

        when(majorResolver.resolveMajorIds(
                any(CreateStudySessionRequest.class),
                eq(42L)
        )).thenReturn(List.of(3L, 5L));

        // Simulate a saved session with an ID.
        StudySession savedSession = mock(StudySession.class);
        when(savedSession.getSessionId()).thenReturn(100L);
        when(savedSession.getCreatorId()).thenReturn(42L);
        when(savedSession.getCourseId()).thenReturn(1L);
        when(savedSession.getDeliveryMode()).thenReturn("IN_PERSON");
        when(savedSession.getCapacity()).thenReturn(10);
        when(savedSession.getStatus()).thenReturn("OPEN");

        when(persistenceService.saveSession(
                any(CreateStudySessionRequest.class),
                eq(42L),
                eq(List.of(3L, 5L))
        )).thenReturn(savedSession);
        StudySessionResponse createdResponse =
                StudySessionResponse.fromEntity(savedSession);
        when(queryService.findSession(100L)).thenReturn(createdResponse);

        ResponseEntity<StudySessionResponse> response =
                controller.createStudySession(
                        validRequest(), auth
                );

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(100L, response.getBody().sessionId());
        assertEquals(42L, response.getBody().creatorId());
        assertEquals(0, response.getBody().participantCount());

        // Check that the correct IDs are saved.
        verify(persistenceService).saveSession(
                any(CreateStudySessionRequest.class),
                eq(42L),
                eq(List.of(3L, 5L))
        );
    }

    // Creates a request used by the tests.
    private CreateStudySessionRequest validRequest() {
        return new CreateStudySessionRequest(
                1L,
                null,
                "IN_PERSON",
                OffsetDateTime.parse("2026-10-15T14:00:00Z"),
                OffsetDateTime.parse("2026-10-15T16:00:00Z"),
                10,
                "Study group",
                List.of(3L, 5L)
        );
    }

    // Creates an authenticated user for testing only.
    private JwtAuthenticationToken authentication() {

        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject("google-123")
                .build();

        return new JwtAuthenticationToken(jwt, List.of());
    }
}
