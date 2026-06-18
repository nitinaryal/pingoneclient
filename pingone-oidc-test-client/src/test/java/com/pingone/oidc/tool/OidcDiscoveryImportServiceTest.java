package com.pingone.oidc.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

class OidcDiscoveryImportServiceTest {

    private final OidcDiscoveryImportService service = new OidcDiscoveryImportService(WebClient.create());

    @Test
    void mapsPingOneDiscoveryDocumentToWizardFields() {
        String json =
                """
                {
                  "issuer": "https://auth.pingone.com/env-123/as",
                  "authorization_endpoint": "https://auth.pingone.com/env-123/as/authorize",
                  "token_endpoint": "https://auth.pingone.com/env-123/as/token",
                  "userinfo_endpoint": "https://auth.pingone.com/env-123/as/userinfo",
                  "jwks_uri": "https://auth.pingone.com/env-123/as/jwks",
                  "end_session_endpoint": "https://auth.pingone.com/env-123/as/signoff",
                  "scopes_supported": ["openid", "profile", "email"],
                  "response_types_supported": ["code", "id_token"],
                  "id_token_signing_alg_values_supported": ["RS256"]
                }
                """;

        var result = service.applyJson(json);

        assertThat(result.fieldValues())
                .containsEntry("issuerUri", "https://auth.pingone.com/env-123/as")
                .containsEntry("authorizationUri", "https://auth.pingone.com/env-123/as/authorize")
                .containsEntry("tokenUri", "https://auth.pingone.com/env-123/as/token")
                .containsEntry("userInfoUri", "https://auth.pingone.com/env-123/as/userinfo")
                .containsEntry("jwksUri", "https://auth.pingone.com/env-123/as/jwks")
                .containsEntry("endSessionEndpoint", "https://auth.pingone.com/env-123/as/signoff")
                .containsEntry("scopes", "openid, profile, email")
                .containsEntry("responseTypesSupported", "code, id_token")
                .containsEntry("idTokenSigningAlgValuesSupported", "RS256");
        assertThat(result.mappedKeys()).contains("issuer", "authorization_endpoint", "jwks_uri");
    }

    @Test
    void rejectsInvalidJson() {
        assertThatThrownBy(() -> service.applyJson("{not-json"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid OIDC discovery JSON");
    }

    @Test
    void rejectsNonHttpsIssuerFetch() {
        assertThatThrownBy(() -> service.fetchFromIssuer("http://insecure.example/as", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("HTTPS");
    }
}
