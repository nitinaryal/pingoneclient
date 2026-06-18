package com.pingone.oidc.tool.trace;

public record FlowClientEventRequest(String flowName, String label, String message, String level) {}
