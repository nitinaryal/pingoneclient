package com.pingone.oidc.tool.model;

import com.pingone.oidc.client.registration.RegistrationValueSource;
import java.util.HashMap;
import java.util.Map;

public class ClientToolConfigRequest implements RegistrationValueSource {

    private String applicationType = "oidc-web-app";
    private Map<String, String> values = new HashMap<>();

    public String getApplicationType() {
        return applicationType;
    }

    public void setApplicationType(String applicationType) {
        this.applicationType = applicationType;
    }

    public Map<String, String> getValues() {
        return values;
    }

    public void setValues(Map<String, String> values) {
        this.values = values;
    }

    public String value(String key, String defaultValue) {
        String value = values.get(key);
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }
}
