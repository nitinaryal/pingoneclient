package com.pingone.oidc.client.adoption;

import com.pingone.oidc.config.properties.PingOneApplicationType;
import com.pingone.oidc.tool.PingOneApplicationTypeCatalog;
import com.pingone.oidc.tool.model.ClientToolConfigRequest;
import com.pingone.oidc.tool.model.GeneratedAdoptionArtifacts;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class OidcWebAppAdoptionGenerator implements AdoptionArtifactGenerator {

    private final PingOneApplicationTypeCatalog catalog;

    public OidcWebAppAdoptionGenerator(PingOneApplicationTypeCatalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public PingOneApplicationType supportedType() {
        return PingOneApplicationType.OIDC_WEB_APP;
    }

    @Override
    public GeneratedAdoptionArtifacts generate(ClientToolConfigRequest request) {
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
                                SharedAdoptionYamlSupport.formatYamlScopes(scopes),
                                providerId,
                                SharedAdoptionYamlSupport.buildProviderYamlBlock(providerId, request, issuerUri),
                                regId,
                                providerId,
                                postLogout,
                                SharedAdoptionYamlSupport.buildDiscoveryMetadataYamlBlock(request));

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
                                SharedAdoptionYamlSupport.buildDiscoveryEnvBlock(request));

        AdoptionCopyManifest manifest = webAppManifest();
        String javaNotes =
                """
                // PingOne OIDC Web App integration (tested in /tool)
                //
                // Login URL: /oauth2/authorization/%s
                // Logout: POST /logout (PingOne end session)
                //
                // PingOne Admin:
                //   Redirect URI: %s
                //   Post-logout Redirect URI: %s
                //
                // Endpoint overrides from discovery import:
                %s"""
                        .formatted(
                                regId,
                                redirectUri,
                                postLogout,
                                SharedAdoptionYamlSupport.buildDiscoveryJavaNotes(request));

        return new GeneratedAdoptionArtifacts(
                "oidc-web-app",
                true,
                yaml,
                env,
                javaNotes,
                String.join("\n", catalog.find("oidc-web-app").pingOneAdminChecklist()),
                manifest.format());
    }

    public static AdoptionCopyManifest webAppManifest() {
        return new AdoptionCopyManifest(
                "OIDC Web App — copy into your Spring Boot app",
                List.of(
                        "com.pingone.oidc.client.registration (pingone-oidc-spring-boot-starter)",
                        "com.pingone.oidc.config.properties (pingone-oidc-spring-boot-starter)",
                        "com.pingone.oidc.config.security (pingone-oidc-spring-boot-starter)"),
                List.of(
                        "spring-boot-starter-security",
                        "spring-boot-starter-oauth2-client"),
                List.of(
                        "Set pingone.application-type=oidc-web-app and OAuth2 client registration values",
                        "Add com.pingone:pingone-oidc-spring-boot-starter dependency (see Library tab)",
                        "Register a SecurityFilterChain using PingOneSecurityConfigurerFactory",
                        "Use /oauth2/authorization/{registrationId} for browser login",
                        "Register redirect and post-logout URIs in PingOne Admin"));
    }
}
