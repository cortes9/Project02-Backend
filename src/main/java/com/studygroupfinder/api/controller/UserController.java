package com.studygroupfinder.api.controller;

import com.studygroupfinder.api.entity.User;
import com.studygroupfinder.api.service.UserService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Manages user profile endpoints for authenticated students.
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // Returns the profile of the authenticated student, auto-provisioning the user if logging in for the first time.
    @GetMapping("/me")
    public ResponseEntity<User> getCurrentUser(JwtAuthenticationToken authentication) {
        User user = userService.getOrCreateUser(authentication);
        return ResponseEntity.ok(user);
    }
}
