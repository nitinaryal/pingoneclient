package com.pingone.oidc.template.worker;

import com.pingone.oidc.config.properties.PingOneClientProperties;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Demo endpoint for Worker (client_credentials) application type testing.
 */
@Controller
public class WorkerTokenController {

    private static final String SERVICE_PRINCIPAL = "pingone-worker-service";

    private final PingOneClientProperties properties;
    private final OAuth2AuthorizedClientManager authorizedClientManager;

    public WorkerTokenController(
            PingOneClientProperties properties, OAuth2AuthorizedClientManager authorizedClientManager) {
        this.properties = properties;
        this.authorizedClientManager = authorizedClientManager;
    }

    @GetMapping("/worker/token")
    @ResponseBody
    public Map<String, Object> token() {
        OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest.withClientRegistrationId(
                        properties.getRegistrationId())
                .principal(SERVICE_PRINCIPAL)
                .build();
        OAuth2AuthorizedClient authorizedClient = authorizedClientManager.authorize(authorizeRequest);
        if (authorizedClient == null || authorizedClient.getAccessToken() == null) {
            throw new IllegalStateException(
                    "Failed to obtain worker access token. Verify client_credentials registration and PingOne scopes.");
        }

        String tokenValue = authorizedClient.getAccessToken().getTokenValue();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("registrationId", properties.getRegistrationId());
        response.put("tokenType", authorizedClient.getAccessToken().getTokenType().getValue());
        response.put("expiresAt", authorizedClient.getAccessToken().getExpiresAt());
        response.put("scopes", authorizedClient.getAccessToken().getScopes());
        response.put("accessToken", mask(tokenValue));
        response.put("message", "Worker client_credentials token acquired successfully.");
        return response;
    }

    private static String mask(String value) {
        if (value == null || value.length() < 8) {
            return "****";
        }
        return value.substring(0, 4) + "..." + value.substring(value.length() - 4);
    }
}
