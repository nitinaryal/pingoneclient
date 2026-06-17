package com.pingone.oidc.tool.oauth;

import com.pingone.oidc.tool.model.ClientToolConfigRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.stereotype.Service;

@Service
public class ToolOAuthService {

    private final ClientToolRegistrationFactory registrationFactory;
    private final ToolSessionClientRegistrationStore sessionStore;

    public ToolOAuthService(
            ClientToolRegistrationFactory registrationFactory,
            ToolSessionClientRegistrationStore sessionStore) {
        this.registrationFactory = registrationFactory;
        this.sessionStore = sessionStore;
    }

    public Map<String, Object> prepareLogin(ClientToolConfigRequest request) {
        ClientRegistration registration = registrationFactory.build(request);
        sessionStore.save(registration, true);
        String loginPath = "/oauth2/authorization/" + registration.getRegistrationId();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("loginPath", loginPath);
        result.put("registrationId", registration.getRegistrationId());
        result.put("clientId", registration.getClientId());
        result.put("redirectUri", registration.getRedirectUri());
        result.put("issuerUri", registration.getProviderDetails().getIssuerUri());
        result.put("usingWizardConfig", true);
        result.put(
                "message",
                "OAuth login will use the wizard Client & Application Settings from this browser session.");
        return result;
    }
}
