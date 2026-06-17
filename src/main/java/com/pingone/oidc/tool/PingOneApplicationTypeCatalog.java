package com.pingone.oidc.tool;

import com.pingone.oidc.config.properties.PingOneApplicationType;
import com.pingone.oidc.tool.model.ApplicationTypeDefinition;
import com.pingone.oidc.tool.model.ConfigFieldDefinition;
import com.pingone.oidc.tool.model.TestFlowDefinition;
import com.pingone.oidc.tool.model.TestStepDefinition;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PingOneApplicationTypeCatalog {

    private static final List<ConfigFieldDefinition> CLIENT_REGISTRATION_FIELDS = List.of(
            field(
                    "registrationId",
                    "Registration ID",
                    true,
                    "pingone",
                    "my-pingone-client",
                    "Spring OAuth2 registration key. Must match the YAML key under spring.security.oauth2.client.registration.",
                    "pingone.registration-id",
                    "PINGONE_REGISTRATION_ID"),
            field(
                    "providerId",
                    "Provider ID",
                    true,
                    "pingone",
                    "pingone",
                    "Spring OAuth2 provider key under spring.security.oauth2.client.provider.",
                    "pingone.provider-id",
                    "PINGONE_PROVIDER_ID"),
            field(
                    "clientId",
                    "Client ID",
                    true,
                    "",
                    "your-client-id",
                    "PingOne application Client ID from the PingOne Admin console.",
                    "spring.security.oauth2.client.registration.<id>.client-id",
                    "PINGONE_CLIENT_ID"),
            field(
                    "issuerUri",
                    "Issuer URI",
                    true,
                    "",
                    "https://auth.pingone.com/{env-id}/as",
                    "PingOne environment issuer. Used for OIDC discovery of authorization, token, userinfo, and JWKS endpoints.",
                    "spring.security.oauth2.client.provider.<id>.issuer-uri",
                    "PINGONE_ISSUER_URI"),
            field(
                    "redirectUri",
                    "Redirect URI",
                    true,
                    "",
                    "http://localhost:8080/login/oauth2/code/pingone",
                    "Callback URL after login. Must match PingOne Redirect URI exactly (scheme, host, port, path).",
                    "spring.security.oauth2.client.registration.<id>.redirect-uri",
                    "PINGONE_REDIRECT_URI"),
            field(
                    "postLogoutRedirectUri",
                    "Post-logout Redirect URI",
                    true,
                    "",
                    "http://localhost:8080/",
                    "Where PingOne redirects after end-session logout. Must be registered in PingOne.",
                    "pingone.security.post-logout-redirect-uri",
                    "PINGONE_POST_LOGOUT_REDIRECT_URI"),
            field(
                    "scopes",
                    "Scopes",
                    false,
                    "openid,profile,email",
                    "openid profile email",
                    "Space or comma-separated OIDC scopes requested during authorization.",
                    "spring.security.oauth2.client.registration.<id>.scope",
                    "—"));

    private static final List<ConfigFieldDefinition> DISCOVERY_ENDPOINT_FIELDS = List.of(
            discoveryField(
                    "authorizationUri",
                    "Authorization Endpoint",
                    "https://auth.pingone.com/{env-id}/as/authorize",
                    "OIDC authorization endpoint from discovery (authorization_endpoint).",
                    "spring.security.oauth2.client.provider.<id>.authorization-uri",
                    "PINGONE_AUTHORIZATION_URI"),
            discoveryField(
                    "tokenUri",
                    "Token Endpoint",
                    "https://auth.pingone.com/{env-id}/as/token",
                    "OIDC token endpoint from discovery (token_endpoint).",
                    "spring.security.oauth2.client.provider.<id>.token-uri",
                    "PINGONE_TOKEN_URI"),
            discoveryField(
                    "userInfoUri",
                    "UserInfo Endpoint",
                    "https://auth.pingone.com/{env-id}/as/userinfo",
                    "OIDC userinfo endpoint from discovery (userinfo_endpoint).",
                    "spring.security.oauth2.client.provider.<id>.user-info-uri",
                    "PINGONE_USERINFO_URI"),
            discoveryField(
                    "jwksUri",
                    "JWKS URI",
                    "https://auth.pingone.com/{env-id}/as/jwks",
                    "JSON Web Key Set URI from discovery (jwks_uri).",
                    "spring.security.oauth2.client.provider.<id>.jwk-set-uri",
                    "PINGONE_JWKS_URI"),
            discoveryField(
                    "endSessionEndpoint",
                    "End Session Endpoint",
                    "https://auth.pingone.com/{env-id}/as/signoff",
                    "PingOne RP-initiated logout URL from discovery (end_session_endpoint).",
                    "provider metadata end_session_endpoint",
                    "—"),
            discoveryField(
                    "revocationUri",
                    "Revocation Endpoint",
                    "https://auth.pingone.com/{env-id}/as/revoke",
                    "OAuth2 token revocation endpoint (revocation_endpoint).",
                    "provider metadata revocation_endpoint",
                    "—"),
            discoveryField(
                    "introspectionUri",
                    "Introspection Endpoint",
                    "https://auth.pingone.com/{env-id}/as/introspect",
                    "Token introspection endpoint (introspection_endpoint).",
                    "provider metadata introspection_endpoint",
                    "—"),
            discoveryField(
                    "registrationEndpoint",
                    "Registration Endpoint",
                    "https://auth.pingone.com/{env-id}/as/clients",
                    "Dynamic client registration endpoint (registration_endpoint). PingOne exposes this for reference.",
                    "provider metadata registration_endpoint",
                    "—"));

    private static final List<ConfigFieldDefinition> DISCOVERY_METADATA_FIELDS = List.of(
            metadataField(
                    "responseTypesSupported",
                    "Response Types Supported",
                    "code, token, id_token, code id_token",
                    "OIDC response_types_supported values.",
                    "discovery.response_types_supported"),
            metadataField(
                    "responseModesSupported",
                    "Response Modes Supported",
                    "query, fragment, form_post",
                    "OIDC response_modes_supported values.",
                    "discovery.response_modes_supported"),
            metadataField(
                    "grantTypesSupported",
                    "Grant Types Supported",
                    "authorization_code, implicit, refresh_token, client_credentials",
                    "OIDC grant_types_supported values.",
                    "discovery.grant_types_supported"),
            metadataField(
                    "subjectTypesSupported",
                    "Subject Types Supported",
                    "public, pairwise",
                    "OIDC subject_types_supported values.",
                    "discovery.subject_types_supported"),
            metadataField(
                    "idTokenSigningAlgValuesSupported",
                    "ID Token Signing Algorithms",
                    "RS256",
                    "OIDC id_token_signing_alg_values_supported values.",
                    "discovery.id_token_signing_alg_values_supported"),
            metadataField(
                    "tokenEndpointAuthMethodsSupported",
                    "Token Endpoint Auth Methods",
                    "client_secret_basic, client_secret_post, none",
                    "OIDC token_endpoint_auth_methods_supported values.",
                    "discovery.token_endpoint_auth_methods_supported"),
            metadataField(
                    "codeChallengeMethodsSupported",
                    "PKCE Code Challenge Methods",
                    "S256",
                    "OIDC code_challenge_methods_supported values (PKCE).",
                    "discovery.code_challenge_methods_supported"),
            metadataField(
                    "claimsSupported",
                    "Claims Supported",
                    "sub, iss, aud, exp, iat, email, name",
                    "OIDC claims_supported values.",
                    "discovery.claims_supported"),
            metadataField(
                    "claimTypesSupported",
                    "Claim Types Supported",
                    "normal",
                    "OIDC claim_types_supported values.",
                    "discovery.claim_types_supported"),
            metadataField(
                    "acrValuesSupported",
                    "ACR Values Supported",
                    "",
                    "Authentication context class reference values (acr_values_supported).",
                    "discovery.acr_values_supported"),
            metadataField(
                    "requestObjectSigningAlgValuesSupported",
                    "Request Object Signing Algorithms",
                    "",
                    "request_object_signing_alg_values_supported from discovery.",
                    "discovery.request_object_signing_alg_values_supported"),
            metadataField(
                    "serviceDocumentation",
                    "Service Documentation",
                    "",
                    "URL to OIDC provider documentation (service_documentation).",
                    "discovery.service_documentation"));

    private static final List<ConfigFieldDefinition> COMMON_OIDC_FIELDS = concatFieldLists(
            CLIENT_REGISTRATION_FIELDS, DISCOVERY_ENDPOINT_FIELDS, DISCOVERY_METADATA_FIELDS);

    public List<ApplicationTypeDefinition> all() {
        return List.of(
                oidcWebApp(),
                oidcSpa(),
                oidcNative(),
                worker(),
                device(),
                saml());
    }

    public ApplicationTypeDefinition find(String configValue) {
        return all().stream()
                .filter(def -> def.configValue().equalsIgnoreCase(configValue))
                .findFirst()
                .orElse(oidcWebApp());
    }

    private ApplicationTypeDefinition oidcWebApp() {
        List<ConfigFieldDefinition> fields = concatFields(
                COMMON_OIDC_FIELDS,
                field(
                        "clientSecret",
                        "Client Secret",
                        true,
                        "",
                        "your-client-secret",
                        "Confidential client secret for Authorization Code flow with client_secret_basic authentication.",
                        "spring.security.oauth2.client.registration.<id>.client-secret",
                        "PINGONE_CLIENT_SECRET"));

        return new ApplicationTypeDefinition(
                PingOneApplicationType.OIDC_WEB_APP,
                "oidc-web-app",
                "OIDC Web App",
                "Server-side confidential OIDC client using Authorization Code flow. Best for Spring MVC / Thymeleaf applications.",
                true,
                fields,
                List.of(
                        "Application type: OIDC Web App",
                        "Grant type: Authorization Code",
                        "Response type: Code",
                        "Client authentication: Client Secret (Basic)",
                        "Register Redirect URI exactly as configured",
                        "Register Post-logout Redirect URI exactly as configured",
                        "Enable scopes: openid, profile, email"),
                List.of(
                        loginFlow("/oauth2/authorization/{registrationId}", true),
                        logoutFlow(true),
                        claimsFlow(true),
                        tokenFlow(true),
                        jwksFlow(true),
                        metadataFlow(true),
                        connectivityFlow(true)));
    }

    private ApplicationTypeDefinition oidcSpa() {
        return templateType(
                PingOneApplicationType.OIDC_SPA,
                "oidc-spa",
                "OIDC Single-Page App",
                "Public client with PKCE. No client secret. Redirect URI typically points to SPA route.",
                concatFields(
                        COMMON_OIDC_FIELDS,
                        field(
                                "pkce",
                                "PKCE",
                                true,
                                "S256",
                                "S256",
                                "SPA applications must use PKCE (Proof Key for Code Exchange) instead of client secret.",
                                "spring.security.oauth2.client.registration.<id> (PKCE)",
                                "—")),
                List.of(
                        "Application type: Single Page App",
                        "Grant type: Authorization Code",
                        "PKCE: Required",
                        "No client secret",
                        "Redirect URI: SPA callback route (e.g. https://app.example.com/callback)"),
                List.of(loginFlow("/oauth2/authorization/{registrationId}", false), logoutFlow(false), connectivityFlow(false)));
    }

    private ApplicationTypeDefinition oidcNative() {
        return templateType(
                PingOneApplicationType.OIDC_NATIVE,
                "oidc-native",
                "OIDC Native App",
                "Mobile/desktop app with PKCE and loopback or custom URI redirect.",
                concatFields(
                        COMMON_OIDC_FIELDS,
                        field(
                                "redirectUri",
                                "Redirect URI",
                                true,
                                "",
                                "http://127.0.0.1:8080/login/oauth2/code/my-app",
                                "Native apps often use loopback IP (127.0.0.1) with dynamic port. Must match PingOne exactly.",
                                "redirect-uri",
                                "PINGONE_REDIRECT_URI")),
                List.of(
                        "Application type: Native App",
                        "Grant type: Authorization Code + PKCE",
                        "Redirect: loopback or custom scheme",
                        "No client secret"),
                List.of(loginFlow("/oauth2/authorization/{registrationId}", false), connectivityFlow(false)));
    }

    private ApplicationTypeDefinition worker() {
        return templateType(
                PingOneApplicationType.WORKER,
                "worker",
                "Worker Application",
                "Machine-to-machine client using Client Credentials grant. No interactive user login.",
                List.of(
                        field(
                                "registrationId",
                                "Registration ID",
                                true,
                                "pingone-worker",
                                "pingone-worker",
                                "Spring registration id for the worker client.",
                                "pingone.registration-id",
                                "PINGONE_REGISTRATION_ID"),
                        field(
                                "clientId",
                                "Client ID",
                                true,
                                "",
                                "worker-client-id",
                                "PingOne Worker application client ID.",
                                "client-id",
                                "PINGONE_CLIENT_ID"),
                        field(
                                "clientSecret",
                                "Client Secret",
                                true,
                                "",
                                "worker-client-secret",
                                "Worker applications authenticate with client credentials.",
                                "client-secret",
                                "PINGONE_CLIENT_SECRET"),
                        field(
                                "issuerUri",
                                "Issuer URI",
                                true,
                                "",
                                "https://auth.pingone.com/{env-id}/as",
                                "Token endpoint issuer for client credentials flow.",
                                "issuer-uri",
                                "PINGONE_ISSUER_URI"),
                        field(
                                "scopes",
                                "Scopes",
                                false,
                                "openid",
                                "openid",
                                "API scopes for the worker token.",
                                "scope",
                                "—")),
                List.of(
                        "Application type: Worker",
                        "Grant type: Client Credentials",
                        "No redirect URI required",
                        "No interactive login/logout"),
                List.of(
                        new TestFlowDefinition(
                                "client-credentials",
                                "Client Credentials Token",
                                "Obtain access token without user interaction.",
                                "/tool/test/worker-token",
                                "GET",
                                false,
                                false,
                                List.of(
                                        step(
                                                1,
                                                "Request token",
                                                "App calls token endpoint with client_id + client_secret.",
                                                "Access token returned"),
                                        step(
                                                2,
                                                "Use token",
                                                "Call PingOne APIs with Bearer token.",
                                                "API responds 200 for valid scope")))));
    }

    private ApplicationTypeDefinition device() {
        return templateType(
                PingOneApplicationType.DEVICE,
                "device",
                "Device Authorization",
                "OAuth 2.0 Device Authorization Grant for input-constrained devices (TV, CLI).",
                List.of(
                        field(
                                "registrationId",
                                "Registration ID",
                                true,
                                "pingone-device",
                                "pingone-device",
                                "Registration id for device flow client.",
                                "pingone.registration-id",
                                "PINGONE_REGISTRATION_ID"),
                        field(
                                "clientId",
                                "Client ID",
                                true,
                                "",
                                "device-client-id",
                                "Device application client ID.",
                                "client-id",
                                "PINGONE_CLIENT_ID"),
                        field(
                                "issuerUri",
                                "Issuer URI",
                                true,
                                "",
                                "https://auth.pingone.com/{env-id}/as",
                                "Issuer providing device authorization and token endpoints.",
                                "issuer-uri",
                                "PINGONE_ISSUER_URI")),
                List.of(
                        "Application type: Device",
                        "Grant type: Device Authorization",
                        "User completes auth on secondary device",
                        "Poll token endpoint until authorized"),
                List.of(
                        new TestFlowDefinition(
                                "device-authorization",
                                "Device Code Flow",
                                "Start device flow and poll for token.",
                                "/tool/test/device-flow",
                                "GET",
                                false,
                                false,
                                List.of(
                                        step(
                                                1,
                                                "Request device code",
                                                "App requests device_code and user_code from PingOne.",
                                                "Codes returned"),
                                        step(
                                                2,
                                                "User authorizes",
                                                "User visits verification URI and enters user_code.",
                                                "Authorization completes"),
                                        step(
                                                3,
                                                "Poll token",
                                                "App polls token endpoint until access token issued.",
                                                "Access token received")))));
    }

    private ApplicationTypeDefinition saml() {
        return templateType(
                PingOneApplicationType.SAML,
                "saml",
                "SAML Application",
                "SAML 2.0 Service Provider integration. Uses assertions instead of OIDC tokens.",
                List.of(
                        field(
                                "entityId",
                                "SP Entity ID",
                                true,
                                "",
                                "https://app.example.com/saml/metadata",
                                "Your service provider entity ID registered in PingOne.",
                                "spring.security.saml2.relyingparty.registration.<id>.entity-id",
                                "—"),
                        field(
                                "acsUrl",
                                "Assertion Consumer URL",
                                true,
                                "",
                                "https://app.example.com/login/saml2/sso/pingone",
                                "Endpoint where PingOne posts SAML assertions after login.",
                                "assertion-consumer-service location",
                                "—"),
                        field(
                                "metadataUrl",
                                "PingOne IdP Metadata URL",
                                true,
                                "",
                                "https://auth.pingone.com/{env-id}/saml20/metadata",
                                "PingOne SAML metadata for trust configuration.",
                                "assertingparty metadata-uri",
                                "—")),
                List.of(
                        "Application type: SAML",
                        "Configure SP metadata in PingOne",
                        "Map SAML attributes to application user fields",
                        "No OIDC redirect URI"),
                List.of(
                        new TestFlowDefinition(
                                "saml-login",
                                "SAML SSO Login",
                                "Initiate SAML login and validate assertion.",
                                "/tool/test/saml-login",
                                "GET",
                                false,
                                false,
                                List.of(
                                        step(
                                                1,
                                                "SP initiates SSO",
                                                "User hits /saml2/authenticate/{registrationId}.",
                                                "Redirect to PingOne"),
                                        step(
                                                2,
                                                "PingOne authenticates",
                                                "User signs in at IdP.",
                                                "SAML assertion posted to ACS"),
                                        step(
                                                3,
                                                "Create session",
                                                "Spring Security validates assertion signature.",
                                                "Authenticated session created")))));
    }

    private ApplicationTypeDefinition templateType(
            PingOneApplicationType type,
            String configValue,
            String displayName,
            String summary,
            List<ConfigFieldDefinition> fields,
            List<String> checklist,
            List<TestFlowDefinition> tests) {
        return new ApplicationTypeDefinition(type, configValue, displayName, summary, false, fields, checklist, tests);
    }

    private TestFlowDefinition loginFlow(String pathTemplate, boolean runnable) {
        return new TestFlowDefinition(
                "login",
                "Login (Authorization Code)",
                "Start interactive OIDC login and validate callback handling.",
                pathTemplate,
                "GET",
                false,
                runnable,
                List.of(
                        step(1, "Start authorization", "Browser navigates to /oauth2/authorization/{registrationId}.", "302 to PingOne authorize URL"),
                        step(2, "User authenticates", "User enters credentials at PingOne.", "PingOne validates credentials"),
                        step(3, "Authorization callback", "PingOne redirects to redirect_uri with ?code=...&state=...", "Spring exchanges code for tokens"),
                        step(4, "Session created", "OidcUser stored in security context.", "Redirect to post-login path (/dashboard)")));
    }

    private TestFlowDefinition logoutFlow(boolean runnable) {
        return new TestFlowDefinition(
                "logout",
                "Logout (End Session)",
                "Clear local session and invoke PingOne end_session_endpoint.",
                "/logout",
                "POST",
                true,
                runnable,
                List.of(
                        step(1, "POST /logout", "Spring Security logout filter runs with CSRF token.", "Local session invalidated"),
                        step(2, "End session redirect", "Redirect to PingOne end_session_endpoint with id_token_hint.", "PingOne ends SSO session"),
                        step(3, "Return to app", "PingOne redirects to post_logout_redirect_uri.", "User lands on home page unauthenticated")));
    }

    private TestFlowDefinition claimsFlow(boolean runnable) {
        return new TestFlowDefinition(
                "claims",
                "View ID Token Claims",
                "Validate ID token claims returned for the authenticated user.",
                "/me",
                "GET",
                true,
                runnable,
                List.of(
                        step(1, "Load /me", "Controller reads OidcUser from security context.", "200 OK"),
                        step(2, "Decode claims", "sub, email, name, iss, aud, exp displayed.", "Claims match PingOne user profile")));
    }

    private TestFlowDefinition tokenFlow(boolean runnable) {
        return new TestFlowDefinition(
                "token",
                "View Access Token",
                "Confirm access token was issued during authorization code exchange.",
                "/token",
                "GET",
                true,
                runnable,
                List.of(
                        step(1, "Load authorized client", "OAuth2AuthorizedClientService loads token for principal.", "Client found"),
                        step(2, "Display token metadata", "Masked token, type, expiry, scopes shown.", "Bearer token with expected scopes")));
    }

    private TestFlowDefinition jwksFlow(boolean runnable) {
        return new TestFlowDefinition(
                "jwks",
                "Fetch JWKS",
                "Verify JWKS endpoint is reachable and returns signing keys.",
                "/jwks",
                "GET",
                true,
                runnable,
                List.of(
                        step(1, "Resolve JWKS URI", "From issuer discovery or pingone.metadata.jwks-uri-override.", "URI resolved"),
                        step(2, "HTTP GET JWKS", "WebClient fetches JSON key set.", "keys[] array returned")));
    }

    private TestFlowDefinition metadataFlow(boolean runnable) {
        return new TestFlowDefinition(
                "metadata",
                "Fetch OIDC Metadata",
                "Verify OpenID Provider configuration document is reachable.",
                "/metadata",
                "GET",
                true,
                runnable,
                List.of(
                        step(1, "Build discovery URL", "issuer-uri + /.well-known/openid-configuration", "URL formed"),
                        step(2, "HTTP GET metadata", "Fetch and display discovery JSON.", "authorization_endpoint, token_endpoint, jwks_uri present")));
    }

    private TestFlowDefinition connectivityFlow(boolean runnable) {
        return new TestFlowDefinition(
                "connectivity",
                "Connectivity Check",
                "Server-side check that issuer metadata and JWKS endpoints respond.",
                "/tool/api/diagnostics",
                "GET",
                false,
                runnable,
                List.of(
                        step(1, "Read runtime config", "Load registration and provider from Spring context.", "Config loaded"),
                        step(2, "Fetch discovery", "GET issuer .well-known/openid-configuration.", "HTTP 200"),
                        step(3, "Fetch JWKS", "GET jwks_uri from provider.", "HTTP 200")));
    }

    private static ConfigFieldDefinition field(
            String key,
            String label,
            boolean required,
            String defaultValue,
            String placeholder,
            String tooltip,
            String yamlHint,
            String envVar) {
        return new ConfigFieldDefinition(key, label, required, defaultValue, placeholder, tooltip, yamlHint, envVar, "client");
    }

    private static ConfigFieldDefinition discoveryField(
            String key,
            String label,
            String placeholder,
            String tooltip,
            String yamlHint,
            String envVar) {
        return new ConfigFieldDefinition(
                key, label, false, "", placeholder, tooltip, yamlHint, envVar, "discovery-endpoints");
    }

    private static ConfigFieldDefinition metadataField(
            String key, String label, String placeholder, String tooltip, String yamlHint) {
        return new ConfigFieldDefinition(
                key, label, false, "", placeholder, tooltip, yamlHint, "—", "discovery-metadata");
    }

    private static List<ConfigFieldDefinition> concatFieldLists(
            List<ConfigFieldDefinition> first, List<ConfigFieldDefinition>... rest) {
        java.util.ArrayList<ConfigFieldDefinition> result = new java.util.ArrayList<>(first);
        for (List<ConfigFieldDefinition> list : rest) {
            result.addAll(list);
        }
        return List.copyOf(result);
    }

    private static TestStepDefinition step(int order, String title, String detail, String expected) {
        return new TestStepDefinition(order, title, detail, expected);
    }

    private static List<ConfigFieldDefinition> concatFields(
            List<ConfigFieldDefinition> base, ConfigFieldDefinition... extra) {
        java.util.ArrayList<ConfigFieldDefinition> result = new java.util.ArrayList<>(base);
        java.util.Collections.addAll(result, extra);
        return result;
    }
}
