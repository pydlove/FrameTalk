package com.pydlove.frametalk.identity.infrastructure.persistence;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.Instant;

@Mapper
public interface VerificationCodeMapper {

    VerificationCodeData findActive(
            @Param("email") String email,
            @Param("now") Instant now);

    Instant findLatestCreatedAt(@Param("email") String email);

    int countCreatedSince(
            @Param("email") String email,
            @Param("since") Instant since);

    int insert(VerificationCodeData data);

    int update(VerificationCodeData data);
}
