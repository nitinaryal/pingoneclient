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
import org.springframework.test.web.servlet.MockMvc;

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
                .andExpect(jsonPath("$.fieldValues.tokenUri").value("https://auth.pingone.com/env/as/token"));
    }
}
