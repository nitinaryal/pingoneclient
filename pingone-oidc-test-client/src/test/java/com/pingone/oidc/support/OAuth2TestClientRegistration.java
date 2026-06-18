package com.pingone.oidc.support;

import java.time.Instant;
import java.util.Map;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

public final class OAuth2TestClientRegistration {

    public static final String REGISTRATION_ID = "pingone";
    public static final String ISSUER_URI = "https://auth.example.com/test-env/as";
    public static final String JWKS_URI = "https://auth.example.com/as/jwks";
    public static final String END_SESSION_URI = "https://auth.example.com/as/signoff";

    private OAuth2TestClientRegistration() {
    }

    public static ClientRegistration pingone() {
        return ClientRegistration.withRegistrationId(REGISTRATION_ID)
                .clientId("test-client-id")
                .clientSecret("test-client-secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("http://localhost:8080/login/oauth2/code/pingone")
                .scope("openid", "profile", "email")
                .authorizationUri("https://auth.example.com/as/authorize")
                .tokenUri("https://auth.example.com/as/token")
                .userInfoUri("https://auth.example.com/as/userinfo")
                .jwkSetUri(JWKS_URI)
                .issuerUri(ISSUER_URI)
                .userNameAttributeName("sub")
                .providerConfigurationMetadata(Map.of("end_session_endpoint", END_SESSION_URI))
                .build();
    }

    public static ClientRegistration pingoneWithoutIssuer() {
        return ClientRegistration.withRegistrationId(REGISTRATION_ID)
                .clientId("test-client-id")
                .clientSecret("test-client-secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("http://localhost:8080/login/oauth2/code/pingone")
                .scope("openid")
                .authorizationUri("https://auth.example.com/as/authorize")
                .tokenUri("https://auth.example.com/as/token")
                .userInfoUri("https://auth.example.com/as/userinfo")
                .jwkSetUri(JWKS_URI)
                .userNameAttributeName("sub")
                .build();
    }

    public static ClientRegistration pingoneWithoutJwks() {
        return ClientRegistration.withRegistrationId(REGISTRATION_ID)
                .clientId("test-client-id")
                .clientSecret("test-client-secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("http://localhost:8080/login/oauth2/code/pingone")
                .scope("openid")
                .authorizationUri("https://auth.example.com/as/authorize")
                .tokenUri("https://auth.example.com/as/token")
                .userInfoUri("https://auth.example.com/as/userinfo")
                .issuerUri(ISSUER_URI)
                .userNameAttributeName("sub")
                .build();
    }

    public static OidcUser oidcUser() {
        Instant expiresAt = Instant.parse("2030-01-01T00:00:00Z");
        OidcIdToken idToken = OidcIdToken.withTokenValue("test-id-token")
                .claim("sub", "user-123")
                .claim("email", "user@example.com")
                .claim("name", "Test User")
                .claim("iss", ISSUER_URI)
                .claim("aud", "test-client-id")
                .issuedAt(expiresAt.minusSeconds(3600))
                .expiresAt(expiresAt)
                .build();
        return new DefaultOidcUser(
                java.util.List.of(() -> "ROLE_USER"),
                idToken,
                "sub");
    }

    public static OAuth2AccessToken accessToken() {
        return new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "abcdefghijklmnopqrstuvwxyz0123456789",
                Instant.now(),
                Instant.now().plusSeconds(3600));
    }
}
