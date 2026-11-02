package com.pydlove.frametalk.identity.interfaces.dto;

public record RegisterRequest(
        String email,
        String emailCode,
        String password,
        String confirmPassword) {
}
