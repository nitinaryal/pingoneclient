package com.pingone.oidc.mock;

import com.pingone.oidc.config.properties.PingOneClientProperties;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Service
@ConditionalOnProperty(name = "mock", havingValue = "true")
public class MockPingOneDiscoveryService {

    private final PingOneClientProperties properties;

    public MockPingOneDiscoveryService(PingOneClientProperties properties) {
        this.properties = properties;
    }

    public String issuerUri(HttpServletRequest request) {
        return ServletUriComponentsBuilder.fromContextPath(request)
                .path(properties.getMock().getBasePath())
                .build()
                .toUriString();
    }

    public Map<String, Object> discoveryDocument(HttpServletRequest request) {
        String issuer = issuerUri(request);
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("issuer", issuer);
        document.put("authorization_endpoint", issuer + "/authorize");
        document.put("token_endpoint", issuer + "/token");
        document.put("userinfo_endpoint", issuer + "/userinfo");
        document.put("jwks_uri", issuer + "/jwks");
        document.put("end_session_endpoint", issuer + "/signoff");
        document.put("response_types_supported", List.of("code"));
        document.put("subject_types_supported", List.of("public"));
        document.put("id_token_signing_alg_values_supported", List.of("RS256"));
        document.put("scopes_supported", List.of("openid", "profile", "email"));
        document.put("token_endpoint_auth_methods_supported", List.of("client_secret_basic", "client_secret_post"));
        document.put("claims_supported", List.of("sub", "email", "name", "iss", "aud", "exp", "iat", "nonce"));
        return document;
    }
}
