package pe.sga.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class HttpRequestLoggingFilter extends OncePerRequestFilter {
    private static final Logger LOG = LoggerFactory.getLogger(HttpRequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String provided = request.getHeader("X-Request-ID");
        String requestId = provided != null && provided.matches("[a-fA-F0-9]{32}")
                ? provided.toLowerCase(Locale.ROOT)
                : UUID.randomUUID().toString().replace("-", "");
        Map<String, String> previous = MDC.getCopyOfContextMap();
        long start = System.nanoTime();
        boolean failed = false;
        MDC.put("requestId", requestId);
        response.setHeader("X-Request-ID", requestId);
        try {
            chain.doFilter(request, response);
        } catch (IOException | ServletException | RuntimeException | Error ex) {
            failed = true;
            throw ex;
        } finally {
            try {
                int status = failed ? 500 : response.getStatus();
                long durationMs = (System.nanoTime() - start) / 1_000_000;
                // Exclude query strings, bodies, credentials and authorization headers.
                String path = request.getRequestURI().replace('\r', '_').replace('\n', '_');
                String message = "request_completed method={} endpoint={} status={} duration_ms={}";
                if (status >= 500) {
                    LOG.error(message, request.getMethod(), path, status, durationMs);
                } else if (status >= 400) {
                    LOG.warn(message, request.getMethod(), path, status, durationMs);
                } else {
                    LOG.info(message, request.getMethod(), path, status, durationMs);
                }
            } finally {
                if (previous == null) MDC.clear();
                else MDC.setContextMap(previous);
            }
        }
    }
}
