package com.pingone.oidc.service;

import com.pingone.oidc.config.properties.PingOneClientProperties;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class PingOneMetadataService {

    private final WebClient webClient;
    private final PingOneClientProperties properties;
    private final ClientRegistrationRepository clientRegistrationRepository;

    public PingOneMetadataService(
            WebClient webClient,
            PingOneClientProperties properties,
            ClientRegistrationRepository clientRegistrationRepository) {
        this.webClient = webClient;
        this.properties = properties;
        this.clientRegistrationRepository = clientRegistrationRepository;
    }

    public String fetchDiscoveryDocument() {
        String issuerUri = pingOneRegistration().getProviderDetails().getIssuerUri();
        if (!StringUtils.hasText(issuerUri)) {
            throw new IllegalStateException(
                    "issuer-uri is not configured. Set spring.security.oauth2.client.provider."
                            + properties.getProviderId()
                            + ".issuer-uri");
        }
        String discoveryPath = properties.getMetadata().getDiscoveryDocumentPath();
        String discoveryUri = issuerUri.endsWith("/")
                ? issuerUri + discoveryPath.substring(1)
                : issuerUri + discoveryPath;

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
        String configuredJwksUri = properties.getMetadata().getJwksUriOverride();
        if (StringUtils.hasText(configuredJwksUri)) {
            return configuredJwksUri;
        }
        String jwkSetUri = pingOneRegistration().getProviderDetails().getJwkSetUri();
        if (!StringUtils.hasText(jwkSetUri)) {
            throw new IllegalStateException(
                    "JWKS URI is not configured. Set issuer-uri, jwk-set-uri, or pingone.metadata.jwks-uri-override");
        }
        return jwkSetUri;
    }

    private ClientRegistration pingOneRegistration() {
        String registrationId = properties.getRegistrationId();
        ClientRegistration registration = clientRegistrationRepository.findByRegistrationId(registrationId);
        if (registration == null) {
            throw new IllegalStateException("OAuth2 client registration '" + registrationId + "' was not found");
        }
        return registration;
    }
}
