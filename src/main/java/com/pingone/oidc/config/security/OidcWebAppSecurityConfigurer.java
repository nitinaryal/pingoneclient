package com.pingone.oidc.config.security;

import com.pingone.oidc.config.properties.PingOneApplicationType;
import com.pingone.oidc.config.properties.PingOneClientProperties;
import com.pingone.oidc.tool.oauth.ToolOAuthAuthenticationFailureHandler;
import com.pingone.oidc.tool.oauth.ToolOAuthAuthenticationSuccessHandler;
import com.pingone.oidc.tool.oauth.ToolOAuthLogoutSuccessHandler;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Component;

@Component
public class OidcWebAppSecurityConfigurer implements PingOneSecurityConfigurer {

    private final ToolOAuthAuthenticationSuccessHandler toolOAuthAuthenticationSuccessHandler;
    private final ToolOAuthAuthenticationFailureHandler toolOAuthAuthenticationFailureHandler;
    private final ToolOAuthLogoutSuccessHandler toolOAuthLogoutSuccessHandler;

    public OidcWebAppSecurityConfigurer(
            ToolOAuthAuthenticationSuccessHandler toolOAuthAuthenticationSuccessHandler,
            ToolOAuthAuthenticationFailureHandler toolOAuthAuthenticationFailureHandler,
            ToolOAuthLogoutSuccessHandler toolOAuthLogoutSuccessHandler) {
        this.toolOAuthAuthenticationSuccessHandler = toolOAuthAuthenticationSuccessHandler;
        this.toolOAuthAuthenticationFailureHandler = toolOAuthAuthenticationFailureHandler;
        this.toolOAuthLogoutSuccessHandler = toolOAuthLogoutSuccessHandler;
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

        http.oauth2Login(oauth2 -> oauth2
                        .clientRegistrationRepository(clientRegistrationRepository)
                        .successHandler(toolOAuthAuthenticationSuccessHandler)
                        .failureHandler(toolOAuthAuthenticationFailureHandler))
                .logout(logout -> logout.logoutSuccessHandler(toolOAuthLogoutSuccessHandler));
    }
}
