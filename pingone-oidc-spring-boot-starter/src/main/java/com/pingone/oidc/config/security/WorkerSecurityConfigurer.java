package com.pingone.oidc.config.security;

import com.pingone.oidc.config.properties.PingOneApplicationType;
import com.pingone.oidc.config.properties.PingOneClientProperties;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Component;

@Component
public class WorkerSecurityConfigurer implements PingOneSecurityConfigurer {

    @Override
    public PingOneApplicationType supportedType() {
        return PingOneApplicationType.WORKER;
    }

    @Override
    public void configure(
            HttpSecurity http,
            PingOneClientProperties properties,
            ClientRegistrationRepository clientRegistrationRepository) throws Exception {
        http.oauth2Client(Customizer.withDefaults());
    }
}
