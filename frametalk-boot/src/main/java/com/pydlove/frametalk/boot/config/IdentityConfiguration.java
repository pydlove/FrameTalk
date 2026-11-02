package com.pydlove.frametalk.boot.config;

import com.pydlove.frametalk.identity.application.command.AuthService;
import com.pydlove.frametalk.identity.application.command.EmailVerificationService;
import com.pydlove.frametalk.identity.domain.port.PasswordHasher;
import com.pydlove.frametalk.identity.domain.port.SessionTokenCodec;
import com.pydlove.frametalk.identity.domain.port.VerificationCodeCodec;
import com.pydlove.frametalk.identity.domain.port.VerificationEmailSender;
import com.pydlove.frametalk.identity.domain.repository.AuthSessionRepository;
import com.pydlove.frametalk.identity.domain.repository.UserRepository;
import com.pydlove.frametalk.identity.domain.repository.VerificationCodeRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Duration;

@Configuration
public class IdentityConfiguration {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public EmailVerificationService emailVerificationService(
            UserRepository userRepository,
            VerificationCodeRepository codeRepository,
            VerificationCodeCodec codec,
            VerificationEmailSender emailSender,
            Clock clock,
            @Value("${frametalk.auth.email-code.ttl:PT5M}") Duration codeTtl,
            @Value("${frametalk.auth.email-code.resend-cooldown:PT60S}")
            Duration resendCooldown,
            @Value("${frametalk.auth.email-code.max-per-day:10}") int maxCodesPerDay) {
        return new EmailVerificationService(
                userRepository,
                codeRepository,
                codec,
                emailSender,
                clock,
                codeTtl,
                resendCooldown,
                maxCodesPerDay);
    }

    @Bean
    public AuthService authService(
            UserRepository userRepository,
            AuthSessionRepository sessionRepository,
            PasswordHasher passwordHasher,
            SessionTokenCodec tokenCodec,
            EmailVerificationService emailVerificationService,
            Clock clock,
            @Value("${frametalk.auth.session-ttl:PT168H}") Duration sessionTtl,
            @Value("${frametalk.auth.remembered-session-ttl:PT4320H}")
            Duration rememberedSessionTtl) {
        return new AuthService(
                userRepository,
                sessionRepository,
                passwordHasher,
                tokenCodec,
                emailVerificationService,
                clock,
                sessionTtl,
                rememberedSessionTtl);
    }
}
