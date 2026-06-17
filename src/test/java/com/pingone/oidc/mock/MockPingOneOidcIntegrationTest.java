package com.pingone.oidc.mock;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@SpringBootTest(properties = "mock=true", webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class MockPingOneOidcIntegrationTest {

  private static final String BASE_PATH = "/mock/pingone/as";
  private static final String CLIENT_ID = "mock-client-id";
  private static final String CLIENT_SECRET = "mock-client-secret";

  @Autowired
  private MockMvc mockMvc;

  @LocalServerPort
  private int port;

  private String redirectUri() {
    return "http://localhost:" + port + "/login/oauth2/code/pingone";
  }

  @Test
  void discoveryDocumentExposesOidcEndpoints() throws Exception {
    mockMvc.perform(get(BASE_PATH + "/.well-known/openid-configuration"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.issuer", containsString(BASE_PATH)))
        .andExpect(jsonPath("$.authorization_endpoint", containsString(BASE_PATH + "/authorize")))
        .andExpect(jsonPath("$.token_endpoint", containsString(BASE_PATH + "/token")))
        .andExpect(jsonPath("$.jwks_uri", containsString(BASE_PATH + "/jwks")))
        .andExpect(jsonPath("$.end_session_endpoint", containsString(BASE_PATH + "/signoff")));
  }

  @Test
  void jwksDocumentContainsRsaKey() throws Exception {
    mockMvc.perform(get(BASE_PATH + "/jwks"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.keys", hasSize(1)))
        .andExpect(jsonPath("$.keys[0].kty").value("RSA"))
        .andExpect(jsonPath("$.keys[0].kid").value("mock-pingone-key"));
  }

  @Test
  void authorizationCodeCanBeExchangedForTokens() throws Exception {
    mockMvc.perform(get(BASE_PATH + "/authorize")
            .param("client_id", CLIENT_ID)
            .param("redirect_uri", redirectUri())
            .param("response_type", "code")
            .param("scope", "openid profile email")
            .param("state", "test-state")
            .param("nonce", "test-nonce"))
        .andExpect(status().isOk())
        .andExpect(view().name("mock/authorize"))
        .andReturn();

    MvcResult callbackResult = mockMvc.perform(post(BASE_PATH + "/authorize")
            .param("client_id", CLIENT_ID)
            .param("redirect_uri", redirectUri())
            .param("response_type", "code")
            .param("scope", "openid profile email")
            .param("state", "test-state")
            .param("nonce", "test-nonce")
            .param("sub", "integration-user")
            .param("email", "integration@example.com")
            .param("name", "Integration User"))
        .andExpect(status().is3xxRedirection())
        .andReturn();

    String callbackUrl = callbackResult.getResponse().getRedirectedUrl();
    String code = extractQueryParam(callbackUrl, "code");

    String basicAuth = Base64.getEncoder()
        .encodeToString((CLIENT_ID + ":" + CLIENT_SECRET).getBytes(StandardCharsets.UTF_8));

    mockMvc.perform(post(BASE_PATH + "/token")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .header("Authorization", "Basic " + basicAuth)
            .param("grant_type", "authorization_code")
            .param("code", code)
            .param("redirect_uri", redirectUri()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.access_token", notNullValue()))
        .andExpect(jsonPath("$.id_token", notNullValue()))
        .andExpect(jsonPath("$.token_type").value("Bearer"));
  }

  @Test
  void oauth2LoginFlowCompletesWithMockProvider() throws Exception {
    MockHttpSession session = new MockHttpSession();

    MvcResult loginRedirect = mockMvc.perform(get("/oauth2/authorization/pingone").session(session))
        .andExpect(status().is3xxRedirection())
        .andReturn();

    String authorizeUrl = loginRedirect.getResponse().getRedirectedUrl();
    mockMvc.perform(get(authorizeUrl).session(session)).andExpect(status().isOk());

    String state = extractQueryParam(authorizeUrl, "state");
    String nonce = extractQueryParam(authorizeUrl, "nonce");
    String codeChallenge = extractQueryParam(authorizeUrl, "code_challenge");
    String codeChallengeMethod = extractQueryParam(authorizeUrl, "code_challenge_method");

    MvcResult callbackRedirect = mockMvc.perform(post(BASE_PATH + "/authorize")
            .session(session)
            .param("client_id", CLIENT_ID)
            .param("redirect_uri", redirectUri())
            .param("response_type", "code")
            .param("scope", "openid profile email")
            .param("state", state)
            .param("nonce", nonce)
            .param("code_challenge", codeChallenge)
            .param("code_challenge_method", codeChallengeMethod)
            .param("sub", "flow-user")
            .param("email", "flow@example.com")
            .param("name", "Flow User"))
        .andExpect(status().is3xxRedirection())
        .andReturn();

    mockMvc.perform(get(callbackRedirect.getResponse().getRedirectedUrl()).session(session))
        .andExpect(status().is3xxRedirection())
        .andExpect(header().string("Location", containsString("/dashboard")));
  }

  @Test
  void mockLogoutRedirectsToPostLogoutUri() throws Exception {
    mockMvc.perform(get(BASE_PATH + "/signoff").param("post_logout_redirect_uri", "http://localhost:" + port + "/"))
        .andExpect(status().is3xxRedirection())
        .andExpect(header().string("Location", "http://localhost:" + port + "/"));
  }

  private static String extractQueryParam(String url, String name) {
    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    int queryIndex = url.indexOf('?');
    if (queryIndex < 0) {
      return null;
    }
    for (String pair : url.substring(queryIndex + 1).split("&")) {
      String[] parts = pair.split("=", 2);
      if (parts.length == 2 && parts[0].equals(name)) {
        return URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
      }
    }
    return null;
  }
}
