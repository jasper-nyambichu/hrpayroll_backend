// audit/AuditDetails.java
package com.company.hrpayroll.audit;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Builds the small JSON text stored in audit_logs.details_json.
 *
 * Only descriptive facts go in — the HTTP method, path and caller address, plus a
 * few explicit labels. Request bodies are NEVER copied in: they carry passwords,
 * PINs and bank details that must not end up in a log.
 */
public final class AuditDetails {

    private AuditDetails() {
    }

    /** {"key":"value",...} from alternating key / value pairs; null values are skipped. */
    public static String of(String... pairs) {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            String key = pairs[i];
            String value = pairs[i + 1];
            if (key == null || value == null) {
                continue;
            }
            if (!first) {
                json.append(',');
            }
            json.append('"').append(escape(key)).append("\":\"").append(escape(value)).append('"');
            first = false;
        }
        return json.append('}').toString();
    }

    /** The current HTTP request's method, path and caller address, followed by any extra pairs. */
    public static String withRequest(String... extra) {
        List<String> pairs = new ArrayList<>();

        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            HttpServletRequest request = attributes.getRequest();
            pairs.addAll(List.of(
                    "method", request.getMethod(),
                    "path", request.getRequestURI(),
                    "ip", clientIp(request)
            ));
        }

        pairs.addAll(Arrays.asList(extra));
        return of(pairs.toArray(new String[0]));
    }

    // First hop of X-Forwarded-For when a proxy fronts the app, otherwise the socket address.
    // The header is client-supplied, so it records what the caller reported.
    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static String escape(String text) {
        StringBuilder out = new StringBuilder(text.length() + 8);
        for (char c : text.toCharArray()) {
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.toString();
    }
}