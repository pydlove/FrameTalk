package com.pydlove.frametalk.identity.infrastructure.mail;

import com.pydlove.frametalk.identity.domain.model.Email;
import com.pydlove.frametalk.identity.domain.port.VerificationEmailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConditionalOnProperty(
        name = "frametalk.mail.mode",
        havingValue = "log",
        matchIfMissing = true)
public class LoggingVerificationEmailSender implements VerificationEmailSender {

    private static final Logger log =
            LoggerFactory.getLogger(LoggingVerificationEmailSender.class);

    @Override
    public void sendRegistrationCode(Email email, String rawCode, Duration ttl) {
        log.info(
                "FrameTalk development email code: email={}, code={}, ttlMinutes={}",
                email.value(),
                rawCode,
                Math.max(1, ttl.toMinutes()));
    }
}
