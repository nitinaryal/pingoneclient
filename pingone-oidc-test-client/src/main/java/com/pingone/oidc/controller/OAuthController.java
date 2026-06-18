package com.pingone.oidc.controller;

import com.pingone.oidc.config.properties.PingOneClientProperties;
import com.pingone.oidc.service.PingOneMetadataService;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class OAuthController {

    private final PingOneClientProperties properties;
    private final PingOneMetadataService metadataService;
    private final OAuth2AuthorizedClientService authorizedClientService;

    public OAuthController(
            PingOneClientProperties properties,
            PingOneMetadataService metadataService,
            OAuth2AuthorizedClientService authorizedClientService) {
        this.properties = properties;
        this.metadataService = metadataService;
        this.authorizedClientService = authorizedClientService;
    }

    @GetMapping("/me")
    public String me(@AuthenticationPrincipal OidcUser oidcUser, Model model) {
        Map<String, Object> claims = new LinkedHashMap<>();
        for (String key : properties.getOidc().getIdTokenClaimKeys()) {
            Object value = oidcUser.getIdToken().getClaim(key);
            if ("exp".equals(key) && value instanceof Number number) {
                claims.put(key, number.longValue() + " (" + Instant.ofEpochSecond(number.longValue()) + ")");
            } else if ("exp".equals(key) && value instanceof Instant instant) {
                claims.put(key, instant.getEpochSecond() + " (" + instant + ")");
            } else {
                claims.put(key, value);
            }
        }
        model.addAttribute("claims", claims);
        model.addAttribute("principalName", oidcUser.getName());
        return "me";
    }

    @GetMapping("/token")
    public String token(@AuthenticationPrincipal OidcUser oidcUser, Model model) {
        OAuth2AuthorizedClient authorizedClient = authorizedClientService.loadAuthorizedClient(
                properties.getRegistrationId(), oidcUser.getName());
        if (authorizedClient == null) {
            throw new IllegalStateException(
                    "No authorized client found for registration '" + properties.getRegistrationId() + "'");
        }
        String accessToken = authorizedClient.getAccessToken().getTokenValue();
        model.addAttribute("maskedAccessToken", maskToken(accessToken));
        model.addAttribute("tokenType", authorizedClient.getAccessToken().getTokenType().getValue());
        model.addAttribute(
                "expiresAt",
                authorizedClient.getAccessToken().getExpiresAt() != null
                        ? authorizedClient.getAccessToken().getExpiresAt().toString()
                        : "n/a");
        model.addAttribute("scopes", authorizedClient.getAccessToken().getScopes());
        return "token";
    }

    @GetMapping("/jwks")
    public String jwks(Model model) {
        model.addAttribute("jwksUri", metadataService.resolveJwksUri());
        model.addAttribute("jwksJson", metadataService.fetchJwks());
        return "jwks";
    }

    @GetMapping("/metadata")
    public String metadata(Model model) {
        model.addAttribute("metadataJson", metadataService.fetchDiscoveryDocument());
        return "metadata";
    }

    private String maskToken(String token) {
        if (token == null || token.length() <= 16) {
            return "****";
        }
        return token.substring(0, 8) + "..." + token.substring(token.length() - 4);
    }
}
