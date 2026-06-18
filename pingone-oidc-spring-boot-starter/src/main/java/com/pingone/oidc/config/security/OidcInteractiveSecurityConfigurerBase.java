package com.pingone.oidc.config.security;

import com.pingone.oidc.config.properties.PingOneApplicationType;
import com.pingone.oidc.config.properties.PingOneClientProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

/**
 * Base security for browser-based OIDC login flows (web app, SPA, native) using oauth2Login.
 */
abstract class OidcInteractiveSecurityConfigurerBase implements PingOneSecurityConfigurer {

    private final ObjectProvider<AuthenticationSuccessHandler> loginSuccessHandler;
    private final ObjectProvider<AuthenticationFailureHandler> loginFailureHandler;
    private final ObjectProvider<LogoutSuccessHandler> logoutSuccessHandler;
    private final PingOneClientProperties properties;

    OidcInteractiveSecurityConfigurerBase(
            ObjectProvider<AuthenticationSuccessHandler> loginSuccessHandler,
            ObjectProvider<AuthenticationFailureHandler> loginFailureHandler,
            ObjectProvider<LogoutSuccessHandler> logoutSuccessHandler,
            PingOneClientProperties properties) {
        this.loginSuccessHandler = loginSuccessHandler;
        this.loginFailureHandler = loginFailureHandler;
        this.logoutSuccessHandler = logoutSuccessHandler;
        this.properties = properties;
    }

    @Override
    public void configure(
            HttpSecurity http,
            PingOneClientProperties ignoredProperties,
            ClientRegistrationRepository clientRegistrationRepository) throws Exception {
        http.oauth2Login(oauth2 -> {
            oauth2.clientRegistrationRepository(clientRegistrationRepository);
            loginSuccessHandler.ifAvailable(oauth2::successHandler);
            loginFailureHandler.ifAvailable(oauth2::failureHandler);
            if (loginSuccessHandler.getIfAvailable() == null) {
                oauth2.defaultSuccessUrl(properties.getUi().getPostLoginPath(), true);
            }
        });
        http.logout(logout -> {
            logoutSuccessHandler.ifAvailable(logout::logoutSuccessHandler);
        });
    }
}

@Component
class OidcSpaSecurityConfigurer extends OidcInteractiveSecurityConfigurerBase {

    OidcSpaSecurityConfigurer(
            ObjectProvider<AuthenticationSuccessHandler> loginSuccessHandler,
            ObjectProvider<AuthenticationFailureHandler> loginFailureHandler,
            ObjectProvider<LogoutSuccessHandler> logoutSuccessHandler,
            PingOneClientProperties properties) {
        super(loginSuccessHandler, loginFailureHandler, logoutSuccessHandler, properties);
    }

    @Override
    public PingOneApplicationType supportedType() {
        return PingOneApplicationType.OIDC_SPA;
    }
}

@Component
class OidcNativeSecurityConfigurer extends OidcInteractiveSecurityConfigurerBase {

    OidcNativeSecurityConfigurer(
            ObjectProvider<AuthenticationSuccessHandler> loginSuccessHandler,
            ObjectProvider<AuthenticationFailureHandler> loginFailureHandler,
            ObjectProvider<LogoutSuccessHandler> logoutSuccessHandler,
            PingOneClientProperties properties) {
        super(loginSuccessHandler, loginFailureHandler, logoutSuccessHandler, properties);
    }

    @Override
    public PingOneApplicationType supportedType() {
        return PingOneApplicationType.OIDC_NATIVE;
    }
}
