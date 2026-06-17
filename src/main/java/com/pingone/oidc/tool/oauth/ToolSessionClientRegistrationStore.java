package com.pingone.oidc.tool.oauth;

import jakarta.servlet.http.HttpSession;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class ToolSessionClientRegistrationStore {

    public void save(ClientRegistration registration, boolean toolInitiatedLogin) {
        HttpSession session = currentSession();
        session.setAttribute(sessionKey(registration.getRegistrationId()), registration);
        if (toolInitiatedLogin) {
            session.setAttribute(ToolSessionOAuthAttributes.TOOL_INITIATED_LOGIN, Boolean.TRUE);
        }
    }

    public ClientRegistration findByRegistrationId(String registrationId) {
        HttpSession session = currentSessionOrNull();
        if (session == null) {
            return null;
        }
        Object value = session.getAttribute(sessionKey(registrationId));
        if (value instanceof ClientRegistration registration) {
            return registration;
        }
        return null;
    }

    public boolean isToolInitiatedLogin(HttpSession session) {
        return Boolean.TRUE.equals(session.getAttribute(ToolSessionOAuthAttributes.TOOL_INITIATED_LOGIN));
    }

    public void clearToolInitiatedLoginFlag(HttpSession session) {
        session.removeAttribute(ToolSessionOAuthAttributes.TOOL_INITIATED_LOGIN);
    }

    private static String sessionKey(String registrationId) {
        return ToolSessionOAuthAttributes.SESSION_REGISTRATION_PREFIX + registrationId;
    }

    private static HttpSession currentSession() {
        HttpSession session = currentSessionOrNull();
        if (session == null) {
            throw new IllegalStateException("No HTTP session available for tool OAuth configuration");
        }
        return session;
    }

    private static HttpSession currentSessionOrNull() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        return attributes.getRequest().getSession(true);
    }
}
