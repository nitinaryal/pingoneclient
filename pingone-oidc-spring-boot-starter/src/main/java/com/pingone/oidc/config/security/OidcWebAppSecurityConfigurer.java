package com.pingone.oidc.config.security;

import com.pingone.oidc.config.properties.PingOneApplicationType;
import com.pingone.oidc.config.properties.PingOneClientProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class OidcWebAppSecurityConfigurer extends OidcInteractiveSecurityConfigurerBase {

    public OidcWebAppSecurityConfigurer(
            ObjectProvider<AuthenticationSuccessHandler> loginSuccessHandler,
            ObjectProvider<AuthenticationFailureHandler> loginFailureHandler,
            ObjectProvider<LogoutSuccessHandler> logoutSuccessHandler,
            PingOneClientProperties properties) {
        super(loginSuccessHandler, loginFailureHandler, logoutSuccessHandler, properties);
    }

    @Override
    public PingOneApplicationType supportedType() {
        return PingOneApplicationType.OIDC_WEB_APP;
    }
}
