package com.pingone.oidc.config;

import com.pingone.oidc.config.properties.PingOneClientProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.util.Assert;

@Configuration
@EnableConfigurationProperties(PingOneClientProperties.class)
public class PingOneClientAutoConfiguration {

    public PingOneClientAutoConfiguration(
            PingOneClientProperties properties, ClientRegistrationRepository clientRegistrationRepository) {
        validateRegistration(properties, clientRegistrationRepository);
    }

    private static void validateRegistration(
            PingOneClientProperties properties, ClientRegistrationRepository clientRegistrationRepository) {
        String registrationId = properties.getRegistrationId();
        ClientRegistration registration = clientRegistrationRepository.findByRegistrationId(registrationId);
        Assert.notNull(
                registration,
                () -> "OAuth2 client registration '" + registrationId + "' was not found. "
                        + "Ensure spring.security.oauth2.client.registration." + registrationId
                        + " matches pingone.registration-id.");
    }
}
