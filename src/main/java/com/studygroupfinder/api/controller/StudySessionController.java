
package com.studygroupfinder.api.controller;

import com.studygroupfinder.api.dto.CreateStudySessionRequest;
import com.studygroupfinder.api.dto.ReplaceStudySessionRequest;
import com.studygroupfinder.api.dto.StudySessionPageResponse;
import com.studygroupfinder.api.dto.StudySessionResponse;
import com.studygroupfinder.api.dto.UpdateStudySessionRequest;
import com.studygroupfinder.api.entity.StudySession;
import com.studygroupfinder.api.entity.User;
import com.studygroupfinder.api.OpenApiConfig;
import com.studygroupfinder.api.service.AuthenticatedCreatorResolver;
import com.studygroupfinder.api.service.StudySessionManagementService;
import com.studygroupfinder.api.service.StudySessionMajorTargetResolver;
import com.studygroupfinder.api.service.StudySessionPersistenceService;
import com.studygroupfinder.api.service.StudySessionQueryService;
import com.studygroupfinder.api.service.StudySessionService;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
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
@Tag(name = "Study Sessions", description = "Browse and manage study sessions")
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
    @Operation(summary = "List study sessions", description = "Returns public, filtered study-session results with offset pagination.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Study sessions returned",
                    content = @Content(schema = @Schema(implementation = StudySessionPageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid filter or pagination value",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected server error",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public StudySessionPageResponse listStudySessions(
            @Parameter(description = "Exact course code, matched case-insensitively")
            @RequestParam(name = "course_code", required = false) String courseCode,
            @Parameter(description = "Building identifier")
            @RequestParam(name = "building_id", required = false) Long buildingId,
            @Parameter(description = "UTC calendar date during which the session occurs")
            @RequestParam(required = false) LocalDate date,
            @Parameter(description = "Maximum records to return", example = "20")
            @RequestParam(defaultValue = "20") int limit,
            @Parameter(description = "Records to skip", example = "0")
            @RequestParam(defaultValue = "0") int offset) {
        return queryService.findSessions(courseCode, buildingId, date, limit, offset);
    }

    @GetMapping("/{sessionId}")
    @Operation(summary = "Get a study session", description = "Returns complete public details for one study session.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Study session returned",
                    content = @Content(schema = @Schema(implementation = StudySessionResponse.class))),
            @ApiResponse(responseCode = "404", description = "Study session not found",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected server error",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public StudySessionResponse getStudySession(
            @Parameter(description = "Study-session identifier", required = true)
            @PathVariable Long sessionId) {
        return queryService.findSession(sessionId);
    }

    // Creates a study session using the approved request body.
    // The creator is obtained from Spring Security, not from the client.
    @PostMapping
    @Operation(summary = "Create a study session")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Study session created",
                    content = @Content(schema = @Schema(implementation = StudySessionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Authentication is missing or invalid",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Referenced resource not found",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Request conflicts with current state",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected server error",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
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
    @Operation(summary = "Join a study session")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Study session joined"),
            @ApiResponse(responseCode = "401", description = "Authentication is missing or invalid",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Study session not found",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Duplicate membership, full session, or schedule conflict",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected server error",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<Void> joinStudySession(
            @Parameter(description = "Study-session identifier", required = true)
            @PathVariable Long sessionId,
            @CurrentSecurityContext(expression = "authentication")
            JwtAuthenticationToken authentication) {
        managementService.join(sessionId, currentUser(authentication));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{sessionId}/leave")
    @Operation(summary = "Leave a study session", description = "Idempotently removes the current user's membership.")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Membership removed or already absent"),
            @ApiResponse(responseCode = "401", description = "Authentication is missing or invalid",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Study session not found",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected server error",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<Void> leaveStudySession(
            @Parameter(description = "Study-session identifier", required = true)
            @PathVariable Long sessionId,
            @CurrentSecurityContext(expression = "authentication")
            JwtAuthenticationToken authentication) {
        managementService.leave(sessionId, currentUser(authentication));
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{sessionId}")
    @Operation(summary = "Replace a study session")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Study session replaced",
                    content = @Content(schema = @Schema(implementation = StudySessionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Authentication is missing or invalid",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Only the creator may update the session",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Session or referenced resource not found",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Capacity conflicts with current membership",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected server error",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public StudySessionResponse replaceStudySession(
            @Parameter(description = "Study-session identifier", required = true)
            @PathVariable Long sessionId,
            @Valid @RequestBody ReplaceStudySessionRequest request,
            @CurrentSecurityContext(expression = "authentication")
            JwtAuthenticationToken authentication) {
        managementService.replace(sessionId, currentUser(authentication), request);
        return queryService.findSession(sessionId);
    }

    @PatchMapping("/{sessionId}")
    @Operation(summary = "Partially update a study session")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Study session updated",
                    content = @Content(schema = @Schema(implementation = StudySessionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Authentication is missing or invalid",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Only the creator may update the session",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Session or referenced resource not found",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Capacity conflicts with current membership",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected server error",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public StudySessionResponse updateStudySession(
            @Parameter(description = "Study-session identifier", required = true)
            @PathVariable Long sessionId,
            @RequestBody UpdateStudySessionRequest request,
            @CurrentSecurityContext(expression = "authentication")
            JwtAuthenticationToken authentication) {
        managementService.update(sessionId, currentUser(authentication), request);
        return queryService.findSession(sessionId);
    }

    @DeleteMapping("/{sessionId}")
    @Operation(summary = "Delete a study session", description = "Deletes a session as its creator or an administrator.")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Study session deleted"),
            @ApiResponse(responseCode = "401", description = "Authentication is missing or invalid",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "User may not delete this session",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Study session not found",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected server error",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<Void> deleteStudySession(
            @Parameter(description = "Study-session identifier", required = true)
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
