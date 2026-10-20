package com.studygroupfinder.api;

import com.studygroupfinder.api.error.ProblemDetailsAccessDeniedHandler;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ApiApplicationTests {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16.15");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ProblemDetailsAccessDeniedHandler accessDeniedHandler;

	@Test
	void contextLoads() {
	}

	@Test
	void healthEndpointIsPublicAndDoesNotExposeDetails() throws Exception {
		mockMvc.perform(get("/actuator/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"))
				.andExpect(jsonPath("$.components").doesNotExist())
				.andExpect(jsonPath("$.details").doesNotExist());
	}

	@Test
	void missingAuthenticationReturnsProblemDetails() throws Exception {
		mockMvc.perform(get("/api/v1/users/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(
						MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.type").value(
						"urn:study-group-finder:problem:unauthorized"))
				.andExpect(jsonPath("$.title").value("Unauthorized"))
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.instance").value("/api/v1/users/me"));
	}

	@Test
	void invalidAuthenticationReturnsProblemDetails() throws Exception {
		mockMvc.perform(get("/api/v1/users/me")
					.header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(
						MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.detail").value(
						"Authentication is missing or invalid."));
	}

	@Test
	void accessDeniedHandlerReturnsSanitizedProblemDetails() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI("/api/v1/restricted");
		MockHttpServletResponse response = new MockHttpServletResponse();

		accessDeniedHandler.handle(
				request,
				response,
				new AccessDeniedException("internal permission rule"));

		assertEquals(403, response.getStatus());
		assertEquals(
				MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				response.getContentType());

		String responseBody = response.getContentAsString();
		assertTrue(responseBody.contains("\"status\":403"));
		assertTrue(responseBody.contains("\"title\":\"Forbidden\""));
		assertTrue(responseBody.contains("\"instance\":\"/api/v1/restricted\""));
		assertFalse(responseBody.contains("internal permission rule"));
	}


}
