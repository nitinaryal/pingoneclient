package com.pingone.oidc.client.guide;

import com.pingone.oidc.client.PingOneClientIntegration;
import com.pingone.oidc.client.PingOneClientIntegrationRegistry;
import com.pingone.oidc.tool.model.GeneratedAdoptionArtifacts;
import org.springframework.stereotype.Component;

@Component
public class AdoptionArtifactEnricher {

    private final PingOneClientIntegrationRegistry integrationRegistry;

    public AdoptionArtifactEnricher(PingOneClientIntegrationRegistry integrationRegistry) {
        this.integrationRegistry = integrationRegistry;
    }

    public GeneratedAdoptionArtifacts enrich(GeneratedAdoptionArtifacts base) {
        String type = base.applicationType();
        if (type == null || type.isBlank()) {
            return base;
        }
        PingOneClientIntegration integration = integrationRegistry.require(type);
        return new GeneratedAdoptionArtifacts(
                base.applicationType(),
                base.runnableInTemplate(),
                base.applicationYaml(),
                base.envVariables(),
                base.javaIntegrationNotes(),
                base.pingOneAdminSteps(),
                base.copyManifest(),
                PingOneClientPackageIndex.format(integration),
                ClientLibraryDependencyGuide.format(integration));
    }
}
