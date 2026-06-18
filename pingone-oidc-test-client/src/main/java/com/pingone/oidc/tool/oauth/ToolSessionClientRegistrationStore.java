package com.pingone.oidc.tool.oauth;

import com.pingone.oidc.tool.session.ToolWizardSessionStore;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.stereotype.Component;

@Component
public class ToolSessionClientRegistrationStore {

    private final ToolWizardSessionStore wizardSessionStore;
    private final ClientToolRegistrationFactory registrationFactory;

    public ToolSessionClientRegistrationStore(
            ToolWizardSessionStore wizardSessionStore, ClientToolRegistrationFactory registrationFactory) {
        this.wizardSessionStore = wizardSessionStore;
        this.registrationFactory = registrationFactory;
    }

    public ClientRegistration findByRegistrationId(String registrationId) {
        return wizardSessionStore
                .load()
                .filter(request -> registrationId.equals(request.value("registrationId", "pingone")))
                .map(registrationFactory::build)
                .orElse(null);
    }

    public boolean isToolInitiatedLogin(HttpSession session) {
        return wizardSessionStore.isToolInitiatedLogin(session);
    }

    public void clearToolInitiatedLoginFlag(HttpSession session) {
        wizardSessionStore.clearToolInitiatedLoginFlag(session);
    }
}
