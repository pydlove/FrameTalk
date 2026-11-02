package com.pydlove.frametalk.identity.application.command;

public record ChangePasswordCommand(
        String currentPassword,
        String newPassword,
        String confirmPassword) {
}
