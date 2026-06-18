package com.pingone.oidc.tool.trace;

import java.util.List;

public record FlowTraceView(List<FlowTraceEvent> events, String mermaidSequence, String activeFlowName) {}
