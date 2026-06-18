package com.pingone.oidc.tool.trace;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.LOWEST_PRECEDENCE - 10)
public class PingOneFlowTraceFilter extends OncePerRequestFilter {

    private final PingOneFlowTraceService flowTraceService;

    public PingOneFlowTraceFilter(PingOneFlowTraceService flowTraceService) {
        this.flowTraceService = flowTraceService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !isTracedPath(path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        filterChain.doFilter(request, response);
        recordRequest(request, response);
    }

    private void recordRequest(HttpServletRequest request, HttpServletResponse response) {
        try {
            String path = request.getRequestURI();
            String method = request.getMethod();
            FlowActor from = resolveFromActor(path, method);
            FlowActor to = resolveToActor(path, method);
            String message = "HTTP " + response.getStatus() + " for " + method + " " + path;
            flowTraceService.recordHttp(from, to, method, path, response.getStatus(), message);
        } catch (Exception ignored) {
            // Flow trace is best-effort and must not break requests.
        }
    }

    private static boolean isTracedPath(String path) {
        return path.startsWith("/oauth2/")
                || path.startsWith("/login/oauth2/")
                || path.equals("/logout")
                || path.startsWith("/mock/pingone/")
                || path.equals("/me")
                || path.equals("/token")
                || path.equals("/jwks")
                || path.equals("/metadata")
                || path.startsWith("/worker/");
    }

    private static FlowActor resolveFromActor(String path, String method) {
        if (path.startsWith("/mock/pingone/")) {
            return "GET".equalsIgnoreCase(method) || path.contains("/authorize")
                    ? FlowActor.BROWSER
                    : FlowActor.TEST_CLIENT;
        }
        if (path.startsWith("/oauth2/") || path.startsWith("/login/oauth2/")) {
            return FlowActor.BROWSER;
        }
        return FlowActor.BROWSER;
    }

    private static FlowActor resolveToActor(String path, String method) {
        if (path.startsWith("/mock/pingone/")) {
            return FlowActor.PINGONE;
        }
        if (path.startsWith("/oauth2/authorization")) {
            return FlowActor.PINGONE;
        }
        if (path.startsWith("/oauth2/") || path.startsWith("/login/oauth2/") || path.equals("/logout")) {
            return FlowActor.TEST_CLIENT;
        }
        return FlowActor.TEST_CLIENT;
    }
}
