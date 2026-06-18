package com.pingone.oidc.client.adoption;

import com.pingone.oidc.config.properties.PingOneApplicationType;
import com.pingone.oidc.tool.PingOneApplicationTypeCatalog;
import com.pingone.oidc.tool.model.ClientToolConfigRequest;
import com.pingone.oidc.tool.model.GeneratedAdoptionArtifacts;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class WorkerAdoptionGenerator implements AdoptionArtifactGenerator {

    private final PingOneApplicationTypeCatalog catalog;

    public WorkerAdoptionGenerator(PingOneApplicationTypeCatalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public PingOneApplicationType supportedType() {
        return PingOneApplicationType.WORKER;
    }

    @Override
    public GeneratedAdoptionArtifacts generate(ClientToolConfigRequest request) {
        String regId = request.value("registrationId", "pingone-worker");
        String issuerUri = request.value("issuerUri", "https://auth.pingone.com/{environment-id}/as");
        String yaml =
                """
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
                            client-authentication-method: client_secret_basic
                            authorization-grant-type: client_credentials
                            scope:
                %s
                            provider: %s
                        provider:
                %s"""
                        .formatted(
                                regId,
                                regId,
                                request.value("clientId", "worker-client-id"),
                                request.value("clientSecret", "worker-client-secret"),
                                SharedAdoptionYamlSupport.formatYamlScopes(request.value("scopes", "openid")),
                                regId,
                                SharedAdoptionYamlSupport.buildProviderYamlBlock(regId, request, issuerUri));

        AdoptionCopyManifest manifest = workerManifest();
        String javaNotes =
                """
                // PingOne Worker (client_credentials) integration
                //
                // Test token acquisition: GET /worker/token
                // Use OAuth2AuthorizedClientManager with client_credentials provider in your services.

                %s"""
                        .formatted(manifest.format());

        return new GeneratedAdoptionArtifacts(
                "worker",
                true,
                yaml,
                """
                PINGONE_APPLICATION_TYPE=worker
                PINGONE_REGISTRATION_ID=%s
                PINGONE_CLIENT_ID=%s
                PINGONE_CLIENT_SECRET=%s
                PINGONE_ISSUER_URI=%s"""
                        .formatted(
                                regId,
                                request.value("clientId", "worker-client-id"),
                                request.value("clientSecret", "worker-client-secret"),
                                issuerUri),
                javaNotes,
                String.join("\n", catalog.find("worker").pingOneAdminChecklist()),
                manifest.format());
    }

    public static AdoptionCopyManifest workerManifest() {
        return new AdoptionCopyManifest(
                "Worker — machine-to-machine client_credentials",
                List.of(
                        "com.pingone.oidc.client",
                        "com.pingone.oidc.client.registration",
                        "com.pingone.oidc.config.properties",
                        "com.pingone.oidc.config.security",
                        "com.pingone.oidc.template.worker"),
                List.of("spring-boot-starter-security", "spring-boot-starter-oauth2-client"),
                List.of(
                        "Set pingone.application-type=worker",
                        "Register WorkerSecurityConfigurer (oauth2Client, no browser login)",
                        "Use OAuth2AuthorizedClientManager to obtain access tokens in services",
                        "Call PingOne APIs with Bearer access tokens"));
    }
}
