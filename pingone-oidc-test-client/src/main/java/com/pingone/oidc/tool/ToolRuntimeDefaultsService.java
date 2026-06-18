package com.pingone.oidc.tool;

import com.pingone.oidc.config.properties.PingOneClientProperties;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.core.env.Environment;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ToolRuntimeDefaultsService {

    private final PingOneClientProperties properties;
    private final ClientRegistrationRepository clientRegistrationRepository;
    private final Environment environment;
    private final ToolRuntimeModeSupport runtimeModeSupport;

    public ToolRuntimeDefaultsService(
            PingOneClientProperties properties,
            ClientRegistrationRepository clientRegistrationRepository,
            Environment environment,
            ToolRuntimeModeSupport runtimeModeSupport) {
        this.properties = properties;
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.environment = environment;
        this.runtimeModeSupport = runtimeModeSupport;
    }

    public Map<String, String> wizardDefaults() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("registrationId", properties.getRegistrationId());
        values.put("providerId", properties.getProviderId());
        values.put("postLogoutRedirectUri", properties.getSecurity().getPostLogoutRedirectUri());

        ClientRegistration registration =
                clientRegistrationRepository.findByRegistrationId(properties.getRegistrationId());
        if (registration != null) {
            putIfHasText(values, "clientId", registration.getClientId());
            putIfHasText(values, "clientSecret", registration.getClientSecret());
            putIfHasText(values, "redirectUri", registration.getRedirectUri());
            putIfHasText(values, "issuerUri", registration.getProviderDetails().getIssuerUri());
            putIfHasText(values, "authorizationUri", registration.getProviderDetails().getAuthorizationUri());
            putIfHasText(values, "tokenUri", registration.getProviderDetails().getTokenUri());
            putIfHasText(values, "jwksUri", registration.getProviderDetails().getJwkSetUri());
            deriveUserInfoFromIssuer(values);
            if (!registration.getScopes().isEmpty()) {
                values.put("scopes", String.join(",", registration.getScopes()));
            }
        }

        putIfBlank(values, "clientId", environment.getProperty("PINGONE_CLIENT_ID"));
        putIfBlank(values, "clientSecret", environment.getProperty("PINGONE_CLIENT_SECRET"));
        putIfBlank(values, "issuerUri", environment.getProperty("PINGONE_ISSUER_URI"));
        putIfBlank(values, "redirectUri", environment.getProperty("PINGONE_REDIRECT_URI"));
        putIfBlank(values, "userInfoUri", environment.getProperty("PINGONE_USERINFO_URI"));
        putIfBlank(
                values,
                "postLogoutRedirectUri",
                environment.getProperty("PINGONE_POST_LOGOUT_REDIRECT_URI"));

        if (runtimeModeSupport.isMockMode()) {
            PingOneClientProperties.Mock mock = properties.getMock();
            putIfHasText(values, "clientId", mock.getClientId());
            putIfHasText(values, "clientSecret", mock.getClientSecret());
            int port = environment.getProperty("server.port", Integer.class, 8080);
            String issuer = "http://localhost:" + port + mock.getBasePath();
            putIfBlank(values, "issuerUri", issuer);
            putIfBlank(values, "authorizationUri", issuer + "/authorize");
            putIfBlank(values, "tokenUri", issuer + "/token");
            putIfBlank(values, "userInfoUri", issuer + "/userinfo");
            putIfBlank(values, "jwksUri", issuer + "/jwks");
            putIfBlank(
                    values,
                    "redirectUri",
                    "http://localhost:" + port + "/login/oauth2/code/" + properties.getRegistrationId());
            putIfBlank(values, "postLogoutRedirectUri", "http://localhost:" + port + "/");
        }

        if (!values.containsKey("scopes") || !StringUtils.hasText(values.get("scopes"))) {
            values.put("scopes", "openid,profile,email");
        }

        deriveUserInfoFromIssuer(values);

        return values.entrySet().stream()
                .filter(entry -> StringUtils.hasText(entry.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> b, LinkedHashMap::new));
    }

    private static void putIfHasText(Map<String, String> values, String key, String value) {
        if (StringUtils.hasText(value)) {
            values.put(key, value.trim());
        }
    }

    private static void putIfBlank(Map<String, String> values, String key, String value) {
        if (!StringUtils.hasText(values.get(key)) && StringUtils.hasText(value)) {
            values.put(key, value.trim());
        }
    }

    private static void deriveUserInfoFromIssuer(Map<String, String> values) {
        putIfBlank(values, "userInfoUri", userInfoUriFromIssuer(values.get("issuerUri")));
    }

    private static String userInfoUriFromIssuer(String issuerUri) {
        if (!StringUtils.hasText(issuerUri)) {
            return null;
        }
        String normalized = issuerUri.trim();
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized + "/userinfo";
    }
}
