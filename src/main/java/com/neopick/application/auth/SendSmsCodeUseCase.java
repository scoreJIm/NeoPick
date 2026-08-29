package com.neopick.application.auth;

import com.neopick.domain.auth.SmsCodeService;
import org.springframework.stereotype.Service;

@Service
public class SendSmsCodeUseCase {

    private final SmsCodeService smsCodeService;

    public SendSmsCodeUseCase(SmsCodeService smsCodeService) {
        this.smsCodeService = smsCodeService;
    }

    public void execute(SendSmsCodeCommand command) {
        smsCodeService.sendCode(command.phone());
    }

    public record SendSmsCodeCommand(String phone) {}
}
