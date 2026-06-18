package com.pingone.oidc.config.properties;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "pingone")
public class PingOneClientProperties {

    private PingOneApplicationType applicationType = PingOneApplicationType.OIDC_WEB_APP;

    /**
     * Must match {@code spring.security.oauth2.client.registration.<registration-id>} key.
     */
    private String registrationId = "pingone";

    /**
     * Must match {@code spring.security.oauth2.client.provider.<provider-id>} key.
     */
    private String providerId = "pingone";

    private final Ui ui = new Ui();
    private final Security security = new Security();
    private final Metadata metadata = new Metadata();
    private final Oidc oidc = new Oidc();
    private final Mock mock = new Mock();

    public PingOneApplicationType getApplicationType() {
        return applicationType;
    }

    public void setApplicationType(PingOneApplicationType applicationType) {
        this.applicationType = applicationType;
    }

    public String getRegistrationId() {
        return registrationId;
    }

    public void setRegistrationId(String registrationId) {
        this.registrationId = registrationId;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public Ui getUi() {
        return ui;
    }

    public Security getSecurity() {
        return security;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public Oidc getOidc() {
        return oidc;
    }

    public Mock getMock() {
        return mock;
    }

    public String getLoginPath() {
        return "/oauth2/authorization/" + registrationId;
    }

    public static class Ui {

        private String homePath = "/";
        private String postLoginPath = "/dashboard";

        public String getHomePath() {
            return homePath;
        }

        public void setHomePath(String homePath) {
            this.homePath = homePath;
        }

        public String getPostLoginPath() {
            return postLoginPath;
        }

        public void setPostLoginPath(String postLoginPath) {
            this.postLoginPath = postLoginPath;
        }
    }

    public static class Security {

        private List<String> publicPaths = new ArrayList<>(List.of("/", "/error", "/css/**", "/webjars/**"));
        private String postLogoutRedirectUri = "http://localhost:8080/";

        public List<String> getPublicPaths() {
            return publicPaths;
        }

        public void setPublicPaths(List<String> publicPaths) {
            this.publicPaths = publicPaths;
        }

        public String getPostLogoutRedirectUri() {
            return postLogoutRedirectUri;
        }

        public void setPostLogoutRedirectUri(String postLogoutRedirectUri) {
            this.postLogoutRedirectUri = postLogoutRedirectUri;
        }
    }

    public static class Metadata {

        private String jwksUriOverride = "";
        private String discoveryDocumentPath = "/.well-known/openid-configuration";

        public String getJwksUriOverride() {
            return jwksUriOverride;
        }

        public void setJwksUriOverride(String jwksUriOverride) {
            this.jwksUriOverride = jwksUriOverride;
        }

        public String getDiscoveryDocumentPath() {
            return discoveryDocumentPath;
        }

        public void setDiscoveryDocumentPath(String discoveryDocumentPath) {
            this.discoveryDocumentPath = discoveryDocumentPath;
        }
    }

    public static class Oidc {

        private List<String> idTokenClaimKeys =
                new ArrayList<>(List.of("sub", "email", "name", "iss", "aud", "exp"));

        public List<String> getIdTokenClaimKeys() {
            return idTokenClaimKeys;
        }

        public void setIdTokenClaimKeys(List<String> idTokenClaimKeys) {
            this.idTokenClaimKeys = idTokenClaimKeys;
        }
    }

    public static class Mock {

        private String basePath = "/mock/pingone/as";
        private String clientId = "mock-client-id";
        private String clientSecret = "mock-client-secret";
        private String defaultSub = "mock-user-001";
        private String defaultEmail = "mock.user@example.com";
        private String defaultName = "Mock PingOne User";

        public String getBasePath() {
            return basePath;
        }

        public void setBasePath(String basePath) {
            this.basePath = basePath;
        }

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
        }

        public String getDefaultSub() {
            return defaultSub;
        }

        public void setDefaultSub(String defaultSub) {
            this.defaultSub = defaultSub;
        }

        public String getDefaultEmail() {
            return defaultEmail;
        }

        public void setDefaultEmail(String defaultEmail) {
            this.defaultEmail = defaultEmail;
        }

        public String getDefaultName() {
            return defaultName;
        }

        public void setDefaultName(String defaultName) {
            this.defaultName = defaultName;
        }
    }
}
