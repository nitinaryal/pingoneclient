package com.pingone.oidc.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pingone.oidc.config.properties.PingOneClientProperties;
import com.pingone.oidc.support.OAuth2TestClientRegistration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class PingOneMetadataServiceTest {

    @Mock
    private WebClient webClient;

    @Mock
    private ClientRegistrationRepository clientRegistrationRepository;

    private PingOneClientProperties properties;
    private PingOneMetadataService metadataService;

    @BeforeEach
    void setUp() {
        properties = new PingOneClientProperties();
        properties.setRegistrationId(OAuth2TestClientRegistration.REGISTRATION_ID);
        metadataService = new PingOneMetadataService(webClient, properties, clientRegistrationRepository);
        lenient()
                .when(clientRegistrationRepository.findByRegistrationId(OAuth2TestClientRegistration.REGISTRATION_ID))
                .thenReturn(OAuth2TestClientRegistration.pingone());
    }

    @Test
    void resolveJwksUriUsesProviderJwkSetUri() {
        assertThat(metadataService.resolveJwksUri()).isEqualTo(OAuth2TestClientRegistration.JWKS_URI);
    }

    @Test
    void resolveJwksUriPrefersConfiguredOverride() {
        properties.getMetadata().setJwksUriOverride("https://override.example.com/jwks");

        assertThat(metadataService.resolveJwksUri()).isEqualTo("https://override.example.com/jwks");
    }

    @Test
    @SuppressWarnings("unchecked")
    void fetchDiscoveryDocumentCallsIssuerWellKnownEndpoint() {
        String discoveryJson = "{\"issuer\":\"https://auth.example.com/test-env/as\"}";
        WebClient.RequestHeadersUriSpec uriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri("https://auth.example.com/test-env/as/.well-known/openid-configuration"))
                .thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(discoveryJson));

        assertThat(metadataService.fetchDiscoveryDocument()).isEqualTo(discoveryJson);
        verify(uriSpec).uri("https://auth.example.com/test-env/as/.well-known/openid-configuration");
    }

    @Test
    @SuppressWarnings("unchecked")
    void fetchJwksCallsResolvedJwksUri() {
        String jwksJson = "{\"keys\":[]}";
        WebClient.RequestHeadersUriSpec uriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(OAuth2TestClientRegistration.JWKS_URI)).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(jwksJson));

        assertThat(metadataService.fetchJwks()).isEqualTo(jwksJson);
        verify(uriSpec).uri(OAuth2TestClientRegistration.JWKS_URI);
    }

    @Test
    void fetchDiscoveryDocumentFailsWhenIssuerUriMissing() {
        when(clientRegistrationRepository.findByRegistrationId(OAuth2TestClientRegistration.REGISTRATION_ID))
                .thenReturn(OAuth2TestClientRegistration.pingoneWithoutIssuer());

        assertThatThrownBy(metadataService::fetchDiscoveryDocument)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("issuer-uri is not configured");
    }

    @Test
    void resolveJwksUriFailsWhenNoUriConfigured() {
        when(clientRegistrationRepository.findByRegistrationId(OAuth2TestClientRegistration.REGISTRATION_ID))
                .thenReturn(OAuth2TestClientRegistration.pingoneWithoutJwks());

        assertThatThrownBy(metadataService::resolveJwksUri)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWKS URI is not configured");
    }

    @Test
    void failsWhenPingOneRegistrationMissing() {
        when(clientRegistrationRepository.findByRegistrationId(anyString())).thenReturn(null);

        assertThatThrownBy(metadataService::resolveJwksUri)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("OAuth2 client registration 'pingone' was not found");
    }
}
