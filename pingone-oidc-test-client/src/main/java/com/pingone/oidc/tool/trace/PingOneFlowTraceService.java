package com.pingone.oidc.tool.trace;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PingOneFlowTraceService {

    private static final Logger log = LoggerFactory.getLogger(PingOneFlowTraceService.class);

    private final PingOneFlowTraceStore store;

    public PingOneFlowTraceService(PingOneFlowTraceStore store) {
        this.store = store;
    }

    public void beginFlow(String flowName) {
        store.setActiveFlowName(flowName);
        record(flowName, FlowActor.BROWSER, FlowActor.TEST_CLIENT, "Flow started: " + flowName, "Beginning " + flowName + " flow trace", "info");
    }

    public void ensureFlow(String flowName) {
        if (!flowName.equals(store.activeFlowName())) {
            beginFlow(flowName);
        }
    }

    public void recordClientEvent(FlowClientEventRequest request) {
        if (request == null || request.label() == null || request.label().isBlank()) {
            return;
        }
        String flowName = request.flowName() != null && !request.flowName().isBlank()
                ? request.flowName()
                : store.activeFlowName();
        store.setActiveFlowName(flowName);
        String level = request.level() != null && !request.level().isBlank() ? request.level() : "info";
        record(
                flowName,
                FlowActor.BROWSER,
                FlowActor.TEST_CLIENT,
                request.label(),
                request.message() != null ? request.message() : request.label(),
                level);
    }

    public FlowTraceEvent record(
            String flowName,
            FlowActor from,
            FlowActor to,
            String label,
            String message,
            String level) {
        int sequence = store.events().size() + 1;
        FlowTraceEvent event = FlowTraceEvent.of(sequence, flowName, from, to, label, message, level);
        store.append(event);
        switch (level) {
            case "error" -> log.error("[flow:{}] {} -> {} | {} | {}", flowName, from.displayName(), to.displayName(), label, message);
            case "warn" -> log.warn("[flow:{}] {} -> {} | {} | {}", flowName, from.displayName(), to.displayName(), label, message);
            case "success" -> log.info("[flow:{}] {} -> {} | {} | {}", flowName, from.displayName(), to.displayName(), label, message);
            default -> log.info("[flow:{}] {} -> {} | {} | {}", flowName, from.displayName(), to.displayName(), label, message);
        }
        return event;
    }

    public FlowTraceEvent record(
            FlowActor from,
            FlowActor to,
            String label,
            String message,
            String level) {
        return record(store.activeFlowName(), from, to, label, message, level);
    }

    public FlowTraceEvent recordHttp(
            FlowActor from,
            FlowActor to,
            String httpMethod,
            String path,
            int httpStatus,
            String message) {
        int sequence = store.events().size() + 1;
        FlowTraceEvent event =
                FlowTraceEvent.http(sequence, store.activeFlowName(), from, to, httpMethod, path, httpStatus, message);
        store.append(event);
        log.info(
                "[flow:{}] HTTP {} {} -> {} (status {})",
                store.activeFlowName(),
                httpMethod,
                path,
                message,
                httpStatus);
        return event;
    }

    public FlowTraceView currentView() {
        List<FlowTraceEvent> events = store.events();
        return new FlowTraceView(events, FlowTraceMermaidRenderer.render(events), store.activeFlowName());
    }

    public void clear() {
        store.clear();
        log.info("Flow trace cleared for current session");
    }
}
