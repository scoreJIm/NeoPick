package com.neopick.domain.auth;

import com.neopick.domain.user.User;

public interface AuthService {

    User authenticate(String phone, String code);
}
