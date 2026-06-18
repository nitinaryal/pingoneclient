package com.pingone.oidc.client.registration;

import com.pingone.oidc.config.properties.PingOneApplicationType;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.stereotype.Component;

/**
 * Native OIDC client using Authorization Code + PKCE (same registration shape as SPA; redirect URI differs).
 */
@Component
public class OidcNativeRegistrationFactory implements PingOneRegistrationFactory {

    @Override
    public PingOneApplicationType supportedType() {
        return PingOneApplicationType.OIDC_NATIVE;
    }

    @Override
    public ClientRegistration build(RegistrationValueSource request) {
        return RegistrationFactorySupport.buildPublicOidcRegistration(request);
    }
}
