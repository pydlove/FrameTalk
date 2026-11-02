package com.pydlove.frametalk.identity.infrastructure.persistence;

import com.pydlove.frametalk.identity.domain.model.Email;
import com.pydlove.frametalk.identity.domain.model.VerificationCode;
import com.pydlove.frametalk.identity.domain.repository.VerificationCodeRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public class MyBatisVerificationCodeRepository implements VerificationCodeRepository {

    private final VerificationCodeMapper mapper;

    public MyBatisVerificationCodeRepository(VerificationCodeMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<VerificationCode> findActive(Email email, Instant now) {
        return Optional.ofNullable(mapper.findActive(email.value(), now))
                .map(this::toDomain);
    }

    @Override
    public Optional<Instant> findLatestCreatedAt(Email email) {
        return Optional.ofNullable(mapper.findLatestCreatedAt(email.value()));
    }

    @Override
    public int countCreatedSince(Email email, Instant since) {
        return mapper.countCreatedSince(email.value(), since);
    }

    @Override
    public VerificationCode save(VerificationCode code) {
        VerificationCodeData data = toData(code);
        if (data.getId() == null) {
            mapper.insert(data);
        } else {
            mapper.update(data);
        }
        return toDomain(data);
    }

    private VerificationCode toDomain(VerificationCodeData data) {
        return new VerificationCode(
                data.getId(),
                data.getEmail(),
                data.getCodeHash(),
                data.getExpiresAt(),
                data.getConsumedAt(),
                data.getFailedAttempts(),
                data.getCreatedAt());
    }

    private VerificationCodeData toData(VerificationCode code) {
        VerificationCodeData data = new VerificationCodeData();
        data.setId(code.id());
        data.setEmail(code.email());
        data.setCodeHash(code.codeHash());
        data.setExpiresAt(code.expiresAt());
        data.setConsumedAt(code.consumedAt());
        data.setFailedAttempts(code.failedAttempts());
        data.setCreatedAt(code.createdAt());
        return data;
    }
}
