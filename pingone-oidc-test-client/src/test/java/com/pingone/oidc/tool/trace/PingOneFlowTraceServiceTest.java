package com.pingone.oidc.tool.trace;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class PingOneFlowTraceServiceTest {

    private PingOneFlowTraceStore store;
    private PingOneFlowTraceService service;

    @BeforeEach
    void setUp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(new org.springframework.mock.web.MockHttpSession());
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        store = new PingOneFlowTraceStore();
        service = new PingOneFlowTraceService(store);
    }

    @Test
    void beginFlowRecordsStartEvent() {
        service.beginFlow("login");

        FlowTraceView view = service.currentView();
        assertThat(view.activeFlowName()).isEqualTo("login");
        assertThat(view.events()).hasSize(1);
        assertThat(view.events().getFirst().label()).contains("Flow started");
        assertThat(view.mermaidSequence()).contains("Browser->>TestClient");
    }

    @Test
    void ensureFlowDoesNotDuplicateWhenAlreadyActive() {
        service.beginFlow("login");
        service.ensureFlow("login");
        service.record("login", FlowActor.TEST_CLIENT, FlowActor.PINGONE, "Step", "Message", "info");

        assertThat(service.currentView().events()).hasSize(2);
    }

    @Test
    void clearRemovesEvents() {
        service.beginFlow("logout");
        service.clear();

        FlowTraceView view = service.currentView();
        assertThat(view.events()).isEmpty();
        assertThat(view.mermaidSequence()).contains("No flow events yet");
    }

    @Test
    void recordClientEventUsesActiveFlow() {
        service.beginFlow("diagnostics");
        service.recordClientEvent(new FlowClientEventRequest(null, "Browser action", "Clicked button", "info"));

        FlowTraceView view = service.currentView();
        assertThat(view.events()).hasSize(2);
        assertThat(view.events().get(1).label()).isEqualTo("Browser action");
    }
}
