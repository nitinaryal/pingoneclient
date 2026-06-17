package com.pingone.oidc.tool.oauth;

import com.pingone.oidc.tool.model.ClientToolConfigRequest;
import java.util.Arrays;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ClientToolRegistrationFactory {

    public ClientRegistration build(ClientToolConfigRequest request) {
        String registrationId = request.value("registrationId", "pingone");
        String clientId = require(request, "clientId");
        String clientSecret = request.value("clientSecret", "");
        String redirectUri = require(request, "redirectUri");
        String issuerUri = request.value("issuerUri", "");
        String authorizationUri = request.value("authorizationUri", "");
        String tokenUri = request.value("tokenUri", "");
        String userInfoUri = request.value("userInfoUri", "");
        String jwksUri = request.value("jwksUri", "");

        ClientRegistration.Builder builder = ClientRegistration.withRegistrationId(registrationId)
                .clientId(clientId)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(redirectUri)
                .scope(parseScopes(request.value("scopes", "openid,profile,email")))
                .userNameAttributeName(IdTokenClaimNames.SUB);

        if (StringUtils.hasText(clientSecret)) {
            builder.clientSecret(clientSecret);
        }

        if (hasExplicitProviderEndpoints(authorizationUri, tokenUri, userInfoUri, jwksUri)) {
            builder.authorizationUri(authorizationUri)
                    .tokenUri(tokenUri)
                    .userInfoUri(userInfoUri)
                    .jwkSetUri(jwksUri);
            if (StringUtils.hasText(issuerUri)) {
                builder.issuerUri(issuerUri);
            }
        } else if (StringUtils.hasText(issuerUri)) {
            String normalizedIssuer = issuerUri.endsWith("/")
                    ? issuerUri.substring(0, issuerUri.length() - 1)
                    : issuerUri;
            builder.issuerUri(normalizedIssuer)
                    .authorizationUri(normalizedIssuer + "/authorize")
                    .tokenUri(normalizedIssuer + "/token")
                    .userInfoUri(normalizedIssuer + "/userinfo")
                    .jwkSetUri(normalizedIssuer + "/jwks");
        } else {
            throw new IllegalArgumentException(
                    "Set Issuer URI or all OIDC provider endpoints (authorization, token, userinfo, JWKS)");
        }

        return builder.build();
    }

    private static boolean hasExplicitProviderEndpoints(
            String authorizationUri, String tokenUri, String userInfoUri, String jwksUri) {
        return StringUtils.hasText(authorizationUri)
                && StringUtils.hasText(tokenUri)
                && StringUtils.hasText(userInfoUri)
                && StringUtils.hasText(jwksUri);
    }

    private static String[] parseScopes(String scopes) {
        return Arrays.stream(scopes.split("[,\\s]+"))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toArray(String[]::new);
    }

    private static String require(ClientToolConfigRequest request, String key) {
        String value = request.value(key, "");
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("Missing required field: " + key);
        }
        return value;
    }
}
