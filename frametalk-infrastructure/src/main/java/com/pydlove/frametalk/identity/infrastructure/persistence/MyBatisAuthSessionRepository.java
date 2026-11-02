package com.pydlove.frametalk.identity.infrastructure.persistence;

import com.pydlove.frametalk.identity.domain.model.AuthSession;
import com.pydlove.frametalk.identity.domain.repository.AuthSessionRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public class MyBatisAuthSessionRepository implements AuthSessionRepository {

    private final AuthSessionMapper mapper;

    public MyBatisAuthSessionRepository(AuthSessionMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public AuthSession save(AuthSession session) {
        AuthSessionData data = toData(session);
        if (data.getId() == null) {
            mapper.insert(data);
        }
        return toDomain(data);
    }

    @Override
    public Optional<AuthSession> findActive(String tokenHash, Instant now) {
        return Optional.ofNullable(mapper.findActive(tokenHash, now))
                .map(this::toDomain);
    }

    @Override
    public void deleteByTokenHash(String tokenHash) {
        mapper.deleteByTokenHash(tokenHash);
    }

    @Override
    public void deleteByUserIdExceptTokenHash(long userId, String currentTokenHash) {
        mapper.deleteByUserIdExceptTokenHash(userId, currentTokenHash);
    }

    private AuthSession toDomain(AuthSessionData data) {
        return new AuthSession(
                data.getId(),
                data.getUserId(),
                data.getTokenHash(),
                data.isRemembered(),
                data.getExpiresAt(),
                data.getCreatedAt());
    }

    private AuthSessionData toData(AuthSession session) {
        AuthSessionData data = new AuthSessionData();
        data.setId(session.id());
        data.setUserId(session.userId());
        data.setTokenHash(session.tokenHash());
        data.setRemembered(session.remembered());
        data.setExpiresAt(session.expiresAt());
        data.setCreatedAt(session.createdAt());
        return data;
    }
}
