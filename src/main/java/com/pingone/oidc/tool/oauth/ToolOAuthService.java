package com.pingone.oidc.tool.oauth;

import com.pingone.oidc.tool.ToolWizardConfigService;
import com.pingone.oidc.tool.model.ClientToolConfigRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.stereotype.Service;

@Service
public class ToolOAuthService {

    private final ClientToolRegistrationFactory registrationFactory;
    private final ToolWizardConfigService wizardConfigService;

    public ToolOAuthService(
            ClientToolRegistrationFactory registrationFactory, ToolWizardConfigService wizardConfigService) {
        this.registrationFactory = registrationFactory;
        this.wizardConfigService = wizardConfigService;
    }

    public Map<String, Object> prepareLogin(ClientToolConfigRequest request) {
        ClientToolConfigRequest saved = wizardConfigService.persistSession(request, true);
        ClientRegistration registration = registrationFactory.build(saved);
        String loginPath = "/oauth2/authorization/" + registration.getRegistrationId();
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
                "OAuth login will use the wizard Client & Application Settings stored in your encrypted session.");
        return result;
    }
}
