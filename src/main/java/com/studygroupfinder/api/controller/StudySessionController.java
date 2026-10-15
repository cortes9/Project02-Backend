
package com.studygroupfinder.api.controller;

import com.studygroupfinder.api.dto.CreateStudySessionRequest;
import com.studygroupfinder.api.dto.StudySessionResponse;
import com.studygroupfinder.api.entity.StudySession;
import com.studygroupfinder.api.service.AuthenticatedCreatorResolver;
import com.studygroupfinder.api.service.StudySessionMajorTargetResolver;
import com.studygroupfinder.api.service.StudySessionPersistenceService;
import com.studygroupfinder.api.service.StudySessionService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.CurrentSecurityContext;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Handles the HTTP endpoint for creating study sessions.
@RestController
@RequestMapping("/api/v1/study-sessions")
public class StudySessionController {

    private final StudySessionService studySessionService;
    private final AuthenticatedCreatorResolver creatorResolver;
    private final StudySessionMajorTargetResolver majorTargetResolver;
    private final StudySessionPersistenceService persistenceService;

    // Spring supplies the services responsible for validation,
    // authenticated creator lookup, major selection, and persistence.
    public StudySessionController(
            StudySessionService studySessionService,
            AuthenticatedCreatorResolver creatorResolver,
            StudySessionMajorTargetResolver majorTargetResolver,
            StudySessionPersistenceService persistenceService
    ) {
        this.studySessionService = studySessionService;
        this.creatorResolver = creatorResolver;
        this.majorTargetResolver = majorTargetResolver;
        this.persistenceService = persistenceService;
    }

    // Creates a study session using the approved request body.
    // The creator is obtained from Spring Security, not from the client.
    @PostMapping
    public ResponseEntity<StudySessionResponse> createStudySession(
            @Valid @RequestBody CreateStudySessionRequest request,
            @CurrentSecurityContext(expression = "authentication")
            JwtAuthenticationToken authentication
    ) {

        // Check the meeting times before attempting database operations.
        studySessionService.validateSessionTime(request);

        // Map the verified authenticated identity to an existing user ID.
        Long creatorId = creatorResolver.resolveCreatorId(authentication);

        // Obtain major targets using the team's approved selection rules.
        List<Long> majorIds = majorTargetResolver.resolveMajorIds(
                request,
                creatorId
        );

        // Save the session and its major relationships in one transaction.
        StudySession savedSession = persistenceService.saveSession(
                request,
                creatorId,
                majorIds
        );

        // Convert the saved entity into the documented response body.
        StudySessionResponse response =
                StudySessionResponse.fromEntity(savedSession);

        // Return HTTP 201 when session creation succeeds.
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}
