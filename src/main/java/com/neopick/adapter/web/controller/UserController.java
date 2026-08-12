package com.neopick.adapter.web.controller;

import com.neopick.adapter.web.dto.common.ApiResponse;
import com.neopick.adapter.web.dto.user.UpdateProfileRequest;
import com.neopick.adapter.web.dto.user.UserResponse;
import com.neopick.application.user.GetCurrentUserUseCase;
import com.neopick.application.user.UpdateProfileUseCase;
import com.neopick.application.user.UpdateProfileUseCase.UpdateProfileCommand;
import com.neopick.domain.user.User;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "Current user profile management")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final GetCurrentUserUseCase getCurrentUserUseCase;
    private final UpdateProfileUseCase updateProfileUseCase;

    public UserController(GetCurrentUserUseCase getCurrentUserUseCase,
                          UpdateProfileUseCase updateProfileUseCase) {
        this.getCurrentUserUseCase = getCurrentUserUseCase;
        this.updateProfileUseCase = updateProfileUseCase;
    }

    @GetMapping("/me")
    @Timed(value = "neopick.users.get_me", description = "Get current user profile")
    @Operation(summary = "Get current user profile", description = "Returns the authenticated user's profile information including nickname, avatar, gender, and membership status.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User profile retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content)
    })
    public ApiResponse<UserResponse> getCurrentUser() {
        User user = getCurrentUserUseCase.execute();
        return ApiResponse.success(toResponse(user));
    }

    @PutMapping("/me")
    @Timed(value = "neopick.users.update_profile", description = "Update user profile")
    @Operation(summary = "Update user profile", description = "Updates the authenticated user's nickname, gender, and avatar URL.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Profile updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content)
    })
    public ApiResponse<UserResponse> updateProfile(@RequestBody UpdateProfileRequest request) {
        User user = updateProfileUseCase.execute(
                new UpdateProfileCommand(request.nickname(), request.gender(), request.avatarUrl()));
        return ApiResponse.success(toResponse(user));
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId().value().toString(),
                user.getPhone().masked(),
                user.getNickname(),
                user.getAvatarUrl(),
                user.getGender() != null ? user.getGender().name() : null,
                user.getRole().name(),
                user.getStatus().name(),
                user.getRegisteredAt() != null ? user.getRegisteredAt().toString() : null,
                user.getLastLoginAt() != null ? user.getLastLoginAt().toString() : null
        );
    }
}
