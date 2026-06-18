package com.pingone.oidc.client.adoption;

import com.pingone.oidc.config.properties.PingOneApplicationType;
import com.pingone.oidc.tool.PingOneApplicationTypeCatalog;
import com.pingone.oidc.tool.model.ClientToolConfigRequest;
import com.pingone.oidc.tool.model.GeneratedAdoptionArtifacts;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DeviceAdoptionGenerator implements AdoptionArtifactGenerator {

    private final PingOneApplicationTypeCatalog catalog;

    public DeviceAdoptionGenerator(PingOneApplicationTypeCatalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public PingOneApplicationType supportedType() {
        return PingOneApplicationType.DEVICE;
    }

    @Override
    public GeneratedAdoptionArtifacts generate(ClientToolConfigRequest request) {
        AdoptionCopyManifest manifest = plannedManifest(
                "Device Authorization Grant",
                "DeviceAuthorizationSecurityConfigurer (planned)",
                List.of(
                        "Configure device authorization endpoints from PingOne discovery",
                        "Implement device code request + token polling in your app",
                        "Register device client in PingOne Admin"));
        return plannedArtifacts(request, "device", manifest);
    }

    static AdoptionCopyManifest plannedManifest(String title, String configurer, List<String> steps) {
        return new AdoptionCopyManifest(
                title,
                List.of("com.pingone.oidc.client", "com.pingone.oidc.config.properties"),
                List.of("spring-boot-starter-security", "spring-boot-starter-oauth2-client"),
                steps);
    }

    GeneratedAdoptionArtifacts plannedArtifacts(
            ClientToolConfigRequest request, String configValue, AdoptionCopyManifest manifest) {
        return new GeneratedAdoptionArtifacts(
                configValue,
                false,
                "# " + manifest.title() + " — planned template support\npingone:\n  application-type: " + configValue,
                "# Set environment variables matching application.yml when implemented",
                "// " + manifest.title() + "\n\n" + manifest.format(),
                String.join("\n", catalog.find(configValue).pingOneAdminChecklist()),
                manifest.format());
    }
}

@Component
class SamlAdoptionGeneratorBean implements AdoptionArtifactGenerator {

    private final PingOneApplicationTypeCatalog catalog;

    SamlAdoptionGeneratorBean(PingOneApplicationTypeCatalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public PingOneApplicationType supportedType() {
        return PingOneApplicationType.SAML;
    }

    @Override
    public GeneratedAdoptionArtifacts generate(ClientToolConfigRequest request) {
        String yaml =
                """
                pingone:
                  application-type: saml

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

        AdoptionCopyManifest manifest = DeviceAdoptionGenerator.plannedManifest(
                "SAML 2.0 Service Provider",
                "SamlSecurityConfigurer (planned)",
                List.of(
                        "Add spring-security-saml2-service-provider dependency",
                        "Import PingOne SAML metadata",
                        "Register ACS URL and entity ID in PingOne Admin"));

        return new GeneratedAdoptionArtifacts(
                "saml",
                false,
                yaml,
                "# SAML uses spring-security-saml2-service-provider",
                "// SAML integration\n\n" + manifest.format(),
                String.join("\n", catalog.find("saml").pingOneAdminChecklist()),
                manifest.format());
    }
}
