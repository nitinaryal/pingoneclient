package com.pingone.oidc.config.security;

import com.pingone.oidc.config.properties.PingOneApplicationType;
import com.pingone.oidc.config.properties.PingOneClientProperties;
import com.pingone.oidc.tool.oauth.ToolOAuthAuthenticationSuccessHandler;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Component;

@Component
public class OidcWebAppSecurityConfigurer implements PingOneSecurityConfigurer {

    private final ToolOAuthAuthenticationSuccessHandler toolOAuthAuthenticationSuccessHandler;

    public OidcWebAppSecurityConfigurer(ToolOAuthAuthenticationSuccessHandler toolOAuthAuthenticationSuccessHandler) {
        this.toolOAuthAuthenticationSuccessHandler = toolOAuthAuthenticationSuccessHandler;
    }

    @Override
    public PingOneApplicationType supportedType() {
        return PingOneApplicationType.OIDC_WEB_APP;
    }

    @Override
    public void configure(
            HttpSecurity http,
            PingOneClientProperties properties,
            ClientRegistrationRepository clientRegistrationRepository) throws Exception {

        OidcClientInitiatedLogoutSuccessHandler logoutSuccessHandler =
                new OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository);
        logoutSuccessHandler.setPostLogoutRedirectUri(properties.getSecurity().getPostLogoutRedirectUri());

        http.oauth2Login(oauth2 -> oauth2
                        .clientRegistrationRepository(clientRegistrationRepository)
                        .successHandler(toolOAuthAuthenticationSuccessHandler))
                .logout(logout -> logout.logoutSuccessHandler(logoutSuccessHandler));
    }
}
