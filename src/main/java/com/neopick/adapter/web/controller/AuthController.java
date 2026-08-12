package com.neopick.adapter.web.controller;

import com.neopick.adapter.web.dto.auth.*;
import com.neopick.adapter.web.dto.common.ApiResponse;
import com.neopick.application.auth.LoginUseCase;
import com.neopick.application.auth.LoginUseCase.LoginCommand;
import com.neopick.application.auth.LoginUseCase.LoginResult;
import com.neopick.application.auth.RefreshTokenUseCase;
import com.neopick.application.auth.RefreshTokenUseCase.RefreshTokenCommand;
import com.neopick.application.auth.SendSmsCodeUseCase;
import com.neopick.application.auth.SendSmsCodeUseCase.SendSmsCodeCommand;
import io.micrometer.core.annotation.Timed;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final SendSmsCodeUseCase sendSmsCodeUseCase;
    private final LoginUseCase loginUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;

    public AuthController(SendSmsCodeUseCase sendSmsCodeUseCase,
                          LoginUseCase loginUseCase,
                          RefreshTokenUseCase refreshTokenUseCase) {
        this.sendSmsCodeUseCase = sendSmsCodeUseCase;
        this.loginUseCase = loginUseCase;
        this.refreshTokenUseCase = refreshTokenUseCase;
    }

    @PostMapping("/send-sms-code")
    @ResponseStatus(HttpStatus.OK)
    @Timed(value = "neopick.auth.send_sms_code", description = "Send SMS verification code")
    public ApiResponse<Void> sendSmsCode(@Valid @RequestBody SendSmsRequest request) {
        sendSmsCodeUseCase.execute(new SendSmsCodeCommand(request.phone()));
        return ApiResponse.success();
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    @Timed(value = "neopick.auth.login", description = "User login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResult result = loginUseCase.execute(new LoginCommand(request.phone(), request.code()));
        LoginResponse.UserProfile profile = new LoginResponse.UserProfile(
                result.user().getId().value().toString(),
                result.user().getPhone().masked(),
                result.user().getNickname(),
                result.user().getAvatarUrl(),
                result.user().getRole().name()
        );
        LoginResponse response = new LoginResponse(
                result.tokens().accessToken(),
                result.tokens().refreshToken(),
                profile
        );
        return ApiResponse.success(response);
    }

    @PostMapping("/refresh")
    @ResponseStatus(HttpStatus.OK)
    @Timed(value = "neopick.auth.refresh", description = "Refresh access token")
    public ApiResponse<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        var tokens = refreshTokenUseCase.execute(new RefreshTokenCommand(request.refreshToken()));
        LoginResponse response = new LoginResponse(
                tokens.accessToken(), tokens.refreshToken(), null);
        return ApiResponse.success(response);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.OK)
    @Timed(value = "neopick.auth.logout", description = "User logout")
    public ApiResponse<Void> logout() {
        return ApiResponse.success();
    }
}
