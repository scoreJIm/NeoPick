package com.neopick.application.auth;

import com.neopick.domain.auth.AuthService;
import com.neopick.domain.auth.SmsCodeService;
import com.neopick.domain.auth.TokenPair;
import com.neopick.domain.user.User;
import com.neopick.domain.user.UserRepository;
import com.neopick.infrastructure.metrics.BusinessMetrics;
import com.neopick.port.security.TokenProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginUseCase {

    private final SmsCodeService smsCodeService;
    private final UserRepository userRepository;
    private final TokenProvider tokenProvider;
    private final BusinessMetrics metrics;

    public LoginUseCase(SmsCodeService smsCodeService, UserRepository userRepository,
                        TokenProvider tokenProvider, BusinessMetrics metrics) {
        this.smsCodeService = smsCodeService;
        this.userRepository = userRepository;
        this.tokenProvider = tokenProvider;
        this.metrics = metrics;
    }

    @Transactional
    public LoginResult execute(LoginCommand command) {
        if (!smsCodeService.verifyCode(command.phone(), command.code())) {
            throw new IllegalArgumentException("Invalid verification code");
        }
        User user = userRepository.findByPhone(
                        com.neopick.domain.user.PhoneNumber.of(command.phone()))
                .orElseGet(() -> registerNewUser(command.phone()));
        user.recordLogin();
        userRepository.save(user);
        String accessToken = tokenProvider.generateAccessToken(
                user.getId().value().toString(), user.getRole().name());
        String refreshToken = tokenProvider.generateRefreshToken(
                user.getId().value().toString());
        TokenPair tokens = new TokenPair(accessToken, refreshToken);
        return new LoginResult(user, tokens);
    }

    private User registerNewUser(String phone) {
        User user = new User(
                com.neopick.domain.user.UserId.generate(),
                com.neopick.domain.user.PhoneNumber.of(phone),
                "User_" + phone.substring(phone.length() - 4),
                com.neopick.domain.user.UserRole.STUDENT);
        User saved = userRepository.save(user);
        metrics.userRegistered();
        return saved;
    }

    public record LoginCommand(String phone, String code) {}

    public record LoginResult(User user, TokenPair tokens) {}
}
