package com.studygroupfinder.api.service;

import com.studygroupfinder.api.entity.User;
import com.studygroupfinder.api.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserServiceTest {

    private UserRepository userRepository;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userService = new UserService(userRepository);
    }

    @Test
    void rejectsMissingAuthentication() {
        assertThrows(IllegalStateException.class, () -> userService.getOrCreateUser(null));
        verifyNoInteractions(userRepository);
    }

    @Test
    void returnsExistingUser() {
        User user = new User("google-sub-123", "Jane Doe", "jane@example.com");
        when(userRepository.findByOauthSubject("google-sub-123")).thenReturn(Optional.of(user));

        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("google-sub-123")
                .build();
        JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt, List.of());

        User result = userService.getOrCreateUser(auth);

        assertEquals("google-sub-123", result.getOauthSubject());
        assertEquals("Jane Doe", result.getName());
        verify(userRepository).findByOauthSubject("google-sub-123");
        verify(userRepository, never()).save(any());
    }

    @Test
    void provisionsNewUserWhenNotFound() {
        when(userRepository.findByOauthSubject("new-google-sub")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("new-google-sub")
                .claim("email", "newuser@example.com")
                .claim("name", "New Student")
                .build();
        JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt, List.of());

        User result = userService.getOrCreateUser(auth);

        assertEquals("new-google-sub", result.getOauthSubject());
        assertEquals("New Student", result.getName());
        assertEquals("newuser@example.com", result.getEmail());
        verify(userRepository).save(any(User.class));
    }
}
