package com.pingone.oidc.support;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

@TestConfiguration
public class TestOAuth2ClientConfig {

    @Bean
    @Primary
    @Qualifier("testClientRegistrationRepository")
    ClientRegistrationRepository testClientRegistrationRepository() {
        return new InMemoryClientRegistrationRepository(OAuth2TestClientRegistration.pingone());
    }
}
