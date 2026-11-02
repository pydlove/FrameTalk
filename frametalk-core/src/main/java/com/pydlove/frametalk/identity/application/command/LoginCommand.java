package com.pydlove.frametalk.identity.application.command;

public record LoginCommand(
        String email,
        String password,
        boolean rememberMe) {
}
