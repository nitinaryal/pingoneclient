package com.pingone.oidc.config.security;

import com.pingone.oidc.config.properties.PingOneApplicationType;
import com.pingone.oidc.config.properties.PingOneClientProperties;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

/**
 * Extension point for PingOne application-type-specific security configuration.
 * Implement this interface to add support for SPA, Native, Worker, Device, or SAML flows.
 */
public interface PingOneSecurityConfigurer {

    PingOneApplicationType supportedType();

    void configure(
            HttpSecurity http,
            PingOneClientProperties properties,
            ClientRegistrationRepository clientRegistrationRepository) throws Exception;
}
