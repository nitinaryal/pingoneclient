package com.pingone.oidc.tool.oauth;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Component;

@Component
public class RuntimeClientRegistrationRepositoryProvider {

    private final ClientRegistrationRepository mockClientRegistrationRepository;
    private final ClientRegistrationRepository testClientRegistrationRepository;
    private final ObjectProvider<ClientRegistrationRepository> clientRegistrationRepositories;

    public RuntimeClientRegistrationRepositoryProvider(
            @Autowired(required = false) @Qualifier("mockClientRegistrationRepository")
                    ClientRegistrationRepository mockClientRegistrationRepository,
            @Autowired(required = false) @Qualifier("testClientRegistrationRepository")
                    ClientRegistrationRepository testClientRegistrationRepository,
            ObjectProvider<ClientRegistrationRepository> clientRegistrationRepositories) {
        this.mockClientRegistrationRepository = mockClientRegistrationRepository;
        this.testClientRegistrationRepository = testClientRegistrationRepository;
        this.clientRegistrationRepositories = clientRegistrationRepositories;
    }

    public ClientRegistrationRepository resolve() {
        if (mockClientRegistrationRepository != null) {
            return mockClientRegistrationRepository;
        }
        if (testClientRegistrationRepository != null) {
            return testClientRegistrationRepository;
        }
        return clientRegistrationRepositories.orderedStream()
                .filter(repository -> !(repository instanceof ToolDelegatingClientRegistrationRepository))
                .findFirst()
                .orElseThrow(
                        () -> new IllegalStateException("No OAuth2 ClientRegistrationRepository is configured"));
    }
}
