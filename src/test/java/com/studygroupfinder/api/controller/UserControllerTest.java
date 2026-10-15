package com.studygroupfinder.api.controller;

import com.studygroupfinder.api.entity.User;
import com.studygroupfinder.api.service.UserService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserControllerTest {

    private UserService userService;
    private UserController controller;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        controller = new UserController(userService);
    }

    @Test
    void returnsCurrentUserProfile() {
        User user = new User("sub-123", "Alex Student", "alex@example.com");
        when(userService.getOrCreateUser(any())).thenReturn(user);

        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("sub-123")
                .build();
        JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt, List.of());

        ResponseEntity<User> response = controller.getCurrentUser(authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("sub-123", response.getBody().getOauthSubject());
        assertEquals("Alex Student", response.getBody().getName());
        verify(userService).getOrCreateUser(authentication);
    }
}
