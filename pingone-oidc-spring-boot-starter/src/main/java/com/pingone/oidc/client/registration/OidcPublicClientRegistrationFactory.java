package com.pingone.oidc.client.registration;

import com.pingone.oidc.config.properties.PingOneApplicationType;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.stereotype.Component;

/**
 * Public OIDC client (SPA) using Authorization Code + PKCE. Spring Security enables PKCE automatically
 * when no client secret is configured.
 */
@Component
public class OidcPublicClientRegistrationFactory implements PingOneRegistrationFactory {

    @Override
    public PingOneApplicationType supportedType() {
        return PingOneApplicationType.OIDC_SPA;
    }

    @Override
    public ClientRegistration build(RegistrationValueSource request) {
        return RegistrationFactorySupport.buildPublicOidcRegistration(request);
    }
}
