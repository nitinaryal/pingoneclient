package com.pingone.oidc.tool.trace;

import java.time.Instant;

public record FlowTraceEvent(
        int sequence,
        Instant timestamp,
        String flowName,
        FlowActor fromActor,
        FlowActor toActor,
        String label,
        String message,
        String level,
        String httpMethod,
        String path,
        Integer httpStatus) {

    public static FlowTraceEvent of(
            int sequence,
            String flowName,
            FlowActor from,
            FlowActor to,
            String label,
            String message,
            String level) {
        return new FlowTraceEvent(sequence, Instant.now(), flowName, from, to, label, message, level, null, null, null);
    }

    public static FlowTraceEvent http(
            int sequence,
            String flowName,
            FlowActor from,
            FlowActor to,
            String httpMethod,
            String path,
            int httpStatus,
            String message) {
        String label = httpMethod + " " + path;
        return new FlowTraceEvent(
                sequence,
                Instant.now(),
                flowName,
                from,
                to,
                label,
                message,
                httpStatus >= 400 ? "error" : "info",
                httpMethod,
                path,
                httpStatus);
    }
}
