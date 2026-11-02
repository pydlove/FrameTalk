package com.pydlove.frametalk.identity.application.command;

public record RegisterCommand(
        String email,
        String emailCode,
        String password,
        String confirmPassword) {
}
