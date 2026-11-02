package com.pydlove.frametalk.identity.domain.port;

public interface VerificationCodeCodec {

    String generateCode();

    String hash(String rawCode);
}
