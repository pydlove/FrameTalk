package com.pydlove.frametalk.identity.domain.repository;

import com.pydlove.frametalk.identity.domain.model.AuthSession;

import java.time.Instant;
import java.util.Optional;

public interface AuthSessionRepository {

    AuthSession save(AuthSession session);

    Optional<AuthSession> findActive(String tokenHash, Instant now);

    void deleteByTokenHash(String tokenHash);

    void deleteByUserIdExceptTokenHash(long userId, String currentTokenHash);
}
