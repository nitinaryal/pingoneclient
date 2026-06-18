package com.pingone.oidc.mock;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "mock", havingValue = "true")
public class MockPingOneSessionStore {

    private final Map<String, MockAuthorizationCode> authorizationCodes = new ConcurrentHashMap<>();
    private final Map<String, MockUserProfile> accessTokens = new ConcurrentHashMap<>();

    public MockAuthorizationCode storeAuthorizationCode(
            String clientId,
            String redirectUri,
            String state,
            String nonce,
            String sub,
            String email,
            String name,
            String codeChallenge,
            String codeChallengeMethod) {
        String code = UUID.randomUUID().toString();
        MockAuthorizationCode authorizationCode = new MockAuthorizationCode(
                code,
                clientId,
                redirectUri,
                state,
                nonce,
                sub,
                email,
                name,
                codeChallenge,
                codeChallengeMethod,
                Instant.now().plusSeconds(600));
        authorizationCodes.put(code, authorizationCode);
        return authorizationCode;
    }

    public Optional<MockAuthorizationCode> consumeAuthorizationCode(String code) {
        MockAuthorizationCode authorizationCode = authorizationCodes.remove(code);
        if (authorizationCode == null || authorizationCode.isExpired()) {
            return Optional.empty();
        }
        return Optional.of(authorizationCode);
    }

    public String storeAccessToken(MockUserProfile profile) {
        String token = UUID.randomUUID().toString();
        accessTokens.put(token, profile);
        return token;
    }

    public Optional<MockUserProfile> findProfileByAccessToken(String accessToken) {
        return Optional.ofNullable(accessTokens.get(accessToken));
    }

    public record MockUserProfile(String sub, String email, String name) {}
}
