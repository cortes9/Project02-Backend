package com.studygroupfinder.api.service;

import com.studygroupfinder.api.entity.User;
import com.studygroupfinder.api.repository.UserRepository;

import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

// Manages student user accounts and provisions new accounts upon initial Google sign-in.
@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Resolves an existing database user or registers a new student from verified Google JWT claims.
    public User getOrCreateUser(JwtAuthenticationToken authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("Authenticated user is required");
        }

        String oauthSubject = authentication.getToken().getSubject();
        if (oauthSubject == null || oauthSubject.isBlank()) {
            throw new IllegalStateException("Authenticated user identity is unavailable");
        }

        return userRepository.findByOauthSubject(oauthSubject)
                .orElseGet(() -> {
                    String email = authentication.getToken().getClaimAsString("email");
                    String fullName = authentication.getToken().getClaimAsString("name");
                    String displayName = (fullName != null) ? fullName : authentication.getToken().getClaimAsString("given_name");

                    User newUser = new User(oauthSubject, displayName, email);
                    return userRepository.save(newUser);
                });
    }
}
