
package com.studygroupfinder.api.controller;

import com.studygroupfinder.api.dto.CreateStudySessionRequest;
import com.studygroupfinder.api.dto.ReplaceStudySessionRequest;
import com.studygroupfinder.api.dto.StudySessionPageResponse;
import com.studygroupfinder.api.dto.StudySessionResponse;
import com.studygroupfinder.api.dto.UpdateStudySessionRequest;
import com.studygroupfinder.api.entity.StudySession;
import com.studygroupfinder.api.entity.User;
import com.studygroupfinder.api.service.AuthenticatedCreatorResolver;
import com.studygroupfinder.api.service.StudySessionManagementService;
import com.studygroupfinder.api.service.StudySessionMajorTargetResolver;
import com.studygroupfinder.api.service.StudySessionPersistenceService;
import com.studygroupfinder.api.service.StudySessionQueryService;
import com.studygroupfinder.api.service.StudySessionService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.CurrentSecurityContext;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

// Handles the HTTP endpoint for creating study sessions.
@RestController
@RequestMapping("/api/v1/study-sessions")
public class StudySessionController {

    private final StudySessionService studySessionService;
    private final AuthenticatedCreatorResolver creatorResolver;
    private final StudySessionMajorTargetResolver majorTargetResolver;
    private final StudySessionPersistenceService persistenceService;
    private final StudySessionQueryService queryService;
    private final StudySessionManagementService managementService;

    // Spring supplies the services responsible for validation,
    // authenticated creator lookup, major selection, and persistence.
    public StudySessionController(
            StudySessionService studySessionService,
            AuthenticatedCreatorResolver creatorResolver,
            StudySessionMajorTargetResolver majorTargetResolver,
            StudySessionPersistenceService persistenceService,
            StudySessionQueryService queryService,
            StudySessionManagementService managementService
    ) {
        this.studySessionService = studySessionService;
        this.creatorResolver = creatorResolver;
        this.majorTargetResolver = majorTargetResolver;
        this.persistenceService = persistenceService;
        this.queryService = queryService;
        this.managementService = managementService;
    }

    @GetMapping
    public StudySessionPageResponse listStudySessions(
            @RequestParam(name = "course_code", required = false) String courseCode,
            @RequestParam(name = "building_id", required = false) Long buildingId,
            @RequestParam(required = false) LocalDate date,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        return queryService.findSessions(courseCode, buildingId, date, limit, offset);
    }

    @GetMapping("/{sessionId}")
    public StudySessionResponse getStudySession(@PathVariable Long sessionId) {
        return queryService.findSession(sessionId);
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
        StudySessionResponse response = queryService.findSession(savedSession.getSessionId());

        // Return HTTP 201 when session creation succeeds.
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/{sessionId}/join")
    public ResponseEntity<Void> joinStudySession(
            @PathVariable Long sessionId,
            @CurrentSecurityContext(expression = "authentication")
            JwtAuthenticationToken authentication) {
        managementService.join(sessionId, currentUser(authentication));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{sessionId}/leave")
    public ResponseEntity<Void> leaveStudySession(
            @PathVariable Long sessionId,
            @CurrentSecurityContext(expression = "authentication")
            JwtAuthenticationToken authentication) {
        managementService.leave(sessionId, currentUser(authentication));
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{sessionId}")
    public StudySessionResponse replaceStudySession(
            @PathVariable Long sessionId,
            @Valid @RequestBody ReplaceStudySessionRequest request,
            @CurrentSecurityContext(expression = "authentication")
            JwtAuthenticationToken authentication) {
        managementService.replace(sessionId, currentUser(authentication), request);
        return queryService.findSession(sessionId);
    }

    @PatchMapping("/{sessionId}")
    public StudySessionResponse updateStudySession(
            @PathVariable Long sessionId,
            @RequestBody UpdateStudySessionRequest request,
            @CurrentSecurityContext(expression = "authentication")
            JwtAuthenticationToken authentication) {
        managementService.update(sessionId, currentUser(authentication), request);
        return queryService.findSession(sessionId);
    }

    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Void> deleteStudySession(
            @PathVariable Long sessionId,
            @CurrentSecurityContext(expression = "authentication")
            JwtAuthenticationToken authentication) {
        managementService.delete(sessionId, currentUser(authentication));
        return ResponseEntity.noContent().build();
    }

    private User currentUser(JwtAuthenticationToken authentication) {
        return creatorResolver.resolveUser(authentication);
    }
}
