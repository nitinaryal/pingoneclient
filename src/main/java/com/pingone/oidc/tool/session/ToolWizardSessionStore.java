package com.pingone.oidc.tool.session;

import com.pingone.oidc.tool.model.ClientToolConfigRequest;
import com.pingone.oidc.tool.oauth.ToolSessionOAuthAttributes;
import jakarta.servlet.http.HttpSession;
import java.util.Optional;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Component
public class ToolWizardSessionStore {

    static final String ENCRYPTED_CONFIG_ATTRIBUTE = "pingone.tool.wizard.encrypted-config";

    private final TextEncryptor encryptor;
    private final JsonMapper jsonMapper;

    public ToolWizardSessionStore(TextEncryptor toolWizardSessionEncryptor) {
        this.encryptor = toolWizardSessionEncryptor;
        this.jsonMapper = JsonMapper.builder().build();
    }

    public void save(ClientToolConfigRequest request) {
        save(request, false);
    }

    public void save(ClientToolConfigRequest request, boolean toolInitiatedLogin) {
        HttpSession session = currentSession();
        try {
            String json = jsonMapper.writeValueAsString(request);
            session.setAttribute(ENCRYPTED_CONFIG_ATTRIBUTE, encryptor.encrypt(json));
            if (toolInitiatedLogin) {
                session.setAttribute(ToolSessionOAuthAttributes.TOOL_INITIATED_LOGIN, Boolean.TRUE);
            }
        } catch (JacksonException ex) {
            throw new IllegalStateException("Failed to persist wizard configuration", ex);
        }
    }

    public Optional<ClientToolConfigRequest> load() {
        HttpSession session = currentSessionOrNull();
        if (session == null) {
            return Optional.empty();
        }
        Object encrypted = session.getAttribute(ENCRYPTED_CONFIG_ATTRIBUTE);
        if (!(encrypted instanceof String ciphertext) || !StringUtils.hasText(ciphertext)) {
            return Optional.empty();
        }
        try {
            String json = encryptor.decrypt(ciphertext);
            return Optional.of(jsonMapper.readValue(json, ClientToolConfigRequest.class));
        } catch (Exception ex) {
            session.removeAttribute(ENCRYPTED_CONFIG_ATTRIBUTE);
            throw new IllegalStateException("Stored wizard configuration could not be decrypted", ex);
        }
    }

    public boolean isToolInitiatedLogin(HttpSession session) {
        return Boolean.TRUE.equals(session.getAttribute(ToolSessionOAuthAttributes.TOOL_INITIATED_LOGIN));
    }

    public void setToolInitiatedLogout(boolean toolInitiatedLogout) {
        HttpSession session = currentSession();
        if (toolInitiatedLogout) {
            session.setAttribute(ToolSessionOAuthAttributes.TOOL_INITIATED_LOGOUT, Boolean.TRUE);
        } else {
            session.removeAttribute(ToolSessionOAuthAttributes.TOOL_INITIATED_LOGOUT);
        }
    }

    public boolean isToolInitiatedLogout(HttpSession session) {
        return Boolean.TRUE.equals(session.getAttribute(ToolSessionOAuthAttributes.TOOL_INITIATED_LOGOUT));
    }

    public void clearToolInitiatedLogoutFlag(HttpSession session) {
        session.removeAttribute(ToolSessionOAuthAttributes.TOOL_INITIATED_LOGOUT);
    }

    public void clearToolInitiatedLoginFlag(HttpSession session) {
        session.removeAttribute(ToolSessionOAuthAttributes.TOOL_INITIATED_LOGIN);
    }

    public void clear() {
        HttpSession session = currentSessionOrNull();
        if (session != null) {
            session.removeAttribute(ENCRYPTED_CONFIG_ATTRIBUTE);
            session.removeAttribute(ToolSessionOAuthAttributes.TOOL_INITIATED_LOGIN);
            session.removeAttribute(ToolSessionOAuthAttributes.TOOL_INITIATED_LOGOUT);
        }
    }

    private static HttpSession currentSession() {
        HttpSession session = currentSessionOrNull();
        if (session == null) {
            throw new IllegalStateException("No HTTP session available for wizard configuration");
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
