package com.pingone.oidc.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.pingone.oidc.service.PingOneMetadataService;
import com.pingone.oidc.support.OAuth2TestClientRegistration;
import com.pingone.oidc.support.OAuth2TestRequestPostProcessors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class OAuthControllerTest {

    private static final String DISCOVERY_JSON =
            "{\"issuer\":\"https://auth.example.com/test-env/as\",\"jwks_uri\":\"https://auth.example.com/as/jwks\"}";
    private static final String JWKS_JSON = "{\"keys\":[{\"kty\":\"RSA\",\"kid\":\"test-key\"}]}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OAuth2AuthorizedClientService authorizedClientService;

    @MockitoBean
    private PingOneMetadataService metadataService;

    @Test
    void meDisplaysIdTokenClaims() throws Exception {
        OidcUser oidcUser = OAuth2TestClientRegistration.oidcUser();

        mockMvc.perform(get("/me")
                        .with(OAuth2TestRequestPostProcessors.oidcAuthentication(oidcUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("me"))
                .andExpect(model().attribute("principalName", "user-123"))
                .andExpect(model().attributeExists("claims"))
                .andExpect(model().attribute("claims", org.hamcrest.Matchers.hasEntry("sub", "user-123")))
                .andExpect(model().attribute("claims", org.hamcrest.Matchers.hasEntry("email", "user@example.com")))
                .andExpect(model().attribute("claims", org.hamcrest.Matchers.hasEntry("name", "Test User")))
                .andExpect(model().attribute("claims", org.hamcrest.Matchers.hasEntry("iss", OAuth2TestClientRegistration.ISSUER_URI)))
                .andExpect(model().attribute("claims", org.hamcrest.Matchers.hasEntry("aud", "test-client-id")))
                .andExpect(model().attribute("claims", org.hamcrest.Matchers.hasKey("exp")));
    }

    @Test
    void tokenDisplaysMaskedAccessToken() throws Exception {
        OidcUser oidcUser = OAuth2TestClientRegistration.oidcUser();
        OAuth2AuthenticationToken authentication = new OAuth2AuthenticationToken(
                oidcUser,
                oidcUser.getAuthorities(),
                OAuth2TestClientRegistration.REGISTRATION_ID);
        OAuth2AuthorizedClient authorizedClient = new OAuth2AuthorizedClient(
                OAuth2TestClientRegistration.pingone(),
                oidcUser.getName(),
                OAuth2TestClientRegistration.accessToken());
        authorizedClientService.saveAuthorizedClient(authorizedClient, authentication);

        mockMvc.perform(get("/token")
                        .with(OAuth2TestRequestPostProcessors.oidcAuthentication(oidcUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("token"))
                .andExpect(model().attribute("maskedAccessToken", "abcdefgh...6789"))
                .andExpect(model().attribute("tokenType", "Bearer"))
                .andExpect(model().attributeExists("expiresAt"))
                .andExpect(model().attributeExists("scopes"));
    }

    @Test
    void jwksFetchesAndDisplaysProviderKeys() throws Exception {
        when(metadataService.resolveJwksUri()).thenReturn(OAuth2TestClientRegistration.JWKS_URI);
        when(metadataService.fetchJwks()).thenReturn(JWKS_JSON);

        mockMvc.perform(get("/jwks")
                        .with(OAuth2TestRequestPostProcessors.oidcAuthentication(
                                OAuth2TestClientRegistration.oidcUser())))
                .andExpect(status().isOk())
                .andExpect(view().name("jwks"))
                .andExpect(model().attribute("jwksUri", OAuth2TestClientRegistration.JWKS_URI))
                .andExpect(model().attribute("jwksJson", JWKS_JSON));
    }

    @Test
    void metadataFetchesAndDisplaysDiscoveryDocument() throws Exception {
        when(metadataService.fetchDiscoveryDocument()).thenReturn(DISCOVERY_JSON);

        mockMvc.perform(get("/metadata")
                        .with(OAuth2TestRequestPostProcessors.oidcAuthentication(
                                OAuth2TestClientRegistration.oidcUser())))
                .andExpect(status().isOk())
                .andExpect(view().name("metadata"))
                .andExpect(model().attribute("metadataJson", DISCOVERY_JSON));
    }
}
