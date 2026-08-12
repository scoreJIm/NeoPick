package com.neopick.adapter.web.controller;

import com.neopick.application.auth.LoginUseCase;
import com.neopick.application.auth.RefreshTokenUseCase;
import com.neopick.application.auth.SendSmsCodeUseCase;
import com.neopick.domain.auth.SmsCodeService;
import com.neopick.domain.auth.StoredRefreshToken;
import com.neopick.domain.user.*;
import com.neopick.port.security.RefreshTokenRepository;
import com.neopick.port.security.TokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({SendSmsCodeUseCase.class, LoginUseCase.class, RefreshTokenUseCase.class})
@DisplayName("Auth API Integration Tests")
class AuthControllerIT {

    @Autowired private MockMvc mockMvc;

    @MockBean private SmsCodeService smsCodeService;
    @MockBean private UserRepository userRepository;
    @MockBean private TokenProvider tokenProvider;
    @MockBean private RefreshTokenRepository refreshTokenRepository;
    @MockBean private com.neopick.infrastructure.metrics.BusinessMetrics businessMetrics;
    @MockBean private com.neopick.adapter.web.security.SecurityContextHolder securityContextHolder;
    @MockBean private com.neopick.infrastructure.security.SecurityEventLogger securityEventLogger;

    private static final String PHONE = "+8613800138000";
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
                            .content("{\"phone\": \"+8613800138000\"}"))
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
            when(userRepository.findByPhone(any())).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenReturn(user);
            when(tokenProvider.generateAccessToken(anyString(), anyString()))
                    .thenReturn("access-token-xxx");
            when(tokenProvider.generateRefreshToken(anyString()))
                    .thenReturn("refresh-token-xxx");
            when(tokenProvider.getExpirationFromToken(anyString()))
                    .thenReturn(Instant.now().plusSeconds(3600));
            when(tokenProvider.hashToken(anyString()))
                    .thenReturn("hash-refresh-token-xxx");

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
            when(userRepository.findByPhone(any())).thenReturn(Optional.empty());
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
            when(tokenProvider.generateAccessToken(anyString(), anyString()))
                    .thenReturn("access-token-new");
            when(tokenProvider.generateRefreshToken(anyString()))
                    .thenReturn("refresh-token-new");
            when(tokenProvider.getExpirationFromToken(anyString()))
                    .thenReturn(Instant.now().plusSeconds(3600));
            when(tokenProvider.hashToken(anyString()))
                    .thenReturn("hash-new");

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
                            .content("{\"phone\": \"+8613800138000\"}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/refresh")
    class RefreshToken {

        private static final String USER_ID = UUID.randomUUID().toString();
        private static final String FAMILY_ID = UUID.randomUUID().toString();
        private static final String TOKEN_ID = UUID.randomUUID().toString();

        @Test
        @DisplayName("should refresh token successfully with rotation")
        void shouldRefreshTokenWithRotation() throws Exception {
            String oldToken = "old-refresh-token";
            String oldHash = "old-token-hash";
            String newRefreshToken = "new-refresh-token";
            String newTokenHash = "new-token-hash";

            StoredRefreshToken storedToken = new StoredRefreshToken(
                    UUID.fromString(TOKEN_ID), UUID.fromString(USER_ID), oldHash,
                    FAMILY_ID, false, null,
                    LocalDateTime.now().plusDays(30), null, LocalDateTime.now());

            when(tokenProvider.validateToken(oldToken)).thenReturn(true);
            when(tokenProvider.hashToken(oldToken)).thenReturn(oldHash);
            when(refreshTokenRepository.findByTokenHash(oldHash))
                    .thenReturn(Optional.of(storedToken));
            when(tokenProvider.getUserIdFromToken(oldToken)).thenReturn(USER_ID);
            when(tokenProvider.getRoleFromToken(oldToken)).thenReturn("STUDENT");
            when(tokenProvider.generateAccessToken(anyString(), anyString()))
                    .thenReturn("new-access-token");
            when(tokenProvider.generateRefreshToken(anyString()))
                    .thenReturn(newRefreshToken);
            when(tokenProvider.hashToken(newRefreshToken)).thenReturn(newTokenHash);
            when(tokenProvider.getExpirationFromToken(newRefreshToken))
                    .thenReturn(Instant.now().plusSeconds(3600));

            mockMvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"refresh_token\": \"%s\"}".formatted(oldToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.access_token").value("new-access-token"))
                    .andExpect(jsonPath("$.data.refresh_token").value(newRefreshToken));

            verify(refreshTokenRepository).markRevoked(eq(oldHash), eq(newTokenHash), any());
            verify(refreshTokenRepository).save(any(), eq(newTokenHash), eq(FAMILY_ID), any());
        }

        @Test
        @DisplayName("should reject already revoked token with reuse detection")
        void shouldRejectRevokedToken() throws Exception {
            String oldToken = "old-refresh-token";
            String oldHash = "old-token-hash";

            StoredRefreshToken storedToken = new StoredRefreshToken(
                    UUID.fromString(TOKEN_ID), UUID.fromString(USER_ID), oldHash,
                    FAMILY_ID, true, "some-replacement",
                    LocalDateTime.now().plusDays(30), LocalDateTime.now(), LocalDateTime.now());

            when(tokenProvider.validateToken(oldToken)).thenReturn(true);
            when(tokenProvider.hashToken(oldToken)).thenReturn(oldHash);
            when(refreshTokenRepository.findByTokenHash(oldHash))
                    .thenReturn(Optional.of(storedToken));

            mockMvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"refresh_token\": \"%s\"}".formatted(oldToken)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error_code").value("TOKEN_REUSE_DETECTED"));
        }

        @Test
        @DisplayName("should detect token reuse and revoke family")
        void shouldDetectTokenReuse() throws Exception {
            String oldToken = "old-refresh-token";
            String oldHash = "old-token-hash";

            StoredRefreshToken storedToken = new StoredRefreshToken(
                    UUID.fromString(TOKEN_ID), UUID.fromString(USER_ID), oldHash,
                    FAMILY_ID, true, "some-replacement",
                    LocalDateTime.now().plusDays(30), LocalDateTime.now(), LocalDateTime.now());

            when(tokenProvider.validateToken(oldToken)).thenReturn(true);
            when(tokenProvider.hashToken(oldToken)).thenReturn(oldHash);
            when(refreshTokenRepository.findByTokenHash(oldHash))
                    .thenReturn(Optional.of(storedToken));

            mockMvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"refresh_token\": \"%s\"}".formatted(oldToken)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error_code").value("TOKEN_REUSE_DETECTED"));

            verify(refreshTokenRepository).revokeAllByFamilyId(eq(FAMILY_ID), any());
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
        @DisplayName("should logout and revoke refresh token")
        void shouldLogoutAndRevokeToken() throws Exception {
            String tokenHash = "hash-to-revoke";
            when(tokenProvider.hashToken("valid-refresh-token")).thenReturn(tokenHash);

            mockMvc.perform(post("/api/v1/auth/logout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"refresh_token\": \"valid-refresh-token\"}"))
                    .andExpect(status().isOk());

            verify(refreshTokenRepository).markRevoked(eq(tokenHash), isNull(), any());
        }
    }
}
