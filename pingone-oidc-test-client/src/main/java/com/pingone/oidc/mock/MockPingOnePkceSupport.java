package com.pingone.oidc.mock;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import org.springframework.util.StringUtils;

final class MockPingOnePkceSupport {

    private MockPingOnePkceSupport() {}

    static void validateCodeVerifier(MockAuthorizationCode authorizationCode, String codeVerifier) {
        if (!StringUtils.hasText(authorizationCode.codeChallenge())) {
            return;
        }
        if (!StringUtils.hasText(codeVerifier)) {
            throw new MockPingOneTokenService.MockPingOneOAuthException(
                    "invalid_grant", "code_verifier is required for this authorization code");
        }
        String method = StringUtils.hasText(authorizationCode.codeChallengeMethod())
                ? authorizationCode.codeChallengeMethod()
                : "plain";
        if (!"S256".equals(method) && !"plain".equals(method)) {
            throw new MockPingOneTokenService.MockPingOneOAuthException(
                    "invalid_grant", "Unsupported PKCE method: " + method);
        }
        String expectedChallenge = "S256".equals(method) ? s256(codeVerifier) : codeVerifier;
        if (!authorizationCode.codeChallenge().equals(expectedChallenge)) {
            throw new MockPingOneTokenService.MockPingOneOAuthException(
                    "invalid_grant", "code_verifier does not match code_challenge");
        }
    }

    private static String s256(String codeVerifier) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }
}
