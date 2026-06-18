package com.pingone.oidc.tool.oauth;

import com.pingone.oidc.tool.model.ToolOAuthLoginError;
import jakarta.servlet.http.HttpSession;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class ToolOAuthLoginErrorStore {

    static final String LAST_LOGIN_ERROR_ATTRIBUTE = "pingone.tool.oauth.last-login-error";

    public void save(ToolOAuthLoginError error) {
        currentSession().setAttribute(LAST_LOGIN_ERROR_ATTRIBUTE, error);
    }

    public Optional<ToolOAuthLoginError> consume() {
        HttpSession session = currentSessionOrNull();
        if (session == null) {
            return Optional.empty();
        }
        Object value = session.getAttribute(LAST_LOGIN_ERROR_ATTRIBUTE);
        session.removeAttribute(LAST_LOGIN_ERROR_ATTRIBUTE);
        if (value instanceof ToolOAuthLoginError error) {
            return Optional.of(error);
        }
        return Optional.empty();
    }

    private static HttpSession currentSession() {
        HttpSession session = currentSessionOrNull();
        if (session == null) {
            throw new IllegalStateException("No HTTP session available for OAuth login error");
        }
        return session;
    }

    private static HttpSession currentSessionOrNull() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        return attributes.getRequest().getSession(false);
    }
}
