package com.pingone.oidc.tool.oauth;

import com.pingone.oidc.config.properties.PingOneClientProperties;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.stereotype.Component;

@Component
public class ToolOAuthAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

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
        } else {
            setDefaultTargetUrl(properties.getUi().getPostLoginPath());
        }
        super.onAuthenticationSuccess(request, response, authentication);
    }
}
