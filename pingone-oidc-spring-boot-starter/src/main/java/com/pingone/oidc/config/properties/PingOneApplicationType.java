package com.pingone.oidc.config.properties;

/**
 * PingOne application types supported by the test client and copy-ready integration packages.
 */
public enum PingOneApplicationType {

    OIDC_WEB_APP("oidc-web-app"),
    OIDC_SPA("oidc-spa"),
    OIDC_NATIVE("oidc-native"),
    WORKER("worker"),
    DEVICE("device"),
    SAML("saml");

    private final String configValue;

    PingOneApplicationType(String configValue) {
        this.configValue = configValue;
    }

    public String getConfigValue() {
        return configValue;
    }

    public static PingOneApplicationType fromConfigValue(String value) {
        if (value == null || value.isBlank()) {
            return OIDC_WEB_APP;
        }
        for (PingOneApplicationType type : values()) {
            if (type.configValue.equalsIgnoreCase(value) || type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException(
                "Unknown pingone.application-type: " + value + ". Supported values: oidc-web-app, oidc-spa, "
                        + "oidc-native, worker, device, saml");
    }
}
