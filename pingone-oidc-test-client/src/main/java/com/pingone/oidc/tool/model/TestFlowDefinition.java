package com.pingone.oidc.tool.model;

import java.util.List;

public record TestFlowDefinition(
        String id,
        String name,
        String description,
        String actionPath,
        String httpMethod,
        boolean requiresAuthentication,
        boolean runnableInTemplate,
        List<TestStepDefinition> steps) {}
