package com.pingone.oidc.tool.model;

import java.util.List;
import java.util.Map;

public record ToolWizardSessionView(
        boolean saved,
        String applicationType,
        Map<String, String> values,
        List<String> sensitiveFields,
        String notice) {

    public ToolWizardSessionView(
            boolean saved, String applicationType, Map<String, String> values, List<String> sensitiveFields) {
        this(saved, applicationType, values, sensitiveFields, null);
    }
}
