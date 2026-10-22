
package com.studygroupfinder.api.service;

import com.studygroupfinder.api.dto.CreateStudySessionRequest;
import com.studygroupfinder.api.entity.SessionMajor;
import com.studygroupfinder.api.entity.StudySession;
import com.studygroupfinder.api.repository.SessionMajorRepository;
import com.studygroupfinder.api.repository.StudySessionRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class StudySessionPersistenceService {

    private final StudySessionRepository studySessionRepository;
    private final SessionMajorRepository sessionMajorRepository;
    private final StudySessionReferenceValidator referenceValidator;

    // Spring injects the repositories and validator used during creation.
    public StudySessionPersistenceService(
            StudySessionRepository studySessionRepository,
            SessionMajorRepository sessionMajorRepository,
            StudySessionReferenceValidator referenceValidator
    ) {
        this.studySessionRepository = studySessionRepository;
        this.sessionMajorRepository = sessionMajorRepository;
        this.referenceValidator = referenceValidator;
    }

    // Creates a study session and its selected major relationships
    // inside one database transaction.
    // The creator ID is obtained from the authenticated user's profile.
    @Transactional
    public StudySession saveSession(
            CreateStudySessionRequest request,
            Long authenticatedCreatorId,
            List<Long> majorIds
    ) {

        // A request must exist before its fields can be validated.
        if (request == null) {
            throw new IllegalArgumentException(
                    "Study session request is required"
            );
        }

        // The creator must be resolved from verified authentication.
        if (authenticatedCreatorId == null) {
            throw new IllegalArgumentException(
                    "Authenticated creator is required"
            );
        }

        // Major selections are required to create join-table records.
        if (majorIds == null) {
            throw new IllegalArgumentException(
                    "Major targets have not been provided"
            );
        }

        // SESSION_MAJOR uses a composite primary key, so each major
        // can only be associated with a session once.
        Set<Long> uniqueMajorIds = new HashSet<>(majorIds);

        if (uniqueMajorIds.size() != majorIds.size()) {
            throw new IllegalArgumentException(
                    "Duplicate major targets are not allowed"
            );
        }

        // The start time must be earlier than the end time.
        if (request.startTime() == null
                || request.endTime() == null
                || !request.startTime().toInstant()
                .isBefore(request.endTime().toInstant())) {

            throw new IllegalArgumentException(
                    "Start time must be before end time"
            );
        }

        if (request.capacity() == null || request.capacity() < 1) {
            throw new IllegalArgumentException(
                    "Capacity must be greater than zero"
            );
        }

        // Verify that the selected course, optional building,
        // and target majors exist in the database.
        referenceValidator.validateReferences(
                request.courseId(),
                request.buildingId(),
                majorIds
        );

        // Create the session entity using the submitted session details.
        StudySession session = new StudySession();
        session.setCreatorId(authenticatedCreatorId);
        session.setCourseId(request.courseId());
        session.setBuildingId(request.buildingId());
        session.setDeliveryMode(request.deliveryMode());
        session.setStartTime(request.startTime());
        session.setEndTime(request.endTime());
        session.setCapacity(request.capacity());
        session.setDescription(request.description());

        // New sessions start in the OPEN state.
        session.setStatus("OPEN");

        // Save the session and obtain the database-generated session ID.
        StudySession savedSession = studySessionRepository.save(session);

        // Associate every selected major with the newly created session.
        // Spring rolls back the transaction if a database operation fails
        // with an unchecked exception.
        for (Long majorId : majorIds) {
            sessionMajorRepository.save(
                    new SessionMajor(
                            savedSession.getSessionId(),
                            majorId
                    )
            );
        }

        // Return the saved entity for conversion into the API response.
        return savedSession;
    }
}
