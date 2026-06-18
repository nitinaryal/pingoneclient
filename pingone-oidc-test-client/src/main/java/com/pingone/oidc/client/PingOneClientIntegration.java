package com.pingone.oidc.client;

import com.pingone.oidc.client.adoption.AdoptionArtifactGenerator;
import com.pingone.oidc.client.registration.PingOneRegistrationFactory;
import com.pingone.oidc.config.properties.PingOneApplicationType;
import java.util.List;

/**
 * Describes one PingOne application-type integration that teams can test in /tool and copy into their apps.
 */
public record PingOneClientIntegration(
        PingOneApplicationType applicationType,
        String displayName,
        boolean runnableInTemplate,
        PingOneRegistrationFactory registrationFactory,
        AdoptionArtifactGenerator adoptionGenerator,
        List<String> copyPackages,
        List<String> setupSteps) {

    public String configValue() {
        return applicationType.getConfigValue();
    }
}
