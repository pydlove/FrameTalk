package com.pydlove.frametalk.identity.domain.port;

public interface SessionTokenCodec {

    String generateRawToken();

    String hash(String rawToken);
}
