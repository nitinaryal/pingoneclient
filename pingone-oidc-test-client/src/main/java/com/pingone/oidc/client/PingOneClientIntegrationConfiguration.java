package com.pingone.oidc.client;

import com.pingone.oidc.client.adoption.AdoptionArtifactGenerator;
import com.pingone.oidc.client.adoption.DeviceAdoptionGenerator;
import com.pingone.oidc.client.adoption.OidcNativeAdoptionGenerator;
import com.pingone.oidc.client.adoption.OidcSpaAdoptionGenerator;
import com.pingone.oidc.client.adoption.OidcWebAppAdoptionGenerator;
import com.pingone.oidc.client.adoption.WorkerAdoptionGenerator;
import com.pingone.oidc.client.registration.OidcNativeRegistrationFactory;
import com.pingone.oidc.client.registration.OidcPublicClientRegistrationFactory;
import com.pingone.oidc.client.registration.OidcWebAppRegistrationFactory;
import com.pingone.oidc.client.registration.WorkerRegistrationFactory;
import com.pingone.oidc.config.properties.PingOneApplicationType;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PingOneClientIntegrationConfiguration {

    @Bean
    PingOneClientIntegration oidcWebAppIntegration(
            OidcWebAppRegistrationFactory registrationFactory, OidcWebAppAdoptionGenerator adoptionGenerator) {
        return new PingOneClientIntegration(
                PingOneApplicationType.OIDC_WEB_APP,
                "OIDC Web App",
                true,
                registrationFactory,
                adoptionGenerator,
                OidcWebAppAdoptionGenerator.webAppManifest().packages(),
                OidcWebAppAdoptionGenerator.webAppManifest().setupSteps());
    }

    @Bean
    PingOneClientIntegration oidcSpaIntegration(
            OidcPublicClientRegistrationFactory registrationFactory, OidcSpaAdoptionGenerator adoptionGenerator) {
        return new PingOneClientIntegration(
                PingOneApplicationType.OIDC_SPA,
                "OIDC Single-Page App",
                true,
                registrationFactory,
                adoptionGenerator,
                List.of("com.pingone.oidc.client", "com.pingone.oidc.config.security"),
                List.of("Use OidcSpaSecurityConfigurer", "PKCE public client — no client secret"));
    }

    @Bean
    PingOneClientIntegration oidcNativeIntegration(
            OidcNativeRegistrationFactory registrationFactory, OidcNativeAdoptionGenerator adoptionGenerator) {
        return new PingOneClientIntegration(
                PingOneApplicationType.OIDC_NATIVE,
                "OIDC Native App",
                true,
                registrationFactory,
                adoptionGenerator,
                List.of("com.pingone.oidc.client", "com.pingone.oidc.config.security"),
                List.of("Use OidcNativeSecurityConfigurer", "Loopback or custom scheme redirect URI"));
    }

    @Bean
    PingOneClientIntegration workerIntegration(
            WorkerRegistrationFactory registrationFactory, WorkerAdoptionGenerator adoptionGenerator) {
        return new PingOneClientIntegration(
                PingOneApplicationType.WORKER,
                "Worker Application",
                true,
                registrationFactory,
                adoptionGenerator,
                WorkerAdoptionGenerator.workerManifest().packages(),
                WorkerAdoptionGenerator.workerManifest().setupSteps());
    }

    @Bean
    PingOneClientIntegration deviceIntegration(DeviceAdoptionGenerator adoptionGenerator) {
        return new PingOneClientIntegration(
                PingOneApplicationType.DEVICE,
                "Device Authorization",
                false,
                null,
                adoptionGenerator,
                List.of("com.pingone.oidc.client"),
                List.of("Planned: device code flow support"));
    }

    @Bean
    PingOneClientIntegration samlIntegration(AdoptionArtifactGenerator samlAdoptionGeneratorBean) {
        return new PingOneClientIntegration(
                PingOneApplicationType.SAML,
                "SAML Application",
                false,
                null,
                samlAdoptionGeneratorBean,
                List.of("com.pingone.oidc.client"),
                List.of("Planned: SAML2 relying party support"));
    }

    @Bean
    PingOneClientIntegrationRegistry pingOneClientIntegrationRegistry(List<PingOneClientIntegration> integrations) {
        return new PingOneClientIntegrationRegistry(integrations);
    }
}
