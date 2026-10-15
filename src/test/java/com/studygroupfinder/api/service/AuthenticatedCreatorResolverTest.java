
package com.studygroupfinder.api.service;

import com.studygroupfinder.api.entity.User;
import com.studygroupfinder.api.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthenticatedCreatorResolverTest {

    private UserRepository userRepository;
    private AuthenticatedCreatorResolver resolver;

    @BeforeEach
    void setUp() {

        // Mock database access so the tests don't require Docker.
        userRepository = mock(UserRepository.class);

        // Create the resolver with the mocked repository.
        resolver = new AuthenticatedCreatorResolver(userRepository);
    }

    @Test
    void rejectsMissingAuthentication() {

        // No authenticated user means no trusted creator ID.
        assertThrows(
                IllegalStateException.class,
                () -> resolver.resolveCreatorId(null)
        );

        // The database should never be queried without authentication.
        verifyNoInteractions(userRepository);
    }

    @Test
    void rejectsJwtWithoutSubject() {

        // A token without a subject cannot identify a student.
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .claim("scope", "openid")
                .build();

        JwtAuthenticationToken authentication =
                new JwtAuthenticationToken(jwt);

        assertThrows(
                IllegalStateException.class,
                () -> resolver.resolveCreatorId(authentication)
        );

        verifyNoInteractions(userRepository);
    }

    @Test
    void rejectsUnknownUser() {

        // Simulates an authenticated student who has no database profile.
        JwtAuthenticationToken authentication = authenticationFor("google-123");

        when(userRepository.findByOauthSubject("google-123"))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalStateException.class,
                () -> resolver.resolveCreatorId(authentication)
        );
    }

    @Test
    void resolvesExistingUserId() {

        // Simulate a database record belonging to the JWT subject.
        User user = mock(User.class);

        when(user.getUserId()).thenReturn(42L);

        when(userRepository.findByOauthSubject("google-123"))
                .thenReturn(Optional.of(user));

        Long creatorId = resolver.resolveCreatorId(
                authenticationFor("google-123")
        );

        // The returned ID must come from the database user record.
        assertEquals(42L, creatorId);

        verify(userRepository).findByOauthSubject("google-123");
    }

    // Creates an authenticated JWT object for unit testing.
    // This simulates a JWT that Spring Security has already verified.
    // It does NOT configure authentication or bypass security in production.
    private JwtAuthenticationToken authenticationFor(String subject) {

        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject(subject)
                .build();

        // Mark this test authentication as authenticated by providing
        // a non-null collection of authorities.
        return new JwtAuthenticationToken(jwt, java.util.List.of());
    }
}
