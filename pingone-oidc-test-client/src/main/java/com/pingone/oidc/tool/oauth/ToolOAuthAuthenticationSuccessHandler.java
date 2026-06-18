package com.pingone.oidc.tool.oauth;

import com.pingone.oidc.config.properties.PingOneClientProperties;
import com.pingone.oidc.tool.trace.FlowActor;
import com.pingone.oidc.tool.trace.PingOneFlowTraceService;
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
    private final PingOneFlowTraceService flowTraceService;
    private final RequestCache requestCache = new HttpSessionRequestCache();

    public ToolOAuthAuthenticationSuccessHandler(
            PingOneClientProperties properties,
            ToolSessionClientRegistrationStore sessionStore,
            PingOneFlowTraceService flowTraceService) {
        this.properties = properties;
        this.sessionStore = sessionStore;
        this.flowTraceService = flowTraceService;
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
            flowTraceService.record(
                    "login",
                    FlowActor.PINGONE,
                    FlowActor.TEST_CLIENT,
                    "OAuth callback succeeded",
                    "Authenticated principal " + authentication.getName() + " — redirecting to tool",
                    "success");
            log.info("Tool OAuth login succeeded for principal '{}'", authentication.getName());
        } else {
            flowTraceService.record(
                    "login",
                    FlowActor.PINGONE,
                    FlowActor.TEST_CLIENT,
                    "OAuth callback succeeded",
                    "Authenticated principal " + authentication.getName(),
                    "success");
            setDefaultTargetUrl(properties.getUi().getPostLoginPath());
            log.info("Application OAuth login succeeded for principal '{}'", authentication.getName());
        }
        super.onAuthenticationSuccess(request, response, authentication);
    }
}
