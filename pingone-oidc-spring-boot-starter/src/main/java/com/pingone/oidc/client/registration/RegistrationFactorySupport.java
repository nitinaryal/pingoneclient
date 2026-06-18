package com.pingone.oidc.client.registration;

import java.util.Arrays;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.util.StringUtils;

final class RegistrationFactorySupport {

    private RegistrationFactorySupport() {}

    static void applyProviderEndpoints(ClientRegistration.Builder builder, RegistrationValueSource request) {
        String issuerUri = request.value("issuerUri", "");
        String authorizationUri = request.value("authorizationUri", "");
        String tokenUri = request.value("tokenUri", "");
        String userInfoUri = request.value("userInfoUri", "");
        String jwksUri = request.value("jwksUri", "");

        if (hasExplicitProviderEndpoints(authorizationUri, tokenUri, userInfoUri, jwksUri)) {
            builder.authorizationUri(authorizationUri)
                    .tokenUri(tokenUri)
                    .userInfoUri(userInfoUri)
                    .jwkSetUri(jwksUri);
            if (StringUtils.hasText(issuerUri)) {
                builder.issuerUri(issuerUri);
            }
            return;
        }

        if (!StringUtils.hasText(issuerUri)) {
            throw new IllegalArgumentException(
                    "Set Issuer URI or all OIDC provider endpoints (authorization, token, userinfo, JWKS)");
        }

        String normalizedIssuer = issuerUri.endsWith("/") ? issuerUri.substring(0, issuerUri.length() - 1) : issuerUri;
        builder.issuerUri(normalizedIssuer)
                .authorizationUri(normalizedIssuer + "/authorize")
                .tokenUri(normalizedIssuer + "/token")
                .userInfoUri(normalizedIssuer + "/userinfo")
                .jwkSetUri(normalizedIssuer + "/jwks");
    }

    static String[] parseScopes(RegistrationValueSource request) {
        return Arrays.stream(request.value("scopes", "openid,profile,email").split("[,\\s]+"))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toArray(String[]::new);
    }

    static String require(RegistrationValueSource request, String key) {
        String value = request.value(key, "");
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("Missing required field: " + key);
        }
        return value;
    }

    static ClientRegistration buildPublicOidcRegistration(RegistrationValueSource request) {
        String registrationId = request.value("registrationId", "pingone");
        ClientRegistration.Builder builder = ClientRegistration.withRegistrationId(registrationId)
                .clientId(require(request, "clientId"))
                .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(require(request, "redirectUri"))
                .scope(parseScopes(request))
                .userNameAttributeName(IdTokenClaimNames.SUB);

        applyProviderEndpoints(builder, request);
        return builder.build();
    }

    private static boolean hasExplicitProviderEndpoints(
            String authorizationUri, String tokenUri, String userInfoUri, String jwksUri) {
        return StringUtils.hasText(authorizationUri)
                && StringUtils.hasText(tokenUri)
                && StringUtils.hasText(userInfoUri)
                && StringUtils.hasText(jwksUri);
    }
}
