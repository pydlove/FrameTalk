package com.pydlove.frametalk.identity.domain.model;

import com.pydlove.frametalk.shared.domain.DomainException;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public record AuthSession(
        Long id,
        long userId,
        String tokenHash,
        boolean remembered,
        Instant expiresAt,
        Instant createdAt) {

    public AuthSession {
        if (userId <= 0) {
            throw new DomainException("用户 ID 无效");
        }
        if (tokenHash == null || tokenHash.isBlank()) {
            throw new DomainException("会话令牌不能为空");
        }
        expiresAt = Objects.requireNonNull(expiresAt, "会话过期时间");
        createdAt = Objects.requireNonNull(createdAt, "会话创建时间");
    }

    public static AuthSession open(
            long userId,
            String tokenHash,
            Instant now,
            Duration ttl,
            boolean remembered) {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new DomainException("会话有效期必须大于零");
        }
        return new AuthSession(
                null,
                userId,
                tokenHash,
                remembered,
                now.plus(ttl),
                now);
    }

    public AuthSession withId(long newId) {
        return new AuthSession(
                newId,
                userId,
                tokenHash,
                remembered,
                expiresAt,
                createdAt);
    }

    public boolean isExpired(Instant now) {
        return !expiresAt.isAfter(now);
    }
}
