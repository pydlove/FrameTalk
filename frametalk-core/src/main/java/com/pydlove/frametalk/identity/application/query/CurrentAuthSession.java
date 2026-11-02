package com.pydlove.frametalk.identity.application.query;

import java.time.Instant;

public record CurrentAuthSession(
        AuthenticatedUser user,
        Instant expiresAt,
        boolean remembered) {
}
