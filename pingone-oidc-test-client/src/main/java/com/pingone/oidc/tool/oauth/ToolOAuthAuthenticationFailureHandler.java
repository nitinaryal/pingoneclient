package com.pingone.oidc.tool.oauth;

import com.pingone.oidc.config.properties.PingOneClientProperties;
import com.pingone.oidc.tool.model.ToolOAuthLoginError;
import com.pingone.oidc.tool.trace.FlowActor;
import com.pingone.oidc.tool.trace.PingOneFlowTraceService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Component
public class ToolOAuthAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private static final Logger log = LoggerFactory.getLogger(ToolOAuthAuthenticationFailureHandler.class);

    private final PingOneClientProperties properties;
    private final ToolSessionClientRegistrationStore sessionStore;
    private final ToolOAuthErrorMapper errorMapper;
    private final ToolOAuthLoginErrorStore loginErrorStore;
    private final PingOneFlowTraceService flowTraceService;

    public ToolOAuthAuthenticationFailureHandler(
            PingOneClientProperties properties,
            ToolSessionClientRegistrationStore sessionStore,
            ToolOAuthErrorMapper errorMapper,
            ToolOAuthLoginErrorStore loginErrorStore,
            PingOneFlowTraceService flowTraceService) {
        this.properties = properties;
        this.sessionStore = sessionStore;
        this.errorMapper = errorMapper;
        this.loginErrorStore = loginErrorStore;
        this.flowTraceService = flowTraceService;
        setDefaultFailureUrl("/?login_error=1");
    }

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
            throws IOException, ServletException {
        ToolOAuthLoginError error = errorMapper.mapCallback(exception);
        flowTraceService.record(
                "login",
                FlowActor.PINGONE,
                FlowActor.TEST_CLIENT,
                "OAuth callback failed",
                error.userMessage() + " (" + error.errorCode() + ")",
                "error");
        log.warn(
                "OAuth login failed during {}: {} - {}",
                error.phase(),
                error.errorCode(),
                error.technicalDetail());

        HttpSession session = request.getSession(false);
        if (session != null && sessionStore.isToolInitiatedLogin(session)) {
            sessionStore.clearToolInitiatedLoginFlag(session);
            loginErrorStore.save(error);
            getRedirectStrategy().sendRedirect(request, response, "/tool?oauth=error#tests");
            return;
        }

        loginErrorStore.save(error);
        getRedirectStrategy().sendRedirect(request, response, properties.getUi().getHomePath() + "?login_error=1");
    }
}
