package com.pingone.oidc.tool.trace;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class FlowTraceMermaidRendererTest {

    @Test
    void rendersEmptyPlaceholder() {
        String diagram = FlowTraceMermaidRenderer.render(List.of());
        assertThat(diagram).contains("sequenceDiagram");
        assertThat(diagram).contains("No flow events yet");
    }

    @Test
    void rendersLoginSequence() {
        List<FlowTraceEvent> events = List.of(
                FlowTraceEvent.of(1, "login", FlowActor.BROWSER, FlowActor.TEST_CLIENT, "Validate config", "Starting", "info"),
                FlowTraceEvent.of(2, "login", FlowActor.TEST_CLIENT, FlowActor.PINGONE, "Authorization redirect", "Redirect", "info"),
                FlowTraceEvent.http(
                        3, "login", FlowActor.BROWSER, FlowActor.PINGONE, "GET", "/mock/pingone/as/authorize", 200, "Authorize page"));

        String diagram = FlowTraceMermaidRenderer.render(events);

        assertThat(diagram).contains("TestClient->>PingOne: Authorization redirect");
        assertThat(diagram).contains("GET /mock/pingone/as/authorize");
    }

    @Test
    void rendersErrorArrowForFailedHttp() {
        FlowTraceEvent failed = FlowTraceEvent.http(
                1, "login", FlowActor.TEST_CLIENT, FlowActor.PINGONE, "POST", "/token", 401, "Unauthorized");

        String diagram = FlowTraceMermaidRenderer.render(List.of(failed));

        assertThat(diagram).contains("TestClient-xPingOne");
    }
}
