package com.pingone.oidc.tool.oauth;

import com.pingone.oidc.config.properties.PingOneClientProperties;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.stereotype.Component;

@Component
public class ToolOAuthAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(ToolOAuthAuthenticationSuccessHandler.class);

    private final PingOneClientProperties properties;
    private final ToolSessionClientRegistrationStore sessionStore;
    private final RequestCache requestCache = new HttpSessionRequestCache();

    public ToolOAuthAuthenticationSuccessHandler(
            PingOneClientProperties properties, ToolSessionClientRegistrationStore sessionStore) {
        this.properties = properties;
        this.sessionStore = sessionStore;
        setAlwaysUseDefaultTargetUrl(true);
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {
        HttpSession session = request.getSession(false);
        if (session != null && sessionStore.isToolInitiatedLogin(session)) {
            sessionStore.clearToolInitiatedLoginFlag(session);
            requestCache.removeRequest(request, response);
            setDefaultTargetUrl("/tool?oauth=success#tests");
            log.info("Tool OAuth login succeeded for principal '{}'", authentication.getName());
        } else {
            setDefaultTargetUrl(properties.getUi().getPostLoginPath());
            log.info("Application OAuth login succeeded for principal '{}'", authentication.getName());
        }
        super.onAuthenticationSuccess(request, response, authentication);
    }
}
