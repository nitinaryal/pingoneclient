package com.pingone.oidc.client.adoption;

import com.pingone.oidc.config.properties.PingOneApplicationType;
import com.pingone.oidc.tool.PingOneApplicationTypeCatalog;
import com.pingone.oidc.tool.model.ClientToolConfigRequest;
import com.pingone.oidc.tool.model.GeneratedAdoptionArtifacts;
import java.util.List;

public class OidcPublicClientAdoptionGenerator implements AdoptionArtifactGenerator {

    private final PingOneApplicationTypeCatalog catalog;
    private final PingOneApplicationType type;
    private final String configValue;
    private final boolean runnableInTemplate;

    protected OidcPublicClientAdoptionGenerator(
            PingOneApplicationTypeCatalog catalog,
            PingOneApplicationType type,
            String configValue,
            boolean runnableInTemplate) {
        this.catalog = catalog;
        this.type = type;
        this.configValue = configValue;
        this.runnableInTemplate = runnableInTemplate;
    }

    @Override
    public PingOneApplicationType supportedType() {
        return type;
    }

    @Override
    public GeneratedAdoptionArtifacts generate(ClientToolConfigRequest request) {
        String regId = request.value("registrationId", "pingone");
        String providerId = request.value("providerId", regId);
        String clientId = request.value("clientId", "your-client-id");
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
                            client-authentication-method: none
                            authorization-grant-type: authorization_code
                            redirect-uri: %s
                            scope:
                %s
                            provider: %s
                        provider:
                %s

                pingone:
                  application-type: %s
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
                                redirectUri,
                                SharedAdoptionYamlSupport.formatYamlScopes(scopes),
                                providerId,
                                SharedAdoptionYamlSupport.buildProviderYamlBlock(providerId, request, issuerUri),
                                configValue,
                                regId,
                                providerId,
                                postLogout,
                                SharedAdoptionYamlSupport.buildDiscoveryMetadataYamlBlock(request));

        String env =
                """
                PINGONE_APPLICATION_TYPE=%s
                PINGONE_REGISTRATION_ID=%s
                PINGONE_PROVIDER_ID=%s
                PINGONE_CLIENT_ID=%s
                PINGONE_ISSUER_URI=%s
                PINGONE_REDIRECT_URI=%s
                PINGONE_POST_LOGOUT_REDIRECT_URI=%s
                %s"""
                        .formatted(
                                configValue,
                                regId,
                                providerId,
                                clientId,
                                issuerUri,
                                redirectUri,
                                postLogout,
                                SharedAdoptionYamlSupport.buildDiscoveryEnvBlock(request));

        AdoptionCopyManifest manifest = publicClientManifest(type);
        String javaNotes =
                """
                // PingOne %s integration (Authorization Code + PKCE, no client secret)
                //
                // Login URL: /oauth2/authorization/%s
                // Spring Security enables PKCE automatically for public clients.
                //
                // PingOne Admin redirect URI: %s"""
                        .formatted(type.name(), regId, redirectUri);

        return new GeneratedAdoptionArtifacts(
                configValue,
                runnableInTemplate,
                yaml,
                env,
                javaNotes,
                String.join("\n", catalog.find(configValue).pingOneAdminChecklist()),
                manifest.format());
    }

    private static AdoptionCopyManifest publicClientManifest(PingOneApplicationType type) {
        String securityConfigurer =
                type == PingOneApplicationType.OIDC_NATIVE
                        ? "OidcNativeSecurityConfigurer"
                        : "OidcSpaSecurityConfigurer";
        return new AdoptionCopyManifest(
                type.name() + " — copy into your Spring Boot app",
                List.of(
                        "com.pingone.oidc.client",
                        "com.pingone.oidc.client.registration",
                        "com.pingone.oidc.config.properties",
                        "com.pingone.oidc.config.security"),
                List.of("spring-boot-starter-security", "spring-boot-starter-oauth2-client"),
                List.of(
                        "Set pingone.application-type=" + type.getConfigValue(),
                        "Register " + securityConfigurer + " (oauth2Login with PKCE public client)",
                        "Do not configure client-secret; use client-authentication-method: none",
                        "Register SPA/native redirect URI in PingOne Admin"));
    }
}
