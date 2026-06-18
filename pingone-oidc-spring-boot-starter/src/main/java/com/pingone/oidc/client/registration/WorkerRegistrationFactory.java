package com.pingone.oidc.client.registration;

import com.pingone.oidc.config.properties.PingOneApplicationType;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class WorkerRegistrationFactory implements PingOneRegistrationFactory {

    @Override
    public PingOneApplicationType supportedType() {
        return PingOneApplicationType.WORKER;
    }

    @Override
    public ClientRegistration build(RegistrationValueSource request) {
        String registrationId = request.value("registrationId", "pingone-worker");
        ClientRegistration.Builder builder = ClientRegistration.withRegistrationId(registrationId)
                .clientId(RegistrationFactorySupport.require(request, "clientId"))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .scope(RegistrationFactorySupport.parseScopes(request));

        String clientSecret = request.value("clientSecret", "");
        if (StringUtils.hasText(clientSecret)) {
            builder.clientSecret(clientSecret);
        }

        RegistrationFactorySupport.applyProviderEndpoints(builder, request);
        return builder.build();
    }
}
