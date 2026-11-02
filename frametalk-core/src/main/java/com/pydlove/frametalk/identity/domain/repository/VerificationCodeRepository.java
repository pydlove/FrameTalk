package com.pydlove.frametalk.identity.domain.repository;

import com.pydlove.frametalk.identity.domain.model.Email;
import com.pydlove.frametalk.identity.domain.model.VerificationCode;

import java.time.Instant;
import java.util.Optional;

public interface VerificationCodeRepository {

    Optional<VerificationCode> findActive(Email email, Instant now);

    Optional<Instant> findLatestCreatedAt(Email email);

    int countCreatedSince(Email email, Instant since);

    VerificationCode save(VerificationCode code);
}
