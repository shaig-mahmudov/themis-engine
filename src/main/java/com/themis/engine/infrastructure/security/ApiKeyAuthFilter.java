package com.themis.engine.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class ApiKeyAuthFilter extends OncePerRequestFilter {
    private final String expectedApiKey;

    public ApiKeyAuthFilter(String expectedApiKey) {
        if (expectedApiKey == null || expectedApiKey.isBlank()) {
            throw new IllegalStateException("THEMIS_API_KEY must be configured");
        }
        this.expectedApiKey = expectedApiKey;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String requestUri = request.getRequestURI();
        
        // Only enforce API Key on /api/** routes
        if (requestUri.startsWith("/api/")) {
            String requestKey = request.getHeader("X-API-KEY");
            
            if (requestKey == null) {
                writeErrorResponse(response, HttpStatus.UNAUTHORIZED, "Missing API Key", "Required header 'X-API-KEY' is missing.");
                return;
            }
            
            if (!secureEquals(expectedApiKey, requestKey)) {
                writeErrorResponse(response, HttpStatus.UNAUTHORIZED, "Invalid API Key", "The provided 'X-API-KEY' is invalid.");
                return;
            }
            
            ApiKeyAuthenticationToken auth = new ApiKeyAuthenticationToken(requestKey, true);
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        
        filterChain.doFilter(request, response);
    }

    private boolean secureEquals(String expected, String actual) {
        return MessageDigest.isEqual(
            expected.getBytes(StandardCharsets.UTF_8),
            actual.getBytes(StandardCharsets.UTF_8)
        );
    }

    private void writeErrorResponse(HttpServletResponse response, HttpStatus status, String error, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String json = String.format("{\"status\":%d,\"error\":\"%s\",\"message\":\"%s\"}", status.value(), error, message);
        response.getWriter().write(json);
    }
}
