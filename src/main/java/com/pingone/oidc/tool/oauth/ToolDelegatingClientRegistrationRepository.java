package com.pingone.oidc.tool.oauth;

import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

public class ToolDelegatingClientRegistrationRepository implements ClientRegistrationRepository {

    private final ToolSessionClientRegistrationStore sessionStore;
    private final RuntimeClientRegistrationRepositoryProvider runtimeRepositoryProvider;
    private volatile ClientRegistrationRepository delegate;

    public ToolDelegatingClientRegistrationRepository(
            ToolSessionClientRegistrationStore sessionStore,
            RuntimeClientRegistrationRepositoryProvider runtimeRepositoryProvider) {
        this.sessionStore = sessionStore;
        this.runtimeRepositoryProvider = runtimeRepositoryProvider;
    }

    @Override
    public ClientRegistration findByRegistrationId(String registrationId) {
        ClientRegistration sessionRegistration = sessionStore.findByRegistrationId(registrationId);
        if (sessionRegistration != null) {
            return sessionRegistration;
        }
        return delegate().findByRegistrationId(registrationId);
    }

    private ClientRegistrationRepository delegate() {
        ClientRegistrationRepository cached = delegate;
        if (cached == null) {
            cached = runtimeRepositoryProvider.resolve();
            delegate = cached;
        }
        return cached;
    }
}
