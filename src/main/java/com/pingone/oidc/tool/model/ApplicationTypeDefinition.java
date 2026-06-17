package com.pingone.oidc.tool.model;

import com.pingone.oidc.config.properties.PingOneApplicationType;
import java.util.List;

public record ApplicationTypeDefinition(
        PingOneApplicationType type,
        String configValue,
        String displayName,
        String summary,
        boolean implementedInTemplate,
        List<ConfigFieldDefinition> configFields,
        List<String> pingOneAdminChecklist,
        List<TestFlowDefinition> testFlows) {}
