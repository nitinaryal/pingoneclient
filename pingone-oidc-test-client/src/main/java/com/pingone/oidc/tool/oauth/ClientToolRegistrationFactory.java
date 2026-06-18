package com.pingone.oidc.tool.oauth;

import com.pingone.oidc.client.PingOneClientIntegrationRegistry;
import com.pingone.oidc.tool.model.ClientToolConfigRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.stereotype.Component;

@Component
public class ClientToolRegistrationFactory {

    private final PingOneClientIntegrationRegistry integrationRegistry;

    public ClientToolRegistrationFactory(PingOneClientIntegrationRegistry integrationRegistry) {
        this.integrationRegistry = integrationRegistry;
    }

    public ClientRegistration build(ClientToolConfigRequest request) {
        return integrationRegistry.buildRegistration(request);
    }
}
