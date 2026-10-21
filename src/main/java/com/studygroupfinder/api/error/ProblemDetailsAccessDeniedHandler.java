package com.studygroupfinder.api.error;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;

@Component
public class ProblemDetailsAccessDeniedHandler implements AccessDeniedHandler {

    private final HandlerExceptionResolver exceptionResolver;

    public ProblemDetailsAccessDeniedHandler(
            @Qualifier("handlerExceptionResolver")
            HandlerExceptionResolver exceptionResolver) {
        this.exceptionResolver = exceptionResolver;
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException)
            throws IOException, ServletException {

        exceptionResolver.resolveException(
                request,
                response,
                null,
                accessDeniedException
        );
    }
}
