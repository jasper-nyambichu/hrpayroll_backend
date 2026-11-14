// config/RequestIdFilter.java
package com.company.hrpayroll.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Gives every request an ID, so one user's problem can be found in the logs. The ID is put on every log line
 * written while the request runs (logging.pattern.level in application.yaml), returned in the X-Request-Id
 * response header, and quoted in "unexpected error" messages. A caller may pass its own X-Request-Id (the
 * web app or a proxy) as long as it is short and plain; anything else is replaced, so a header can never be
 * used to forge or break log lines. Runs first, before security, and always clears the ID afterwards so a
 * pooled thread never carries it into the next request.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Request-Id";
    public static final String MDC_KEY = "requestId";
    private static final Pattern SAFE = Pattern.compile("[A-Za-z0-9._-]{8,64}");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String incoming = request.getHeader(HEADER);
        String id = incoming != null && SAFE.matcher(incoming).matches()
                ? incoming
                : UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        MDC.put(MDC_KEY, id);
        response.setHeader(HEADER, id);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    /** The current request's ID, or null outside a request. */
    public static String current() {
        return MDC.get(MDC_KEY);
    }
}
