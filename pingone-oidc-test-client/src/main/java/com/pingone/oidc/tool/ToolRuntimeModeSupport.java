package com.pingone.oidc.tool;

import com.pingone.oidc.config.properties.PingOneClientProperties;
import java.util.Map;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ToolRuntimeModeSupport {

    private final PingOneClientProperties properties;
    private final Environment environment;

    public ToolRuntimeModeSupport(PingOneClientProperties properties, Environment environment) {
        this.properties = properties;
        this.environment = environment;
    }

    public boolean isMockMode() {
        return environment.getProperty("mock", Boolean.class, false);
    }

    /**
     * Detects wizard values that were saved for local mock PingOne (not real PingOne Admin credentials).
     */
    public boolean looksLikeMockConfiguration(Map<String, String> values) {
        if (values == null || values.isEmpty()) {
            return false;
        }
        String mockClientId = properties.getMock().getClientId();
        if (StringUtils.hasText(mockClientId) && mockClientId.equals(values.get("clientId"))) {
            return true;
        }
        String issuerUri = values.get("issuerUri");
        if (StringUtils.hasText(issuerUri) && issuerUri.contains(properties.getMock().getBasePath())) {
            return true;
        }
        String authorizationUri = values.get("authorizationUri");
        return StringUtils.hasText(authorizationUri) && authorizationUri.contains(properties.getMock().getBasePath());
    }

    public boolean isIncompatibleWithCurrentRuntime(Map<String, String> values) {
        if (values == null || values.isEmpty()) {
            return false;
        }
        return isMockMode() != looksLikeMockConfiguration(values);
    }
}
