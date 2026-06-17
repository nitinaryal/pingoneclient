package com.pingone.oidc.tool;

import com.pingone.oidc.tool.model.DiscoveryApplyResult;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class OidcDiscoveryImportService {

    private static final Map<String, String> DISCOVERY_TO_FIELD = Map.ofEntries(
            Map.entry("issuer", "issuerUri"),
            Map.entry("authorization_endpoint", "authorizationUri"),
            Map.entry("token_endpoint", "tokenUri"),
            Map.entry("userinfo_endpoint", "userInfoUri"),
            Map.entry("jwks_uri", "jwksUri"),
            Map.entry("registration_endpoint", "registrationEndpoint"),
            Map.entry("end_session_endpoint", "endSessionEndpoint"),
            Map.entry("revocation_endpoint", "revocationUri"),
            Map.entry("introspection_endpoint", "introspectionUri"),
            Map.entry("scopes_supported", "scopes"),
            Map.entry("response_types_supported", "responseTypesSupported"),
            Map.entry("response_modes_supported", "responseModesSupported"),
            Map.entry("grant_types_supported", "grantTypesSupported"),
            Map.entry("subject_types_supported", "subjectTypesSupported"),
            Map.entry("id_token_signing_alg_values_supported", "idTokenSigningAlgValuesSupported"),
            Map.entry("token_endpoint_auth_methods_supported", "tokenEndpointAuthMethodsSupported"),
            Map.entry("claims_supported", "claimsSupported"),
            Map.entry("code_challenge_methods_supported", "codeChallengeMethodsSupported"),
            Map.entry("request_object_signing_alg_values_supported", "requestObjectSigningAlgValuesSupported"),
            Map.entry("request_object_encryption_alg_values_supported", "requestObjectEncryptionAlgValuesSupported"),
            Map.entry("request_object_encryption_enc_values_supported", "requestObjectEncryptionEncValuesSupported"),
            Map.entry("id_token_encryption_alg_values_supported", "idTokenEncryptionAlgValuesSupported"),
            Map.entry("id_token_encryption_enc_values_supported", "idTokenEncryptionEncValuesSupported"),
            Map.entry("userinfo_signing_alg_values_supported", "userinfoSigningAlgValuesSupported"),
            Map.entry("userinfo_encryption_alg_values_supported", "userinfoEncryptionAlgValuesSupported"),
            Map.entry("userinfo_encryption_enc_values_supported", "userinfoEncryptionEncValuesSupported"),
            Map.entry("acr_values_supported", "acrValuesSupported"),
            Map.entry("display_values_supported", "displayValuesSupported"),
            Map.entry("claim_types_supported", "claimTypesSupported"),
            Map.entry("service_documentation", "serviceDocumentation"));

    private final JsonParser jsonParser;
    private final WebClient webClient;

    public OidcDiscoveryImportService(WebClient webClient) {
        this.jsonParser = JsonParserFactory.getJsonParser();
        this.webClient = webClient;
    }

    public DiscoveryApplyResult applyJson(String json) {
        if (!StringUtils.hasText(json)) {
            throw new IllegalArgumentException("Discovery JSON is empty");
        }
        try {
            Map<String, Object> document = jsonParser.parseMap(json);
            return mapDocument(document);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid OIDC discovery JSON: " + ex.getMessage(), ex);
        }
    }

    public DiscoveryApplyResult fetchFromIssuer(String issuerUri, String discoveryPath) {
        String normalizedIssuer = normalizeIssuer(issuerUri);
        validateHttpsIssuer(normalizedIssuer);
        String path = StringUtils.hasText(discoveryPath) ? discoveryPath : "/.well-known/openid-configuration";
        String discoveryUri = normalizedIssuer + (path.startsWith("/") ? path : "/" + path);
        String json = webClient
                .get()
                .uri(discoveryUri)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        return applyJson(json);
    }

    private DiscoveryApplyResult mapDocument(Map<String, Object> document) {
        Map<String, String> fieldValues = new LinkedHashMap<>();
        List<String> mappedKeys = new ArrayList<>();
        List<String> unmappedKeys = new ArrayList<>();

        for (Map.Entry<String, Object> entry : document.entrySet()) {
            String discoveryKey = entry.getKey();
            String fieldKey = DISCOVERY_TO_FIELD.get(discoveryKey);
            if (fieldKey != null) {
                String value = stringify(entry.getValue());
                if (StringUtils.hasText(value)) {
                    fieldValues.put(fieldKey, value);
                    mappedKeys.add(discoveryKey);
                }
            } else {
                unmappedKeys.add(discoveryKey);
            }
        }
        return new DiscoveryApplyResult(fieldValues, document, mappedKeys, unmappedKeys);
    }

    static String normalizeIssuer(String issuerUri) {
        if (!StringUtils.hasText(issuerUri)) {
            throw new IllegalArgumentException("issuerUri is required");
        }
        return issuerUri.endsWith("/") ? issuerUri.substring(0, issuerUri.length() - 1) : issuerUri.trim();
    }

    private static void validateHttpsIssuer(String issuerUri) {
        if (!issuerUri.toLowerCase().startsWith("https://")) {
            throw new IllegalArgumentException("Issuer URI must use HTTPS");
        }
    }

    private static String stringify(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).collect(Collectors.joining(", "));
        }
        return String.valueOf(value);
    }
}
