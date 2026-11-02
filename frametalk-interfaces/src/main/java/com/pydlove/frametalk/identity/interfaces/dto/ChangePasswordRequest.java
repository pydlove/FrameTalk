package com.pydlove.frametalk.identity.interfaces.dto;

public record ChangePasswordRequest(
        String currentPassword,
        String newPassword,
        String confirmPassword) {
}
