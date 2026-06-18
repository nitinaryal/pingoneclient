package com.pingone.oidc.tool;

import com.pingone.oidc.config.properties.PingOneClientProperties;
import com.pingone.oidc.service.PingOneMetadataService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Service;

@Service
public class ClientToolDiagnosticsService {

    private final PingOneClientProperties properties;
    private final ClientRegistrationRepository clientRegistrationRepository;
    private final PingOneMetadataService metadataService;
    private final Environment environment;

    public ClientToolDiagnosticsService(
            PingOneClientProperties properties,
            ClientRegistrationRepository clientRegistrationRepository,
            PingOneMetadataService metadataService,
            Environment environment) {
        this.properties = properties;
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.metadataService = metadataService;
        this.environment = environment;
    }

    public Map<String, Object> runtimeDiagnostics() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("mockMode", isMockMode());
        result.put("applicationType", properties.getApplicationType().getConfigValue());
        result.put("registrationId", properties.getRegistrationId());
        result.put("providerId", properties.getProviderId());
        result.put("loginPath", properties.getLoginPath());
        result.put("postLogoutRedirectUri", properties.getSecurity().getPostLogoutRedirectUri());
        result.put("postLoginPath", properties.getUi().getPostLoginPath());

        ClientRegistration registration =
                clientRegistrationRepository.findByRegistrationId(properties.getRegistrationId());
        if (registration != null) {
            result.put("clientId", mask(registration.getClientId()));
            result.put("redirectUri", registration.getRedirectUri());
            result.put("grantType", registration.getAuthorizationGrantType().getValue());
            result.put("issuerUri", registration.getProviderDetails().getIssuerUri());
            result.put("authorizationUri", registration.getProviderDetails().getAuthorizationUri());
            result.put("tokenUri", registration.getProviderDetails().getTokenUri());
            result.put("jwksUri", registration.getProviderDetails().getJwkSetUri());
        } else {
            result.put("registrationError", "Registration '" + properties.getRegistrationId() + "' not found");
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        result.put("authenticated", authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal()));
        if (authentication != null) {
            result.put("principal", authentication.getName());
        }

        Map<String, Object> checks = new LinkedHashMap<>();
        checks.put("metadata", runCheck(this::fetchMetadata));
        checks.put("jwks", runCheck(this::fetchJwks));
        result.put("connectivityChecks", checks);

        return result;
    }

    private String fetchMetadata() {
        metadataService.fetchDiscoveryDocument();
        return "OK";
    }

    private String fetchJwks() {
        metadataService.fetchJwks();
        return "OK";
    }

    private Map<String, String> runCheck(CheckRunnable runnable) {
        Map<String, String> check = new LinkedHashMap<>();
        try {
            runnable.run();
            check.put("status", "PASS");
            check.put("message", "Endpoint reachable");
        } catch (Exception ex) {
            check.put("status", "FAIL");
            check.put("message", ex.getMessage());
        }
        return check;
    }

    private static String mask(String value) {
        if (value == null || value.length() < 8) {
            return "****";
        }
        return value.substring(0, 4) + "..." + value.substring(value.length() - 4);
    }

    private boolean isMockMode() {
        return environment.getProperty("mock", Boolean.class, false);
    }

    @FunctionalInterface
    private interface CheckRunnable {
        void run() throws Exception;
    }
}
