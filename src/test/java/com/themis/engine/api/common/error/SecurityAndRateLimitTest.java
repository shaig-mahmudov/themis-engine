package com.themis.engine.api.common.error;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityAndRateLimitTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testGetActuatorHealth_WithoutApiKey_Succeeds() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void testGetCharacters_WithoutApiKey_Fails401() throws Exception {
        mockMvc.perform(get("/api/characters/some-id"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Missing API Key"))
                .andExpect(jsonPath("$.message").value("Required header 'X-API-KEY' is missing."));
    }

    @Test
    void testGetCharacters_WithInvalidApiKey_Fails401() throws Exception {
        mockMvc.perform(get("/api/characters/some-id")
                .header("X-API-KEY", "wrong-key"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid API Key"))
                .andExpect(jsonPath("$.message").value("The provided 'X-API-KEY' is invalid."));
    }

    @Test
    void testRateLimit_Exceeded_Returns429() throws Exception {
        // Send 100 requests with valid API key (default-dev-key) to trigger rate limit.
        // We'll use an IP that is unique to this test to avoid interfering with other tests.
        String testIp = "192.168.1.100";
        for (int i = 0; i < 100; i++) {
            mockMvc.perform(get("/api/characters/some-id")
                    .header("X-API-KEY", "default-dev-key")
                    .header("X-Forwarded-For", testIp))
                    .andExpect(status().isNotFound()); // NotFound since the character doesn't exist, but it's not rate limited yet
        }

        // 101st request from same IP should get 429
        mockMvc.perform(get("/api/characters/some-id")
                .header("X-API-KEY", "default-dev-key")
                .header("X-Forwarded-For", testIp))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("Too Many Requests"))
                .andExpect(jsonPath("$.message").value("Rate limit exceeded. Maximum 100 requests per minute allowed."));
    }
}
