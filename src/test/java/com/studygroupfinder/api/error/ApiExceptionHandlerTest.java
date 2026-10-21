package com.studygroupfinder.api.error;

import com.studygroupfinder.api.controller.StudySessionController;
import com.studygroupfinder.api.dto.CreateStudySessionRequest;
import com.studygroupfinder.api.service.AuthenticatedCreatorResolver;
import com.studygroupfinder.api.service.StudySessionMajorTargetResolver;
import com.studygroupfinder.api.service.StudySessionPersistenceService;
import com.studygroupfinder.api.service.StudySessionService;
import com.studygroupfinder.api.service.StudySessionQueryService;
import com.studygroupfinder.api.service.StudySessionManagementService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApiExceptionHandlerTest {

    private static final String ENDPOINT = "/api/v1/study-sessions";

    private AuthenticatedCreatorResolver creatorResolver;
    private StudySessionMajorTargetResolver majorResolver;
    private StudySessionPersistenceService persistenceService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        creatorResolver = mock(AuthenticatedCreatorResolver.class);
        majorResolver = mock(StudySessionMajorTargetResolver.class);
        persistenceService = mock(StudySessionPersistenceService.class);

        StudySessionController controller = new StudySessionController(
                new StudySessionService(),
                creatorResolver,
                majorResolver,
                persistenceService,
                mock(StudySessionQueryService.class),
                mock(StudySessionManagementService.class)
        );

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void returnsProblemDetailsForValidationFailure() throws Exception {
        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(
                        "urn:study-group-finder:problem:validation-error"))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").isNotEmpty())
                .andExpect(jsonPath("$.instance").value(ENDPOINT));
    }

    @Test
    void returnsProblemDetailsForMissingResource() throws Exception {
        prepareSuccessfulResolution();
        when(persistenceService.saveSession(
                any(CreateStudySessionRequest.class),
                eq(42L),
                eq(List.of(3L))))
                .thenThrow(new ResourceNotFoundException(
                        "Course does not exist"));

        mockMvc.perform(validRequest())
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value(
                        "Course does not exist"))
                .andExpect(jsonPath("$.instance").value(ENDPOINT));
    }

    @Test
    void returnsProblemDetailsForConflict() throws Exception {
        prepareSuccessfulResolution();
        when(persistenceService.saveSession(
                any(CreateStudySessionRequest.class),
                eq(42L),
                eq(List.of(3L))))
                .thenThrow(new ConflictException(
                        "The user has already joined this session"));

        mockMvc.perform(validRequest())
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value(
                        "The user has already joined this session"));
    }

    @Test
    void sanitizesUnexpectedServerErrors() throws Exception {
        prepareSuccessfulResolution();
        when(persistenceService.saveSession(
                any(CreateStudySessionRequest.class),
                eq(42L),
                eq(List.of(3L))))
                .thenThrow(new RuntimeException(
                        "database password and internal stack details"));

        mockMvc.perform(validRequest())
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(
                        "urn:study-group-finder:problem:internal-server-error"))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.detail").value(
                        "An unexpected error occurred."))
                .andExpect(content().string(not(containsString(
                        "database password"))));
    }

    private void prepareSuccessfulResolution() {
        when(creatorResolver.resolveCreatorId(
                nullable(org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken.class)))
                .thenReturn(42L);
        when(majorResolver.resolveMajorIds(
                any(CreateStudySessionRequest.class),
                eq(42L)))
                .thenReturn(List.of(3L));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder validRequest() {
        return post(ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "courseId": 1,
                          "deliveryMode": "REMOTE",
                          "startTime": "2026-10-21T18:00:00Z",
                          "endTime": "2026-10-21T19:00:00Z",
                          "capacity": 10,
                          "description": "Study group",
                          "majorIds": [3]
                        }
                        """);
    }
}
