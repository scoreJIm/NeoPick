package com.neopick.domain.user;

import java.util.Optional;

public interface UserRepository {

    User save(User user);

    Optional<User> findById(UserId id);

    Optional<User> findByPhone(PhoneNumber phone);

    boolean existsByPhone(PhoneNumber phone);
}
