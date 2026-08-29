package com.neopick.adapter.web.controller;

import com.neopick.adapter.web.dto.auth.*;
import com.neopick.adapter.web.dto.common.ApiResponse;
import com.neopick.application.auth.LoginUseCase;
import com.neopick.application.auth.LoginUseCase.LoginCommand;
import com.neopick.application.auth.LoginUseCase.LoginResult;
import com.neopick.application.auth.RefreshTokenUseCase;
import com.neopick.application.auth.RefreshTokenUseCase.LogoutCommand;
import com.neopick.application.auth.RefreshTokenUseCase.RefreshTokenCommand;
import com.neopick.application.auth.SendSmsCodeUseCase;
import com.neopick.application.auth.SendSmsCodeUseCase.SendSmsCodeCommand;
import com.neopick.infrastructure.ratelimit.RateLimit;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "Authentication endpoints for SMS login, token refresh, and logout")
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
    @RateLimit(limit = 5, windowSeconds = 60, scope = "IP")
    @Timed(value = "neopick.auth.send_sms_code", description = "Send SMS verification code")
    @Operation(summary = "Send SMS verification code", description = "Sends a 6-digit verification code to the specified phone number. Rate limit: 1 per 60 seconds, 5 per day.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "SMS code sent successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid phone number format", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "Too many requests -- rate limit exceeded", content = @Content)
    })
    public ApiResponse<Void> sendSmsCode(@Valid @RequestBody SendSmsRequest request) {
        sendSmsCodeUseCase.execute(new SendSmsCodeCommand(request.phone()));
        return ApiResponse.success();
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    @RateLimit(limit = 10, windowSeconds = 60, scope = "IP")
    @Timed(value = "neopick.auth.login", description = "User login")
    @Operation(summary = "Login with phone and SMS code", description = "Authenticates a user using phone number and SMS verification code. Returns JWT access and refresh tokens.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Login successful -- returns tokens and user profile"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid phone number or verification code", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid or expired verification code", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "Too many login attempts", content = @Content)
    })
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
    @RateLimit(limit = 30, windowSeconds = 60, scope = "IP")
    @Timed(value = "neopick.auth.refresh", description = "Refresh access token")
    @Operation(summary = "Refresh access token", description = "Exchanges a valid refresh token for a new access token and refresh token pair. Old refresh token is invalidated (token rotation).")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Missing or invalid refresh token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Expired, revoked, or reused refresh token", content = @Content)
    })
    public ApiResponse<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        var tokens = refreshTokenUseCase.execute(new RefreshTokenCommand(request.refreshToken()));
        LoginResponse response = new LoginResponse(tokens.accessToken(), tokens.refreshToken(), null);
        return ApiResponse.success(response);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.OK)
    @RateLimit(limit = 10, windowSeconds = 60, scope = "IP")
    @Timed(value = "neopick.auth.logout", description = "User logout")
    @Operation(summary = "Logout", description = "Revokes the current refresh token and invalidates the session.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Logged out successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content)
    })
    public ApiResponse<Void> logout(@Valid @RequestBody LogoutRequest request) {
        refreshTokenUseCase.logout(new LogoutCommand(request.refreshToken()));
        return ApiResponse.success();
    }
}
