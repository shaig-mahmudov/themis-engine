package com.themis.engine.infrastructure.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class RateLimitFilter extends OncePerRequestFilter {
    private final boolean trustForwardedHeaders;
    private final int maxClients;
    private final Map<String, Bucket> cache;

    public RateLimitFilter(boolean trustForwardedHeaders, int maxClients) {
        if (maxClients < 1) {
            throw new IllegalArgumentException("Rate-limit max clients must be at least 1");
        }
        this.trustForwardedHeaders = trustForwardedHeaders;
        this.maxClients = maxClients;
        this.cache = Collections.synchronizedMap(new LinkedHashMap<>(128, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Bucket> eldest) {
                return size() > RateLimitFilter.this.maxClients;
            }
        });
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    private Bucket createNewBucket() {
        // Refill 100 tokens per minute (i.e. capacity 100)
        return Bucket.builder()
                .addLimit(Bandwidth.classic(100, Refill.intervally(100, Duration.ofMinutes(1))))
                .build();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String ip = getClientIp(request);
        Bucket bucket = getOrCreateBucket(ip);

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            writeErrorResponse(response, HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", "Rate limit exceeded. Maximum 100 requests per minute allowed.");
        }
    }

    private Bucket getOrCreateBucket(String clientId) {
        synchronized (cache) {
            return cache.computeIfAbsent(clientId, ignored -> createNewBucket());
        }
    }

    private String getClientIp(HttpServletRequest request) {
        if (!trustForwardedHeaders) {
            return request.getRemoteAddr();
        }

        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isBlank()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }

    private void writeErrorResponse(HttpServletResponse response, HttpStatus status, String error, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String json = String.format("{\"status\":%d,\"error\":\"%s\",\"message\":\"%s\"}", status.value(), error, message);
        response.getWriter().write(json);
    }
}
