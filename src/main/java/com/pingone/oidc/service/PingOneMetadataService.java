package com.pingone.oidc.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class PingOneMetadataService {

    private static final String REGISTRATION_ID = "pingone";

    private final WebClient webClient;
    private final ClientRegistrationRepository clientRegistrationRepository;

    @Value("${pingone.jwks-uri:}")
    private String configuredJwksUri;

    public PingOneMetadataService(
            WebClient webClient,
            ClientRegistrationRepository clientRegistrationRepository) {
        this.webClient = webClient;
        this.clientRegistrationRepository = clientRegistrationRepository;
    }

    public String fetchDiscoveryDocument() {
        String issuerUri = pingOneRegistration().getProviderDetails().getIssuerUri();
        if (!StringUtils.hasText(issuerUri)) {
            throw new IllegalStateException(
                    "issuer-uri is not configured. Set spring.security.oauth2.client.provider.pingone.issuer-uri");
        }
        String discoveryUri = issuerUri.endsWith("/")
                ? issuerUri + ".well-known/openid-configuration"
                : issuerUri + "/.well-known/openid-configuration";

        return webClient.get()
                .uri(discoveryUri)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    public String fetchJwks() {
        String jwksUri = resolveJwksUri();
        return webClient.get()
                .uri(jwksUri)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    public String resolveJwksUri() {
        if (StringUtils.hasText(configuredJwksUri)) {
            return configuredJwksUri;
        }
        String jwkSetUri = pingOneRegistration().getProviderDetails().getJwkSetUri();
        if (!StringUtils.hasText(jwkSetUri)) {
            throw new IllegalStateException(
                    "JWKS URI is not configured. Set issuer-uri, jwk-set-uri, or pingone.jwks-uri");
        }
        return jwkSetUri;
    }

    private ClientRegistration pingOneRegistration() {
        ClientRegistration registration = clientRegistrationRepository.findByRegistrationId(REGISTRATION_ID);
        if (registration == null) {
            throw new IllegalStateException("OAuth2 client registration 'pingone' was not found");
        }
        return registration;
    }
}
