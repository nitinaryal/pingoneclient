package com.pingone.oidc.client.registration;

import com.pingone.oidc.config.properties.PingOneApplicationType;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class OidcWebAppRegistrationFactory implements PingOneRegistrationFactory {

    @Override
    public PingOneApplicationType supportedType() {
        return PingOneApplicationType.OIDC_WEB_APP;
    }

    @Override
    public ClientRegistration build(RegistrationValueSource request) {
        String registrationId = request.value("registrationId", "pingone");
        ClientRegistration.Builder builder = ClientRegistration.withRegistrationId(registrationId)
                .clientId(RegistrationFactorySupport.require(request, "clientId"))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(RegistrationFactorySupport.require(request, "redirectUri"))
                .scope(RegistrationFactorySupport.parseScopes(request))
                .userNameAttributeName(IdTokenClaimNames.SUB);

        String clientSecret = request.value("clientSecret", "");
        if (StringUtils.hasText(clientSecret)) {
            builder.clientSecret(clientSecret);
        }

        RegistrationFactorySupport.applyProviderEndpoints(builder, request);
        return builder.build();
    }
}
