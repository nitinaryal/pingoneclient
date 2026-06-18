package com.pingone.oidc.tool;

import static org.assertj.core.api.Assertions.assertThat;

import com.pingone.oidc.client.adoption.ClientAdoptionSnippetService;
import com.pingone.oidc.tool.model.ClientToolConfigRequest;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ClientAdoptionSnippetServiceTest {

    @Autowired
    private ClientAdoptionSnippetService service;

    @Test
    void generatesOidcWebAppArtifacts() {
        ClientToolConfigRequest request = new ClientToolConfigRequest();
        request.setApplicationType("oidc-web-app");
        request.setValues(Map.of(
                "registrationId", "acme",
                "providerId", "acme",
                "clientId", "cid",
                "clientSecret", "secret",
                "issuerUri", "https://auth.pingone.com/env/as",
                "redirectUri", "http://localhost:8080/login/oauth2/code/acme",
                "postLogoutRedirectUri", "http://localhost:8080/"));

        var artifacts = service.generate(request);

        assertThat(artifacts.applicationType()).isEqualTo("oidc-web-app");
        assertThat(artifacts.runnableInTemplate()).isTrue();
        assertThat(artifacts.applicationYaml()).contains("application-type: oidc-web-app");
        assertThat(artifacts.applicationYaml()).contains("client-id: cid");
        assertThat(artifacts.envVariables()).contains("PINGONE_CLIENT_ID=cid");
        assertThat(artifacts.javaIntegrationNotes()).contains("/oauth2/authorization/acme");
        assertThat(artifacts.copyManifest()).contains("com.pingone.oidc.client");
        assertThat(artifacts.adoptionGuide()).contains("PACKAGE INDEX");
        assertThat(artifacts.libraryDependency()).contains("pingone-oidc-spring-boot-starter");
    }

    @Test
    void generatesOidcWebAppArtifactsWithExplicitProviderEndpoints() {
        ClientToolConfigRequest request = new ClientToolConfigRequest();
        request.setApplicationType("oidc-web-app");
        request.setValues(Map.of(
                "registrationId", "acme",
                "providerId", "acme",
                "clientId", "cid",
                "clientSecret", "secret",
                "issuerUri", "https://auth.pingone.com/env/as",
                "authorizationUri", "https://auth.pingone.com/env/as/authorize",
                "tokenUri", "https://auth.pingone.com/env/as/token",
                "redirectUri", "http://localhost:8080/login/oauth2/code/acme",
                "postLogoutRedirectUri", "http://localhost:8080/",
                "responseTypesSupported", "code, id_token"));

        var artifacts = service.generate(request);

        assertThat(artifacts.applicationYaml()).contains("authorization-uri: https://auth.pingone.com/env/as/authorize");
        assertThat(artifacts.applicationYaml()).contains("token-uri: https://auth.pingone.com/env/as/token");
        assertThat(artifacts.applicationYaml()).contains("response-types-supported: \"code, id_token\"");
        assertThat(artifacts.envVariables()).contains("PINGONE_AUTHORIZATION_URI=");
    }
}
