package com.neopick.application.user;

import com.neopick.domain.user.User;
import com.neopick.domain.user.UserId;
import com.neopick.domain.user.UserRepository;
import com.neopick.port.security.SecurityContext;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class GetCurrentUserUseCase {

    private final UserRepository userRepository;
    private final SecurityContext securityContext;

    public GetCurrentUserUseCase(UserRepository userRepository, SecurityContext securityContext) {
        this.userRepository = userRepository;
        this.securityContext = securityContext;
    }

    public User execute() {
        String userId = securityContext.requireCurrentUserId();
        return userRepository.findById(new UserId(UUID.fromString(userId)))
                .orElseThrow(() -> new IllegalStateException("User not found: " + userId));
    }
}
