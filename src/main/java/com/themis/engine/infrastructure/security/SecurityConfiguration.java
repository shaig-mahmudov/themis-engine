package com.themis.engine.infrastructure.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    @Value("${themis.api-key}")
    private String apiKey;

    @Value("#{'${themis.cors.allowed-origins}'.split(',')}")
    private List<String> allowedOrigins;

    @Value("${themis.rate-limit.trust-forwarded-headers:false}")
    private boolean trustForwardedHeaders;

    @Value("${themis.rate-limit.max-clients:10000}")
    private int maxRateLimitClients;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        List<String> corsOrigins = allowedOrigins.stream().map(String::trim).filter(origin -> !origin.isBlank()).toList();
        if (corsOrigins.isEmpty() || corsOrigins.contains("*")) {
            throw new IllegalStateException("Configure an explicit THEMIS_CORS_ALLOWED_ORIGINS allowlist");
        }

        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(request -> {
                CorsConfiguration config = new CorsConfiguration();
                config.setAllowedOrigins(corsOrigins);
                config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
                config.setAllowedHeaders(List.of("Content-Type", "X-API-KEY", "If-Match"));
                config.setExposedHeaders(List.of("ETag"));
                config.setAllowCredentials(false);
                config.setMaxAge(3600L);
                return config;
            }))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                .requestMatchers("/error").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(new ApiKeyAuthFilter(apiKey), UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(new RateLimitFilter(trustForwardedHeaders, maxRateLimitClients), ApiKeyAuthFilter.class);

        return http.build();
    }
}
