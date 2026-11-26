package pe.sga.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unitaria")
class HttpRequestLoggingFilterTest {
    final HttpRequestLoggingFilter filter = new HttpRequestLoggingFilter();
    final Logger logger = (Logger) LoggerFactory.getLogger(HttpRequestLoggingFilter.class);
    ListAppender<ILoggingEvent> appender;
    Level previousLevel;

    @BeforeEach void prepare() {
        MDC.clear();
        previousLevel = logger.getLevel();
        logger.setLevel(Level.INFO);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach void cleanup() {
        logger.detachAppender(appender);
        logger.setLevel(previousLevel);
        appender.stop();
        MDC.clear();
    }

    @Test void preservesProxyIdAndDoesNotLogSecretsOrLeakContext() throws Exception {
        String id = "abcdef0123456789abcdef0123456789";
        var req = new MockHttpServletRequest("GET", "/api/v1/catalogos/puertos");
        req.addHeader("X-Request-ID", id);
        req.addHeader("Authorization", "Bearer PRIVATE_TOKEN");
        req.setQueryString("password=PRIVATE_PASSWORD");
        var res = new MockHttpServletResponse();
        filter.doFilter(req, res, (a,b) -> assertEquals(id, MDC.get("requestId")));
        assertEquals(id, res.getHeader("X-Request-ID"));
        assertNull(MDC.get("requestId"));
        assertEquals(1, appender.list.size());
        var event = appender.list.getFirst();
        assertEquals(Level.INFO, event.getLevel());
        assertTrue(event.getFormattedMessage().contains("status=200"));
        assertFalse(event.getFormattedMessage().contains("PRIVATE_"));
    }

    @Test void replacesUntrustedIdWithNewValidId() throws Exception {
        var req = new MockHttpServletRequest("GET", "/api/v1/health");
        req.addHeader("X-Request-ID", "invalid-request-id");
        var res = new MockHttpServletResponse();
        filter.doFilter(req, res, (a,b) -> {});
        assertTrue(res.getHeader("X-Request-ID").matches("[a-f0-9]{32}"));
        assertNotEquals("invalid-request-id", res.getHeader("X-Request-ID"));
    }

    @Test void logsActualErrorStatusAtErrorLevel() throws Exception {
        var req = new MockHttpServletRequest("GET", "/api/v1/health");
        var res = new MockHttpServletResponse();
        filter.doFilter(req, res, (a,b) -> ((MockHttpServletResponse)b).setStatus(503));
        assertEquals(Level.ERROR, appender.list.getFirst().getLevel());
        assertTrue(appender.list.getFirst().getFormattedMessage().contains("status=503"));
    }

    @Test void preservesExceptionAndRestoresPreviousContext() {
        MDC.put("outer", "existing-context");
        var req = new MockHttpServletRequest("GET", "/api/v1/health");
        var res = new MockHttpServletResponse();
        var failure = new IllegalStateException("controlled unit test");
        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> filter.doFilter(req, res, (a,b) -> {throw failure;})));
        assertEquals("existing-context", MDC.get("outer"));
        assertNull(MDC.get("requestId"));
        assertEquals(Level.ERROR, appender.list.getFirst().getLevel());
    }
}
