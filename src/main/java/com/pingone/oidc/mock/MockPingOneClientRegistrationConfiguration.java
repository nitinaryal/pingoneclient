package com.pingone.oidc.mock;

import com.pingone.oidc.config.properties.PingOneClientProperties;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;

@Configuration
@ConditionalOnProperty(name = "mock", havingValue = "true")
public class MockPingOneClientRegistrationConfiguration {

    @Bean
    @Primary
    ClientRegistrationRepository mockClientRegistrationRepository(
            PingOneClientProperties properties, Environment environment) {
        return registrationId -> {
            if (!properties.getRegistrationId().equals(registrationId)) {
                return null;
            }
            return buildRegistration(properties, resolveServerPort(environment));
        };
    }

    static ClientRegistration buildRegistration(PingOneClientProperties properties, int serverPort) {
        PingOneClientProperties.Mock mock = properties.getMock();
        String issuer = "http://localhost:" + serverPort + mock.getBasePath();
        String redirectUri =
                "http://localhost:" + serverPort + "/login/oauth2/code/" + properties.getRegistrationId();

        return ClientRegistration.withRegistrationId(properties.getRegistrationId())
                .clientId(mock.getClientId())
                .clientSecret(mock.getClientSecret())
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(redirectUri)
                .scope("openid", "profile", "email")
                .authorizationUri(issuer + "/authorize")
                .tokenUri(issuer + "/token")
                .userInfoUri(issuer + "/userinfo")
                .jwkSetUri(issuer + "/jwks")
                .issuerUri(issuer)
                .userNameAttributeName(IdTokenClaimNames.SUB)
                .providerConfigurationMetadata(Map.of("end_session_endpoint", issuer + "/signoff"))
                .build();
    }

    private static int resolveServerPort(Environment environment) {
        Integer localServerPort = environment.getProperty("local.server.port", Integer.class);
        if (localServerPort != null && localServerPort > 0) {
            return localServerPort;
        }
        return environment.getProperty("server.port", Integer.class, 8080);
    }
}
