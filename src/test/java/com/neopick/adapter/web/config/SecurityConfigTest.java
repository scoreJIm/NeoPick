package com.neopick.adapter.web.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Security Configuration")
class SecurityConfigTest {

    @Autowired private MockMvc mockMvc;

    @Nested
    @DisplayName("Public endpoints — no auth required")
    class PublicEndpoints {

        @Test
        @DisplayName("GET /api/v1/health should be public")
        void healthShouldBePublic() throws Exception {
            mockMvc.perform(get("/api/v1/health"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("POST /api/v1/auth/send-sms-code should be public")
        void sendSmsShouldBePublic() throws Exception {
            mockMvc.perform(post("/api/v1/auth/send-sms-code")
                            .contentType("application/json")
                            .content("{\"phone\": \"+8613800138000\"}"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("POST /api/v1/auth/login should be public")
        void loginShouldBePublic() throws Exception {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType("application/json")
                            .content("{\"phone\": \"+8613800138000\", \"code\": \"123456\"}"))
                    .andExpect(status().isBadRequest()); // bad request = passed security
        }

        @Test
        @DisplayName("GET /api/v1/teachers should be public")
        void teachersShouldBePublic() throws Exception {
            mockMvc.perform(get("/api/v1/teachers")
                            .param("city", "SH"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET /api/v1/cities should be public")
        void citiesShouldBePublic() throws Exception {
            mockMvc.perform(get("/api/v1/cities"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET /api/v1/home should be public")
        void homeShouldBePublic() throws Exception {
            mockMvc.perform(get("/api/v1/home"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Secured endpoints — require authentication")
    class SecuredEndpoints {

        @Test
        @DisplayName("GET /api/v1/users/me should require auth")
        void userMeShouldRequireAuth() throws Exception {
            mockMvc.perform(get("/api/v1/users/me"))
                    .andExpect(status().is4xxClientError());
        }

        @Test
        @DisplayName("POST /api/v1/bookings should require auth")
        void bookingsShouldRequireAuth() throws Exception {
            mockMvc.perform(post("/api/v1/bookings")
                            .contentType("application/json")
                            .content("{}"))
                    .andExpect(status().is4xxClientError());
        }

        @Test
        @DisplayName("GET /api/v1/favorites should require auth")
        void favoritesShouldRequireAuth() throws Exception {
            mockMvc.perform(get("/api/v1/favorites"))
                    .andExpect(status().is4xxClientError());
        }
    }
}
