package com.pydlove.frametalk.identity.domain.port;

import com.pydlove.frametalk.identity.domain.model.Email;

import java.time.Duration;

public interface VerificationEmailSender {

    void sendRegistrationCode(Email email, String rawCode, Duration ttl);
}
