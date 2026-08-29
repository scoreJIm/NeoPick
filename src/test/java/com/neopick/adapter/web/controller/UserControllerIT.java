package com.neopick.adapter.web.controller;

import com.neopick.application.user.GetCurrentUserUseCase;
import com.neopick.application.user.UpdateProfileUseCase;
import com.neopick.domain.user.*;
import com.neopick.port.security.SecurityContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import({GetCurrentUserUseCase.class, UpdateProfileUseCase.class})
@DisplayName("User API Integration Tests")
class UserControllerIT {

    @Autowired private MockMvc mockMvc;

    @MockBean private UserRepository userRepository;
    @MockBean private SecurityContext securityContext;

    private static final String USER_ID = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        when(securityContext.requireCurrentUserId()).thenReturn(USER_ID);
        when(securityContext.getCurrentUserId()).thenReturn(Optional.of(USER_ID));
    }

    @Nested
    @DisplayName("GET /api/v1/users/me — Get current user")
    class GetCurrentUser {

        @Test
        @DisplayName("should return current user profile")
        void shouldReturnCurrentUser() throws Exception {
            User user = new User(UserId.from(USER_ID), PhoneNumber.of("13800138000"),
                    "TestUser", UserRole.STUDENT);
            when(userRepository.findById(any(UserId.class)))
                    .thenReturn(Optional.of(user));

            mockMvc.perform(get("/api/v1/users/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.nickname").value("TestUser"))
                    .andExpect(jsonPath("$.data.role").value("STUDENT"))
                    .andExpect(jsonPath("$.data.phone").value("138****8000"));
        }

        @Test
        @DisplayName("should return 500 when user not found")
        void shouldReturnErrorWhenNotFound() throws Exception {
            when(userRepository.findById(any(UserId.class)))
                    .thenReturn(Optional.empty());

            mockMvc.perform(get("/api/v1/users/me"))
                    .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/users/me — Update profile")
    class UpdateProfile {

        @Test
        @DisplayName("should update user nickname and gender")
        void shouldUpdateProfile() throws Exception {
            User user = new User(UserId.from(USER_ID), PhoneNumber.of("13800138000"),
                    "NewName", UserRole.STUDENT);
            when(userRepository.findById(any(UserId.class)))
                    .thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenReturn(user);

            mockMvc.perform(put("/api/v1/users/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"nickname": "NewName", "gender": "MALE"}"""))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.nickname").value("NewName"));
        }
    }
}
