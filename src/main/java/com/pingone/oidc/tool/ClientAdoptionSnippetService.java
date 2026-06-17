package com.pingone.oidc.tool;

import com.pingone.oidc.config.properties.PingOneApplicationType;
import com.pingone.oidc.tool.model.ClientToolConfigRequest;
import com.pingone.oidc.tool.model.GeneratedAdoptionArtifacts;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ClientAdoptionSnippetService {

    private final PingOneApplicationTypeCatalog catalog;

    public ClientAdoptionSnippetService(PingOneApplicationTypeCatalog catalog) {
        this.catalog = catalog;
    }

    public GeneratedAdoptionArtifacts generate(ClientToolConfigRequest request) {
        PingOneApplicationType type = PingOneApplicationType.fromConfigValue(request.getApplicationType());
        return switch (type) {
            case OIDC_WEB_APP -> oidcWebAppArtifacts(request);
            case OIDC_SPA -> placeholderArtifacts(request, "OIDC SPA (PKCE public client)");
            case OIDC_NATIVE -> placeholderArtifacts(request, "OIDC Native (PKCE + loopback redirect)");
            case WORKER -> workerArtifacts(request);
            case DEVICE -> placeholderArtifacts(request, "Device Authorization Grant");
            case SAML -> samlArtifacts(request);
        };
    }

    private GeneratedAdoptionArtifacts oidcWebAppArtifacts(ClientToolConfigRequest request) {
        String regId = request.value("registrationId", "pingone");
        String providerId = request.value("providerId", regId);
        String clientId = request.value("clientId", "your-client-id");
        String clientSecret = request.value("clientSecret", "your-client-secret");
        String issuerUri = request.value("issuerUri", "https://auth.pingone.com/{environment-id}/as");
        String redirectUri = request.value("redirectUri", "http://localhost:8080/login/oauth2/code/" + regId);
        String postLogout = request.value("postLogoutRedirectUri", "http://localhost:8080/");
        String scopes = request.value("scopes", "openid,profile,email");

        String yaml =
                """
                server:
                  port: 8080

                spring:
                  security:
                    oauth2:
                      client:
                        registration:
                          %s:
                            client-id: %s
                            client-secret: %s
                            client-authentication-method: client_secret_basic
                            authorization-grant-type: authorization_code
                            redirect-uri: %s
                            scope:
                %s
                            provider: %s
                        provider:
                %s

                pingone:
                  application-type: oidc-web-app
                  registration-id: %s
                  provider-id: %s
                  ui:
                    post-login-path: /dashboard
                  security:
                    post-logout-redirect-uri: %s
                %s"""
                        .formatted(
                                regId,
                                clientId,
                                clientSecret,
                                redirectUri,
                                formatYamlScopes(scopes),
                                providerId,
                                buildProviderYamlBlock(providerId, request, issuerUri),
                                regId,
                                providerId,
                                postLogout,
                                buildDiscoveryMetadataYamlBlock(request));

        String env =
                """
                PINGONE_APPLICATION_TYPE=oidc-web-app
                PINGONE_REGISTRATION_ID=%s
                PINGONE_PROVIDER_ID=%s
                PINGONE_CLIENT_ID=%s
                PINGONE_CLIENT_SECRET=%s
                PINGONE_ISSUER_URI=%s
                PINGONE_REDIRECT_URI=%s
                PINGONE_POST_LOGOUT_REDIRECT_URI=%s
                %s"""
                        .formatted(
                                regId,
                                providerId,
                                clientId,
                                clientSecret,
                                issuerUri,
                                redirectUri,
                                postLogout,
                                buildDiscoveryEnvBlock(request));

        String javaNotes =
                """
                // 1. Use this template project as dependency or copy these packages:
                //    com.pingone.oidc.config.*, controller.*, service.*

                // 2. Security is configured via OidcWebAppSecurityConfigurer (oauth2Login + OIDC logout)

                // 3. Login URL: /oauth2/authorization/%s

                // 4. PingOne Admin values:
                //    Redirect URI: %s
                //    Post-logout Redirect URI: %s

                // 5. OIDC discovery endpoints (override in YAML when not using issuer-uri discovery only):
                %s"""
                        .formatted(regId, redirectUri, postLogout, buildDiscoveryJavaNotes(request));

        String admin = String.join("\n", catalog.find("oidc-web-app").pingOneAdminChecklist());

        return new GeneratedAdoptionArtifacts(yaml, env, javaNotes, admin);
    }

    private static String buildProviderYamlBlock(
            String providerId, ClientToolConfigRequest request, String issuerUri) {
        String indent = "                          ";
        StringBuilder block = new StringBuilder();
        block.append(indent).append(providerId).append(":\n");
        block.append(indent).append("  issuer-uri: ").append(issuerUri).append('\n');
        appendProviderYamlLine(block, indent, "authorization-uri", request, "authorizationUri");
        appendProviderYamlLine(block, indent, "token-uri", request, "tokenUri");
        appendProviderYamlLine(block, indent, "user-info-uri", request, "userInfoUri");
        appendProviderYamlLine(block, indent, "jwk-set-uri", request, "jwksUri");
        block.append(indent).append("  user-name-attribute: sub\n");
        return block.toString();
    }

    private static void appendProviderYamlLine(
            StringBuilder block, String indent, String yamlKey, ClientToolConfigRequest request, String valueKey) {
        String value = request.value(valueKey, "");
        if (StringUtils.hasText(value)) {
            block.append(indent).append("  ").append(yamlKey).append(": ").append(value).append('\n');
        }
    }

    private static String buildDiscoveryMetadataYamlBlock(ClientToolConfigRequest request) {
        List<String> lines = new ArrayList<>();
        lines.add("  oidc:");
        lines.add("    discovery-metadata:");
        appendMetadataYamlLine(lines, request, "responseTypesSupported", "response-types-supported");
        appendMetadataYamlLine(lines, request, "responseModesSupported", "response-modes-supported");
        appendMetadataYamlLine(lines, request, "grantTypesSupported", "grant-types-supported");
        appendMetadataYamlLine(lines, request, "subjectTypesSupported", "subject-types-supported");
        appendMetadataYamlLine(lines, request, "idTokenSigningAlgValuesSupported", "id-token-signing-alg-values-supported");
        appendMetadataYamlLine(
                lines, request, "tokenEndpointAuthMethodsSupported", "token-endpoint-auth-methods-supported");
        appendMetadataYamlLine(lines, request, "codeChallengeMethodsSupported", "code-challenge-methods-supported");
        appendMetadataYamlLine(lines, request, "claimsSupported", "claims-supported");
        appendMetadataYamlLine(lines, request, "claimTypesSupported", "claim-types-supported");
        appendMetadataYamlLine(lines, request, "acrValuesSupported", "acr-values-supported");
        appendMetadataYamlLine(
                lines, request, "requestObjectSigningAlgValuesSupported", "request-object-signing-alg-values-supported");
        appendMetadataYamlLine(lines, request, "serviceDocumentation", "service-documentation");
        appendMetadataYamlLine(lines, request, "endSessionEndpoint", "end-session-endpoint");
        appendMetadataYamlLine(lines, request, "revocationUri", "revocation-endpoint");
        appendMetadataYamlLine(lines, request, "introspectionUri", "introspection-endpoint");
        appendMetadataYamlLine(lines, request, "registrationEndpoint", "registration-endpoint");
        if (lines.size() <= 2) {
            return "";
        }
        return "\n" + String.join("\n", lines);
    }

    private static void appendMetadataYamlLine(
            List<String> lines, ClientToolConfigRequest request, String valueKey, String yamlKey) {
        String value = request.value(valueKey, "");
        if (StringUtils.hasText(value)) {
            lines.add("      " + yamlKey + ": \"" + value.replace("\"", "\\\"") + "\"");
        }
    }

    private static String buildDiscoveryEnvBlock(ClientToolConfigRequest request) {
        StringBuilder block = new StringBuilder();
        appendEnvLine(block, request, "authorizationUri", "PINGONE_AUTHORIZATION_URI");
        appendEnvLine(block, request, "tokenUri", "PINGONE_TOKEN_URI");
        appendEnvLine(block, request, "userInfoUri", "PINGONE_USERINFO_URI");
        appendEnvLine(block, request, "jwksUri", "PINGONE_JWKS_URI");
        return block.toString().stripTrailing();
    }

    private static void appendEnvLine(StringBuilder block, ClientToolConfigRequest request, String key, String envName) {
        String value = request.value(key, "");
        if (StringUtils.hasText(value)) {
            block.append(envName).append('=').append(value).append('\n');
        }
    }

    private static String buildDiscoveryJavaNotes(ClientToolConfigRequest request) {
        StringBuilder notes = new StringBuilder();
        appendJavaNoteLine(notes, request, "authorizationUri", "Authorization");
        appendJavaNoteLine(notes, request, "tokenUri", "Token");
        appendJavaNoteLine(notes, request, "userInfoUri", "UserInfo");
        appendJavaNoteLine(notes, request, "jwksUri", "JWKS");
        appendJavaNoteLine(notes, request, "endSessionEndpoint", "End session (PingOne signoff)");
        if (notes.isEmpty()) {
            return "//    (import discovery JSON in /tool to populate endpoint overrides)";
        }
        return notes.toString().stripTrailing();
    }

    private static void appendJavaNoteLine(
            StringBuilder notes, ClientToolConfigRequest request, String key, String label) {
        String value = request.value(key, "");
        if (StringUtils.hasText(value)) {
            notes.append("//    ").append(label).append(": ").append(value).append('\n');
        }
    }

    private GeneratedAdoptionArtifacts workerArtifacts(ClientToolConfigRequest request) {
        String regId = request.value("registrationId", "pingone-worker");
        String yaml =
                """
                # Worker application (future template support)
                pingone:
                  application-type: worker
                  registration-id: %s

                spring:
                  security:
                    oauth2:
                      client:
                        registration:
                          %s:
                            client-id: %s
                            client-secret: %s
                            authorization-grant-type: client_credentials
                            provider: %s
                        provider:
                %s"""
                        .formatted(
                                regId,
                                regId,
                                request.value("clientId", "worker-client-id"),
                                request.value("clientSecret", "worker-client-secret"),
                                regId,
                                buildProviderYamlBlock(
                                        regId,
                                        request,
                                        request.value("issuerUri", "https://auth.pingone.com/{environment-id}/as")));

        return new GeneratedAdoptionArtifacts(
                yaml,
                "# Implement OidcWorkerSecurityConfigurer for client_credentials flow",
                "// Add WorkerSecurityConfigurer implementing PingOneSecurityConfigurer",
                String.join("\n", catalog.find("worker").pingOneAdminChecklist()));
    }

    private GeneratedAdoptionArtifacts samlArtifacts(ClientToolConfigRequest request) {
        String yaml =
                """
                # SAML integration (future template support)
                spring:
                  security:
                    saml2:
                      relyingparty:
                        registration:
                          pingone:
                            entity-id: %s
                            assertingparty:
                              metadata-uri: %s
                            acs:
                              location: %s
                """
                        .formatted(
                                request.value("entityId", "https://app.example.com/saml/metadata"),
                                request.value("metadataUrl", "https://auth.pingone.com/{env-id}/saml20/metadata"),
                                request.value("acsUrl", "https://app.example.com/login/saml2/sso/pingone"));

        return new GeneratedAdoptionArtifacts(
                yaml,
                "# SAML uses spring-security-saml2-service-provider",
                "// Add SamlSecurityConfigurer implementing PingOneSecurityConfigurer",
                String.join("\n", catalog.find("saml").pingOneAdminChecklist()));
    }

    private GeneratedAdoptionArtifacts placeholderArtifacts(ClientToolConfigRequest request, String label) {
        return new GeneratedAdoptionArtifacts(
                "# " + label + " — implement corresponding PingOneSecurityConfigurer\npingone:\n  application-type: "
                        + request.getApplicationType(),
                "# Set environment variables matching generated application.yml when implemented",
                "// Implement PingOneSecurityConfigurer for " + request.getApplicationType(),
                String.join("\n", catalog.find(request.getApplicationType()).pingOneAdminChecklist()));
    }

    private static String formatYamlScopes(String scopes) {
        return scopes.replace(",", " ").trim().lines()
                .flatMap(line -> java.util.Arrays.stream(line.split("[,\\s]+")))
                .filter(s -> !s.isBlank())
                .map(s -> "              - " + s)
                .reduce((a, b) -> a + "\n" + b)
                .orElse("              - openid");
    }
}
