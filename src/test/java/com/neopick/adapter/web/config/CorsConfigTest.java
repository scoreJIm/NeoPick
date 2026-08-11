package com.neopick.adapter.web.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("CORS Configuration")
class CorsConfigTest {

    @Autowired private MockMvc mockMvc;

    @Test
    @DisplayName("OPTIONS preflight should return CORS headers")
    void shouldReturnCorsHeaders() throws Exception {
        mockMvc.perform(options("/api/v1/teachers")
                        .header("Origin", "https://neopick.com")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Access-Control-Allow-Origin"))
                .andExpect(header().exists("Access-Control-Allow-Methods"))
                .andExpect(header().exists("Access-Control-Max-Age"));
    }

    @Test
    @DisplayName("GET request should include CORS allow origin")
    void shouldIncludeCorsOnGet() throws Exception {
        mockMvc.perform(get("/api/v1/teachers")
                        .header("Origin", "https://neopick.com")
                        .param("city", "SH"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Access-Control-Allow-Origin"));
    }

    @Test
    @DisplayName("CORS should work for health endpoint")
    void shouldIncludeCorsOnHealth() throws Exception {
        mockMvc.perform(get("/api/v1/health")
                        .header("Origin", "https://neopick.com"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Access-Control-Allow-Origin"));
    }
}
