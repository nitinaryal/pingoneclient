package com.pingone.oidc.tool.model;

import java.util.List;

public record ToolOAuthLoginError(
        String phase,
        String errorCode,
        String userMessage,
        String technicalDetail,
        List<String> hints) {

    public static ToolOAuthLoginError of(
            String phase, String errorCode, String userMessage, String technicalDetail, List<String> hints) {
        return new ToolOAuthLoginError(phase, errorCode, userMessage, technicalDetail, List.copyOf(hints));
    }
}
