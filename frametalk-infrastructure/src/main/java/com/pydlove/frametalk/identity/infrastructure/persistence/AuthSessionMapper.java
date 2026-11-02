package com.pydlove.frametalk.identity.infrastructure.persistence;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.Instant;

@Mapper
public interface AuthSessionMapper {

    AuthSessionData findActive(
            @Param("tokenHash") String tokenHash,
            @Param("now") Instant now);

    int insert(AuthSessionData data);

    int deleteByTokenHash(@Param("tokenHash") String tokenHash);

    int deleteByUserIdExceptTokenHash(
            @Param("userId") long userId,
            @Param("tokenHash") String tokenHash);
}
