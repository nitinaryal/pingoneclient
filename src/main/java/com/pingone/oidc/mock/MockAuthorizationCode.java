package com.pingone.oidc.mock;

import java.time.Instant;

public record MockAuthorizationCode(
        String code,
        String clientId,
        String redirectUri,
        String state,
        String nonce,
        String sub,
        String email,
        String name,
        String codeChallenge,
        String codeChallengeMethod,
        Instant expiresAt) {

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
