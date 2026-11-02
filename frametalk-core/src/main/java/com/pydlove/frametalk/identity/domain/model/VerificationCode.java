package com.pydlove.frametalk.identity.domain.model;

import com.pydlove.frametalk.shared.domain.DomainException;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public record VerificationCode(
        Long id,
        String email,
        String codeHash,
        Instant expiresAt,
        Instant consumedAt,
        int failedAttempts,
        Instant createdAt) {

    private static final int MAX_FAILED_ATTEMPTS = 5;

    public VerificationCode {
        if (email == null || email.isBlank()) {
            throw new DomainException("验证码邮箱不能为空");
        }
        if (codeHash == null || codeHash.isBlank()) {
            throw new DomainException("验证码摘要不能为空");
        }
        expiresAt = Objects.requireNonNull(expiresAt, "验证码过期时间");
        createdAt = Objects.requireNonNull(createdAt, "验证码创建时间");
        if (failedAttempts < 0) {
            throw new DomainException("验证码失败次数无效");
        }
    }

    public static VerificationCode issue(
            String email,
            String codeHash,
            Instant now,
            Duration ttl) {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new DomainException("验证码有效期必须大于零");
        }
        return new VerificationCode(
                null,
                email,
                codeHash,
                now.plus(ttl),
                null,
                0,
                now);
    }

    public VerificationCode withId(long newId) {
        return new VerificationCode(
                newId,
                email,
                codeHash,
                expiresAt,
                consumedAt,
                failedAttempts,
                createdAt);
    }

    public VerificationCode consume(Instant now) {
        return new VerificationCode(
                id,
                email,
                codeHash,
                expiresAt,
                consumedAt == null ? now : consumedAt,
                failedAttempts,
                createdAt);
    }

    public VerificationCode registerFailure(Instant now) {
        int nextAttempts = failedAttempts + 1;
        return new VerificationCode(
                id,
                email,
                codeHash,
                expiresAt,
                nextAttempts >= MAX_FAILED_ATTEMPTS ? now : consumedAt,
                nextAttempts,
                createdAt);
    }

    public boolean isActive(Instant now) {
        return consumedAt == null
                && failedAttempts < MAX_FAILED_ATTEMPTS
                && expiresAt.isAfter(now);
    }
}
