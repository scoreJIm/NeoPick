package com.neopick.adapter.web.controller;

import com.neopick.adapter.web.dto.common.ApiResponse;
import com.neopick.adapter.web.dto.user.UpdateProfileRequest;
import com.neopick.adapter.web.dto.user.UserResponse;
import com.neopick.application.user.GetCurrentUserUseCase;
import com.neopick.application.user.UpdateProfileUseCase;
import com.neopick.application.user.UpdateProfileUseCase.UpdateProfileCommand;
import com.neopick.domain.user.User;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final GetCurrentUserUseCase getCurrentUserUseCase;
    private final UpdateProfileUseCase updateProfileUseCase;

    public UserController(GetCurrentUserUseCase getCurrentUserUseCase,
                          UpdateProfileUseCase updateProfileUseCase) {
        this.getCurrentUserUseCase = getCurrentUserUseCase;
        this.updateProfileUseCase = updateProfileUseCase;
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> getCurrentUser() {
        User user = getCurrentUserUseCase.execute();
        return ApiResponse.success(toResponse(user));
    }

    @PutMapping("/me")
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
