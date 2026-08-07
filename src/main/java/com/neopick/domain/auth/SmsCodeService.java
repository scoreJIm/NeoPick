package com.neopick.domain.auth;

public interface SmsCodeService {

    void sendCode(String phone);

    boolean verifyCode(String phone, String code);
}
