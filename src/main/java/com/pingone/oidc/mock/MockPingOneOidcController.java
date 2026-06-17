package com.pingone.oidc.mock;

import com.pingone.oidc.config.properties.PingOneClientProperties;
import com.pingone.oidc.mock.MockPingOneSessionStore.MockUserProfile;
import com.pingone.oidc.mock.MockPingOneTokenService.MockPingOneOAuthException;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

@Controller
@ConditionalOnProperty(name = "mock", havingValue = "true")
@RequestMapping("${pingone.mock.basePath:/mock/pingone/as}")
public class MockPingOneOidcController {

    private final PingOneClientProperties properties;
    private final ClientRegistrationRepository clientRegistrationRepository;
    private final MockPingOneDiscoveryService discoveryService;
    private final MockPingOneKeyPairHolder keyPairHolder;
    private final MockPingOneSessionStore sessionStore;
    private final MockPingOneTokenService tokenService;

    public MockPingOneOidcController(
            PingOneClientProperties properties,
            ClientRegistrationRepository clientRegistrationRepository,
            MockPingOneDiscoveryService discoveryService,
            MockPingOneKeyPairHolder keyPairHolder,
            MockPingOneSessionStore sessionStore,
            MockPingOneTokenService tokenService) {
        this.properties = properties;
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.discoveryService = discoveryService;
        this.keyPairHolder = keyPairHolder;
        this.sessionStore = sessionStore;
        this.tokenService = tokenService;
    }

    @GetMapping("/.well-known/openid-configuration")
    @ResponseBody
    public Map<String, Object> openIdConfiguration(HttpServletRequest request) {
        return discoveryService.discoveryDocument(request);
    }

    @GetMapping("/jwks")
    @ResponseBody
    public Map<String, Object> jwks() {
        return keyPairHolder.toJwksDocument();
    }

