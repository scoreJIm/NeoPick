package com.neopick.port.sms;

public interface SmsSender {

    void sendVerificationCode(String phone, String code);

    boolean verifyCode(String phone, String code);
}
