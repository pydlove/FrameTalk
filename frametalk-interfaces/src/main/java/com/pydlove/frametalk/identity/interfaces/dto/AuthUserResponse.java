package com.pydlove.frametalk.identity.interfaces.dto;

import com.pydlove.frametalk.identity.application.query.AuthenticatedUser;

public record AuthUserResponse(
        long id,
        String email,
        String displayName) {

    public static AuthUserResponse from(AuthenticatedUser user) {
        return new AuthUserResponse(
                user.id(),
                user.email(),
                user.displayName());
    }
}
