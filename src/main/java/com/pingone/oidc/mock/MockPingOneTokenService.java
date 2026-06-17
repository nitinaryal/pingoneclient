package com.pingone.oidc.mock;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.pingone.oidc.config.properties.PingOneClientProperties;
import com.pingone.oidc.mock.MockPingOneSessionStore.MockUserProfile;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@ConditionalOnProperty(name = "mock", havingValue = "true")
public class MockPingOneTokenService {

    private static final long TOKEN_TTL_SECONDS = 3600;

    private final PingOneClientProperties properties;
    private final MockPingOneKeyPairHolder keyPairHolder;
    private final MockPingOneSessionStore sessionStore;

    public MockPingOneTokenService(
            PingOneClientProperties properties,
            MockPingOneKeyPairHolder keyPairHolder,
            MockPingOneSessionStore sessionStore) {
        this.properties = properties;
        this.keyPairHolder = keyPairHolder;
        this.sessionStore = sessionStore;
    }

    public Map<String, Object> exchangeAuthorizationCode(
            HttpServletRequest request, String code, String redirectUri, String codeVerifier, String issuerUri)
            throws Exception {
        if (!validateClientCredentials(request)) {
            throw new MockPingOneOAuthException("invalid_client", "Client authentication failed");
        }
        MockAuthorizationCode authorizationCode = sessionStore
                .consumeAuthorizationCode(code)
                .orElseThrow(() -> new MockPingOneOAuthException("invalid_grant", "Authorization code is invalid"));

        if (!authorizationCode.clientId().equals(properties.getMock().getClientId())) {
            throw new MockPingOneOAuthException("invalid_grant", "Authorization code client mismatch");
        }
        if (!authorizationCode.redirectUri().equals(redirectUri)) {
            throw new MockPingOneOAuthException("invalid_grant", "Redirect URI mismatch");
        }
        MockPingOnePkceSupport.validateCodeVerifier(authorizationCode, codeVerifier);

        MockUserProfile profile = new MockUserProfile(
                authorizationCode.sub(), authorizationCode.email(), authorizationCode.name());
        String accessToken = sessionStore.storeAccessToken(profile);
        String idToken = createIdToken(issuerUri, profile, authorizationCode.nonce());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("access_token", accessToken);
        response.put("token_type", "Bearer");
        response.put("expires_in", TOKEN_TTL_SECONDS);
        response.put("scope", "openid profile email");
        response.put("id_token", idToken);
        return response;
    }

    public String createIdToken(String issuerUri, MockUserProfile profile, String nonce) throws Exception {
        Instant now = Instant.now();
        JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder()
                .issuer(issuerUri)
                .subject(profile.sub())
                .audience(properties.getMock().getClientId())
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(TOKEN_TTL_SECONDS)))
                .claim("email", profile.email())
                .claim("name", profile.name());
        if (StringUtils.hasText(nonce)) {
            claimsBuilder.claim("nonce", nonce);
        }

        SignedJWT signedJwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(keyPairHolder.getKeyId()).build(),
                claimsBuilder.build());
        signedJwt.sign(new RSASSASigner(keyPairHolder.getPrivateKey()));
        return signedJwt.serialize();
    }

    public boolean validateClientCredentials(HttpServletRequest request) {
        PingOneClientProperties.Mock mock = properties.getMock();
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(authorizationHeader) && authorizationHeader.startsWith("Basic ")) {
            String decoded = new String(
                    Base64.getDecoder().decode(authorizationHeader.substring(6)), StandardCharsets.UTF_8);
            int separator = decoded.indexOf(':');
            if (separator < 0) {
                return false;
            }
            String clientId = decoded.substring(0, separator);
            String clientSecret = decoded.substring(separator + 1);
            return mock.getClientId().equals(clientId) && mock.getClientSecret().equals(clientSecret);
        }

        String clientId = request.getParameter("client_id");
        String clientSecret = request.getParameter("client_secret");
        return mock.getClientId().equals(clientId) && mock.getClientSecret().equals(clientSecret);
    }

    public static class MockPingOneOAuthException extends RuntimeException {

        private final String error;
        private final String description;

        public MockPingOneOAuthException(String error, String description) {
            super(description);
            this.error = error;
            this.description = description;
        }

        public String getError() {
            return error;
        }

        public String getDescription() {
            return description;
        }
    }
}
