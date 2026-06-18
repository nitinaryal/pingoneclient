package com.pingone.oidc.tool.oauth;

import com.pingone.oidc.config.properties.PingOneClientProperties;
import com.pingone.oidc.tool.session.ToolWizardSessionStore;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Component
public class ToolOAuthLogoutSuccessHandler implements LogoutSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(ToolOAuthLogoutSuccessHandler.class);

    private final ClientRegistrationRepository clientRegistrationRepository;
    private final PingOneClientProperties properties;
    private final ToolWizardSessionStore wizardSessionStore;

    public ToolOAuthLogoutSuccessHandler(
            ClientRegistrationRepository clientRegistrationRepository,
            PingOneClientProperties properties,
            ToolWizardSessionStore wizardSessionStore) {
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.properties = properties;
        this.wizardSessionStore = wizardSessionStore;
    }

    @Override
    public void onLogoutSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {
        String postLogoutRedirectUri = properties.getSecurity().getPostLogoutRedirectUri();
        HttpSession session = request.getSession(false);
        if (session != null && wizardSessionStore.isToolInitiatedLogout(session)) {
            wizardSessionStore.clearToolInitiatedLogoutFlag(session);
            postLogoutRedirectUri = ServletUriComponentsBuilder.fromContextPath(request)
                    .path("/tool")
                    .queryParam("logout", "success")
                    .build()
                    .toUriString();
            log.info("Tool-initiated logout: redirecting to PingOne end session, then {}", postLogoutRedirectUri);
        } else {
            log.info("Application logout: redirecting to PingOne end session, then {}", postLogoutRedirectUri);
        }

        OidcClientInitiatedLogoutSuccessHandler delegate =
                new OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository);
        delegate.setPostLogoutRedirectUri(postLogoutRedirectUri);
        delegate.onLogoutSuccess(request, response, authentication);
    }
}
