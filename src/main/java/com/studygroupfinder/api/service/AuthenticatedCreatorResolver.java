
package com.studygroupfinder.api.service;

import com.studygroupfinder.api.entity.User;
import com.studygroupfinder.api.repository.UserRepository;

import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

// Converts an authenticated JWT identity into an existing database user ID.
// This does not validate JWT signatures itself; Spring Security must do that.
@Service
public class AuthenticatedCreatorResolver {

    private final UserRepository userRepository;

    // Spring injects the repository when creating this service.
    public AuthenticatedCreatorResolver(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Accepts a JWT authentication object produced by Spring Security.
    // Never accepts a creator ID supplied in the HTTP request body.
    public Long resolveCreatorId(JwtAuthenticationToken authentication) {

        // Reject missing or unauthenticated identities.
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException(
                    "Authenticated user is required"
            );
        }

        // The JWT subject identifies the signed-in Google account.
        String oauthSubject = authentication.getToken().getSubject();

        if (oauthSubject == null || oauthSubject.isBlank()) {
            throw new IllegalStateException(
                    "Authenticated user identity is unavailable"
            );
        }

        // The user must already exist in our database.
        // Creating new Google user profiles belongs to another issue.
        User user = userRepository.findByOauthSubject(oauthSubject)
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated user has no existing profile"
                ));

        // Return the trusted database ID used by STUDY_SESSION.creator_id.
        return user.getUserId();
    }
}