    @GetMapping("/authorize")
    public String authorizePage(
            @RequestParam("client_id") String clientId,
            @RequestParam("redirect_uri") String redirectUri,
            @RequestParam("response_type") String responseType,
            @RequestParam(value = "scope", required = false) String scope,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "nonce", required = false) String nonce,
            @RequestParam(value = "code_challenge", required = false) String codeChallenge,
            @RequestParam(value = "code_challenge_method", required = false) String codeChallengeMethod,
            Model model) {
        validateAuthorizeRequest(clientId, redirectUri, responseType);
        PingOneClientProperties.Mock mock = properties.getMock();
        model.addAttribute("clientId", clientId);
        model.addAttribute("redirectUri", redirectUri);
        model.addAttribute("responseType", responseType);
        model.addAttribute("scope", scope);
        model.addAttribute("state", state);
        model.addAttribute("nonce", nonce);
        model.addAttribute("codeChallenge", codeChallenge);
        model.addAttribute("codeChallengeMethod", codeChallengeMethod);
        model.addAttribute("mockAuthorizePath", properties.getMock().getBasePath() + "/authorize");
        model.addAttribute("defaultSub", mock.getDefaultSub());
        model.addAttribute("defaultEmail", mock.getDefaultEmail());
        model.addAttribute("defaultName", mock.getDefaultName());
        return "mock/authorize";
    }

    @PostMapping("/authorize")
    public String authorizeSubmit(
            @RequestParam("client_id") String clientId,
            @RequestParam("redirect_uri") String redirectUri,
            @RequestParam("response_type") String responseType,
            @RequestParam(value = "scope", required = false) String scope,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "nonce", required = false) String nonce,
            @RequestParam(value = "code_challenge", required = false) String codeChallenge,
            @RequestParam(value = "code_challenge_method", required = false) String codeChallengeMethod,
            @RequestParam(value = "sub", required = false) String sub,
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "name", required = false) String name) {
        validateAuthorizeRequest(clientId, redirectUri, responseType);
        PingOneClientProperties.Mock mock = properties.getMock();
        MockAuthorizationCode authorizationCode = sessionStore.storeAuthorizationCode(
                clientId,
                redirectUri,
                state,
                nonce,
                StringUtils.hasText(sub) ? sub : mock.getDefaultSub(),
                StringUtils.hasText(email) ? email : mock.getDefaultEmail(),
                StringUtils.hasText(name) ? name : mock.getDefaultName(),
                codeChallenge,
                codeChallengeMethod);

        UriComponentsBuilder redirectBuilder =
                UriComponentsBuilder.fromUriString(redirectUri).queryParam("code", authorizationCode.code());
        if (StringUtils.hasText(state)) {
            redirectBuilder.queryParam("state", state);
        }
        return "redirect:" + redirectBuilder.build().toUriString();
    }

    @PostMapping("/token")
    @ResponseBody
    public ResponseEntity<?> token(
            HttpServletRequest request,
            @RequestParam("grant_type") String grantType,
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "redirect_uri", required = false) String redirectUri,
            @RequestParam(value = "code_verifier", required = false) String codeVerifier) {
        if (!"authorization_code".equals(grantType)) {
            return oauthError(HttpStatus.BAD_REQUEST, "unsupported_grant_type", "Only authorization_code is supported");
        }
        try {
            Map<String, Object> tokenResponse = tokenService.exchangeAuthorizationCode(
                    request, code, redirectUri, codeVerifier, discoveryService.issuerUri(request));
            return ResponseEntity.ok(tokenResponse);
        } catch (MockPingOneOAuthException ex) {
            HttpStatus status = "invalid_client".equals(ex.getError()) ? HttpStatus.UNAUTHORIZED : HttpStatus.BAD_REQUEST;
            return oauthError(status, ex.getError(), ex.getDescription());
        } catch (Exception ex) {
            return oauthError(HttpStatus.INTERNAL_SERVER_ERROR, "server_error", "Failed to issue tokens");
        }
    }

    @GetMapping("/userinfo")
    @ResponseBody
    public ResponseEntity<?> userInfo(@RequestParam(value = "access_token", required = false) String accessTokenParam,
            HttpServletRequest request) {
        String accessToken = accessTokenParam;
        if (!StringUtils.hasText(accessToken)) {
            String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (StringUtils.hasText(authorizationHeader) && authorizationHeader.startsWith("Bearer ")) {
                accessToken = authorizationHeader.substring(7);
            }
        }
        if (!StringUtils.hasText(accessToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "invalid_token", "error_description", "Access token is missing"));
        }

        return sessionStore
                .findProfileByAccessToken(accessToken)
                .map(profile -> {
                    Map<String, Object> claims = new LinkedHashMap<>();
                    claims.put("sub", profile.sub());
                    claims.put("email", profile.email());
                    claims.put("name", profile.name());
                    return ResponseEntity.ok(claims);
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "invalid_token", "error_description", "Access token is invalid")));
    }

    @GetMapping("/signoff")
    public String signOff(
            @RequestParam(value = "post_logout_redirect_uri", required = false) String postLogoutRedirectUri,
            @RequestParam(value = "id_token_hint", required = false) String idTokenHint) {
        if (StringUtils.hasText(postLogoutRedirectUri)) {
            return "redirect:" + postLogoutRedirectUri;
        }
        return "redirect:" + ServletUriComponentsBuilder.fromCurrentContextPath().path("/").build().toUriString();
    }

    private void validateAuthorizeRequest(String clientId, String redirectUri, String responseType) {
        if (!properties.getMock().getClientId().equals(clientId)) {
            throw new MockPingOneOAuthException("invalid_client", "Unknown client_id");
        }
        if (!"code".equals(responseType)) {
            throw new MockPingOneOAuthException("unsupported_response_type", "Only code response type is supported");
        }
        ClientRegistration registration =
                clientRegistrationRepository.findByRegistrationId(properties.getRegistrationId());
        if (registration == null) {
            throw new MockPingOneOAuthException("invalid_client", "Client registration not found");
        }
        String registeredRedirectUri = registration.getRedirectUri();
        if (!redirectUri.equals(registeredRedirectUri)) {
            throw new MockPingOneOAuthException("invalid_request", "Redirect URI does not match registration");
        }
        try {
            URI.create(redirectUri);
        } catch (IllegalArgumentException ex) {
            throw new MockPingOneOAuthException("invalid_request", "Redirect URI is malformed");
        }
    }

    private static ResponseEntity<Map<String, String>> oauthError(HttpStatus status, String error, String description) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("error", error);
        body.put("error_description", description);
        return ResponseEntity.status(status).body(body);
    }
}
