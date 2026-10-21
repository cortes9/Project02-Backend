package com.studygroupfinder.api;

import com.studygroupfinder.api.error.ProblemDetailsAccessDeniedHandler;
import com.studygroupfinder.api.error.ProblemDetailsAuthenticationEntryPoint;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;

@Configuration
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(
			HttpSecurity http,
			ProblemDetailsAuthenticationEntryPoint authenticationEntryPoint,
			ProblemDetailsAccessDeniedHandler accessDeniedHandler) throws Exception {
		http
				.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers(EndpointRequest.to(HealthEndpoint.class)).permitAll()
						.requestMatchers(HttpMethod.GET, "/api/v1/study-sessions").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/v1/study-sessions/*").permitAll()
						.anyRequest().authenticated())
				.exceptionHandling(exceptions -> exceptions
						.authenticationEntryPoint(authenticationEntryPoint)
						.accessDeniedHandler(accessDeniedHandler))
				.oauth2ResourceServer(oauth2 -> oauth2
						.authenticationEntryPoint(authenticationEntryPoint)
						.accessDeniedHandler(accessDeniedHandler)
						.jwt(Customizer.withDefaults()));

		return http.build();
	}

	@Bean
	JwtDecoder jwtDecoder(
			@Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri,
			@Value("${google.client.id:}") String clientId) {

		NimbusJwtDecoder jwtDecoder = JwtDecoders.fromOidcIssuerLocation(issuerUri);

		OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuerUri);

		OAuth2TokenValidator<Jwt> withAudience = new JwtClaimValidator<List<String>>(
				JwtClaimNames.AUD,
				aud -> clientId == null || clientId.isBlank() || aud.contains(clientId)
		);

		OAuth2TokenValidator<Jwt> combinedValidator =
				new DelegatingOAuth2TokenValidator<>(withIssuer, withAudience);

		jwtDecoder.setJwtValidator(combinedValidator);
		return jwtDecoder;
	}
}



