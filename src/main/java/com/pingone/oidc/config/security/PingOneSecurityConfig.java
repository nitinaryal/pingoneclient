package com.pingone.oidc.config.security;

import com.pingone.oidc.config.properties.PingOneClientProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class PingOneSecurityConfig {

    private final PingOneClientProperties properties;
    private final PingOneSecurityConfigurerFactory configurerFactory;

    public PingOneSecurityConfig(
            PingOneClientProperties properties, PingOneSecurityConfigurerFactory configurerFactory) {
        this.properties = properties;
        this.configurerFactory = configurerFactory;
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http, ClientRegistrationRepository clientRegistrationRepository) throws Exception {

        String[] publicPaths = properties.getSecurity().getPublicPaths().toArray(String[]::new);

        http.authorizeHttpRequests(auth -> auth.requestMatchers(publicPaths).permitAll().anyRequest().authenticated());

        http.csrf(csrf -> csrf.ignoringRequestMatchers("/mock/**"));

        configurerFactory.resolve().configure(http, properties, clientRegistrationRepository);

        return http.build();
    }
}
