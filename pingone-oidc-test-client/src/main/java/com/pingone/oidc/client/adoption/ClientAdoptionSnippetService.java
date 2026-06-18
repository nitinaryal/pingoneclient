package com.pingone.oidc.client.adoption;

import com.pingone.oidc.client.PingOneClientIntegrationRegistry;
import com.pingone.oidc.client.guide.AdoptionArtifactEnricher;
import com.pingone.oidc.tool.model.ClientToolConfigRequest;
import com.pingone.oidc.tool.model.GeneratedAdoptionArtifacts;
import org.springframework.stereotype.Service;

@Service
public class ClientAdoptionSnippetService {

    private final PingOneClientIntegrationRegistry integrationRegistry;
    private final AdoptionArtifactEnricher artifactEnricher;

    public ClientAdoptionSnippetService(
            PingOneClientIntegrationRegistry integrationRegistry, AdoptionArtifactEnricher artifactEnricher) {
        this.integrationRegistry = integrationRegistry;
        this.artifactEnricher = artifactEnricher;
    }

    public GeneratedAdoptionArtifacts generate(ClientToolConfigRequest request) {
        return artifactEnricher.enrich(integrationRegistry.generateArtifacts(request));
    }
}
