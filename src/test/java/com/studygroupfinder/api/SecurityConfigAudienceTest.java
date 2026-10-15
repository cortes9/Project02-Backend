package com.studygroupfinder.api;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SecurityConfigAudienceTest {

    @Test
    void rejectsJwtWithIncorrectAudience() {
        String issuer = "https://accounts.google.com";
        String validClientId = "expected-study-group-client-id";

        JwtClaimValidator<List<String>> audienceValidator = new JwtClaimValidator<>(
                JwtClaimNames.AUD,
                aud -> aud.contains(validClientId)
        );

        Jwt jwtWithWrongAudience = Jwt.withTokenValue("mock-token")
                .header("alg", "RS256")
                .issuer(issuer)
                .audience(List.of("malicious-different-app-id"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        OAuth2TokenValidatorResult result = audienceValidator.validate(jwtWithWrongAudience);

        assertTrue(result.hasErrors(), "JWT with incorrect audience should fail validation");
    }

    @Test
    void acceptsJwtWithMatchingAudience() {
        String issuer = "https://accounts.google.com";
        String validClientId = "expected-study-group-client-id";

        JwtClaimValidator<List<String>> audienceValidator = new JwtClaimValidator<>(
                JwtClaimNames.AUD,
                aud -> aud.contains(validClientId)
        );

        Jwt jwtWithValidAudience = Jwt.withTokenValue("mock-token")
                .header("alg", "RS256")
                .issuer(issuer)
                .audience(List.of("expected-study-group-client-id"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        OAuth2TokenValidatorResult result = audienceValidator.validate(jwtWithValidAudience);

        assertFalse(result.hasErrors(), "JWT with matching audience should pass validation");
    }
}
