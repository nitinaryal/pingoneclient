package com.pingone.oidc.tool.trace;

import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class PingOneFlowTraceStore {

    static final String SESSION_ATTRIBUTE = "pingone.tool.flow.trace.events";
    static final String ACTIVE_FLOW_ATTRIBUTE = "pingone.tool.flow.trace.active-flow";
    private static final int MAX_EVENTS = 200;

    public List<FlowTraceEvent> events() {
        HttpSession session = currentSessionOrNull();
        if (session == null) {
            return List.of();
        }
        Object stored = session.getAttribute(SESSION_ATTRIBUTE);
        if (!(stored instanceof List<?> list)) {
            return List.of();
        }
        return Collections.unmodifiableList(new ArrayList<>((List<FlowTraceEvent>) list));
    }

    public String activeFlowName() {
        HttpSession session = currentSessionOrNull();
        if (session == null) {
            return "general";
        }
        Object value = session.getAttribute(ACTIVE_FLOW_ATTRIBUTE);
        return value instanceof String name && !name.isBlank() ? name : "general";
    }

    public void setActiveFlowName(String flowName) {
        sessionForWrite().setAttribute(ACTIVE_FLOW_ATTRIBUTE, flowName);
    }

    public synchronized FlowTraceEvent append(FlowTraceEvent event) {
        HttpSession session = sessionForWrite();
        @SuppressWarnings("unchecked")
        List<FlowTraceEvent> events = (List<FlowTraceEvent>) session.getAttribute(SESSION_ATTRIBUTE);
        if (events == null) {
            events = new ArrayList<>();
            session.setAttribute(SESSION_ATTRIBUTE, events);
        }
        events.add(event);
        while (events.size() > MAX_EVENTS) {
            events.removeFirst();
        }
        return event;
    }

    public void clear() {
        HttpSession session = currentSessionOrNull();
        if (session != null) {
            session.removeAttribute(SESSION_ATTRIBUTE);
            session.removeAttribute(ACTIVE_FLOW_ATTRIBUTE);
        }
    }

    private static HttpSession sessionForWrite() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new IllegalStateException("No HTTP request available for flow trace");
        }
        return attributes.getRequest().getSession(true);
    }

    private static HttpSession currentSessionOrNull() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        return attributes.getRequest().getSession(false);
    }
}
