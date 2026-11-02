package com.pydlove.frametalk.identity.application.command;

import com.pydlove.frametalk.identity.domain.model.Email;
import com.pydlove.frametalk.identity.domain.model.VerificationCode;
import com.pydlove.frametalk.identity.domain.port.VerificationCodeCodec;
import com.pydlove.frametalk.identity.domain.port.VerificationEmailSender;
import com.pydlove.frametalk.identity.domain.repository.UserRepository;
import com.pydlove.frametalk.identity.domain.repository.VerificationCodeRepository;
import com.pydlove.frametalk.shared.domain.DomainException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public class EmailVerificationService {

    private final UserRepository userRepository;
    private final VerificationCodeRepository codeRepository;
    private final VerificationCodeCodec codec;
    private final VerificationEmailSender emailSender;
    private final Clock clock;
    private final Duration codeTtl;
    private final Duration resendCooldown;
    private final int maxCodesPerDay;

    public EmailVerificationService(
            UserRepository userRepository,
            VerificationCodeRepository codeRepository,
            VerificationCodeCodec codec,
            VerificationEmailSender emailSender,
            Clock clock,
            Duration codeTtl,
            Duration resendCooldown,
            int maxCodesPerDay) {
        this.userRepository = Objects.requireNonNull(userRepository);
        this.codeRepository = Objects.requireNonNull(codeRepository);
        this.codec = Objects.requireNonNull(codec);
        this.emailSender = Objects.requireNonNull(emailSender);
        this.clock = Objects.requireNonNull(clock);
        this.codeTtl = requirePositive(codeTtl, "验证码有效期");
        this.resendCooldown = requirePositive(resendCooldown, "验证码重发间隔");
        if (maxCodesPerDay < 1) {
            throw new DomainException("每日验证码发送上限必须大于零");
        }
        this.maxCodesPerDay = maxCodesPerDay;
    }

    public void sendRegistrationCode(String rawEmail) {
        Email email = Email.of(rawEmail);
        if (userRepository.existsByEmail(email)) {
            throw new DomainException("该邮箱已经注册，请直接登录");
        }

        Instant now = clock.instant();
        codeRepository.findLatestCreatedAt(email).ifPresent(latest -> {
            if (latest.plus(resendCooldown).isAfter(now)) {
                throw new DomainException("验证码发送过于频繁，请稍后再试");
            }
        });
        if (codeRepository.countCreatedSince(email, now.minus(Duration.ofDays(1)))
                >= maxCodesPerDay) {
            throw new DomainException("该邮箱今日验证码发送次数已达上限");
        }

        String rawCode = codec.generateCode();
        emailSender.sendRegistrationCode(email, rawCode, codeTtl);

        codeRepository.findActive(email, now)
                .ifPresent(active -> codeRepository.save(active.consume(now)));
        codeRepository.save(VerificationCode.issue(
                email.value(),
                codec.hash(rawCode),
                now,
                codeTtl));
    }

    public void verifyRegistrationCode(Email email, String rawCode) {
        if (rawCode == null || !rawCode.trim().matches("\\d{6}")) {
            throw new DomainException("请输入 6 位邮箱验证码");
        }

        Instant now = clock.instant();
        VerificationCode code = codeRepository.findActive(email, now)
                .orElseThrow(() -> new DomainException("验证码已失效，请重新获取"));
        String candidateHash = codec.hash(rawCode.trim());
        boolean verified = MessageDigest.isEqual(
                code.codeHash().getBytes(StandardCharsets.US_ASCII),
                candidateHash.getBytes(StandardCharsets.US_ASCII));
        if (verified) {
            codeRepository.save(code.consume(now));
            return;
        }

        VerificationCode failed = codeRepository.save(code.registerFailure(now));
        if (!failed.isActive(now)) {
            throw new DomainException("验证码错误次数过多，请重新获取");
        }
        throw new DomainException("邮箱验证码错误");
    }

    private static Duration requirePositive(Duration value, String label) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new DomainException(label + "必须大于零");
        }
        return value;
    }
}
