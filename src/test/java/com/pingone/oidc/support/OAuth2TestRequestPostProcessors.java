package com.pingone.oidc.support;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

public final class OAuth2TestRequestPostProcessors {

    private OAuth2TestRequestPostProcessors() {
    }

    public static RequestPostProcessor oidcAuthentication(OidcUser oidcUser) {
        OAuth2AuthenticationToken token = new OAuth2AuthenticationToken(
                oidcUser,
                oidcUser.getAuthorities(),
                OAuth2TestClientRegistration.REGISTRATION_ID);
        return authentication(token);
    }
}
