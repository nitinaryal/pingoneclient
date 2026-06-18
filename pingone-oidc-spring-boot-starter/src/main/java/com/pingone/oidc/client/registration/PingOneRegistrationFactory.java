package com.pingone.oidc.client.registration;

import com.pingone.oidc.config.properties.PingOneApplicationType;
import org.springframework.security.oauth2.client.registration.ClientRegistration;

/**
 * Builds a Spring {@link ClientRegistration} for a PingOne application type.
 */
public interface PingOneRegistrationFactory {

    PingOneApplicationType supportedType();

    ClientRegistration build(RegistrationValueSource request);
}
