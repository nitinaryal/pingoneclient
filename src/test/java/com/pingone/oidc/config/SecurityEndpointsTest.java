package com.pingone.oidc.config;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.hamcrest.Matchers.startsWith;

import com.pingone.oidc.support.OAuth2TestClientRegistration;
import com.pingone.oidc.support.OAuth2TestRequestPostProcessors;
import com.pingone.oidc.support.TestOAuth2ClientConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestOAuth2ClientConfig.class)
class SecurityEndpointsTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void homeIsPublic() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("home"));
    }

    @Test
    void dashboardRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void authenticatedUserCanAccessDashboard() throws Exception {
        mockMvc.perform(get("/dashboard")
                        .with(OAuth2TestRequestPostProcessors.oidcAuthentication(
                                OAuth2TestClientRegistration.oidcUser())))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"));
    }

    @Test
    void protectedOidcEndpointsRequireAuthentication() throws Exception {
        for (String path : new String[] {"/me", "/token", "/jwks", "/metadata"}) {
            mockMvc.perform(get(path))
                    .andExpect(status().is3xxRedirection());
        }
    }

    @Test
    void logoutRedirectsToPingOneEndSessionEndpoint() throws Exception {
        mockMvc.perform(post("/logout")
                        .with(csrf())
                        .with(oauth2Login()
                                .clientRegistration(OAuth2TestClientRegistration.pingone()))
                        .with(OAuth2TestRequestPostProcessors.oidcAuthentication(
                                OAuth2TestClientRegistration.oidcUser())))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", startsWith(OAuth2TestClientRegistration.END_SESSION_URI)));
    }

    @Test
    void logoutWithoutAuthenticationIsAllowed() throws Exception {
        mockMvc.perform(post("/logout").with(csrf()))
                .andExpect(unauthenticated());
    }
}
