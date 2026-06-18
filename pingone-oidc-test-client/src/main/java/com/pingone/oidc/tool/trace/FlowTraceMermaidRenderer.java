package com.pingone.oidc.tool.trace;

import java.util.List;

final class FlowTraceMermaidRenderer {

    private FlowTraceMermaidRenderer() {}

    static String render(List<FlowTraceEvent> events) {
        if (events.isEmpty()) {
            return "sequenceDiagram\n  participant Browser\n  participant TestClient as Test Client\n  participant PingOne\n  Note over Browser,PingOne: No flow events yet. Run login, logout, or diagnostics.";
        }
        StringBuilder builder = new StringBuilder();
        builder.append("sequenceDiagram\n");
        builder.append("  actor Browser\n");
        builder.append("  participant TestClient as Test Client\n");
        builder.append("  participant PingOne\n");
        for (FlowTraceEvent event : events) {
            String arrow = "error".equals(event.level()) ? "-x" : "->>";
            builder.append("  ")
                    .append(event.fromActor().mermaidId())
                    .append(arrow)
                    .append(event.toActor().mermaidId())
                    .append(": ")
                    .append(sanitize(event.label()))
                    .append('\n');
        }
        return builder.toString();
    }

    private static String sanitize(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\"", "'").replace("\n", " ").replace(":", " -");
    }
}
