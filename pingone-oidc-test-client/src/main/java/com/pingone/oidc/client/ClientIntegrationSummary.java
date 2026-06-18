package com.pingone.oidc.client;

import java.util.List;

public record ClientIntegrationSummary(
        String applicationType,
        String displayName,
        boolean runnableInTemplate,
        List<String> copyPackages,
        List<String> setupSteps) {

    public static ClientIntegrationSummary from(PingOneClientIntegration integration) {
        return new ClientIntegrationSummary(
                integration.configValue(),
                integration.displayName(),
                integration.runnableInTemplate(),
                integration.copyPackages(),
                integration.setupSteps());
    }
}
