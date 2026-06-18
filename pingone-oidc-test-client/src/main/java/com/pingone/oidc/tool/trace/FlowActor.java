package com.pingone.oidc.tool.trace;

public enum FlowActor {
    BROWSER("Browser"),
    TEST_CLIENT("Test Client"),
    PINGONE("PingOne");

    private final String displayName;

    FlowActor(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public String mermaidId() {
        return switch (this) {
            case BROWSER -> "Browser";
            case TEST_CLIENT -> "TestClient";
            case PINGONE -> "PingOne";
        };
    }
}
