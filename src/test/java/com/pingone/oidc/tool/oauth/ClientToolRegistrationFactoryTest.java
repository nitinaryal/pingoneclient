package com.pingone.oidc.tool.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pingone.oidc.tool.model.ClientToolConfigRequest;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ClientToolRegistrationFactoryTest {

    private final ClientToolRegistrationFactory factory = new ClientToolRegistrationFactory();

    @Test
    void buildsRegistrationFromWizardValues() {
        ClientToolConfigRequest request = new ClientToolConfigRequest();
        request.setValues(Map.of(
                "registrationId", "acme",
                "clientId", "acme-client",
                "clientSecret", "acme-secret",
                "redirectUri", "http://localhost:8080/login/oauth2/code/acme",
                "issuerUri", "https://auth.example.com/acme/as",
                "scopes", "openid profile email"));

        var registration = factory.build(request);

        assertThat(registration.getRegistrationId()).isEqualTo("acme");
        assertThat(registration.getClientId()).isEqualTo("acme-client");
        assertThat(registration.getClientSecret()).isEqualTo("acme-secret");
        assertThat(registration.getRedirectUri())
                .isEqualTo("http://localhost:8080/login/oauth2/code/acme");
        assertThat(registration.getProviderDetails().getIssuerUri())
                .isEqualTo("https://auth.example.com/acme/as");
    }

    @Test
    void buildsRegistrationFromExplicitEndpoints() {
        ClientToolConfigRequest request = new ClientToolConfigRequest();
        request.setValues(Map.of(
                "registrationId", "pingone",
                "clientId", "client",
                "clientSecret", "secret",
                "redirectUri", "http://localhost:8080/login/oauth2/code/pingone",
                "authorizationUri", "https://auth.example.com/as/authorize",
                "tokenUri", "https://auth.example.com/as/token",
                "userInfoUri", "https://auth.example.com/as/userinfo",
                "jwksUri", "https://auth.example.com/as/jwks"));

        var registration = factory.build(request);

        assertThat(registration.getProviderDetails().getAuthorizationUri())
                .isEqualTo("https://auth.example.com/as/authorize");
        assertThat(registration.getProviderDetails().getTokenUri())
                .isEqualTo("https://auth.example.com/as/token");
    }

    @Test
    void requiresClientId() {
        ClientToolConfigRequest request = new ClientToolConfigRequest();
        request.setValues(Map.of(
                "redirectUri", "http://localhost:8080/login/oauth2/code/pingone",
                "issuerUri", "https://auth.example.com/as"));

        assertThatThrownBy(() -> factory.build(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("clientId");
    }
}
