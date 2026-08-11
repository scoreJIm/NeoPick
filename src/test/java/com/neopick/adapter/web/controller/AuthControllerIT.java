package com.neopick.adapter.web.controller;

import com.neopick.application.auth.LoginUseCase;
import com.neopick.application.auth.RefreshTokenUseCase;
import com.neopick.application.auth.SendSmsCodeUseCase;
import com.neopick.domain.auth.SmsCodeService;
import com.neopick.domain.auth.TokenPair;
import com.neopick.domain.user.*;
import com.neopick.port.security.TokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import({SendSmsCodeUseCase.class, LoginUseCase.class, RefreshTokenUseCase.class})
@DisplayName("Auth API Integration Tests")
class AuthControllerIT {

    @Autowired private MockMvc mockMvc;

    @MockBean private SmsCodeService smsCodeService;
    @MockBean private UserRepository userRepository;
    @MockBean private TokenProvider tokenProvider;
    @MockBean private com.neopick.infrastructure.metrics.BusinessMetrics businessMetrics;

    private static final String PHONE = "13800138000";
    private static final String CODE = "123456";

    @Nested
    @DisplayName("POST /api/v1/auth/send-sms-code")
    class SendSmsCode {

        @Test
        @DisplayName("should send SMS code successfully")
        void shouldSendSmsCode() throws Exception {
            doNothing().when(smsCodeService).sendCode(anyString());

            mockMvc.perform(post("/api/v1/auth/send-sms-code")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"phone\": \"13800138000\"}"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should reject empty phone number")
        void shouldRejectEmptyPhone() throws Exception {
            mockMvc.perform(post("/api/v1/auth/send-sms-code")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"phone\": \"\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should reject missing phone field")
        void shouldRejectMissingPhone() throws Exception {
            mockMvc.perform(post("/api/v1/auth/send-sms-code")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/login")
    class Login {

        @Test
        @DisplayName("should login existing user with valid code")
        void shouldLoginExistingUser() throws Exception {
            when(smsCodeService.verifyCode(PHONE, CODE)).thenReturn(true);

            User user = new User(UserId.generate(), PhoneNumber.of(PHONE),
                    "TestUser", UserRole.STUDENT);
            when(userRepository.findByPhone(any())).thenReturn(java.util.Optional.of(user));
            when(userRepository.save(any(User.class))).thenReturn(user);
            when(tokenProvider.generateAccessToken(anyString(), anyString()))
                    .thenReturn("access-token-xxx");
            when(tokenProvider.generateRefreshToken(anyString()))
                    .thenReturn("refresh-token-xxx");

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"phone": "%s", "code": "%s"}""".formatted(PHONE, CODE)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.access_token").value("access-token-xxx"))
                    .andExpect(jsonPath("$.data.refresh_token").value("refresh-token-xxx"))
                    .andExpect(jsonPath("$.data.user.nickname").value("TestUser"))
                    .andExpect(jsonPath("$.data.user.role").value("STUDENT"));
        }

        @Test
        @DisplayName("should register new user when phone not found")
        void shouldRegisterNewUser() throws Exception {
            when(smsCodeService.verifyCode(PHONE, CODE)).thenReturn(true);
            when(userRepository.findByPhone(any())).thenReturn(java.util.Optional.empty());
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
            when(tokenProvider.generateAccessToken(anyString(), anyString()))
                    .thenReturn("access-token-new");
            when(tokenProvider.generateRefreshToken(anyString()))
                    .thenReturn("refresh-token-new");

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"phone": "%s", "code": "%s"}""".formatted(PHONE, CODE)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.user.nickname").value(containsString("User_")));
        }

        @Test
        @DisplayName("should reject invalid verification code")
        void shouldRejectInvalidCode() throws Exception {
            when(smsCodeService.verifyCode(PHONE, "wrong")).thenReturn(false);

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"phone": "%s", "code": "wrong"}""".formatted(PHONE)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should reject missing code")
        void shouldRejectMissingCode() throws Exception {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"phone\": \"13800138000\"}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/refresh")
    class RefreshToken {

        @Test
        @DisplayName("should refresh token successfully")
        void shouldRefreshToken() throws Exception {
            when(tokenProvider.validateToken("old-refresh-token")).thenReturn(true);
            when(tokenProvider.getUserIdFromToken("old-refresh-token")).thenReturn("user-1");
            when(tokenProvider.getRoleFromToken("old-refresh-token")).thenReturn("STUDENT");
            when(tokenProvider.generateAccessToken(anyString(), any()))
                    .thenReturn("new-access-token");
            when(tokenProvider.generateRefreshToken(anyString()))
                    .thenReturn("new-refresh-token");

            mockMvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"refresh_token\": \"old-refresh-token\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.access_token").value("new-access-token"))
                    .andExpect(jsonPath("$.data.refresh_token").value("new-refresh-token"));
        }

        @Test
        @DisplayName("should reject missing refresh_token")
        void shouldRejectMissingToken() throws Exception {
            mockMvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/logout")
    class Logout {

        @Test
        @DisplayName("should logout successfully")
        void shouldLogout() throws Exception {
            mockMvc.perform(post("/api/v1/auth/logout"))
                    .andExpect(status().isOk());
        }
    }
}
