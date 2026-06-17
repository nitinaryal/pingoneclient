package com.pingone.oidc.controller;

import com.pingone.oidc.service.PingOneMetadataService;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class OAuthController {

    private static final String[] CLAIM_KEYS = {"sub", "email", "name", "iss", "aud", "exp"};

    private final PingOneMetadataService metadataService;

    public OAuthController(PingOneMetadataService metadataService) {
        this.metadataService = metadataService;
    }

    @GetMapping("/me")
    public String me(@AuthenticationPrincipal OidcUser oidcUser, Model model) {
        Map<String, Object> claims = new LinkedHashMap<>();
        for (String key : CLAIM_KEYS) {
            Object value = oidcUser.getIdToken().getClaim(key);
            if ("exp".equals(key) && value instanceof Number number) {
                claims.put(key, number.longValue() + " (" + Instant.ofEpochSecond(number.longValue()) + ")");
            } else {
                claims.put(key, value);
            }
        }
        model.addAttribute("claims", claims);
        model.addAttribute("principalName", oidcUser.getName());
        return "me";
    }

    @GetMapping("/token")
    public String token(
            @RegisteredOAuth2AuthorizedClient("pingone") OAuth2AuthorizedClient authorizedClient,
            Model model) {
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
