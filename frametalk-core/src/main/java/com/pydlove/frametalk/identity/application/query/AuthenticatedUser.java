package com.pydlove.frametalk.identity.application.query;

import com.pydlove.frametalk.identity.domain.model.User;

public record AuthenticatedUser(
        long id,
        String email,
        String displayName) {

    public static AuthenticatedUser from(User user) {
        if (user.id() == null) {
            throw new IllegalArgumentException("未持久化用户不能用于认证");
        }
        return new AuthenticatedUser(
                user.id(),
                user.email().value(),
                user.displayName());
    }
}
