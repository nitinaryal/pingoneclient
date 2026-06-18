package com.pingone.oidc.config;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class ClientToolControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void toolPageIsPublic() throws Exception {
        mockMvc.perform(get("/tool"))
                .andExpect(status().isOk())
                .andExpect(view().name("tool/index"))
                .andExpect(content().string(containsString("tool-catalog-json")))
                .andExpect(content().string(containsString("oidc-web-app")));
    }

    @Test
    void toolJavaScriptIsPublic() throws Exception {
        mockMvc.perform(get("/js/client-tool.js"))
                .andExpect(status().isOk());
    }

    @Test
    void toolCatalogApiIsPublic() throws Exception {
        mockMvc.perform(get("/tool/api/catalog"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].configValue").exists())
                .andExpect(jsonPath("$[0].configFields[?(@.key=='authorizationUri')]").exists());
    }

    @Test
    void toolOAuthLoginUsesWizardConfig() throws Exception {
        String body =
                """
                {
                  "applicationType":"oidc-web-app",
                  "values":{
                    "registrationId":"acme",
                    "clientId":"wizard-client",
                    "clientSecret":"wizard-secret",
                    "redirectUri":"http://localhost:8080/login/oauth2/code/acme",
                    "issuerUri":"https://auth.example.com/acme/as"
                  }
                }
                """;
        mockMvc.perform(post("/tool/api/oauth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loginPath").value("/oauth2/authorization/acme"))
                .andExpect(jsonPath("$.usingWizardConfig").value(true))
                .andExpect(jsonPath("$.clientId").value("wizard-client"));
    }

    @Test
    void discoveryApplyMapsJson() throws Exception {
        String json =
                """
                {"issuer":"https://auth.pingone.com/env/as","token_endpoint":"https://auth.pingone.com/env/as/token"}
                """;
        mockMvc.perform(post("/tool/api/discovery/apply")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fieldValues.issuerUri").value("https://auth.pingone.com/env/as"))
                .andExpect(jsonPath("$.fieldValues.tokenUri").value("https://auth.pingone.com/env/as/token"))
                .andExpect(jsonPath("$.sessionSaved").value(true));
    }

    @Test
    void toolOAuthLogoutPrepareSetsReturnToTool() throws Exception {
        mockMvc.perform(post("/tool/api/oauth/logout/prepare")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.logoutPath").value("/logout"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void authStatusEndpointReportsAnonymousByDefault() throws Exception {
        mockMvc.perform(get("/tool/api/auth/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(false));
    }

    @Test
    void sessionPersistAndLoadRoundTrip() throws Exception {
        String body =
                """
                {
                  "applicationType":"oidc-web-app",
                  "values":{
                    "registrationId":"acme",
                    "clientId":"session-client",
                    "clientSecret":"session-secret",
                    "issuerUri":"https://auth.example.com/acme/as"
                  }
                }
                """;
        MvcResult persistResult = mockMvc.perform(post("/tool/api/session/persist")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saved").value(true))
                .andExpect(jsonPath("$.values.clientId").value("session-client"))
                .andExpect(jsonPath("$.sensitiveFields").isArray())
                .andReturn();

        MockHttpSession session = (MockHttpSession) persistResult.getRequest().getSession(false);

        mockMvc.perform(get("/tool/api/session/config").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saved").value(true))
                .andExpect(jsonPath("$.values.clientId").value("session-client"))
                .andExpect(jsonPath("$.values.clientSecret").value("session-secret"));
    }

    @Test
    void flowTraceApiRecordsAndClearsEvents() throws Exception {
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(get("/tool/api/flow/trace").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events").isArray())
                .andExpect(jsonPath("$.mermaidSequence").exists());

        mockMvc.perform(post("/tool/api/flow/client-event")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"flowName":"login","label":"Browser click","message":"User started login","level":"info"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events.length()").value(1))
                .andExpect(jsonPath("$.events[0].label").value("Browser click"))
                .andExpect(jsonPath("$.activeFlowName").value("login"));

        mockMvc.perform(post("/tool/api/flow/clear")
                        .with(csrf())
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cleared").value(true));

        mockMvc.perform(get("/tool/api/flow/trace").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events.length()").value(0));
    }
}
