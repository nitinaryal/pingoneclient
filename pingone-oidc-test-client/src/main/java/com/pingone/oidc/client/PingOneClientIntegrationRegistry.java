package com.pingone.oidc.client;

import com.pingone.oidc.client.adoption.AdoptionArtifactGenerator;
import com.pingone.oidc.client.registration.PingOneRegistrationFactory;
import com.pingone.oidc.config.properties.PingOneApplicationType;
import com.pingone.oidc.tool.model.ClientToolConfigRequest;
import com.pingone.oidc.tool.model.GeneratedAdoptionArtifacts;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.stereotype.Component;

@Component
public class PingOneClientIntegrationRegistry {

    private final Map<PingOneApplicationType, PingOneClientIntegration> integrationsByType;
    private final Map<PingOneApplicationType, PingOneRegistrationFactory> registrationFactoriesByType;
    private final Map<PingOneApplicationType, AdoptionArtifactGenerator> adoptionGeneratorsByType;

    public PingOneClientIntegrationRegistry(List<PingOneClientIntegration> integrations) {
        this.integrationsByType = integrations.stream()
                .collect(Collectors.toMap(PingOneClientIntegration::applicationType, Function.identity()));
        this.registrationFactoriesByType = integrations.stream()
                .filter(integration -> integration.registrationFactory() != null)
                .collect(Collectors.toMap(
                        integration -> integration.registrationFactory().supportedType(),
                        PingOneClientIntegration::registrationFactory,
                        (left, right) -> left));
        this.adoptionGeneratorsByType = integrations.stream()
                .filter(integration -> integration.adoptionGenerator() != null)
                .collect(Collectors.toMap(
                        integration -> integration.adoptionGenerator().supportedType(),
                        PingOneClientIntegration::adoptionGenerator,
                        (left, right) -> left));
    }

    public List<PingOneClientIntegration> all() {
        return List.copyOf(integrationsByType.values());
    }

    public PingOneClientIntegration require(PingOneApplicationType type) {
        PingOneClientIntegration integration = integrationsByType.get(type);
        if (integration == null) {
            throw new IllegalArgumentException("No PingOne client integration registered for type: " + type);
        }
        return integration;
    }

    public PingOneClientIntegration require(String configValue) {
        return require(PingOneApplicationType.fromConfigValue(configValue));
    }

    public ClientRegistration buildRegistration(ClientToolConfigRequest request) {
        PingOneApplicationType type = PingOneApplicationType.fromConfigValue(request.getApplicationType());
        PingOneRegistrationFactory factory = registrationFactoriesByType.get(type);
        if (factory == null) {
            throw new IllegalArgumentException("No registration factory for application type: " + type);
        }
        return factory.build(request);
    }

    public GeneratedAdoptionArtifacts generateArtifacts(ClientToolConfigRequest request) {
        PingOneApplicationType type = PingOneApplicationType.fromConfigValue(request.getApplicationType());
        AdoptionArtifactGenerator generator = adoptionGeneratorsByType.get(type);
        if (generator == null) {
            throw new IllegalArgumentException("No adoption generator for application type: " + type);
        }
        return generator.generate(request);
    }
}
