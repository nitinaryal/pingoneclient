package com.pingone.oidc.tool.oauth;

import com.pingone.oidc.tool.ToolWizardConfigService;
import com.pingone.oidc.tool.session.ToolWizardSessionStore;
import com.pingone.oidc.tool.model.ClientToolConfigRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.stereotype.Service;

@Service
public class ToolOAuthService {

    private static final Logger log = LoggerFactory.getLogger(ToolOAuthService.class);

    private final ClientToolRegistrationFactory registrationFactory;
    private final ToolWizardConfigService wizardConfigService;
    private final ToolWizardSessionStore wizardSessionStore;

    public ToolOAuthService(
            ClientToolRegistrationFactory registrationFactory,
            ToolWizardConfigService wizardConfigService,
            ToolWizardSessionStore wizardSessionStore) {
        this.registrationFactory = registrationFactory;
        this.wizardConfigService = wizardConfigService;
        this.wizardSessionStore = wizardSessionStore;
    }

    public Map<String, Object> prepareLogin(ClientToolConfigRequest request) {
        ClientToolConfigRequest saved = wizardConfigService.persistSession(request, true);
        ClientRegistration registration = registrationFactory.build(saved);
        String loginPath = "/oauth2/authorization/" + registration.getRegistrationId();
        log.info(
                "Tool login prepared for registration '{}' (clientId={})",
                registration.getRegistrationId(),
                registration.getClientId());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("loginPath", loginPath);
        result.put("registrationId", registration.getRegistrationId());
        result.put("clientId", registration.getClientId());
        result.put("redirectUri", registration.getRedirectUri());
        result.put("issuerUri", registration.getProviderDetails().getIssuerUri());
        result.put("usingWizardConfig", true);
        result.put("sessionSaved", true);
        result.put(
                "message",
                "Redirecting to PingOne authorization using wizard Client & Application Settings.");
        return result;
    }

    public Map<String, Object> prepareLogout() {
        wizardSessionStore.setToolInitiatedLogout(true);
        log.info("Tool logout prepared; user will return to /tool after PingOne end session");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("logoutPath", "/logout");
        result.put(
                "message",
                "Local session will be cleared and PingOne end session will run (same flow as dashboard logout).");
        return result;
    }
}
