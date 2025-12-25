package com.xy.lucky.logging.trace;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;

import java.io.IOException;
import java.util.UUID;

public class TraceIdFilter implements Filter {
    private String traceHeader = "X-Trace-Id";
    private String spanHeader = "X-Span-Id";
    private String mdcTraceKey = "traceId";
    private String mdcSpanKey = "spanId";

    public TraceIdFilter() {
    }

    public TraceIdFilter(String traceHeader, String spanHeader, String mdcTraceKey, String mdcSpanKey) {
        this.traceHeader = traceHeader;
        this.spanHeader = spanHeader;
        this.mdcTraceKey = mdcTraceKey;
        this.mdcSpanKey = mdcSpanKey;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        String th = filterConfig.getInitParameter("traceHeader");
        String sh = filterConfig.getInitParameter("spanHeader");
        String tk = filterConfig.getInitParameter("mdcTraceKey");
        String sk = filterConfig.getInitParameter("mdcSpanKey");
        if (th != null && !th.isBlank()) traceHeader = th;
        if (sh != null && !sh.isBlank()) spanHeader = sh;
        if (tk != null && !tk.isBlank()) mdcTraceKey = tk;
        if (sk != null && !sk.isBlank()) mdcSpanKey = sk;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        String traceId = getOrGenerate(req, traceHeader, mdcTraceKey);
        String spanId = getOrGenerate(req, spanHeader, mdcSpanKey);
        try {
            resp.setHeader(traceHeader, traceId);
            resp.setHeader(spanHeader, spanId);
            chain.doFilter(request, response);
        } finally {
            MDC.remove(mdcTraceKey);
            MDC.remove(mdcSpanKey);
        }
    }

    private String getOrGenerate(HttpServletRequest request, String header, String mdcKey) {
        String id = request.getHeader(header);
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString().replace("-", "");
        }
        MDC.put(mdcKey, id);
        return id;
    }
}
