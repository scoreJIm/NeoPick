package com.neopick.application.user;

import com.neopick.domain.user.*;
import com.neopick.port.security.SecurityContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UpdateProfileUseCase {

    private final UserRepository userRepository;
    private final SecurityContext securityContext;

    public UpdateProfileUseCase(UserRepository userRepository, SecurityContext securityContext) {
        this.userRepository = userRepository;
        this.securityContext = securityContext;
    }

    @Transactional
    public User execute(UpdateProfileCommand command) {
        String userId = securityContext.requireCurrentUserId();
        User user = userRepository.findById(new UserId(UUID.fromString(userId)))
                .orElseThrow(() -> new IllegalStateException("User not found: " + userId));
        Gender gender = command.gender() != null ? Gender.valueOf(command.gender()) : null;
        user.updateProfile(command.nickname(), gender, command.avatarUrl());
        return userRepository.save(user);
    }

    public record UpdateProfileCommand(String nickname, String gender, String avatarUrl) {}
}
