package com.pydlove.frametalk.identity.interfaces.dto;

public record LoginRequest(
        String email,
        String password,
        boolean rememberMe) {
}
