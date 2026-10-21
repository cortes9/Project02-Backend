
package com.studygroupfinder.api.service;

import com.studygroupfinder.api.dto.CreateStudySessionRequest;
import com.studygroupfinder.api.entity.SessionMajor;
import com.studygroupfinder.api.entity.StudySession;
import com.studygroupfinder.api.error.ResourceNotFoundException;
import com.studygroupfinder.api.repository.SessionMajorRepository;
import com.studygroupfinder.api.repository.StudySessionRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class StudySessionPersistenceServiceTest {

    private StudySessionRepository studySessionRepository;
    private SessionMajorRepository sessionMajorRepository;
    private StudySessionReferenceValidator referenceValidator;
    private StudySessionPersistenceService service;

    @BeforeEach
    void setUp() {

        // Mock the database repositories and reference validator.
        // These tests verify service logic without connecting to PostgreSQL.
        studySessionRepository = mock(StudySessionRepository.class);
        sessionMajorRepository = mock(SessionMajorRepository.class);
        referenceValidator = mock(StudySessionReferenceValidator.class);

        // Construct the service with the dependencies used in production.
        service = new StudySessionPersistenceService(
                studySessionRepository,
                sessionMajorRepository,
                referenceValidator
        );
    }

    @Test
    void savesSessionWithAuthenticatedCreator() {

        // Simulate the repository returning the entity that was saved.
        when(studySessionRepository.save(any(StudySession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.saveSession(validRequest(), 42L, List.of());

        // Capture the session passed to the repository.
        ArgumentCaptor<StudySession> sessionCaptor =
                ArgumentCaptor.forClass(StudySession.class);

        verify(studySessionRepository).save(sessionCaptor.capture());

        // Confirm the saved entity contains the trusted creator ID.
        StudySession savedSession = sessionCaptor.getValue();

        assertEquals(42L, savedSession.getCreatorId());
        assertEquals(1L, savedSession.getCourseId());
        assertEquals("IN_PERSON", savedSession.getDeliveryMode());

        // Verify that referenced records were checked.
        verify(referenceValidator).validateReferences(
                1L,
                null,
                List.of()
        );
    }

    @Test
    void rejectsMissingAuthenticatedCreator() {

        // A study session cannot be saved without a creator ID.
        assertThrows(
                IllegalArgumentException.class,
                () -> service.saveSession(
                        validRequest(),
                        null,
                        List.of()
                )
        );

        // Reject the request before accessing the database.
        verifyNoInteractions(
                studySessionRepository,
                sessionMajorRepository,
                referenceValidator
        );
    }

    @Test
    void rejectsMissingMajorTargetInformation() {

        // A null list indicates missing major-target information.
        assertThrows(
                IllegalArgumentException.class,
                () -> service.saveSession(
                        validRequest(),
                        42L,
                        null
                )
        );

        verifyNoInteractions(
                studySessionRepository,
                sessionMajorRepository,
                referenceValidator
        );
    }

    @Test
    void rejectsInvalidTimesBeforeDatabaseWrites() {

        // Create an invalid session that ends before it starts.
        CreateStudySessionRequest request =
                new CreateStudySessionRequest(
                        1L,
                        null,
                        "IN_PERSON",
                        OffsetDateTime.parse("2026-10-15T16:00:00Z"),
                        OffsetDateTime.parse("2026-10-15T14:00:00Z"),
                        10,
                        "Study session"
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.saveSession(
                        request,
                        42L,
                        List.of()
                )
        );

        // Invalid times must be rejected before any database access.
        verifyNoInteractions(
                studySessionRepository,
                sessionMajorRepository,
                referenceValidator
        );
    }

    @Test
    void rejectsDuplicateMajorIds() {

        // Duplicate IDs would represent the same SESSION_MAJOR relationship.
        assertThrows(
                IllegalArgumentException.class,
                () -> service.saveSession(
                        validRequest(),
                        42L,
                        List.of(3L, 3L)
                )
        );

        verifyNoInteractions(
                studySessionRepository,
                sessionMajorRepository,
                referenceValidator
        );
    }

    @Test
    void validatesReferencesBeforeSaving() {

        // Simulate an invalid course being rejected by the validator.
        doThrow(new ResourceNotFoundException("Course does not exist"))
                .when(referenceValidator)
                .validateReferences(1L, null, List.of(3L));

        assertThrows(
                ResourceNotFoundException.class,
                () -> service.saveSession(
                        validRequest(),
                        42L,
                        List.of(3L)
                )
        );

        // Failed validation must prevent all repository writes.
        verifyNoInteractions(
                studySessionRepository,
                sessionMajorRepository
        );
    }

    @Test
    void validatesReferencesBeforeSessionInsert() {

        // Return the entity supplied to the repository.
        when(studySessionRepository.save(any(StudySession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.saveSession(validRequest(), 42L, List.of());

        // Confirm that reference validation occurs before insertion.
        InOrder order = inOrder(
                referenceValidator,
                studySessionRepository
        );

        order.verify(referenceValidator)
                .validateReferences(1L, null, List.of());

        order.verify(studySessionRepository)
                .save(any(StudySession.class));
    }

    @Test
    void attemptsMajorSaveAfterSessionSave() {

        // Simulate a persisted session with a generated database ID.
        StudySession savedSession = mock(StudySession.class);
        when(savedSession.getSessionId()).thenReturn(100L);

        when(studySessionRepository.save(any(StudySession.class)))
                .thenReturn(savedSession);

        // Save a study session targeting major 3.
        service.saveSession(validRequest(), 42L, List.of(3L));

        // The session must be saved before its major relationship.
        InOrder order = inOrder(
                studySessionRepository,
                sessionMajorRepository
        );

        order.verify(studySessionRepository)
                .save(any(StudySession.class));

        order.verify(sessionMajorRepository)
                .save(any(SessionMajor.class));

        // Capture the relationship to confirm its two ID values.
        ArgumentCaptor<SessionMajor> majorCaptor =
                ArgumentCaptor.forClass(SessionMajor.class);

        verify(sessionMajorRepository)
                .save(majorCaptor.capture());

        SessionMajor savedMajor = majorCaptor.getValue();

        assertEquals(100L, savedMajor.getId().getSessionId());
        assertEquals(3L, savedMajor.getId().getMajorId());
    }

    // Provides a valid session request shared by the tests.
    private CreateStudySessionRequest validRequest() {
        return new CreateStudySessionRequest(
                1L,
                null,
                "IN_PERSON",
                OffsetDateTime.parse("2026-10-15T14:00:00Z"),
                OffsetDateTime.parse("2026-10-15T16:00:00Z"),
                10,
                "Study session"
        );
    }
}
