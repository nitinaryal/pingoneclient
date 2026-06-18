package com.pingone.oidc.tool.model;

public record ConfigFieldDefinition(
        String key,
        String label,
        boolean required,
        String defaultValue,
        String placeholder,
        String tooltip,
        String yamlHint,
        String envVar,
        String group) {

    public ConfigFieldDefinition(
            String key,
            String label,
            boolean required,
            String defaultValue,
            String placeholder,
            String tooltip,
            String yamlHint,
            String envVar) {
        this(key, label, required, defaultValue, placeholder, tooltip, yamlHint, envVar, "client");
    }
}
