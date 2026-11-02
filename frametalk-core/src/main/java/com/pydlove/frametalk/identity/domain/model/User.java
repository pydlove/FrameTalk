package com.pydlove.frametalk.identity.domain.model;

import com.pydlove.frametalk.shared.domain.DomainException;

import java.time.Instant;
import java.util.Objects;

public record User(
        Long id,
        Email email,
        String displayName,
        String passwordHash,
        Instant createdAt,
        Instant updatedAt) {

    public User {
        email = Objects.requireNonNull(email, "邮箱");
        displayName = requireDisplayName(displayName);
        passwordHash = requirePasswordHash(passwordHash);
        createdAt = Objects.requireNonNull(createdAt, "创建时间");
        updatedAt = Objects.requireNonNull(updatedAt, "更新时间");
    }

    public static User register(
            Email email,
            String displayName,
            String passwordHash,
            Instant now) {
        return new User(null, email, displayName, passwordHash, now, now);
    }

    public User withPasswordHash(String newPasswordHash, Instant now) {
        return new User(
                id,
                email,
                displayName,
                newPasswordHash,
                createdAt,
                now);
    }

    public User withId(long newId) {
        return new User(
                newId,
                email,
                displayName,
                passwordHash,
                createdAt,
                updatedAt);
    }

    private static String requireDisplayName(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isBlank()) {
            throw new DomainException("显示名称不能为空");
        }
        if (normalized.length() > 64) {
            throw new DomainException("显示名称不能超过 64 个字符");
        }
        return normalized;
    }

    private static String requirePasswordHash(String value) {
        if (value == null || value.isBlank()) {
            throw new DomainException("密码摘要不能为空");
        }
        return value;
    }
}
